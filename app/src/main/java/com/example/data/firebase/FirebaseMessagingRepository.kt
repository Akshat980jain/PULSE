package com.example.data.firebase

import android.content.Context
import com.example.data.model.UserProfileState
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseMessagingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
) {
    // CRITICAL: Always resolve the custom named database ID via FirebaseDatabaseModule
    constructor(context: Context) : this(
        db = FirebaseDatabaseModule.provideFirestore(context)
    )

    val crudHelper: FirebaseFirestoreCrudHelper by lazy {
        FirebaseFirestoreCrudHelper(db = db, auth = auth)
    }

    private fun requireUserId(): String {
        val uid = auth?.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firebase Messaging.")
        }
        return uid
    }

    fun observeDirectoryUsers(currentUid: String): Flow<List<MessagingUser>> = flow {
        require(currentUid.isNotBlank()) { "Current UID must not be blank." }
        val path = COLLECTION_MESSAGING_USERS
        emitAll(
            db.collection(COLLECTION_MESSAGING_USERS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MessagingUser::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeThreads(currentUid: String): Flow<List<DmThread>> = flow {
        require(currentUid.isNotBlank()) { "Current UID must not be blank." }
        val path = COLLECTION_DM_THREADS
        emitAll(
            db.collection(COLLECTION_DM_THREADS)
                .whereArrayContains("memberUids", currentUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DmThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending {
                        it.lastMessageAt ?: it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(threadId: String, currentUid: String): Flow<List<DmMessage>> = flow {
        require(threadId.isNotBlank() && currentUid.isNotBlank()) {
            "Thread ID and current UID must not be blank."
        }
        val path = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_MESSAGES"
        emitAll(
            db.collection(COLLECTION_DM_THREADS)
                .document(threadId)
                .collection(SUBCOLLECTION_MESSAGES)
                .whereArrayContains("memberUids", currentUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DmMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun syncCurrentUserProfile(supabaseProfile: UserProfileState): Result<MessagingUser> {
        var docPath = COLLECTION_MESSAGING_USERS
        return try {
            val user = auth?.currentUser
                ?: throw IllegalStateException("User must be signed in with Google before syncing profile.")
            val uid = user.uid
            docPath = "$COLLECTION_MESSAGING_USERS/$uid"

            val email = (user.email ?: supabaseProfile.email).trim().take(160)
            val fallbackHandle = email.substringBefore("@").ifBlank { "pulse_${uid.take(6)}" }
            val resolvedUsername = supabaseProfile.username
                .ifBlank { fallbackHandle }
                .trim()
                .removePrefix("@")
                .replace(" ", "_")
                .lowercase()
                .take(48)
                .ifBlank { "creator" }
            val resolvedDisplayName = supabaseProfile.displayName
                .ifBlank { user.displayName.orEmpty() }
                .ifBlank { resolvedUsername }
                .trim()
                .take(80)
                .ifBlank { "Pulse Creator" }
            val resolvedAvatar = supabaseProfile.avatarUrl
                .orEmpty()
                .ifBlank { user.photoUrl?.toString().orEmpty() }
                .trim()
                .take(1024)
            val resolvedSupabaseId = supabaseProfile.id.trim().take(128)

            val payload = mapOf(
                "uid" to uid,
                "displayName" to resolvedDisplayName,
                "username" to resolvedUsername,
                "email" to email,
                "avatarUrl" to resolvedAvatar,
                "supabaseUserId" to resolvedSupabaseId,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection(COLLECTION_MESSAGING_USERS)
                .document(uid)
                .set(payload)
                .await()

            Result.success(
                MessagingUser(
                    id = uid,
                    uid = uid,
                    displayName = resolvedDisplayName,
                    username = resolvedUsername,
                    email = email,
                    avatarUrl = resolvedAvatar,
                    supabaseUserId = resolvedSupabaseId
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ensureThread(
        currentUser: MessagingUser,
        targetUser: MessagingUser
    ): Result<DmThread> {
        var docPath = COLLECTION_DM_THREADS
        return try {
            val uid = requireUserId()
            require(targetUser.uid.isNotBlank() && targetUser.uid != uid) {
                "Target messaging user must be another valid Firebase user."
            }
            val threadId = buildDeterministicThreadId(uid, targetUser.uid)
            docPath = "$COLLECTION_DM_THREADS/$threadId"
            val docRef = db.collection(COLLECTION_DM_THREADS).document(threadId)

            val existing = docRef.get().await()
            if (existing.exists()) {
                val parsed = existing.toObject(
                    DmThread::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (parsed != null) {
                    return Result.success(parsed)
                }
            }

            val (userA, userB) = if (uid < targetUser.uid) {
                currentUser.copy(uid = uid) to targetUser
            } else {
                targetUser to currentUser.copy(uid = uid)
            }
            val memberUids = listOf(userA.uid, userB.uid)

            val payload = mapOf(
                "memberUids" to memberUids,
                "participantAId" to userA.uid,
                "participantAName" to userA.displayName.ifBlank { "Pulse Creator" }.take(80),
                "participantAUsername" to userA.username.ifBlank { "creator" }.take(48),
                "participantAAvatarUrl" to userA.avatarUrl.take(1024),
                "participantBId" to userB.uid,
                "participantBName" to userB.displayName.ifBlank { "Pulse Creator" }.take(80),
                "participantBUsername" to userB.username.ifBlank { "creator" }.take(48),
                "participantBAvatarUrl" to userB.avatarUrl.take(1024),
                "lastMessageText" to "",
                "lastSenderUid" to "",
                "unreadCountA" to 0L,
                "unreadCountB" to 0L,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            Result.success(
                DmThread(
                    id = threadId,
                    memberUids = memberUids,
                    participantAId = userA.uid,
                    participantAName = userA.displayName.ifBlank { "Pulse Creator" }.take(80),
                    participantAUsername = userA.username.ifBlank { "creator" }.take(48),
                    participantAAvatarUrl = userA.avatarUrl.take(1024),
                    participantBId = userB.uid,
                    participantBName = userB.displayName.ifBlank { "Pulse Creator" }.take(80),
                    participantBUsername = userB.username.ifBlank { "creator" }.take(48),
                    participantBAvatarUrl = userB.avatarUrl.take(1024)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        currentUser: MessagingUser,
        targetUser: MessagingUser,
        text: String,
        sharedPostId: String? = null,
        sharedPostCaption: String? = null,
        sharedPostImageUri: String? = null
    ): Result<Unit> {
        val cleanText = text.trim().take(2000)
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message text cannot be empty."))
        }
        var docPath = COLLECTION_DM_THREADS
        return try {
            val uid = requireUserId()
            val threadResult = ensureThread(currentUser.copy(uid = uid), targetUser)
            val thread = threadResult.getOrElse { return Result.failure(it) }
            val threadId = thread.id.ifBlank { buildDeterministicThreadId(uid, targetUser.uid) }
            val memberUids = listOf(thread.participantAId, thread.participantBId)

            val threadRef = db.collection(COLLECTION_DM_THREADS).document(threadId)
            val msgRef = threadRef.collection(SUBCOLLECTION_MESSAGES).document()
            docPath = msgRef.path

            val rawMessageMap = mutableMapOf<String, Any?>(
                "threadId" to threadId,
                "memberUids" to memberUids,
                "senderUid" to uid,
                "senderUsername" to currentUser.username.ifBlank { "creator" }.take(48),
                "senderDisplayName" to currentUser.displayName.ifBlank { "Pulse Creator" }.take(80),
                "senderAvatarUrl" to currentUser.avatarUrl.take(1024),
                "text" to cleanText,
                "likedByUids" to emptyList<String>(),
                "readByUids" to listOf(uid),
                "sharedPostId" to sharedPostId?.trim()?.take(128)?.takeIf { it.isNotEmpty() },
                "sharedPostCaption" to sharedPostCaption?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
                "sharedPostImageUri" to sharedPostImageUri?.trim()?.take(1024)?.takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val messagePayload = rawMessageMap.filterValues { it != null } as Map<String, Any>

            val previewText = if (!sharedPostId.isNullOrBlank()) {
                "Shared a post · $cleanText".take(500)
            } else {
                cleanText.take(500)
            }

            val isSenderA = thread.participantAId == uid
            val threadUpdates = mutableMapOf<String, Any>(
                "lastMessageText" to previewText,
                "lastSenderUid" to uid,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isSenderA) {
                threadUpdates["participantAName"] = currentUser.displayName.ifBlank { "Pulse Creator" }.take(80)
                threadUpdates["participantAUsername"] = currentUser.username.ifBlank { "creator" }.take(48)
                threadUpdates["participantAAvatarUrl"] = currentUser.avatarUrl.take(1024)
                threadUpdates["unreadCountB"] = FieldValue.increment(1L)
                threadUpdates["unreadCountA"] = 0L
            } else {
                threadUpdates["participantBName"] = currentUser.displayName.ifBlank { "Pulse Creator" }.take(80)
                threadUpdates["participantBUsername"] = currentUser.username.ifBlank { "creator" }.take(48)
                threadUpdates["participantBAvatarUrl"] = currentUser.avatarUrl.take(1024)
                threadUpdates["unreadCountA"] = FieldValue.increment(1L)
                threadUpdates["unreadCountB"] = 0L
            }

            val batch = db.batch()
            batch.set(msgRef, messagePayload)
            batch.update(threadRef, threadUpdates)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markThreadRead(
        thread: DmThread,
        messages: List<DmMessage>
    ): Result<Unit> {
        var docPath = "$COLLECTION_DM_THREADS/${thread.id}"
        return try {
            val uid = requireUserId()
            if (thread.id.isBlank()) return Result.success(Unit)

            val unreadMessages = messages.filter { msg ->
                msg.id.isNotBlank() && msg.senderUid != uid && uid !in msg.readByUids
            }
            val needsThreadReset = thread.unreadCountFor(uid) > 0
            if (unreadMessages.isEmpty() && !needsThreadReset) {
                return Result.success(Unit)
            }

            val threadRef = db.collection(COLLECTION_DM_THREADS).document(thread.id)
            val batch = db.batch()
            if (needsThreadReset) {
                val unreadKey = if (thread.participantAId == uid) "unreadCountA" else "unreadCountB"
                batch.update(
                    threadRef,
                    mapOf(
                        unreadKey to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            unreadMessages.take(40).forEach { msg ->
                val msgRef = threadRef.collection(SUBCOLLECTION_MESSAGES).document(msg.id)
                batch.update(msgRef, mapOf("readByUids" to FieldValue.arrayUnion(uid)))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMessageLike(
        threadId: String,
        message: DmMessage
    ): Result<Unit> {
        val docPath = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_MESSAGES/${message.id}"
        return try {
            val uid = requireUserId()
            require(threadId.isNotBlank() && message.id.isNotBlank()) {
                "Thread ID and message ID must not be blank."
            }
            val alreadyLiked = uid in message.likedByUids
            val updateOp = if (alreadyLiked) {
                FieldValue.arrayRemove(uid)
            } else {
                FieldValue.arrayUnion(uid)
            }
            db.collection(COLLECTION_DM_THREADS)
                .document(threadId)
                .collection(SUBCOLLECTION_MESSAGES)
                .document(message.id)
                .update(mapOf("likedByUids" to updateOp))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeTypingStatuses(threadId: String, currentUid: String): Flow<List<TypingStatus>> = flow {
        require(threadId.isNotBlank() && currentUid.isNotBlank()) {
            "Thread ID and current UID must not be blank."
        }
        val path = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_TYPING"
        emitAll(
            db.collection(COLLECTION_DM_THREADS)
                .document(threadId)
                .collection(SUBCOLLECTION_TYPING)
                .whereArrayContains("memberUids", currentUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TypingStatus::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setTypingStatus(
        threadId: String,
        memberUids: List<String>,
        isTyping: Boolean
    ): Result<Unit> {
        var docPath = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_TYPING"
        return try {
            val uid = requireUserId()
            require(threadId.isNotBlank()) { "Thread ID must not be blank." }
            require(memberUids.size == 2 && uid in memberUids) {
                "Thread memberUids must contain exactly 2 valid participants including current user."
            }
            docPath = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_TYPING/$uid"
            val payload = mapOf(
                "threadId" to threadId,
                "memberUids" to memberUids,
                "userId" to uid,
                "isTyping" to isTyping,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection(COLLECTION_DM_THREADS)
                .document(threadId)
                .collection(SUBCOLLECTION_TYPING)
                .document(uid)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeBlockedUsers(currentUid: String): Flow<List<BlockedUserRecord>> = flow {
        require(currentUid.isNotBlank()) { "Current UID must not be blank." }
        val path = COLLECTION_BLOCKED
        emitAll(
            db.collection(COLLECTION_BLOCKED)
                .whereEqualTo("blockerUid", currentUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BlockedUserRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun blockUser(targetUser: MessagingUser): Result<Unit> {
        var docPath = COLLECTION_BLOCKED
        return try {
            val uid = requireUserId()
            require(targetUser.uid.isNotBlank() && targetUser.uid != uid) {
                "Target user UID must be a valid distinct user."
            }
            val blockId = buildBlockDocumentId(uid, targetUser.uid)
            docPath = "$COLLECTION_BLOCKED/$blockId"
            val payload = mapOf(
                "blockerUid" to uid,
                "blockedUid" to targetUser.uid.trim().take(128),
                "blockedUsername" to targetUser.username.ifBlank { "creator" }.take(48),
                "blockedDisplayName" to targetUser.displayName.ifBlank { "Pulse Creator" }.take(80),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection(COLLECTION_BLOCKED)
                .document(blockId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(blockedUid: String): Result<Unit> {
        var docPath = COLLECTION_BLOCKED
        return try {
            val uid = requireUserId()
            require(blockedUid.isNotBlank() && blockedUid != uid) {
                "Blocked user UID must be valid."
            }
            val blockId = buildBlockDocumentId(uid, blockedUid)
            docPath = "$COLLECTION_BLOCKED/$blockId"
            db.collection(COLLECTION_BLOCKED)
                .document(blockId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_MESSAGING_USERS = "messaging_users"
        const val COLLECTION_DM_THREADS = "dm_threads"
        const val SUBCOLLECTION_MESSAGES = "messages"
        const val SUBCOLLECTION_TYPING = "typing"
        const val COLLECTION_BLOCKED = "blocked"
    }
}
