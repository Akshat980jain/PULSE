package com.example.data.firebase

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun buildDeterministicThreadId(uidFirst: String, uidSecond: String): String {
    val sorted = listOf(uidFirst.trim(), uidSecond.trim()).sorted()
    return "${sorted[0]}_${sorted[1]}"
}

fun formatFirestoreRelativeTime(timestamp: Timestamp?): String {
    val millis = timestamp?.toDate()?.time ?: return "Now"
    val diffMs = (System.currentTimeMillis() - millis).coerceAtLeast(0L)
    val minutes = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    val days = diffMs / 86_400_000L
    return when {
        minutes < 1L -> "Just now"
        minutes < 60L -> "${minutes}m"
        hours < 24L -> "${hours}h"
        days < 7L -> "${days}d"
        else -> "${days / 7L}w"
    }
}

@Keep
@IgnoreExtraProperties
data class MessagingUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val supabaseUserId: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DmThread(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val participantAId: String = "",
    val participantAName: String = "",
    val participantAUsername: String = "",
    val participantAAvatarUrl: String = "",
    val participantBId: String = "",
    val participantBName: String = "",
    val participantBUsername: String = "",
    val participantBAvatarUrl: String = "",
    val lastMessageText: String = "",
    val lastSenderUid: String = "",
    val unreadCountA: Long = 0L,
    val unreadCountB: Long = 0L,
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun otherUid(currentUid: String): String =
        if (participantAId == currentUid) participantBId else participantAId

    fun otherDisplayName(currentUid: String): String =
        if (participantAId == currentUid) participantBName else participantAName

    fun otherUsername(currentUid: String): String =
        if (participantAId == currentUid) participantBUsername else participantAUsername

    fun otherAvatarUrl(currentUid: String): String =
        if (participantAId == currentUid) participantBAvatarUrl else participantAAvatarUrl

    fun unreadCountFor(currentUid: String): Int =
        if (participantAId == currentUid) unreadCountA.toInt().coerceAtLeast(0)
        else unreadCountB.toInt().coerceAtLeast(0)

    fun toOtherMessagingUser(currentUid: String): MessagingUser {
        val targetUid = otherUid(currentUid)
        return MessagingUser(
            id = targetUid,
            uid = targetUid,
            displayName = otherDisplayName(currentUid).ifBlank { "Pulse Creator" },
            username = otherUsername(currentUid).ifBlank { "creator" },
            avatarUrl = otherAvatarUrl(currentUid)
        )
    }
}

@Keep
@IgnoreExtraProperties
data class DmMessage(
    @DocumentId val id: String = "",
    val threadId: String = "",
    val memberUids: List<String> = emptyList(),
    val senderUid: String = "",
    val senderUsername: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val text: String = "",
    val likedByUids: List<String> = emptyList(),
    val readByUids: List<String> = emptyList(),
    val sharedPostId: String? = null,
    val sharedPostCaption: String? = null,
    val sharedPostImageUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun sentBy(currentUid: String): Boolean = senderUid == currentUid

    fun likedBy(uid: String): Boolean = uid in likedByUids

    fun seenByOther(currentUid: String): Boolean {
        val other = memberUids.firstOrNull { it != currentUid } ?: return false
        return other in readByUids
    }
}

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val parentCommentId: String? = null,
    val replyToUsername: String? = null,
    val likedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun likedBy(uid: String): Boolean = uid in likedByUids

    val likeCount: Int
        get() = likedByUids.size
}

