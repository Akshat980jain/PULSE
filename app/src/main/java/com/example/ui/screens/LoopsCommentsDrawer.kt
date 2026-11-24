package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.ui.components.PulseAvatarImage
import com.example.ui.components.rememberPulseHaptics
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.CobaltPulseSoft
import com.example.ui.theme.SignalCrimson

private data class ThreadedCommentRow(
    val comment: CommentEntity,
    val isReply: Boolean
)

/**
 * In-place half-height Loops comment drawer that keeps the full-screen Loop video
 * playing live and unobstructed in the upper viewport while users view, like, react,
 * and reply to comments.
 */
@Composable
fun LoopsCommentsOverlayDrawer(
    post: PostEntity?,
    comments: List<CommentEntity>,
    onDismiss: () -> Unit,
    onAddComment: (post: PostEntity, text: String, parentCommentId: String?, replyToUsername: String?) -> Unit,
    onToggleLikeComment: (CommentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = post != null
    BackHandler(enabled = isVisible) {
        onDismiss()
    }

    var isExpanded by remember(post?.id) { mutableStateOf(false) }
    var commentDraft by remember(post?.id) { mutableStateOf("") }
    var replyingToComment by remember(post?.id) { mutableStateOf<CommentEntity?>(null) }
    val haptics = rememberPulseHaptics()
    val quickEmojis = remember { listOf("🔥", "🎞️", "👏", "❤️", "✨", "😍") }

    val sheetFraction by animateFloatAsState(
        targetValue = if (isExpanded) 0.88f else 0.60f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "loops_comments_sheet_fraction"
    )

    val threadedRows = remember(comments) {
        val topLevel = comments
            .filter { it.parentCommentId.isNullOrBlank() }
            .sortedByDescending { it.timestampMs }
        val repliesByParent = comments
            .filter { !it.parentCommentId.isNullOrBlank() }
            .groupBy { it.parentCommentId!! }
        val emittedIds = mutableSetOf<String>()
        val result = mutableListOf<ThreadedCommentRow>()
        topLevel.forEach { parent ->
            result.add(ThreadedCommentRow(parent, isReply = false))
            emittedIds.add(parent.id)
            repliesByParent[parent.id]
                .orEmpty()
                .sortedBy { it.timestampMs }
                .forEach { reply ->
                    result.add(ThreadedCommentRow(reply, isReply = true))
                    emittedIds.add(reply.id)
                }
        }
        // Any orphaned replies whose parent isn't in the list still appear cleanly
        comments
            .filter { it.id !in emittedIds }
            .sortedBy { it.timestampMs }
            .forEach { orphan ->
                result.add(ThreadedCommentRow(orphan, isReply = !orphan.replyToUsername.isNullOrBlank()))
            }
        result
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        val activePost = post ?: return@AnimatedVisibility

        val submitComment: (String) -> Unit = { rawText ->
            val trimmed = rawText.trim()
            if (trimmed.isNotEmpty()) {
                haptics.performSendHaptic()
                val targetReply = replyingToComment
                val finalText = if (targetReply != null && !trimmed.startsWith("@${targetReply.authorUsername}")) {
                    "@${targetReply.authorUsername} $trimmed"
                } else {
                    trimmed
                }
                val rootParentId = targetReply?.parentCommentId?.takeIf { it.isNotBlank() } ?: targetReply?.id
                onAddComment(activePost, finalText, rootParentId, targetReply?.authorUsername)
                commentDraft = ""
                replyingToComment = null
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Transparent tap-to-dismiss zone over the live looping video above (NO dark scrim!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(1f - sheetFraction)
                    .align(Alignment.TopCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .testTag("loops_comments_video_dismiss_zone")
            )

            // Anchored half-height / expandable elevated slate drawer
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(sheetFraction)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("loops_comments_drawer")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                    ) {
                        // Draggable Handle + Header
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(isExpanded) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount < -18f) {
                                            isExpanded = true
                                        } else if (dragAmount > 22f) {
                                            if (isExpanded) isExpanded = false else onDismiss()
                                        }
                                    }
                                }
                                .padding(top = 10.dp, bottom = 6.dp, start = 16.dp, end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Comments (${comments.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "@${activePost.authorUsername} · ${activePost.audioTrack.ifBlank { "Original Loop Audio" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { isExpanded = !isExpanded },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("loops_comments_expand_button")
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                        contentDescription = if (isExpanded) "Half height comments" else "Expand comments",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("loops_comments_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close comments",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        // Comments Thread List
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("loops_comments_list")
                        ) {
                            if (threadedRows.isEmpty()) {
                                item {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Text(
                                            text = "No comments yet",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap an emoji or drop a reply while the Loop plays above.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(threadedRows, key = { it.comment.id }) { row ->
                                    val comment = row.comment
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = if (row.isReply) 28.dp else 0.dp)
                                            .testTag("loops_comment_item_${comment.id}")
                                    ) {
                                        if (row.isReply) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp, end = 8.dp)
                                                    .width(2.dp)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(CobaltPulse.copy(alpha = 0.45f))
                                            )
                                        }

                                        PulseAvatarImage(
                                            avatarRes = comment.authorAvatarRes,
                                            avatarUrl = comment.authorAvatarUrl,
                                            contentDescription = comment.authorDisplayName,
                                            modifier = Modifier
                                                .size(if (row.isReply) 28.dp else 34.dp)
                                                .clip(CircleShape)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "@${comment.authorUsername}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = comment.timeAgo,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = buildAnnotatedString {
                                                    val words = comment.text.split(" ")
                                                    words.forEachIndexed { idx, word ->
                                                        if (word.startsWith("@") && word.length > 1) {
                                                            withStyle(
                                                                SpanStyle(
                                                                    color = CobaltPulseSoft,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            ) {
                                                                append(word)
                                                            }
                                                        } else {
                                                            append(word)
                                                        }
                                                        if (idx < words.lastIndex) append(" ")
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = "Reply",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        replyingToComment = comment
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                                    .testTag("loops_comment_reply_${comment.id}")
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    haptics.performLikeHaptic()
                                                    onToggleLikeComment(comment)
                                                }
                                                .testTag("loops_comment_like_${comment.id}"),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                contentDescription = if (comment.isLiked) "Unlike comment" else "Like comment",
                                                tint = if (comment.isLiked) SignalCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            if (comment.likeCount > 0) {
                                                Text(
                                                    text = comment.likeCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (comment.isLiked) SignalCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        // Sticky Reply Banner + Quick Emoji Bar + Input Composer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            val replyTarget = replyingToComment
                            if (replyTarget != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CobaltPulse.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .testTag("loops_comment_reply_banner")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Replying to @${replyTarget.authorUsername}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CobaltPulseSoft,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Cancel reply",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { replyingToComment = null }
                                                .testTag("loops_comment_reply_cancel")
                                        )
                                    }
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("loops_quick_emoji_bar")
                            ) {
                                itemsIndexed(quickEmojis) { index, emoji ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .heightIn(min = 36.dp)
                                            .clickable {
                                                if (commentDraft.isBlank()) {
                                                    submitComment(emoji)
                                                } else {
                                                    commentDraft = "$commentDraft $emoji"
                                                }
                                            }
                                            .testTag("loops_quick_emoji_$index")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = emoji, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = commentDraft,
                                    onValueChange = { if (it.length <= 500) commentDraft = it },
                                    placeholder = {
                                        Text(
                                            text = if (replyTarget != null) {
                                                "Reply to @${replyTarget.authorUsername}…"
                                            } else {
                                                "Add a comment…"
                                            }
                                        )
                                    },
                                    singleLine = false,
                                    maxLines = 3,
                                    shape = RoundedCornerShape(22.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CobaltPulse,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("loops_comment_input")
                                )

                                Surface(
                                    color = if (commentDraft.isNotBlank()) CobaltPulse else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = commentDraft.isNotBlank()) {
                                            submitComment(commentDraft)
                                        }
                                        .testTag("loops_comment_send_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Post comment",
                                            tint = if (commentDraft.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
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
