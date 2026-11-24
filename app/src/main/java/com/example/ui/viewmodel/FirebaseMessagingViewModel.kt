package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.DmMessage
import com.example.data.firebase.DmThread
import com.example.data.firebase.FirebaseMessagingRepository
import com.example.data.firebase.MessagingUser
import com.example.data.model.PostEntity
import com.example.data.model.UserProfileState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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

    val directoryUsersState: StateFlow<MessagingUiState<List<MessagingUser>>> =
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

    val threadsState: StateFlow<MessagingUiState<List<DmThread>>> =
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

    val activeMessagesState: StateFlow<MessagingUiState<List<DmMessage>>> =
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
        _activeTargetUser.value = thread.toOtherMessagingUser(currentUserId)
        _activeThreadId.value = thread.id
    }

    fun closeActiveThread() {
        _activeThreadId.value = null
        _activeTargetUser.value = null
    }

    fun sendMessage(
        text: String,
        sharedPost: PostEntity? = null
    ) {
        val target = _activeTargetUser.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isSending.value) return

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

    fun clearTransientError() {
        _transientError.value = null
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "FirebaseMessagingVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
