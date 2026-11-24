package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileState
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.ShutterAmber
import kotlinx.coroutines.delay

/**
 * Formats an ephemeral 24-hour remaining time label for a [StoryEntity].
 */
fun formatEphemeralRemainingLabel(story: StoryEntity, nowMs: Long = System.currentTimeMillis()): String {
    if (story.isHighlight) return "Highlight"
    val ttlMs = 24L * 60L * 60L * 1000L
    val elapsedMs = (nowMs - story.timestampMs).coerceAtLeast(0L)
    if (story.timestampMs <= 0L || elapsedMs >= ttlMs) {
        return story.createdAtLabel.ifBlank { "24h" }
    }
    val remainingHours = ((ttlMs - elapsedMs) / (60L * 60L * 1000L)).toInt()
    return when {
        remainingHours >= 1 -> "${remainingHours}h left"
        else -> "<1h left"
    }
}

/**
 * Horizontal scrolling tray of circular avatars at the top of the feed.
 * Tapping an avatar opens the full-screen [EphemeralStoryViewer] for ephemeral image/video stories.
 */
@Composable
fun StoriesAvatarTray(
    userProfile: UserProfileState,
    stories: List<StoryEntity>,
    isRefreshing: Boolean,
    onOpenStoryAtIndex: (Int) -> Unit,
    onCreateStoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPulseHaptics()
    val myStoryIndex = remember(stories, userProfile.id, userProfile.username) {
        stories.indexOfFirst { story ->
            (userProfile.id.isNotBlank() && story.authorId.equals(userProfile.id, ignoreCase = true)) ||
                (userProfile.username.isNotBlank() && story.authorUsername.equals(userProfile.username, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stories_avatar_tray")
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stories_row")
        ) {
            // Leading "Your story" circular avatar with quick-add or view action
            item(key = "your_story_button") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .testTag("story_your_story_item")
                ) {
                    StoryAvatarRing(
                        avatarRes = userProfile.avatarRes,
                        avatarUrl = userProfile.avatarUrl,
                        contentDescription = "Your story",
                        isUnviewed = myStoryIndex >= 0,
                        showAddBadge = true,
                        onClick = {
                            haptics.performControlTapHaptic()
                            if (myStoryIndex >= 0) {
                                onOpenStoryAtIndex(myStoryIndex)
                            } else {
                                onCreateStoryClick()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your story",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (myStoryIndex >= 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Ephemeral image/video story circular avatars
            itemsIndexed(stories, key = { _, s -> s.id }) { index, story ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .testTag("story_item_${story.id}")
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        StoryAvatarRing(
                            avatarRes = story.authorAvatarRes,
                            avatarUrl = story.authorAvatarUrl,
                            contentDescription = story.authorDisplayName.ifBlank { story.authorUsername },
                            isUnviewed = !story.isViewed,
                            showAddBadge = false,
                            onClick = {
                                haptics.performControlTapHaptic()
                                onOpenStoryAtIndex(index)
                            }
                        )
                        if (story.hasVideo) {
                            Surface(
                                color = Color(0xFF0E131D),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.5.dp, ShutterAmber, CircleShape)
                                    .testTag("story_video_badge_${story.id}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = "Video story",
                                        tint = ShutterAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = story.authorUsername.substringBefore(".").ifBlank { story.authorDisplayName },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (!story.isViewed) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (!story.isViewed) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isRefreshing && stories.isEmpty()) {
            StoriesRowSkeletonLoader()
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Full-screen edge-to-edge viewer for ephemeral image and video stories.
 * Features segmented auto-advancing progress bars, tap left/right navigation,
 * press-and-hold pause, swipe-down dismiss, video audio mute toggle,
 * 24-hour ephemeral countdown pill, and interactive quick reactions/replies.
 */
@Composable
fun EphemeralStoryViewer(
    stories: List<StoryEntity>,
    initialIndex: Int,
    onStorySeen: (StoryEntity) -> Unit,
    onSendReply: (StoryEntity, String) -> Unit,
    onAddNewStory: () -> Unit,
    onDismiss: () -> Unit,
    onCreatorClick: ((String) -> Unit)? = null,
    onToggleHighlight: ((StoryEntity) -> Unit)? = null,
    imageDurationMs: Long = 5000L,
    videoDurationMs: Long = 10_000L
) {
    if (stories.isEmpty()) return

    var currentIndex by remember {
        mutableIntStateOf(initialIndex.coerceIn(0, stories.lastIndex))
    }
    var isPaused by remember { mutableStateOf(false) }
    var isVideoMuted by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    val storyHaptics = rememberPulseHaptics()

    val story = stories.getOrNull(currentIndex) ?: return
    val isAutoAdvancePaused = isPaused || replyText.isNotEmpty()
    val effectiveDurationMs = if (story.hasVideo) videoDurationMs else imageDurationMs
    val ephemeralLabel = remember(story.id, story.timestampMs, story.isHighlight, story.createdAtLabel) {
        formatEphemeralRemainingLabel(story)
    }

    LaunchedEffect(currentIndex, stories.size, effectiveDurationMs) {
        progress = 0f
        onStorySeen(story)
        val tickIntervalMs = 32L
        val stepIncrement = (tickIntervalMs.toFloat() / effectiveDurationMs.coerceAtLeast(500L).toFloat())
        while (progress < 1f) {
            delay(tickIntervalMs)
            if (!isPaused && replyText.isEmpty()) {
                progress = (progress + stepIncrement).coerceAtMost(1f)
            }
        }
        if (currentIndex < stories.lastIndex) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 40, easing = LinearEasing),
        label = "ephemeral_story_progress"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070A10))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .testTag("story_viewer_dialog")
        ) {
            // 1. Ephemeral Image or Video Media Layer
            PulseFilteredImage(
                imageRes = story.imageRes,
                imageUri = story.imageUri,
                videoUri = story.effectiveVideoUri,
                filterPreset = story.filterPreset,
                contentDescription = story.captionOverlay.ifBlank { "Story by @${story.authorUsername}" },
                showFilmBadge = !story.hasVideo,
                autoPlayVideo = !isAutoAdvancePaused,
                loopsControlMode = false,
                externalIsMuted = isVideoMuted,
                onExternalToggleMute = { isVideoMuted = !isVideoMuted },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("story_media_stage")
            )

            // 2. Transparent Gesture Overlay above media (handles tap-left, tap-right, hold-to-pause, swipe-down)
            // Works consistently across both Image and AndroidView Video players.
            var accumulatedDragY by remember(currentIndex) { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex, stories.size) {
                        detectTapGestures(
                            onPress = {
                                val wasPaused = isPaused
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = wasPaused
                            },
                            onTap = { offset ->
                                if (offset.x < size.width * 0.35f) {
                                    storyHaptics.performFilterTickHaptic()
                                    if (currentIndex > 0) {
                                        currentIndex--
                                    } else {
                                        progress = 0f
                                    }
                                } else {
                                    storyHaptics.performFilterTickHaptic()
                                    if (currentIndex < stories.lastIndex) {
                                        currentIndex++
                                    } else {
                                        onDismiss()
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(currentIndex) {
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedDragY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount > 0f) {
                                    accumulatedDragY += dragAmount
                                }
                            },
                            onDragEnd = {
                                if (accumulatedDragY > 140f) {
                                    onDismiss()
                                }
                                accumulatedDragY = 0f
                            }
                        )
                    }
                    .testTag("story_gesture_overlay")
            )

            // 3. Top Header: Auto-Advancing Progress Bars + Author Info + Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.84f),
                                Color.Black.copy(alpha = 0.44f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                StoryAutoProgressHeaderBar(
                    storyCount = stories.size,
                    currentIndex = currentIndex,
                    activeSegmentProgress = animatedProgress,
                    onSelectSegment = { targetIdx ->
                        currentIndex = targetIdx.coerceIn(0, stories.lastIndex)
                        progress = 0f
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("story_author_header")
                    ) {
                        val openAuthorModifier = if (onCreatorClick != null && story.authorId.isNotBlank()) {
                            Modifier.clickable {
                                onCreatorClick(story.authorId)
                                onDismiss()
                            }
                        } else {
                            Modifier
                        }
                        PulseAvatarImage(
                            avatarRes = story.authorAvatarRes,
                            avatarUrl = story.authorAvatarUrl,
                            contentDescription = story.authorDisplayName.ifBlank { story.authorUsername },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, ShutterAmber, CircleShape)
                                .then(openAuthorModifier)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = story.authorUsername,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .then(openAuthorModifier)
                                )
                                Text(
                                    text = "${currentIndex + 1}/${stories.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShutterAmber,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("story_position_counter")
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("story_ephemeral_ttl_badge")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (story.hasVideo) Icons.Filled.Videocam else Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = ShutterAmber,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (story.hasVideo) "VIDEO · $ephemeralLabel" else ephemeralLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (story.locationTag.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = ShutterAmber,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = story.locationTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isAutoAdvancePaused) {
                        Surface(
                            color = ShutterAmber.copy(alpha = 0.24f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("story_paused_badge")
                        ) {
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ShutterAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (story.hasVideo) {
                        IconButton(
                            onClick = {
                                storyHaptics.performLoopMuteToggleHaptic()
                                isVideoMuted = !isVideoMuted
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("story_mute_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isVideoMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isVideoMuted) "Unmute video story" else "Mute video story",
                                tint = if (isVideoMuted) ShutterAmber else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("story_pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (isAutoAdvancePaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isAutoAdvancePaused) "Resume story" else "Pause story",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onToggleHighlight != null) {
                        Surface(
                            color = if (story.isHighlight) ShutterAmber.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .clickable {
                                    storyHaptics.performBookmarkHaptic()
                                    onToggleHighlight(story)
                                }
                                .testTag("story_toggle_highlight_button")
                        ) {
                            Text(
                                text = if (story.isHighlight) "Pinned" else "Pin",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (story.isHighlight) Color(0xFF0B0E14) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Surface(
                        color = CobaltPulse.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable(onClick = onAddNewStory)
                            .testTag("story_add_new_button")
                    ) {
                        Text(
                            text = "+ Add",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("story_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close story viewer",
                            tint = Color.White
                        )
                    }
                }
            }

            // 4. Bottom Caption, Quick Reactions & Direct Reply Composer
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (story.captionOverlay.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.56f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = story.captionOverlay,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("🔥 Flame", "🎞️ Film gold", "✨ Sublime", "❤️ Love").forEach { reaction ->
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.clickable {
                                storyHaptics.performSendHaptic()
                                onSendReply(story, "Reacted $reaction to your story: \"${story.captionOverlay}\"")
                            }
                        ) {
                            Text(
                                text = reaction,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = {
                            Text(
                                text = "Reply to @${story.authorUsername}…",
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CobaltPulse,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("story_reply_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                storyHaptics.performSendHaptic()
                                onSendReply(story, replyText)
                                replyText = ""
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("story_reply_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send story reply",
                            tint = CobaltPulse
                        )
                    }
                }
            }
        }
    }
}

/**
 * Segmented auto-advancing story progress bar header mimicking modern social media stories.
 * Completed segments render at 100%, the active segment animates smoothly from 0% to 100%,
 * and upcoming segments remain unfilled. Tapping any segment jumps directly to that story.
 */
@Composable
fun StoryAutoProgressHeaderBar(
    storyCount: Int,
    currentIndex: Int,
    activeSegmentProgress: Float,
    onSelectSegment: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag("story_progress_bars_row")
    ) {
        for (idx in 0 until storyCount.coerceAtLeast(1)) {
            val segProgress = when {
                idx < currentIndex -> 1f
                idx == currentIndex -> activeSegmentProgress.coerceIn(0f, 1f)
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.28f))
                    .clickable { onSelectSegment(idx) }
                    .testTag("story_progress_segment_$idx")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(segProgress)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (idx == currentIndex) {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.White, ShutterAmber)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.White, Color.White)
                                )
                            }
                        )
                )
            }
        }
    }
}
