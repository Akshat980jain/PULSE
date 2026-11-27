package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.firebase.DmMessage
import com.example.data.firebase.DmThread
import com.example.data.firebase.MessagingUser
import com.example.data.firebase.buildDeterministicThreadId
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.ui.screens.FirebaseMessagesWorkspace
import com.example.ui.screens.LoopsScreen
import com.example.ui.theme.PulseTheme
import com.example.ui.viewmodel.MessagingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class LoopsCommentsAndFirebaseMessagingRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loopsCommentsDrawer_opensInPlace_supportsEmojiLikesAndThreadedReplies() {
        val loopPost = PostEntity(
            id = "loop_video_99",
            authorId = "creator_1",
            authorUsername = "kairo",
            authorDisplayName = "Kairo Vance",
            caption = "Midnight rain loop",
            videoUri = "https://example.com/loop_99.mp4",
            isLoop = true,
            audioTrack = "Analog Rain 94",
            commentCount = 1
        )
        val initialComment = CommentEntity(
            id = "c_parent_1",
            postId = loopPost.id,
            authorUsername = "mira",
            authorDisplayName = "Mira Sato",
            text = "Color grading is unreal!",
            likeCount = 2,
            isLiked = false,
            timeAgo = "2m",
            timestampMs = 1_000L
        )

        var addedReplies = mutableListOf<Triple<String, String?, String?>>()
        var likedCommentIds = mutableListOf<String>()

        composeRule.setContent {
            var comments by remember { mutableStateOf(listOf(initialComment)) }
            PulseTheme {
                LoopsScreen(
                    loopPosts = listOf(loopPost),
                    creators = emptyList(),
                    onLikeToggle = {},
                    onDoubleTapLike = {},
                    onBookmarkToggle = {},
                    onOpenComments = {},
                    onSharePost = {},
                    onCreatorClick = {},
                    onMoreOptions = {},
                    onCreateLoopClick = {},
                    commentsByPostId = mapOf(loopPost.id to comments),
                    onAddComment = { _, text, parentId, replyUser ->
                        addedReplies.add(Triple(text, parentId, replyUser))
                        comments = comments + CommentEntity(
                            id = "c_new_${comments.size}",
                            postId = loopPost.id,
                            authorUsername = "tester",
                            authorDisplayName = "Tester",
                            text = text,
                            parentCommentId = parentId,
                            replyToUsername = replyUser,
                            timeAgo = "Just now",
                            timestampMs = 2_000L + comments.size
                        )
                    },
                    onToggleLikeComment = { target ->
                        likedCommentIds.add(target.id)
                        comments = comments.map {
                            if (it.id == target.id) {
                                it.copy(
                                    isLiked = !it.isLiked,
                                    likeCount = if (it.isLiked) it.likeCount - 1 else it.likeCount + 1
                                )
                            } else it
                        }
                    },
                    isLoopsActive = false
                )
            }
        }

        // Open half-height drawer without leaving Loops pager
        composeRule.onNodeWithTag("loops_comment_button_${loopPost.id}").performClick()
        composeRule.onNodeWithTag("loops_vertical_pager").assertIsDisplayed()
        composeRule.onNodeWithTag("loops_comments_drawer").assertIsDisplayed()
        composeRule.onNodeWithTag("loops_comments_video_dismiss_zone").assertIsDisplayed()

        // Like existing comment
        composeRule.onNodeWithTag("loops_comment_like_c_parent_1").performClick()
        assertEquals(listOf("c_parent_1"), likedCommentIds)

        // Tap Reply -> reply banner appears -> submit threaded reply
        composeRule.onNodeWithTag("loops_comment_reply_c_parent_1").performClick()
        composeRule.onNodeWithTag("loops_comment_reply_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("loops_comment_input").performTextInput("Agreed!")
        composeRule.onNodeWithTag("loops_comment_send_button").performClick()

        assertEquals(1, addedReplies.size)
        assertEquals("@mira Agreed!", addedReplies[0].first)
        assertEquals("c_parent_1", addedReplies[0].second)
        assertEquals("mira", addedReplies[0].third)

        // Tap quick emoji when input is empty -> quick-sends reaction
        composeRule.onNodeWithTag("loops_quick_emoji_0").performClick()
        assertEquals(2, addedReplies.size)
        assertEquals("🔥", addedReplies[1].first)

        // Tap upper live video zone -> dismisses comments drawer smoothly
        composeRule.onNodeWithTag("loops_comments_video_dismiss_zone").performClick()
        composeRule.onNodeWithTag("loops_comments_drawer").assertDoesNotExist()
    }

    @Test
    fun firebaseMessagingModels_threadIdUnreadAndReadReceiptsBehaveDeterministically() {
        assertEquals("alice_uid_bob_uid", buildDeterministicThreadId("bob_uid", "alice_uid"))
        assertEquals("alice_uid_bob_uid", buildDeterministicThreadId("alice_uid", "bob_uid"))

        val thread = DmThread(
            id = "alice_uid_bob_uid",
            memberUids = listOf("alice_uid", "bob_uid"),
            participantAId = "alice_uid",
            participantAName = "Alice",
            participantAUsername = "alice",
            participantBId = "bob_uid",
            participantBName = "Bob",
            participantBUsername = "bob",
            unreadCountA = 3L,
            unreadCountB = 0L
        )
        assertEquals("Bob", thread.otherDisplayName("alice_uid"))
        assertEquals("Alice", thread.otherDisplayName("bob_uid"))
        assertEquals(3, thread.unreadCountFor("alice_uid"))
        assertEquals(0, thread.unreadCountFor("bob_uid"))

        val sentOnly = DmMessage(
            id = "m1",
            threadId = thread.id,
            memberUids = listOf("alice_uid", "bob_uid"),
            senderUid = "alice_uid",
            text = "Ping",
            readByUids = listOf("alice_uid"),
            likedByUids = emptyList()
        )
        assertTrue(sentOnly.sentBy("alice_uid"))
        assertFalse(sentOnly.seenByOther("alice_uid"))

        val seenAndLiked = sentOnly.copy(
            readByUids = listOf("alice_uid", "bob_uid"),
            likedByUids = listOf("bob_uid")
        )
        assertTrue(seenAndLiked.seenByOther("alice_uid"))
        assertTrue(seenAndLiked.likedBy("bob_uid"))
    }

    @Test
    fun firebaseMessagesWorkspace_showsUnreadBadgesPickerReadReceiptsAndLikes() {
        val currentUid = "alice_uid"
        val bob = MessagingUser(
            id = "bob_uid",
            uid = "bob_uid",
            displayName = "Bob Park",
            username = "bobpark"
        )
        val thread = DmThread(
            id = "alice_uid_bob_uid",
            memberUids = listOf("alice_uid", "bob_uid"),
            participantAId = "alice_uid",
            participantAName = "Alice",
            participantAUsername = "alice",
            participantBId = "bob_uid",
            participantBName = "Bob Park",
            participantBUsername = "bobpark",
            lastMessageText = "Ready to shoot?",
            lastSenderUid = "bob_uid",
            unreadCountA = 2L,
            unreadCountB = 0L
        )
        val msgSeen = DmMessage(
            id = "msg_seen",
            threadId = thread.id,
            memberUids = listOf("alice_uid", "bob_uid"),
            senderUid = currentUid,
            senderUsername = "alice",
            senderDisplayName = "Alice",
            text = "On my way!",
            readByUids = listOf("alice_uid", "bob_uid"),
            likedByUids = emptyList()
        )
        val msgSent = DmMessage(
            id = "msg_sent",
            threadId = thread.id,
            memberUids = listOf("alice_uid", "bob_uid"),
            senderUid = currentUid,
            senderUsername = "alice",
            senderDisplayName = "Alice",
            text = "Bring the 35mm lens",
            readByUids = listOf("alice_uid"),
            likedByUids = emptyList()
        )

        val sentTexts = mutableListOf<String>()
        val toggledLikes = mutableListOf<String>()

        composeRule.setContent {
            var activeThreadId by remember { mutableStateOf<String?>(null) }
            var activeTarget by remember { mutableStateOf<MessagingUser?>(null) }
            PulseTheme {
                FirebaseMessagesWorkspace(
                    currentUserId = currentUid,
                    currentUserProfile = MessagingUser(
                        id = currentUid,
                        uid = currentUid,
                        displayName = "Alice",
                        username = "alice"
                    ),
                    directoryState = MessagingUiState.Success(listOf(bob)),
                    threadsState = MessagingUiState.Success(listOf(thread)),
                    activeThreadId = activeThreadId,
                    activeTargetUser = activeTarget,
                    activeMessagesState = MessagingUiState.Success(listOf(msgSeen, msgSent)),
                    isSending = false,
                    transientError = null,
                    onSelectUserForChat = { user ->
                        activeTarget = user
                        activeThreadId = thread.id
                    },
                    onSelectThread = { t ->
                        activeTarget = t.toOtherMessagingUser(currentUid)
                        activeThreadId = t.id
                    },
                    onCloseActiveThread = {
                        activeThreadId = null
                        activeTarget = null
                    },
                    onSendMessage = { sentTexts.add(it) },
                    onToggleLikeMessage = { _, msg -> toggledLikes.add(msg.id) },
                    onDismissError = {},
                    onSignOutMessaging = {}
                )
            }
        }

        // Inbox displays unread badge
        composeRule.onNodeWithTag("firebase_unread_badge_${thread.id}", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("2", useUnmergedTree = true).assertIsDisplayed()

        // Open New Chat Picker sheet, search, and tap Bob
        composeRule.onNodeWithTag("firebase_new_chat_button").performClick()
        composeRule.onNodeWithTag("firebase_new_chat_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_new_chat_search").performTextInput("bob")
        composeRule.onNodeWithTag("firebase_picker_user_bob_uid").performClick()

        // Inside 1-on-1 chat: verify Seen vs Sent receipts, heart like, and send
        composeRule.onNodeWithTag("firebase_conversation_view").assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_receipt_msg_seen").assertTextContains("Seen")
        composeRule.onNodeWithTag("firebase_receipt_msg_sent").assertTextContains("Sent")

        composeRule.onNodeWithTag("firebase_message_like_msg_seen").performClick()
        assertEquals(listOf("msg_seen"), toggledLikes)

        composeRule.onNodeWithTag("firebase_message_input").performTextInput("See you in 5!")
        composeRule.onNodeWithTag("firebase_send_message_button").performClick()
        assertEquals(listOf("See you in 5!"), sentTexts)
    }

    @Test
    fun firebaseDatabaseModule_crudHelperAndAuthHelper_behaveSafelyAndValidateInputs() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()

        // 1. FirebaseDatabaseModule initializes custom named Firestore instance & provides helpers
        com.example.data.firebase.FirebaseDatabaseModule.clearForTesting()
        val db1 = com.example.data.firebase.FirebaseDatabaseModule.initializeFirestore(context)
        val db2 = com.example.data.firebase.FirebaseDatabaseModule.provideFirestore(context)
        assertEquals(db1, db2)
        val crudHelper = com.example.data.firebase.FirebaseDatabaseModule.provideCrudHelper(context)
        val repo = com.example.data.firebase.FirebaseDatabaseModule.provideMessagingRepository(context)
        org.junit.Assert.assertNotNull(crudHelper)
        org.junit.Assert.assertNotNull(repo)

        // 2. PostComment model helpers
        val comment = com.example.data.firebase.PostComment(
            id = "c_1",
            postId = "loop_1",
            authorUid = "alice_uid",
            authorUsername = "alice",
            authorDisplayName = "Alice",
            text = "Great loop!",
            likedByUids = listOf("bob_uid", "carol_uid")
        )
        assertEquals(2, comment.likeCount)
        assertTrue(comment.likedBy("bob_uid"))
        assertFalse(comment.likedBy("dave_uid"))

        // 3. FirebaseFirestoreCrudHelper validates blank inputs and unauthenticated writes safely
        kotlinx.coroutines.runBlocking {
            val emptyCommentRes = crudHelper.createComment(
                postId = "loop_1",
                text = "   ",
                authorUsername = "alice",
                authorDisplayName = "Alice"
            )
            assertTrue(emptyCommentRes.isFailure)

            val unauthCreateMsg = crudHelper.createMessage(
                thread = DmThread(
                    id = "a_b",
                    memberUids = listOf("a", "b"),
                    participantAId = "a",
                    participantBId = "b"
                ),
                currentUser = MessagingUser(uid = "a", username = "a", displayName = "A"),
                text = "Hello"
            )
            assertTrue(unauthCreateMsg.isFailure)

            val unauthUpdateComment = crudHelper.updateCommentText("loop_1", "c_1", "Edited")
            assertTrue(unauthUpdateComment.isFailure)

            val unauthDeleteMsg = crudHelper.deleteMessage("a_b", "m_1")
            assertTrue(unauthDeleteMsg.isFailure)
        }

        // 4. FirebaseAuthHelper safely initializes and tracks sign-in state
        @Suppress("UNCHECKED_CAST")
        val fakeCredentialManager = java.lang.reflect.Proxy.newProxyInstance(
            androidx.credentials.CredentialManager::class.java.classLoader,
            arrayOf(androidx.credentials.CredentialManager::class.java)
        ) { _, method, args ->
            val callback = args?.lastOrNull() as? androidx.credentials.CredentialManagerCallback<Any?, Any?>
            when (method.name) {
                "getCredentialAsync" -> callback?.onError(
                    androidx.credentials.exceptions.NoCredentialException("No saved credential")
                )
                "clearCredentialStateAsync" -> callback?.onResult(null)
            }
            null
        } as androidx.credentials.CredentialManager

        val authHelper = com.example.ui.firebase.FirebaseAuthHelper(
            context = context,
            providedCredentialManager = fakeCredentialManager
        )
        assertFalse(authHelper.isSignedIn)
        assertEquals(null, authHelper.currentUserIdOrNull())
        assertEquals(com.example.ui.firebase.FirebaseAuthSignInState.SignedOut, authHelper.signInState.value)
        kotlinx.coroutines.runBlocking {
            val silentRes = authHelper.attemptSilentSignIn()
            assertTrue(silentRes.isFailure)
            authHelper.signOut()
            assertEquals(com.example.ui.firebase.FirebaseAuthSignInState.SignedOut, authHelper.signInState.value)
        }
    }

    @Test
    fun firebaseMessaging_typingIndicatorAndBlockedUsersFiltering_workEndToEnd() {
        val currentUid = "alice_uid"
        val bobUid = "bob_uid"
        val now = java.util.Date()
        val bob = MessagingUser(
            id = bobUid,
            uid = bobUid,
            displayName = "Bob Park",
            username = "bobpark"
        )
        val msgFromBob = DmMessage(
            id = "m_bob",
            threadId = "alice_uid_bob_uid",
            memberUids = listOf(currentUid, bobUid),
            senderUid = bobUid,
            senderUsername = "bobpark",
            senderDisplayName = "Bob Park",
            text = "Hey there"
        )
        val msgFromCarol = DmMessage(
            id = "m_carol",
            threadId = "alice_uid_carol_uid",
            memberUids = listOf(currentUid, "carol_uid"),
            senderUid = "carol_uid",
            senderUsername = "carol",
            senderDisplayName = "Carol",
            text = "Hi Alice"
        )

        // 1. Message filtering excludes blocked senders/members from appearing in threads
        val filtered = com.example.ui.viewmodel.FirebaseMessagingViewModel.filterVisibleMessages(
            messages = listOf(msgFromBob, msgFromCarol),
            blockedUids = setOf(bobUid)
        )
        assertEquals(listOf("m_carol"), filtered.map { it.id })

        // 2. Typing indicator evaluation respects other participant, blocked set, and staleness
        val activeTyping = com.example.data.firebase.TypingStatus(
            id = bobUid,
            threadId = "alice_uid_bob_uid",
            memberUids = listOf(currentUid, bobUid),
            userId = bobUid,
            isTyping = true,
            updatedAt = com.google.firebase.Timestamp(now)
        )
        assertTrue(
            com.example.ui.viewmodel.FirebaseMessagingViewModel.evaluateOtherParticipantTyping(
                statuses = listOf(activeTyping),
                currentUid = currentUid,
                blockedUids = emptySet(),
                nowMs = now.time + 2_000L
            )
        )
        assertFalse(
            com.example.ui.viewmodel.FirebaseMessagingViewModel.evaluateOtherParticipantTyping(
                statuses = listOf(activeTyping),
                currentUid = currentUid,
                blockedUids = setOf(bobUid),
                nowMs = now.time + 2_000L
            )
        )
        assertFalse(
            com.example.ui.viewmodel.FirebaseMessagingViewModel.evaluateOtherParticipantTyping(
                statuses = listOf(activeTyping),
                currentUid = currentUid,
                blockedUids = emptySet(),
                nowMs = now.time + 20_000L
            )
        )

        // 3. UI shows live typing indicator banner, block dialog, and blocked users sheet
        val blockedLog = mutableListOf<String>()
        val unblockedLog = mutableListOf<String>()
        val draftEvents = mutableListOf<String>()

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            var activeThreadId by remember { mutableStateOf<String?>("alice_uid_bob_uid") }
            var activeTarget by remember { mutableStateOf<MessagingUser?>(bob) }
            var blockedList by remember {
                mutableStateOf(
                    listOf(
                        com.example.data.firebase.BlockedUserRecord(
                            id = "${currentUid}_carol_uid",
                            blockerUid = currentUid,
                            blockedUid = "carol_uid",
                            blockedUsername = "carol",
                            blockedDisplayName = "Carol"
                        )
                    )
                )
            }
            PulseTheme {
                FirebaseMessagesWorkspace(
                    currentUserId = currentUid,
                    currentUserProfile = MessagingUser(
                        id = currentUid,
                        uid = currentUid,
                        displayName = "Alice",
                        username = "alice"
                    ),
                    directoryState = MessagingUiState.Success(listOf(bob)),
                    threadsState = MessagingUiState.Success(emptyList()),
                    activeThreadId = activeThreadId,
                    activeTargetUser = activeTarget,
                    activeMessagesState = MessagingUiState.Success(listOf(msgFromBob)),
                    blockedUsers = blockedList,
                    isOtherParticipantTyping = true,
                    isSending = false,
                    transientError = null,
                    onSelectUserForChat = {},
                    onSelectThread = {},
                    onCloseActiveThread = {
                        activeThreadId = null
                        activeTarget = null
                    },
                    onDraftTextChanged = { draftEvents.add(it) },
                    onClearTyping = {},
                    onSendMessage = {},
                    onToggleLikeMessage = { _, _ -> },
                    onBlockUser = { target ->
                        blockedLog.add(target.uid)
                        activeThreadId = null
                        activeTarget = null
                    },
                    onUnblockUser = { uid ->
                        unblockedLog.add(uid)
                        blockedList = blockedList.filterNot { it.blockedUid == uid }
                    },
                    onDismissError = {},
                    onSignOutMessaging = {}
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(300L)

        // Live typing indicator visible in header & animated banner
        composeRule.onNodeWithTag("firebase_header_typing_status").assertTextContains("Typing…", substring = true)
        composeRule.onNodeWithTag("firebase_typing_indicator").assertIsDisplayed()

        // Typing into composer broadcasts draft changes
        composeRule.onNodeWithTag("firebase_message_input").performTextInput("On my way")
        composeRule.mainClock.advanceTimeBy(100L)
        assertTrue(draftEvents.isNotEmpty())

        // Blocking Bob opens confirm dialog and returns to inbox
        composeRule.onNodeWithTag("firebase_block_user_button").performClick()
        composeRule.mainClock.advanceTimeBy(100L)
        composeRule.onNodeWithTag("firebase_block_confirm_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_confirm_block_button").performClick()
        composeRule.mainClock.autoAdvance = true
        assertEquals(listOf(bobUid), blockedLog)

        // Blocked users management sheet in inbox allows unblocking
        composeRule.onNodeWithTag("firebase_blocked_users_button").performClick()
        composeRule.onNodeWithTag("firebase_blocked_users_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_unblock_user_carol_uid").performClick()
        assertEquals(listOf("carol_uid"), unblockedLog)
    }
}
