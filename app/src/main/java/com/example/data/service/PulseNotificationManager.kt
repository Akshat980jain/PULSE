package com.example.data.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ActivityItemEntity
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local notification manager that alerts users to new likes, comments, and incoming direct messages
 * using Android's [NotificationManager] and [NotificationCompat].
 */
class PulseNotificationManager(context: Context) {

    data class LocalNotificationRecord(
        val notificationId: Int,
        val channelId: String,
        val type: String,
        val title: String,
        val body: String,
        val targetId: String,
        val timestampMs: Long = System.currentTimeMillis()
    )

    private val appContext = context.applicationContext
    private val systemNotificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val prefs = appContext.getSharedPreferences("pulse_local_notifications_prefs", Context.MODE_PRIVATE)

    private val _dispatchedNotifications = MutableStateFlow<List<LocalNotificationRecord>>(emptyList())
    val dispatchedNotifications: StateFlow<List<LocalNotificationRecord>> = _dispatchedNotifications.asStateFlow()

    init {
        ensureNotificationChannelsCreated()
    }

    fun ensureNotificationChannelsCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = systemNotificationManager ?: return
            val likesChannel = NotificationChannel(
                CHANNEL_LIKES,
                "Pulse Likes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when someone likes your photos or loops"
            }
            val commentsChannel = NotificationChannel(
                CHANNEL_COMMENTS,
                "Pulse Comments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when someone comments on or replies to your posts"
            }
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Pulse Direct Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you receive a new direct message"
            }
            nm.createNotificationChannel(likesChannel)
            nm.createNotificationChannel(commentsChannel)
            nm.createNotificationChannel(messagesChannel)
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun clearHistory() {
        _dispatchedNotifications.value = emptyList()
        prefs.edit().remove(KEY_SEEN_NOTIFICATION_KEYS).apply()
        systemNotificationManager?.cancelAll()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Posts a local system notification alerting the user to a new Like on a post.
     */
    fun notifyNewLike(
        actorUsername: String,
        postCaption: String,
        postId: String,
        dedupeKey: String? = null
    ): LocalNotificationRecord? {
        val cleanHandle = actorUsername.trim().removePrefix("@").ifBlank { "creator" }
        val snippet = postCaption.trim().take(56).let {
            if (it.isNotEmpty()) "\"$it\"" else "your visual post"
        }
        return dispatchNotification(
            channelId = CHANNEL_LIKES,
            type = TYPE_LIKE,
            title = "New Like from @$cleanHandle",
            body = "@$cleanHandle liked $snippet",
            targetId = postId,
            dedupeKey = dedupeKey
        )
    }

    /**
     * Posts a local system notification alerting the user to a new Comment or Reply on a post.
     */
    fun notifyNewComment(
        actorUsername: String,
        commentText: String,
        postId: String,
        dedupeKey: String? = null
    ): LocalNotificationRecord? {
        val cleanHandle = actorUsername.trim().removePrefix("@").ifBlank { "creator" }
        val cleanComment = commentText.trim().take(90).ifBlank { "Left a comment on your post" }
        return dispatchNotification(
            channelId = CHANNEL_COMMENTS,
            type = TYPE_COMMENT,
            title = "New Comment from @$cleanHandle",
            body = "@$cleanHandle commented: \"$cleanComment\"",
            targetId = postId,
            dedupeKey = dedupeKey
        )
    }

    /**
     * Posts a local system notification alerting the user to an incoming Direct Message.
     */
    fun notifyIncomingMessage(
        senderUsername: String,
        senderDisplayName: String = "",
        messageText: String,
        threadId: String,
        dedupeKey: String? = null
    ): LocalNotificationRecord? {
        val cleanHandle = senderUsername.trim().removePrefix("@").ifBlank { "creator" }
        val displayHeader = senderDisplayName.trim().takeIf { it.isNotBlank() }
            ?.let { "$it (@$cleanHandle)" }
            ?: "@$cleanHandle"
        val cleanMessage = messageText.trim().take(100).ifBlank { "Sent you a message" }
        return dispatchNotification(
            channelId = CHANNEL_MESSAGES,
            type = TYPE_MESSAGE,
            title = "Message from $displayHeader",
            body = cleanMessage,
            targetId = threadId,
            dedupeKey = dedupeKey
        )
    }

    /**
     * Routes an [ActivityItemEntity] (LIKE, COMMENT, MESSAGE) to the corresponding notification channel.
     */
    fun notifyActivityItem(activity: ActivityItemEntity): LocalNotificationRecord? {
        val normalizedType = activity.type.uppercase()
        val key = "activity_${activity.id}"
        return when {
            normalizedType.contains("COMMENT") || normalizedType.contains("REPLY") -> {
                notifyNewComment(
                    actorUsername = activity.actorUsername,
                    commentText = activity.message,
                    postId = activity.targetPostId.orEmpty(),
                    dedupeKey = key
                )
            }
            normalizedType.contains("MESSAGE") || normalizedType.contains("DM") -> {
                notifyIncomingMessage(
                    senderUsername = activity.actorUsername,
                    senderDisplayName = activity.actorDisplayName,
                    messageText = activity.message,
                    threadId = activity.targetPostId.orEmpty().ifBlank { "thread_${activity.actorId}" },
                    dedupeKey = key
                )
            }
            else -> {
                notifyNewLike(
                    actorUsername = activity.actorUsername,
                    postCaption = activity.message,
                    postId = activity.targetPostId.orEmpty(),
                    dedupeKey = key
                )
            }
        }
    }

    private fun dispatchNotification(
        channelId: String,
        type: String,
        title: String,
        body: String,
        targetId: String,
        dedupeKey: String?
    ): LocalNotificationRecord? {
        if (!areNotificationsEnabled()) return null

        if (!dedupeKey.isNullOrBlank()) {
            val seenSet = prefs.getStringSet(KEY_SEEN_NOTIFICATION_KEYS, emptySet()).orEmpty()
            if (dedupeKey in seenSet) return null
            val updatedSeen = (seenSet + dedupeKey).toList().takeLast(250).toSet()
            prefs.edit().putStringSet(KEY_SEEN_NOTIFICATION_KEYS, updatedSeen).apply()
        }

        ensureNotificationChannelsCreated()
        val notificationId = nextNotificationId.incrementAndGet()
        val record = LocalNotificationRecord(
            notificationId = notificationId,
            channelId = channelId,
            type = type,
            title = title,
            body = body,
            targetId = targetId
        )
        _dispatchedNotifications.value = listOf(record) + _dispatchedNotifications.value.take(49)

        try {
            val launchIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NOTIFICATION_TYPE, type)
                putExtra(EXTRA_NOTIFICATION_TARGET_ID, targetId)
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                notificationId,
                launchIntent,
                pendingFlags
            )

            val category = when (type) {
                TYPE_MESSAGE -> NotificationCompat.CATEGORY_MESSAGE
                else -> NotificationCompat.CATEGORY_SOCIAL
            }
            val priority = when (type) {
                TYPE_LIKE -> NotificationCompat.PRIORITY_DEFAULT
                else -> NotificationCompat.PRIORITY_HIGH
            }

            val notification = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(category)
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            systemNotificationManager?.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Runtime permission POST_NOTIFICATIONS not yet granted by user on API 33+
        } catch (_: Throwable) {
        }

        return record
    }

    companion object {
        const val CHANNEL_LIKES = "pulse_likes_channel"
        const val CHANNEL_COMMENTS = "pulse_comments_channel"
        const val CHANNEL_MESSAGES = "pulse_messages_channel"

        const val TYPE_LIKE = "LIKE"
        const val TYPE_COMMENT = "COMMENT"
        const val TYPE_MESSAGE = "MESSAGE"

        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
        const val EXTRA_NOTIFICATION_TARGET_ID = "extra_notification_target_id"

        private const val KEY_NOTIFICATIONS_ENABLED = "local_notifications_enabled"
        private const val KEY_SEEN_NOTIFICATION_KEYS = "seen_notification_keys"

        private val nextNotificationId = AtomicInteger(2000)

        @Volatile
        private var instance: PulseNotificationManager? = null

        fun getInstance(context: Context): PulseNotificationManager {
            return instance ?: synchronized(this) {
                instance ?: PulseNotificationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
