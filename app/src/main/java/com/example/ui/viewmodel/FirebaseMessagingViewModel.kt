package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.BlockedUserRecord
import com.example.data.firebase.DmMessage
import com.example.data.firebase.DmThread
import com.example.data.firebase.FirebaseMessagingRepository
import com.example.data.firebase.MessagingUser
import com.example.data.firebase.TypingStatus
import com.example.data.model.PostEntity
import com.example.data.model.UserProfileState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MessagingUiState<out T> {
    data object Loading : MessagingUiState<Nothing>
    data class Success<T>(val data: T) : MessagingUiState<T>
    data class Error(val message: String) : MessagingUiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseMessagingViewModel(
    private val repository: FirebaseMessagingRepository,
    val currentUserId: String
) : ViewModel() {

    private val _currentMessagingUser = MutableStateFlow(
        MessagingUser(
            id = currentUserId,
            uid = currentUserId,
            displayName = "Pulse Creator",
            username = "creator"
        )
    )
    val currentMessagingUser: StateFlow<MessagingUser> = _currentMessagingUser.asStateFlow()

    private val _activeThreadId = MutableStateFlow<String?>(null)
    val activeThreadId: StateFlow<String?> = _activeThreadId.asStateFlow()

    private val _activeTargetUser = MutableStateFlow<MessagingUser?>(null)
    val activeTargetUser: StateFlow<MessagingUser?> = _activeTargetUser.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _transientError = MutableStateFlow<String?>(null)
    val transientError: StateFlow<String?> = _transientError.asStateFlow()

    private var typingTimeoutJob: Job? = null
    private var lastBroadcastTypingThreadId: String? = null
    private var lastBroadcastIsTyping: Boolean = false

    val blockedUsersState: StateFlow<List<BlockedUserRecord>> =
        repository.observeBlockedUsers(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing blocked users", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    private val blockedUidSet: StateFlow<Set<String>> = blockedUsersState
        .map { list -> list.mapNotNull { it.blockedUid.takeIf(String::isNotBlank) }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptySet()
        )

    private val rawDirectoryUsersState: StateFlow<MessagingUiState<List<MessagingUser>>> =
        repository.observeDirectoryUsers(currentUserId)
            .map<List<MessagingUser>, MessagingUiState<List<MessagingUser>>> { users ->
                MessagingUiState.Success(users.filter { it.uid != currentUserId && it.uid.isNotBlank() })
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing messaging directory", rootError)
                emit(MessagingUiState.Error(rootError.localizedMessage ?: "Failed to load people directory"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = MessagingUiState.Loading
            )

    val directoryUsersState: StateFlow<MessagingUiState<List<MessagingUser>>> =
        combine(rawDirectoryUsersState, blockedUidSet) { state, blocked ->
            when (state) {
                is MessagingUiState.Success -> MessagingUiState.Success(
                    state.data.filter { it.uid !in blocked }
                )
                else -> state
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MessagingUiState.Loading
        )

    private val rawThreadsState: StateFlow<MessagingUiState<List<DmThread>>> =
        repository.observeThreads(currentUserId)
            .map<List<DmThread>, MessagingUiState<List<DmThread>>> { MessagingUiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing DM threads", rootError)
                emit(MessagingUiState.Error(rootError.localizedMessage ?: "Failed to load conversations"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = MessagingUiState.Loading
            )

    val threadsState: StateFlow<MessagingUiState<List<DmThread>>> =
        combine(rawThreadsState, blockedUidSet) { state, blocked ->
            when (state) {
                is MessagingUiState.Success -> MessagingUiState.Success(
                    state.data.filter { thread -> thread.otherUid(currentUserId) !in blocked }
                )
                else -> state
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MessagingUiState.Loading
        )

    val totalUnreadCount: StateFlow<Int> = threadsState
        .map { state ->
            when (state) {
                is MessagingUiState.Success -> state.data.sumOf { it.unreadCountFor(currentUserId) }
                else -> 0
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = 0
        )

    private val rawActiveMessagesState: StateFlow<MessagingUiState<List<DmMessage>>> =
        _activeThreadId.flatMapLatest { threadId ->
            if (threadId.isNullOrBlank()) {
                flowOf(MessagingUiState.Success(emptyList()))
            } else {
                repository.observeMessages(threadId, currentUserId)
                    .map<List<DmMessage>, MessagingUiState<List<DmMessage>>> { MessagingUiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing messages for $threadId", rootError)
                        emit(MessagingUiState.Error(rootError.localizedMessage ?: "Failed to load messages"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MessagingUiState.Loading
        )

    val activeMessagesState: StateFlow<MessagingUiState<List<DmMessage>>> =
        combine(rawActiveMessagesState, blockedUidSet, _activeTargetUser) { state, blocked, target ->
            if (target != null && target.uid in blocked) {
                MessagingUiState.Success(emptyList())
            } else {
                when (state) {
                    is MessagingUiState.Success -> MessagingUiState.Success(
                        filterVisibleMessages(state.data, blocked)
                    )
                    else -> state
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MessagingUiState.Loading
        )

    val isOtherParticipantTyping: StateFlow<Boolean> =
        _activeThreadId.flatMapLatest { threadId ->
            if (threadId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeTypingStatuses(threadId, currentUserId)
                    .catch { error ->
                        val firestoreError = unwrapFirestoreException(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Error observing typing indicators for $threadId", firestoreError ?: error)
                        emit(emptyList())
                    }
            }
        }.combine(blockedUidSet) { statuses, blocked ->
            evaluateOtherParticipantTyping(statuses, currentUserId, blocked)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = false
        )

    fun syncProfile(supabaseProfile: UserProfileState) {
        viewModelScope.launch {
            repository.syncCurrentUserProfile(supabaseProfile)
                .onSuccess { synced -> _currentMessagingUser.value = synced }
                .onFailure { err ->
                    _transientError.value = err.localizedMessage ?: "Unable to sync messaging profile."
                }
        }
    }

    fun openThreadWithUser(targetUser: MessagingUser) {
        if (targetUser.uid.isBlank() || targetUser.uid == currentUserId) return
        if (targetUser.uid in blockedUidSet.value) {
            _transientError.value = "Unblock @${targetUser.username} first to start messaging."
            return
        }
        _activeTargetUser.value = targetUser
        viewModelScope.launch {
            repository.ensureThread(_currentMessagingUser.value, targetUser)
                .onSuccess { thread ->
                    _activeThreadId.value = thread.id
                }
                .onFailure { err ->
                    _transientError.value = err.localizedMessage ?: "Unable to open conversation."
                }
        }
    }

    fun openExistingThread(thread: DmThread) {
        if (thread.id.isBlank()) return
        val other = thread.toOtherMessagingUser(currentUserId)
        if (other.uid in blockedUidSet.value) return
        _activeTargetUser.value = other
        _activeThreadId.value = thread.id
    }

    fun closeActiveThread() {
        clearTypingStatus()
        _activeThreadId.value = null
        _activeTargetUser.value = null
    }

    fun onComposerTextChanged(text: String) {
        val threadId = _activeThreadId.value ?: return
        val target = _activeTargetUser.value ?: return
        if (target.uid in blockedUidSet.value) return

        val hasText = text.isNotBlank()
        typingTimeoutJob?.cancel()
        if (!hasText) {
            broadcastTyping(threadId, target.uid, isTyping = false)
            return
        }

        broadcastTyping(threadId, target.uid, isTyping = true)
        typingTimeoutJob = viewModelScope.launch {
            delay(TYPING_IDLE_TIMEOUT_MS)
            broadcastTyping(threadId, target.uid, isTyping = false)
        }
    }

    fun clearTypingStatus() {
        typingTimeoutJob?.cancel()
        typingTimeoutJob = null
        val threadId = _activeThreadId.value ?: lastBroadcastTypingThreadId ?: return
        val targetUid = _activeTargetUser.value?.uid
            ?: threadId.split("_").firstOrNull { it != currentUserId }
            ?: return
        broadcastTyping(threadId, targetUid, isTyping = false)
    }

    private fun broadcastTyping(threadId: String, targetUid: String, isTyping: Boolean) {
        if (lastBroadcastTypingThreadId == threadId && lastBroadcastIsTyping == isTyping) return
        lastBroadcastTypingThreadId = threadId
        lastBroadcastIsTyping = isTyping
        val memberUids = listOf(currentUserId, targetUid).sorted()
        viewModelScope.launch {
            repository.setTypingStatus(
                threadId = threadId,
                memberUids = memberUids,
                isTyping = isTyping
            ).onFailure { err ->
                Log.w(TAG, "Typing status update failed: ${err.message}")
            }
        }
    }

    fun sendMessage(
        text: String,
        sharedPost: PostEntity? = null
    ) {
        val target = _activeTargetUser.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isSending.value) return

        clearTypingStatus()
        viewModelScope.launch {
            _isSending.value = true
            _transientError.value = null
            repository.sendMessage(
                currentUser = _currentMessagingUser.value,
                targetUser = target,
                text = trimmed,
                sharedPostId = sharedPost?.id,
                sharedPostCaption = sharedPost?.caption,
                sharedPostImageUri = sharedPost?.imageUri
            ).onFailure { err ->
                _transientError.value = err.localizedMessage ?: "Failed to send message."
            }
            _isSending.value = false
        }
    }

    fun markThreadRead(thread: DmThread, messages: List<DmMessage>) {
        viewModelScope.launch {
            repository.markThreadRead(thread, messages)
                .onFailure { err ->
                    Log.w(TAG, "Failed to update read receipts: ${err.message}")
                }
        }
    }

    fun toggleLikeMessage(threadId: String, message: DmMessage) {
        viewModelScope.launch {
            repository.toggleMessageLike(threadId, message)
                .onFailure { err ->
                    _transientError.value = err.localizedMessage ?: "Could not update message like."
                }
        }
    }

    fun blockUser(targetUser: MessagingUser) {
        if (targetUser.uid.isBlank() || targetUser.uid == currentUserId) return
        clearTypingStatus()
        viewModelScope.launch {
            repository.blockUser(targetUser)
                .onSuccess {
                    if (_activeTargetUser.value?.uid == targetUser.uid) {
                        _activeThreadId.value = null
                        _activeTargetUser.value = null
                    }
                }
                .onFailure { err ->
                    _transientError.value = err.localizedMessage ?: "Could not block user."
                }
        }
    }

    fun unblockUser(blockedUid: String) {
        if (blockedUid.isBlank()) return
        viewModelScope.launch {
            repository.unblockUser(blockedUid)
                .onFailure { err ->
                    _transientError.value = err.localizedMessage ?: "Could not unblock user."
                }
        }
    }

    fun clearTransientError() {
        _transientError.value = null
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    companion object {
        private const val TAG = "FirebaseMessagingVM"
        private const val STOP_TIMEOUT_MS = 5000L
        private const val TYPING_IDLE_TIMEOUT_MS = 3500L
        private const val TYPING_STALE_WINDOW_MS = 12_000L

        fun filterVisibleMessages(
            messages: List<DmMessage>,
            blockedUids: Set<String>
        ): List<DmMessage> = messages.filter { msg ->
            msg.senderUid !in blockedUids && msg.memberUids.none { it in blockedUids }
        }

        fun evaluateOtherParticipantTyping(
            statuses: List<TypingStatus>,
            currentUid: String,
            blockedUids: Set<String>,
            nowMs: Long = System.currentTimeMillis()
        ): Boolean = statuses.any { status ->
            status.userId.isNotBlank() &&
                status.userId != currentUid &&
                status.userId !in blockedUids &&
                status.isTyping &&
                (status.updatedAt == null || (nowMs - status.updatedAt.toDate().time) <= TYPING_STALE_WINDOW_MS)
        }
    }
}
