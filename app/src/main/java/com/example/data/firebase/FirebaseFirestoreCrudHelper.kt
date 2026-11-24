package com.example.data.firebase

import android.content.Context
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

/**
 * Helper class for performing authenticated Cloud Firestore CRUD (Create, Read, Update, Delete)
 * operations on both Message ([DmMessage]) and Comment ([PostComment]) data structures.
 *
 * All mutations enforce fail-fast user authentication via [requireUserId], validate input
 * against `firestore.rules` constraints, use [FieldValue.serverTimestamp], and route errors
 * through [handleFirestoreError] while returning [Result].
 */
class FirebaseFirestoreCrudHelper(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
) {
    constructor(context: Context) : this(
        db = FirebaseDatabaseModule.provideFirestore(context)
    )

    private fun requireUserId(): String {
        val uid = auth?.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    // ============================================================================================
    // MESSAGE CRUD OPERATIONS (/dm_threads/{threadId}/messages/{messageId})
    // ============================================================================================

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
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getMessage(threadId: String, messageId: String): Result<DmMessage?> {
        val path = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_MESSAGES/$messageId"
        return try {
            requireUserId()
            require(threadId.isNotBlank() && messageId.isNotBlank()) {
                "Thread ID and message ID must not be blank."
            }
            val snapshot = db.collection(COLLECTION_DM_THREADS)
                .document(threadId)
                .collection(SUBCOLLECTION_MESSAGES)
                .document(messageId)
                .get()
                .await()
            val message = if (snapshot.exists()) {
                snapshot.toObject(
                    DmMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createMessage(
        thread: DmThread,
        currentUser: MessagingUser,
        text: String,
        sharedPostId: String? = null,
        sharedPostCaption: String? = null,
        sharedPostImageUri: String? = null
    ): Result<DmMessage> {
        val cleanText = text.trim().take(2000)
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message text cannot be empty."))
        }
        var path = "$COLLECTION_DM_THREADS/${thread.id}/$SUBCOLLECTION_MESSAGES"
        return try {
            val uid = requireUserId()
            val threadId = thread.id.ifBlank {
                buildDeterministicThreadId(thread.participantAId, thread.participantBId)
            }
            require(threadId.isNotBlank()) { "Thread ID must not be blank." }
            val memberUids = listOf(thread.participantAId, thread.participantBId)
            require(uid in memberUids) { "Current user must be a participant of this thread." }

            val threadRef = db.collection(COLLECTION_DM_THREADS).document(threadId)
            val msgRef = threadRef.collection(SUBCOLLECTION_MESSAGES).document()
            path = msgRef.path

            val cleanUsername = currentUser.username.ifBlank { "creator" }.take(48)
            val cleanDisplayName = currentUser.displayName.ifBlank { "Pulse Creator" }.take(80)
            val cleanAvatar = currentUser.avatarUrl.take(1024)
            val cleanSharedId = sharedPostId?.trim()?.take(128)?.takeIf { it.isNotEmpty() }
            val cleanSharedCaption = sharedPostCaption?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
            val cleanSharedImage = sharedPostImageUri?.trim()?.take(1024)?.takeIf { it.isNotEmpty() }

            val rawMessageMap = mutableMapOf<String, Any?>(
                "threadId" to threadId,
                "memberUids" to memberUids,
                "senderUid" to uid,
                "senderUsername" to cleanUsername,
                "senderDisplayName" to cleanDisplayName,
                "senderAvatarUrl" to cleanAvatar,
                "text" to cleanText,
                "likedByUids" to emptyList<String>(),
                "readByUids" to listOf(uid),
                "sharedPostId" to cleanSharedId,
                "sharedPostCaption" to cleanSharedCaption,
                "sharedPostImageUri" to cleanSharedImage,
                "createdAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val messagePayload = rawMessageMap.filterValues { it != null } as Map<String, Any>

            val previewText = if (cleanSharedId != null) {
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
                threadUpdates["participantAName"] = cleanDisplayName
                threadUpdates["participantAUsername"] = cleanUsername
                threadUpdates["participantAAvatarUrl"] = cleanAvatar
                threadUpdates["unreadCountB"] = FieldValue.increment(1L)
                threadUpdates["unreadCountA"] = 0L
            } else {
                threadUpdates["participantBName"] = cleanDisplayName
                threadUpdates["participantBUsername"] = cleanUsername
                threadUpdates["participantBAvatarUrl"] = cleanAvatar
                threadUpdates["unreadCountA"] = FieldValue.increment(1L)
                threadUpdates["unreadCountB"] = 0L
            }

            val batch = db.batch()
            batch.set(msgRef, messagePayload)
            batch.update(threadRef, threadUpdates)
            batch.commit().await()

            Result.success(
                DmMessage(
                    id = msgRef.id,
                    threadId = threadId,
                    memberUids = memberUids,
                    senderUid = uid,
                    senderUsername = cleanUsername,
                    senderDisplayName = cleanDisplayName,
                    senderAvatarUrl = cleanAvatar,
                    text = cleanText,
                    likedByUids = emptyList(),
                    readByUids = listOf(uid),
                    sharedPostId = cleanSharedId,
                    sharedPostCaption = cleanSharedCaption,
                    sharedPostImageUri = cleanSharedImage
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMessageText(
        threadId: String,
        messageId: String,
        newText: String
    ): Result<Unit> {
        val cleanText = newText.trim().take(2000)
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Updated message text cannot be empty."))
        }
        val path = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_MESSAGES/$messageId"
        return try {
            requireUserId()
            require(threadId.isNotBlank() && messageId.isNotBlank()) {
                "Thread ID and message ID must not be blank."
            }
            db.collection(COLLECTION_DM_THREADS)
                .document(threadId)
                .collection(SUBCOLLECTION_MESSAGES)
                .document(messageId)
                .update(mapOf("text" to cleanText))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMessageLike(
        threadId: String,
        message: DmMessage
    ): Result<Unit> {
        val path = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_MESSAGES/${message.id}"
        return try {
            val uid = requireUserId()
            require(threadId.isNotBlank() && message.id.isNotBlank()) {
                "Thread ID and message ID must not be blank."
            }
            val updateOp = if (uid in message.likedByUids) {
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
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markMessageRead(threadId: String, messageId: String): Result<Unit> {
        val path = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_MESSAGES/$messageId"
        return try {
            val uid = requireUserId()
            require(threadId.isNotBlank() && messageId.isNotBlank()) {
                "Thread ID and message ID must not be blank."
            }
            db.collection(COLLECTION_DM_THREADS)
                .document(threadId)
                .collection(SUBCOLLECTION_MESSAGES)
                .document(messageId)
                .update(mapOf("readByUids" to FieldValue.arrayUnion(uid)))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit> {
        val path = "$COLLECTION_DM_THREADS/$threadId/$SUBCOLLECTION_MESSAGES/$messageId"
        return try {
            requireUserId()
            require(threadId.isNotBlank() && messageId.isNotBlank()) {
                "Thread ID and message ID must not be blank."
            }
            db.collection(COLLECTION_DM_THREADS)
                .document(threadId)
                .collection(SUBCOLLECTION_MESSAGES)
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ============================================================================================
    // COMMENT CRUD OPERATIONS (/posts/{postId}/comments/{commentId})
    // ============================================================================================

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        require(postId.isNotBlank()) { "Post ID must not be blank." }
        val activeUid = requireUserId()
        val path = "$COLLECTION_POSTS/$postId/$SUBCOLLECTION_COMMENTS"
        emitAll(
            db.collection(COLLECTION_POSTS)
                .document(postId)
                .collection(SUBCOLLECTION_COMMENTS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PostComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == activeUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getComment(postId: String, commentId: String): Result<PostComment?> {
        val path = "$COLLECTION_POSTS/$postId/$SUBCOLLECTION_COMMENTS/$commentId"
        return try {
            requireUserId()
            require(postId.isNotBlank() && commentId.isNotBlank()) {
                "Post ID and comment ID must not be blank."
            }
            val snapshot = db.collection(COLLECTION_POSTS)
                .document(postId)
                .collection(SUBCOLLECTION_COMMENTS)
                .document(commentId)
                .get()
                .await()
            val comment = if (snapshot.exists()) {
                snapshot.toObject(
                    PostComment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(comment)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createComment(
        postId: String,
        text: String,
        authorUsername: String,
        authorDisplayName: String,
        authorAvatarUrl: String = "",
        parentCommentId: String? = null,
        replyToUsername: String? = null
    ): Result<PostComment> {
        val cleanPostId = postId.trim()
        val cleanText = text.trim().take(1000)
        if (cleanPostId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Post ID cannot be empty."))
        }
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Comment text cannot be empty."))
        }
        var path = "$COLLECTION_POSTS/$cleanPostId/$SUBCOLLECTION_COMMENTS"
        return try {
            val uid = requireUserId()
            val commentRef = db.collection(COLLECTION_POSTS)
                .document(cleanPostId)
                .collection(SUBCOLLECTION_COMMENTS)
                .document()
            path = commentRef.path

            val cleanUsername = authorUsername.trim().removePrefix("@").ifBlank { "creator" }.take(48)
            val cleanDisplayName = authorDisplayName.trim().ifBlank { cleanUsername }.take(80)
            val cleanAvatar = authorAvatarUrl.trim().take(1024)
            val cleanParentId = parentCommentId?.trim()?.take(128)?.takeIf { it.isNotEmpty() }
            val cleanReplyUser = replyToUsername?.trim()?.removePrefix("@")?.take(48)?.takeIf { it.isNotEmpty() }

            val rawPayload = mutableMapOf<String, Any?>(
                "postId" to cleanPostId,
                "authorUid" to uid,
                "authorUsername" to cleanUsername,
                "authorDisplayName" to cleanDisplayName,
                "authorAvatarUrl" to cleanAvatar,
                "text" to cleanText,
                "parentCommentId" to cleanParentId,
                "replyToUsername" to cleanReplyUser,
                "likedByUids" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val payload = rawPayload.filterValues { it != null } as Map<String, Any>

            commentRef.set(payload).await()

            Result.success(
                PostComment(
                    id = commentRef.id,
                    postId = cleanPostId,
                    authorUid = uid,
                    authorUsername = cleanUsername,
                    authorDisplayName = cleanDisplayName,
                    authorAvatarUrl = cleanAvatar,
                    text = cleanText,
                    parentCommentId = cleanParentId,
                    replyToUsername = cleanReplyUser,
                    likedByUids = emptyList()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCommentText(
        postId: String,
        commentId: String,
        newText: String
    ): Result<Unit> {
        val cleanText = newText.trim().take(1000)
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Comment text cannot be empty."))
        }
        val path = "$COLLECTION_POSTS/$postId/$SUBCOLLECTION_COMMENTS/$commentId"
        return try {
            requireUserId()
            require(postId.isNotBlank() && commentId.isNotBlank()) {
                "Post ID and comment ID must not be blank."
            }
            db.collection(COLLECTION_POSTS)
                .document(postId)
                .collection(SUBCOLLECTION_COMMENTS)
                .document(commentId)
                .update(
                    mapOf(
                        "text" to cleanText,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleCommentLike(
        postId: String,
        comment: PostComment
    ): Result<Unit> {
        val path = "$COLLECTION_POSTS/$postId/$SUBCOLLECTION_COMMENTS/${comment.id}"
        return try {
            val uid = requireUserId()
            require(postId.isNotBlank() && comment.id.isNotBlank()) {
                "Post ID and comment ID must not be blank."
            }
            val updateOp = if (uid in comment.likedByUids) {
                FieldValue.arrayRemove(uid)
            } else {
                FieldValue.arrayUnion(uid)
            }
            db.collection(COLLECTION_POSTS)
                .document(postId)
                .collection(SUBCOLLECTION_COMMENTS)
                .document(comment.id)
                .update(
                    mapOf(
                        "likedByUids" to updateOp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        val path = "$COLLECTION_POSTS/$postId/$SUBCOLLECTION_COMMENTS/$commentId"
        return try {
            requireUserId()
            require(postId.isNotBlank() && commentId.isNotBlank()) {
                "Post ID and comment ID must not be blank."
            }
            db.collection(COLLECTION_POSTS)
                .document(postId)
                .collection(SUBCOLLECTION_COMMENTS)
                .document(commentId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    companion object {
        const val COLLECTION_DM_THREADS = "dm_threads"
        const val SUBCOLLECTION_MESSAGES = "messages"
        const val COLLECTION_POSTS = "posts"
        const val SUBCOLLECTION_COMMENTS = "comments"
    }
}
