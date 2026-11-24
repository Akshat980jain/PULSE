package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.firebase.DmMessage
import com.example.data.firebase.DmThread
import com.example.data.firebase.FirebaseDatabaseModule
import com.example.data.firebase.MessagingUser
import com.example.data.firebase.formatFirestoreRelativeTime
import com.example.data.model.FilmPreset
import com.example.data.model.UserProfileState
import com.example.ui.components.PulseAvatarImage
import com.example.ui.components.PulseFilteredImage
import com.example.ui.components.rememberPulseHaptics
import com.example.ui.firebase.FirebaseAuthHelper
import com.example.ui.firebase.FirebaseAuthSignInState
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.CobaltPulseSoft
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson
import com.example.ui.viewmodel.FirebaseMessagingViewModel
import com.example.ui.viewmodel.MessagingUiState

@Composable
fun FirebaseMessagesTabScreen(
    supabaseProfile: UserProfileState,
    initialTargetSupabaseId: String? = null,
    initialTargetUsername: String? = null,
    onUnreadCountChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authHelper = remember(context) { FirebaseAuthHelper(context) }
    val currentUser by remember(authHelper) {
        authHelper.authStateFlow()
    }.collectAsStateWithLifecycle(initialValue = authHelper.currentUser)

    val signedInUser = currentUser
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("messages_screen")
    ) {
        if (signedInUser == null) {
            LaunchedEffect(Unit) {
                onUnreadCountChanged(0)
            }
            FirebaseMessagingAuthGate(
                supabaseProfile = supabaseProfile,
                authHelper = authHelper,
                modifier = Modifier.fillMaxSize()
            )
        } else {
        val uid = signedInUser.uid
        key(uid) {
            val messagingViewModel: FirebaseMessagingViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        FirebaseMessagingViewModel(
                            repository = FirebaseDatabaseModule.provideMessagingRepository(app),
                            currentUserId = uid
                        )
                    }
                }
            )

            val scope = rememberCoroutineScope()

            val currentMessagingUser by messagingViewModel.currentMessagingUser.collectAsStateWithLifecycle()
            val directoryState by messagingViewModel.directoryUsersState.collectAsStateWithLifecycle()
            val threadsState by messagingViewModel.threadsState.collectAsStateWithLifecycle()
            val activeThreadId by messagingViewModel.activeThreadId.collectAsStateWithLifecycle()
            val activeTargetUser by messagingViewModel.activeTargetUser.collectAsStateWithLifecycle()
            val activeMessagesState by messagingViewModel.activeMessagesState.collectAsStateWithLifecycle()
            val totalUnreadCount by messagingViewModel.totalUnreadCount.collectAsStateWithLifecycle()
            val isSending by messagingViewModel.isSending.collectAsStateWithLifecycle()
            val transientError by messagingViewModel.transientError.collectAsStateWithLifecycle()

            LaunchedEffect(
                uid,
                supabaseProfile.id,
                supabaseProfile.username,
                supabaseProfile.displayName,
                supabaseProfile.avatarUrl
            ) {
                messagingViewModel.syncProfile(supabaseProfile)
            }

            LaunchedEffect(totalUnreadCount) {
                onUnreadCountChanged(totalUnreadCount)
            }

            // Auto-open chat if navigated from a creator profile that is linked to Firebase Messaging
            LaunchedEffect(initialTargetSupabaseId, initialTargetUsername, directoryState) {
                if (!initialTargetSupabaseId.isNullOrBlank() || !initialTargetUsername.isNullOrBlank()) {
                    val users = (directoryState as? MessagingUiState.Success)?.data.orEmpty()
                    val matched = users.firstOrNull { u ->
                        (!initialTargetSupabaseId.isNullOrBlank() && u.supabaseUserId.equals(initialTargetSupabaseId, ignoreCase = true)) ||
                            (!initialTargetUsername.isNullOrBlank() && u.username.equals(initialTargetUsername, ignoreCase = true))
                    }
                    if (matched != null && activeThreadId == null) {
                        messagingViewModel.openThreadWithUser(matched)
                    }
                }
            }

            val threads = (threadsState as? MessagingUiState.Success)?.data.orEmpty()
            val activeThread = remember(activeThreadId, threads) {
                threads.firstOrNull { it.id == activeThreadId }
            }
            val activeMessages = (activeMessagesState as? MessagingUiState.Success)?.data.orEmpty()

            // Automatically mark incoming messages read while thread is open
            LaunchedEffect(activeThread?.id, activeMessages.size, activeThread?.unreadCountFor(uid)) {
                val thread = activeThread ?: return@LaunchedEffect
                val hasUnreadMsgs = activeMessages.any { it.senderUid != uid && uid !in it.readByUids }
                if (thread.unreadCountFor(uid) > 0 || hasUnreadMsgs) {
                    messagingViewModel.markThreadRead(thread, activeMessages)
                }
            }

            FirebaseMessagesWorkspace(
                currentUserId = uid,
                currentUserProfile = currentMessagingUser,
                directoryState = directoryState,
                threadsState = threadsState,
                activeThreadId = activeThreadId,
                activeTargetUser = activeTargetUser,
                activeMessagesState = activeMessagesState,
                isSending = isSending,
                transientError = transientError,
                onSelectUserForChat = { messagingViewModel.openThreadWithUser(it) },
                onSelectThread = { messagingViewModel.openExistingThread(it) },
                onCloseActiveThread = { messagingViewModel.closeActiveThread() },
                onSendMessage = { text -> messagingViewModel.sendMessage(text) },
                onToggleLikeMessage = { threadId, msg -> messagingViewModel.toggleLikeMessage(threadId, msg) },
                onDismissError = { messagingViewModel.clearTransientError() },
                onSignOutMessaging = {
                    messagingViewModel.closeActiveThread()
                    authHelper.signOut(
                        scope = scope,
                        onSignOutComplete = { onUnreadCountChanged(0) }
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    }
}

@Composable
fun FirebaseMessagingAuthGate(
    supabaseProfile: UserProfileState,
    authHelper: FirebaseAuthHelper? = null,
    onSignedIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolvedAuthHelper = remember(context, authHelper) {
        authHelper ?: FirebaseAuthHelper(context)
    }
    val signInState by resolvedAuthHelper.signInState.collectAsStateWithLifecycle()

    val isSigningIn = signInState is FirebaseAuthSignInState.SigningIn
    val authError = (signInState as? FirebaseAuthSignInState.Error)?.message

    // Attempt silent Google Sign-In once for returning users
    LaunchedEffect(resolvedAuthHelper) {
        resolvedAuthHelper.attemptSilentSignIn(
            scope = scope,
            onAuthSuccess = { onSignedIn() },
            onUnauthenticated = { /* Stay on interactive card */ }
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("firebase_messaging_auth_gate")
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    CobaltPulse.copy(alpha = 0.28f),
                                    ShutterAmber.copy(alpha = 0.24f)
                                )
                            )
                        )
                        .border(1.5.dp, CobaltPulseSoft.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MarkChatUnread,
                        contentDescription = "Real-time Firebase messaging",
                        tint = CobaltPulseSoft,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = "Real-Time Studio Messages",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Connect Google to unlock instant 1-on-1 chats, read receipts, unread badges, and message likes on Firebase. Your Supabase feed, Loops, and profile stay untouched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (supabaseProfile.username.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = ShutterAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Syncs as @${supabaseProfile.username}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = !authError.isNullOrBlank()) {
                    Surface(
                        color = SignalCrimson.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SignalCrimson.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        resolvedAuthHelper.clearError()
                        resolvedAuthHelper.signInWithGoogle(
                            activityContext = context,
                            scope = scope,
                            onAuthSuccess = { onSignedIn() }
                        )
                    },
                    enabled = !isSigningIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CobaltPulse,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("firebase_google_sign_in_button")
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connecting Google…",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseMessagesWorkspace(
    currentUserId: String,
    currentUserProfile: MessagingUser,
    directoryState: MessagingUiState<List<MessagingUser>>,
    threadsState: MessagingUiState<List<DmThread>>,
    activeThreadId: String?,
    activeTargetUser: MessagingUser?,
    activeMessagesState: MessagingUiState<List<DmMessage>>,
    isSending: Boolean,
    transientError: String?,
    onSelectUserForChat: (MessagingUser) -> Unit,
    onSelectThread: (DmThread) -> Unit,
    onCloseActiveThread: () -> Unit,
    onSendMessage: (String) -> Unit,
    onToggleLikeMessage: (threadId: String, message: DmMessage) -> Unit,
    onDismissError: () -> Unit,
    onSignOutMessaging: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewChatSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = activeThreadId != null) {
        onCloseActiveThread()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("firebase_messages_workspace")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = !transientError.isNullOrBlank()) {
                Surface(
                    color = SignalCrimson.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, SignalCrimson.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = transientError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissError,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (activeThreadId == null || activeTargetUser == null) {
                FirebaseInboxView(
                    currentUserId = currentUserId,
                    currentUserProfile = currentUserProfile,
                    directoryState = directoryState,
                    threadsState = threadsState,
                    onOpenNewChatPicker = { showNewChatSheet = true },
                    onSelectUserForChat = onSelectUserForChat,
                    onSelectThread = onSelectThread,
                    onSignOutMessaging = onSignOutMessaging,
                    modifier = Modifier.weight(1f)
                )
            } else {
                FirebaseConversationView(
                    currentUserId = currentUserId,
                    threadId = activeThreadId,
                    targetUser = activeTargetUser,
                    messagesState = activeMessagesState,
                    isSending = isSending,
                    onBack = onCloseActiveThread,
                    onSendMessage = onSendMessage,
                    onToggleLikeMessage = { msg -> onToggleLikeMessage(activeThreadId, msg) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showNewChatSheet) {
        val users = (directoryState as? MessagingUiState.Success)?.data.orEmpty()
        FirebaseNewChatPickerSheet(
            users = users,
            isLoading = directoryState is MessagingUiState.Loading,
            errorMessage = (directoryState as? MessagingUiState.Error)?.message,
            onDismiss = { showNewChatSheet = false },
            onSelectUser = { user ->
                showNewChatSheet = false
                onSelectUserForChat(user)
            }
        )
    }
}

@Composable
private fun FirebaseInboxView(
    currentUserId: String,
    currentUserProfile: MessagingUser,
    directoryState: MessagingUiState<List<MessagingUser>>,
    threadsState: MessagingUiState<List<DmThread>>,
    onOpenNewChatPicker: () -> Unit,
    onSelectUserForChat: (MessagingUser) -> Unit,
    onSelectThread: (DmThread) -> Unit,
    onSignOutMessaging: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Firebase real-time · @${currentUserProfile.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onOpenNewChatPicker,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("firebase_new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddComment,
                    contentDescription = "New chat",
                    tint = CobaltPulseSoft
                )
            }

            IconButton(
                onClick = onSignOutMessaging,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("firebase_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Disconnect Firebase Messaging",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Connected people quick-start strip
        val directoryUsers = (directoryState as? MessagingUiState.Success)?.data.orEmpty()
        if (directoryUsers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "People on Firebase Messaging",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_directory_strip")
                ) {
                    items(directoryUsers, key = { it.uid }) { user ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(70.dp)
                                .clickable { onSelectUserForChat(user) }
                                .testTag("firebase_directory_user_${user.uid}")
                        ) {
                            PulseAvatarImage(
                                avatarRes = R.drawable.img_post_portrait_1794021773202,
                                avatarUrl = user.avatarUrl.takeIf { it.isNotBlank() },
                                contentDescription = user.displayName,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, CobaltPulse, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        }

        when (threadsState) {
            is MessagingUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = CobaltPulse)
                }
            }

            is MessagingUiState.Error -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Could not load conversations",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = threadsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is MessagingUiState.Success -> {
                val threads = threadsState.data
                if (threads.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp)
                            .testTag("firebase_empty_inbox")
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = CobaltPulseSoft,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "No conversations yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Start a live 1-on-1 Firebase chat with any connected creator.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = onOpenNewChatPicker,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CobaltPulse,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("firebase_empty_inbox_cta")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddComment,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Start a New Chat",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("firebase_threads_list")
                    ) {
                        items(threads, key = { it.id }) { thread ->
                            val unread = thread.unreadCountFor(currentUserId)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (unread > 0) CobaltPulse.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectThread(thread) }
                                    .testTag("firebase_thread_row_${thread.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    PulseAvatarImage(
                                        avatarRes = R.drawable.img_post_portrait_1794021773202,
                                        avatarUrl = thread.otherAvatarUrl(currentUserId).takeIf { it.isNotBlank() },
                                        contentDescription = thread.otherDisplayName(currentUserId),
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = thread.otherDisplayName(currentUserId),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "@${thread.otherUsername(currentUserId)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = thread.lastMessageText.ifBlank { "Tap to start chatting" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (unread > 0) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatFirestoreRelativeTime(thread.lastMessageAt ?: thread.updatedAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (unread > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(CobaltPulse)
                                                    .testTag("firebase_unread_badge_${thread.id}")
                                            ) {
                                                Text(
                                                    text = if (unread > 99) "99+" else unread.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirebaseConversationView(
    currentUserId: String,
    threadId: String,
    targetUser: MessagingUser,
    messagesState: MessagingUiState<List<DmMessage>>,
    isSending: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onToggleLikeMessage: (DmMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    var draftText by remember(threadId) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val haptics = rememberPulseHaptics()

    val messages = (messagesState as? MessagingUiState.Success)?.data.orEmpty()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("firebase_conversation_view")
    ) {
        // Conversation Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("firebase_chat_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to messages",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            PulseAvatarImage(
                avatarRes = R.drawable.img_post_portrait_1794021773202,
                avatarUrl = targetUser.avatarUrl.takeIf { it.isNotBlank() },
                contentDescription = targetUser.displayName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = targetUser.displayName.ifBlank { "Pulse Creator" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${targetUser.username} · Live Firebase chat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Messages Stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (messagesState) {
                is MessagingUiState.Loading -> {
                    CircularProgressIndicator(
                        color = CobaltPulse,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is MessagingUiState.Error -> {
                    Text(
                        text = messagesState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SignalCrimson,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                is MessagingUiState.Success -> {
                    if (messages.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(28.dp)
                        ) {
                            Text(
                                text = "Say hello to @${targetUser.username}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Messages sync instantaneously with read receipts & heart likes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("firebase_messages_list")
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                val isMine = msg.sentBy(currentUserId)
                                val isLikedByMe = msg.likedBy(currentUserId)
                                val seenByOther = isMine && msg.seenByOther(currentUserId)

                                Column(
                                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("firebase_message_item_${msg.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!isMine) {
                                            PulseAvatarImage(
                                                avatarRes = R.drawable.img_post_portrait_1794021773202,
                                                avatarUrl = msg.senderAvatarUrl.takeIf { it.isNotBlank() },
                                                contentDescription = msg.senderDisplayName,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                            )
                                        }

                                        Surface(
                                            color = if (isMine) CobaltPulse else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(
                                                topStart = 18.dp,
                                                topEnd = 18.dp,
                                                bottomStart = if (isMine) 18.dp else 4.dp,
                                                bottomEnd = if (isMine) 4.dp else 18.dp
                                            ),
                                            border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.pointerInput(msg.id, msg.likedByUids) {
                                                detectTapGestures(
                                                    onDoubleTap = {
                                                        haptics.performLikeHaptic()
                                                        onToggleLikeMessage(msg)
                                                    }
                                                )
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                                if (!msg.sharedPostImageUri.isNullOrBlank()) {
                                                    PulseFilteredImage(
                                                        imageRes = 0,
                                                        imageUri = msg.sharedPostImageUri,
                                                        filterPreset = FilmPreset.NATURAL,
                                                        contentDescription = "Shared studio post",
                                                        showFilmBadge = false,
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.68f)
                                                            .height(120.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                }
                                                Text(
                                                    text = msg.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                haptics.performLikeHaptic()
                                                onToggleLikeMessage(msg)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("firebase_message_like_${msg.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                contentDescription = if (isLikedByMe) "Unlike message" else "Like message",
                                                tint = if (isLikedByMe) SignalCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Timestamp, heart reaction pill, and Sent / Seen read receipt
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(
                                            start = if (isMine) 0.dp else 36.dp,
                                            top = 3.dp
                                        )
                                    ) {
                                        if (msg.likedByUids.isNotEmpty()) {
                                            Surface(
                                                color = SignalCrimson.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(999.dp),
                                                border = BorderStroke(1.dp, SignalCrimson.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Favorite,
                                                        contentDescription = "Liked",
                                                        tint = SignalCrimson,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = msg.likedByUids.size.toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = formatFirestoreRelativeTime(msg.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (isMine) {
                                            Icon(
                                                imageVector = if (seenByOther) Icons.Filled.DoneAll else Icons.Filled.Check,
                                                contentDescription = if (seenByOther) "Seen" else "Sent",
                                                tint = if (seenByOther) CobaltPulseSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (seenByOther) "Seen" else "Sent",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (seenByOther) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (seenByOther) CobaltPulseSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.testTag("firebase_receipt_${msg.id}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Sticky Message Composer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = draftText,
                onValueChange = { if (it.length <= 2000) draftText = it },
                placeholder = { Text("Message @${targetUser.username}…") },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CobaltPulse,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("firebase_message_input")
            )

            Surface(
                color = if (draftText.isNotBlank() && !isSending) CobaltPulse else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = draftText.isNotBlank() && !isSending) {
                        val toSend = draftText.trim()
                        if (toSend.isNotEmpty()) {
                            haptics.performSendHaptic()
                            onSendMessage(toSend)
                            draftText = ""
                        }
                    }
                    .testTag("firebase_send_message_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (draftText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirebaseNewChatPickerSheet(
    users: List<MessagingUser>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSelectUser: (MessagingUser) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        val q = searchQuery.trim().lowercase().removePrefix("@")
        if (q.isEmpty()) users
        else users.filter { u ->
            u.displayName.lowercase().contains(q) ||
                u.username.lowercase().contains(q) ||
                u.email.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("firebase_new_chat_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "New Firebase Chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose any connected creator to start a 1-on-1 thread",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close new chat picker",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name or @handle") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firebase_new_chat_search")
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        CircularProgressIndicator(color = CobaltPulse)
                    }
                }

                !errorMessage.isNullOrBlank() -> {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SignalCrimson,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }

                filteredUsers.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Text(
                            text = if (users.isEmpty()) "No other users connected yet" else "No matching people found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Once another Pulse user connects Google in Messages, they appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onDismiss) {
                            Text("Done")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(filteredUsers, key = { it.uid }) { user ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUser(user) }
                                    .testTag("firebase_picker_user_${user.uid}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    PulseAvatarImage(
                                        avatarRes = R.drawable.img_post_portrait_1794021773202,
                                        avatarUrl = user.avatarUrl.takeIf { it.isNotBlank() },
                                        contentDescription = user.displayName,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Chat",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CobaltPulseSoft
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
