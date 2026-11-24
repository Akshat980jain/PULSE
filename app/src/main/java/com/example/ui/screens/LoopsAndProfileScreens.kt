package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ActivityItemEntity
import com.example.data.model.CommentEntity
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.DirectMessageEntity
import com.example.data.model.DirectMessageThreadSummary
import com.example.data.model.FilmPreset
import com.example.data.model.PostEntity
import com.example.data.service.DarkroomProcessingService
import com.example.ui.components.PulseAvatarImage
import com.example.ui.components.PulseFilteredImage
import com.example.ui.components.formatMetricCount
import com.example.ui.components.rememberPulseHaptics
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.CobaltPulseSoft
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoopsScreen(
    loopPosts: List<PostEntity>,
    creators: List<CreatorProfileEntity>,
    onLikeToggle: (PostEntity) -> Unit,
    onDoubleTapLike: (PostEntity) -> Unit,
    onBookmarkToggle: (PostEntity) -> Unit,
    onOpenComments: (PostEntity) -> Unit,
    onSharePost: (PostEntity) -> Unit,
    onCreatorClick: (String) -> Unit,
    onMoreOptions: (PostEntity) -> Unit,
    onCreateLoopClick: () -> Unit,
    onUseAudioClick: (audioTrack: String) -> Unit = {},
    onFollowToggle: (CreatorProfileEntity) -> Unit = {},
    commentsByPostId: Map<String, List<CommentEntity>> = emptyMap(),
    onAddComment: (post: PostEntity, text: String, parentCommentId: String?, replyToUsername: String?) -> Unit = { _, _, _, _ -> },
    onToggleLikeComment: (CommentEntity) -> Unit = {},
    isLoopsActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Strictly short videos only — photos remain in Feed (Instagram Reels behavior)
    val videoOnlyLoops = remember(loopPosts) {
        loopPosts.filter { it.hasVideo }
    }

    if (videoOnlyLoops.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("loops_empty_state")
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        CobaltPulse.copy(alpha = 0.28f),
                                        ShutterAmber.copy(alpha = 0.24f)
                                    )
                                )
                            )
                            .border(1.5.dp, CobaltPulseSoft.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = "Short videos only",
                            tint = ShutterAmber,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "No short videos yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Loops is strictly for short videos — photos stay in your Feed. Record or upload a clip to start the reel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onCreateLoopClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CobaltPulse,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("loops_empty_record_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Record a Loop",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { videoOnlyLoops.size })
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberPulseHaptics()

    // Loops Audio State Machine:
    // - First video starts muted when opening Loops.
    // - Swiping to the next video transitions to unmuted unless the user explicitly muted the current video.
    // - Whenever the user mutes or unmutes a video (while paused), that exact state carries over to all subsequent videos.
    var isLoopMuted by remember { mutableStateOf(true) }
    var hasUserToggledMute by remember { mutableStateOf(false) }
    var activeAudioSheetPost by remember { mutableStateOf<PostEntity?>(null) }
    var activeCommentsPostId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState.currentPage) {
        activeCommentsPostId = null
        if (pagerState.currentPage > 0 && !hasUserToggledMute) {
            isLoopMuted = false
            hasUserToggledMute = true
        }
    }

    val discTransition = rememberInfiniteTransition(label = "loops_audio_disc_spin")
    val discRotation by discTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loops_disc_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .testTag("loops_vertical_pager")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val post = videoOnlyLoops[pageIndex]
            val creator = creators.find { it.id == post.authorId }
            val resolvedAudioTrack = post.audioTrack.trim().ifBlank { "Original Studio Audio" }

            Box(modifier = Modifier.fillMaxSize()) {
                PulseFilteredImage(
                    imageRes = post.imageRes,
                    imageUri = post.imageUri,
                    videoUri = post.effectiveVideoUri,
                    filterPreset = post.filterPreset,
                    contentDescription = post.caption,
                    showFilmBadge = false,
                    autoPlayVideo = isLoopsActive && (pageIndex == pagerState.currentPage) && activeAudioSheetPost == null,
                    loopsControlMode = true,
                    externalIsMuted = isLoopMuted,
                    onExternalToggleMute = {
                        hasUserToggledMute = true
                        isLoopMuted = !isLoopMuted
                    },
                    onDoubleTap = { onDoubleTapLike(post) },
                    modifier = Modifier.fillMaxSize()
                )

                // Top & Bottom Cinematic Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                // Right-Side Action Rail
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 52.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptics.performLikeHaptic()
                                onLikeToggle(post)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .testTag("loops_like_button_${post.id}")
                        ) {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Like loop",
                                tint = if (post.isLiked) SignalCrimson else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMetricCount(post.likeCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                haptics.performControlTapHaptic()
                                activeCommentsPostId = post.id
                                onOpenComments(post)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .testTag("loops_comment_button_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Loop comments",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val liveCommentCount = maxOf(post.commentCount, commentsByPostId[post.id]?.size ?: 0)
                        Text(
                            text = liveCommentCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.performBookmarkHaptic()
                            onBookmarkToggle(post)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .testTag("loops_bookmark_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Save loop",
                            tint = if (post.isBookmarked) ShutterAmber else Color.White
                        )
                    }

                    IconButton(
                        onClick = { onSharePost(post) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share loop",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { onMoreOptions(post) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More loop options",
                            tint = Color.White
                        )
                    }

                    // Spinning Audio Disc Shortcut on Bottom of Rail (Instagram Reels style)
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.5.dp, ShutterAmber.copy(alpha = 0.85f), CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                haptics.performControlTapHaptic()
                                activeAudioSheetPost = post
                            }
                            .testTag("loops_audio_disc_${post.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Album,
                                contentDescription = "Open audio track",
                                tint = ShutterAmber,
                                modifier = Modifier
                                    .size(26.dp)
                                    .rotate(discRotation)
                            )
                        }
                    }
                }

                // Bottom-Left Attribution, Caption & Interactive Audio Pill
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.78f)
                        .padding(start = 16.dp, bottom = 46.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCreatorClick(post.authorId) }
                    ) {
                        PulseAvatarImage(
                            avatarRes = post.authorAvatarRes,
                            avatarUrl = post.authorAvatarUrl,
                            contentDescription = post.authorDisplayName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, ShutterAmber, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "@${post.authorUsername}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (post.authorVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = CobaltPulseSoft,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (creator != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                color = if (creator.isFollowing) {
                                    Color.White.copy(alpha = 0.2f)
                                } else {
                                    CobaltPulse
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptics.performControlTapHaptic()
                                        onFollowToggle(creator)
                                    }
                            ) {
                                Text(
                                    text = if (creator.isFollowing) "Following" else "Follow",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.94f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Spinning Audio Pill (Opens Audio Sheet -> Use Audio)
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                haptics.performControlTapHaptic()
                                activeAudioSheetPost = post
                            }
                            .testTag("loops_audio_pill_${post.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = ShutterAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = resolvedAudioTrack,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = CobaltPulse.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Use Audio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CobaltPulseSoft,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Floating Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = "Pulse Loops",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    text = "Short videos · Hold for 2x · Clip ${pagerState.currentPage + 1} of ${videoOnlyLoops.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.80f)
                )
            }

            Button(
                onClick = onCreateLoopClick,
                colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("loops_new_loop_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("New Loop", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }

        // In-place half-height Loops comments drawer (video stays live above)
        val activeCommentsPost = remember(activeCommentsPostId, videoOnlyLoops) {
            videoOnlyLoops.find { it.id == activeCommentsPostId }
        }
        LoopsCommentsOverlayDrawer(
            post = activeCommentsPost,
            comments = activeCommentsPost?.let { commentsByPostId[it.id].orEmpty() }.orEmpty(),
            onDismiss = { activeCommentsPostId = null },
            onAddComment = onAddComment,
            onToggleLikeComment = onToggleLikeComment
        )

        // Interactive Audio Sheet with "Use Audio" Shortcut & Matching Clips
        val sheetPost = activeAudioSheetPost
        if (sheetPost != null) {
            val trackName = sheetPost.audioTrack.trim().ifBlank { "Original Studio Audio" }
            val matchingLoops = remember(videoOnlyLoops, trackName) {
                videoOnlyLoops.filter {
                    it.audioTrack.trim().ifBlank { "Original Studio Audio" }
                        .equals(trackName, ignoreCase = true)
                }
            }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { activeAudioSheetPost = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("loops_audio_sheet")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CobaltPulse.copy(alpha = 0.22f))
                                .border(1.5.dp, ShutterAmber.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = ShutterAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = trackName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Original by @${sheetPost.authorUsername} · ${matchingLoops.size} ${if (matchingLoops.size == 1) "Loop" else "Loops"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            activeAudioSheetPost = null
                            onUseAudioClick(trackName)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CobaltPulse,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("loops_use_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use Audio in Create Studio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (matchingLoops.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "Loops with this audio",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            items(matchingLoops, key = { it.id }) { clip ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val targetIdx = videoOnlyLoops.indexOfFirst { it.id == clip.id }
                                            activeAudioSheetPost = null
                                            if (targetIdx >= 0) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(targetIdx)
                                                }
                                            }
                                        }
                                        .testTag("loops_audio_match_${clip.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        PulseAvatarImage(
                                            avatarRes = clip.authorAvatarRes,
                                            avatarUrl = clip.authorAvatarUrl,
                                            contentDescription = clip.authorDisplayName,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "@${clip.authorUsername}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = clip.caption.ifBlank { "Short video loop" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.PlayCircleFilled,
                                            contentDescription = "Jump to clip",
                                            tint = CobaltPulseSoft,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun MessagesScreen(
    threads: List<DirectMessageThreadSummary>,
    creators: List<CreatorProfileEntity> = emptyList(),
    initialSelectedParticipantId: String? = null,
    onMarkThreadRead: (String) -> Unit,
    onSendMessage: (
        participantId: String,
        participantUsername: String,
        participantDisplayName: String,
        participantAvatarRes: Int,
        text: String,
        attachedPhotoUri: String?,
        voiceNoteDurationSec: Int
    ) -> Unit,
    onToggleLikeMessage: (DirectMessageEntity) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeParticipantId by remember(initialSelectedParticipantId) {
        mutableStateOf(initialSelectedParticipantId)
    }

    val activeThread = remember(threads, creators, activeParticipantId) {
        val pid = activeParticipantId ?: return@remember null
        threads.find { it.participantId == pid || it.threadId == pid }
            ?: creators.find { it.id == pid }?.let { creator ->
                DirectMessageThreadSummary(
                    threadId = "thread_${creator.id}",
                    participantId = creator.id,
                    participantUsername = creator.username,
                    participantDisplayName = creator.displayName,
                    participantAvatarRes = creator.avatarRes,
                    participantAvatarUrl = creator.avatarUrl,
                    lastMessageText = "Start a studio conversation",
                    lastTimeLabel = "Now",
                    unreadCount = 0,
                    messages = emptyList()
                )
            }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("messages_screen")
    ) {
        if (activeThread == null) {
            // Inbox List
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close inbox"
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Direct studio messages",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "1-on-1 conversations, voice notes & shared frames",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                if (creators.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = "Message a Creator",
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
                                .testTag("messages_creators_row")
                        ) {
                            items(creators, key = { it.id }) { creator ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .clickable {
                                            activeParticipantId = creator.id
                                        }
                                ) {
                                    PulseAvatarImage(
                                        avatarRes = creator.avatarRes,
                                        avatarUrl = creator.avatarUrl,
                                        contentDescription = creator.displayName,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, CobaltPulse, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "@${creator.username}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }

                if (threads.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = CobaltPulse,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No messages yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap a creator above or share any post to start a conversation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("messages_threads_list")
                    ) {
                        items(threads, key = { it.threadId }) { thread ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMarkThreadRead(thread.threadId)
                                        activeParticipantId = thread.participantId
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    PulseAvatarImage(
                                        avatarRes = thread.participantAvatarRes,
                                        avatarUrl = thread.participantAvatarUrl,
                                        contentDescription = thread.participantDisplayName,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = thread.participantDisplayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "@${thread.participantUsername}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = thread.lastMessageText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (thread.unreadCount > 0) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = thread.lastTimeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (thread.unreadCount > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(CobaltPulse)
                                            ) {
                                                Text(
                                                    text = thread.unreadCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
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
        } else {
            // Active 1-on-1 Conversation Thread with Voice Note & Photo Attachment
            var draftMessage by remember { mutableStateOf("") }
            var isRecordingVoiceNote by remember { mutableStateOf(false) }
            var voiceNoteSeconds by remember { mutableIntStateOf(0) }
            var playingVoiceMessageId by remember { mutableStateOf<String?>(null) }
            val dmHaptics = rememberPulseHaptics()

            val context = LocalContext.current
            val dmPhotoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    val permanentUri = DarkroomProcessingService.copyUriToPermanentInternalFile(
                        context = context,
                        sourceUri = uri,
                        isVideo = false
                    )
                    dmHaptics.performSendHaptic()
                    onSendMessage(
                        activeThread.participantId,
                        activeThread.participantUsername,
                        activeThread.participantDisplayName,
                        activeThread.participantAvatarRes,
                        draftMessage.ifBlank { "Shared a studio photo" },
                        permanentUri,
                        0
                    )
                    draftMessage = ""
                }
            }

            LaunchedEffect(isRecordingVoiceNote) {
                if (isRecordingVoiceNote) {
                    voiceNoteSeconds = 1
                    while (isRecordingVoiceNote && voiceNoteSeconds < 60) {
                        delay(1000L)
                        voiceNoteSeconds++
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    IconButton(onClick = { activeParticipantId = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to threads"
                        )
                    }
                    PulseAvatarImage(
                        avatarRes = activeThread.participantAvatarRes,
                        avatarUrl = activeThread.participantAvatarUrl,
                        contentDescription = activeThread.participantDisplayName,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeThread.participantDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "@${activeThread.participantUsername}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close messages"
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(activeThread.messages, key = { it.id }) { msg ->
                        val align = if (msg.isFromMe) Alignment.End else Alignment.Start
                        Column(
                            horizontalAlignment = align,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = if (msg.isFromMe) {
                                    CobaltPulse
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (msg.isFromMe) 16.dp else 4.dp,
                                    bottomEnd = if (msg.isFromMe) 4.dp else 16.dp
                                ),
                                modifier = Modifier.clickable {
                                    dmHaptics.performLikeHaptic()
                                    if (msg.voiceNoteDurationSec > 0) {
                                        playingVoiceMessageId = if (playingVoiceMessageId == msg.id) null else msg.id
                                    } else {
                                        onToggleLikeMessage(msg)
                                    }
                                }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (msg.sharedPostImageRes != null || !msg.sharedPostImageUri.isNullOrBlank()) {
                                        PulseFilteredImage(
                                            imageRes = msg.sharedPostImageRes ?: 0,
                                            imageUri = msg.sharedPostImageUri,
                                            filterPreset = FilmPreset.NATURAL,
                                            contentDescription = "Shared post",
                                            showFilmBadge = false,
                                            modifier = Modifier
                                                .fillMaxWidth(0.7f)
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    if (msg.voiceNoteDurationSec > 0) {
                                        val isPlayingVoice = playingVoiceMessageId == msg.id
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayCircleFilled,
                                                contentDescription = "Play voice note",
                                                tint = if (msg.isFromMe) ShutterAmber else CobaltPulse,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf(8, 16, 12, 20, 14, 18, 10, 15, 9).forEach { barH ->
                                                    Box(
                                                        modifier = Modifier
                                                            .width(3.dp)
                                                            .height(barH.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(
                                                                if (isPlayingVoice) {
                                                                    ShutterAmber
                                                                } else if (msg.isFromMe) {
                                                                    Color.White.copy(alpha = 0.8f)
                                                                } else {
                                                                    CobaltPulse
                                                                }
                                                            )
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "0:${msg.voiceNoteDurationSec.toString().padStart(2, '0')}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (msg.isFromMe) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (msg.isFromMe) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
                            ) {
                                Text(
                                    text = msg.timeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (msg.isLiked) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Liked message",
                                        tint = SignalCrimson,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    IconButton(
                        onClick = {
                            dmHaptics.performControlTapHaptic()
                            dmPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("dm_attach_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Attach photo",
                            tint = CobaltPulse
                        )
                    }

                    IconButton(
                        onClick = {
                            dmHaptics.performSendHaptic()
                            if (isRecordingVoiceNote) {
                                val duration = voiceNoteSeconds.coerceAtLeast(2)
                                isRecordingVoiceNote = false
                                onSendMessage(
                                    activeThread.participantId,
                                    activeThread.participantUsername,
                                    activeThread.participantDisplayName,
                                    activeThread.participantAvatarRes,
                                    "",
                                    null,
                                    duration
                                )
                            } else {
                                isRecordingVoiceNote = true
                            }
                        },
                        modifier = Modifier.testTag("dm_voice_note_button")
                    ) {
                        Icon(
                            imageVector = if (isRecordingVoiceNote) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isRecordingVoiceNote) "Send voice note" else "Record voice note",
                            tint = if (isRecordingVoiceNote) SignalCrimson else ShutterAmber
                        )
                    }

                    OutlinedTextField(
                        value = draftMessage,
                        onValueChange = { draftMessage = it },
                        placeholder = {
                            Text(
                                if (isRecordingVoiceNote) {
                                    "Recording voice note 0:${voiceNoteSeconds.toString().padStart(2, '0')}… tap Stop to send"
                                } else {
                                    "Message @${activeThread.participantUsername}…"
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dm_input_field")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (draftMessage.isNotBlank()) {
                                dmHaptics.performSendHaptic()
                                onSendMessage(
                                    activeThread.participantId,
                                    activeThread.participantUsername,
                                    activeThread.participantDisplayName,
                                    activeThread.participantAvatarRes,
                                    draftMessage,
                                    null,
                                    0
                                )
                                draftMessage = ""
                            }
                        },
                        enabled = draftMessage.isNotBlank(),
                        modifier = Modifier.testTag("dm_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send direct message",
                            tint = if (draftMessage.isNotBlank()) CobaltPulse else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DirectMessagesDialog(
    threads: List<DirectMessageThreadSummary>,
    creators: List<CreatorProfileEntity> = emptyList(),
    initialSelectedParticipantId: String? = null,
    onMarkThreadRead: (String) -> Unit,
    onSendMessage: (
        participantId: String,
        participantUsername: String,
        participantDisplayName: String,
        participantAvatarRes: Int,
        text: String,
        attachedPhotoUri: String?,
        voiceNoteDurationSec: Int
    ) -> Unit,
    onToggleLikeMessage: (DirectMessageEntity) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        MessagesScreen(
            threads = threads,
            creators = creators,
            initialSelectedParticipantId = initialSelectedParticipantId,
            onMarkThreadRead = onMarkThreadRead,
            onSendMessage = onSendMessage,
            onToggleLikeMessage = onToggleLikeMessage,
            onClose = onDismiss,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityNotificationsSheet(
    activities: List<ActivityItemEntity>,
    onDismiss: () -> Unit,
    onCreatorClick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Studio activity",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Recent likes, comments, and new followers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(min = 200.dp, max = 400.dp)
            ) {
                items(activities, key = { it.id }) { act ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onCreatorClick(act.actorId)
                            }
                    ) {
                        PulseAvatarImage(
                            avatarRes = act.actorAvatarRes,
                            avatarUrl = act.actorAvatarUrl,
                            contentDescription = act.actorDisplayName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "@${act.actorUsername} ${act.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = act.timeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (act.targetPostImageRes != null || !act.targetPostImageUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            PulseFilteredImage(
                                imageRes = act.targetPostImageRes ?: 0,
                                imageUri = act.targetPostImageUri,
                                filterPreset = FilmPreset.NATURAL,
                                contentDescription = "Activity target post",
                                showFilmBadge = false,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
