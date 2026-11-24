package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.model.CommentEntity
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.FilmPreset
import com.example.data.model.PostEntity
import com.example.data.model.isVideoUrl
import com.example.data.remote.SupabaseClient
import com.example.data.service.DarkroomProcessingService
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val decodedLocalOrDataBitmapCache = object : LruCache<String, ImageBitmap>(48) {}
private val compactVideoThumbnailUriCache = object : LruCache<String, String>(36) {}

@Composable
fun rememberDecodedLocalOrDataBitmap(context: Context, rawUriOrData: String?): ImageBitmap? {
    val clean = rawUriOrData?.trim()?.takeIf { it.isNotBlank() && !isVideoUrl(it) } ?: return null
    val isLocalOrData = clean.startsWith("data:image/", ignoreCase = true) ||
        clean.startsWith("file://", ignoreCase = true) ||
        clean.startsWith("content://", ignoreCase = true) ||
        clean.startsWith("/")
    if (!isLocalOrData) return null

    var cached by remember(clean) {
        mutableStateOf(
            synchronized(decodedLocalOrDataBitmapCache) {
                decodedLocalOrDataBitmapCache.get(clean)
            }
        )
    }
    LaunchedEffect(clean, context) {
        if (cached == null) {
            val decoded = withContext(Dispatchers.IO) {
                try {
                    DarkroomProcessingService.decodeSourceBitmap(
                        context = context,
                        sourceUri = clean,
                        fallbackResId = 0
                    )?.asImageBitmap()
                } catch (_: Throwable) {
                    null
                }
            }
            if (decoded != null) {
                synchronized(decodedLocalOrDataBitmapCache) {
                    decodedLocalOrDataBitmapCache.put(clean, decoded)
                }
                cached = decoded
            }
        }
    }
    return cached
}

@Composable
fun safePainterResource(resId: Int): Painter {
    val validResId = if (resId != 0) resId else R.drawable.img_post_portrait_1794021773202
    return painterResource(id = validResId)
}

/**
 * Opens the native Android system share sheet (`Intent.createChooser`) to share a Feed post externally.
 */
fun sharePostExternally(context: Context, post: PostEntity) {
    try {
        val mediaUrl = post.effectiveVideoUri
            ?: post.imageUri
                ?.takeIf { it.isNotBlank() }
                ?.let { SupabaseClient.resolveSupabaseMediaUrl(it, defaultBucket = "posts") }
        val tagsLine = if (post.tags.isNotEmpty()) {
            "\n" + post.tags.joinToString(" ") { "#$it" }
        } else {
            ""
        }
        val locationSegment = if (post.location.isNotBlank()) " · ${post.location}" else ""
        val mediaSegment = if (!mediaUrl.isNullOrBlank()) "\n$mediaUrl" else ""
        val shareText = buildString {
            append("@${post.authorUsername}$locationSegment on Pulse:\n")
            append("\"${post.caption}\"")
            append(tagsLine)
            append(mediaSegment)
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Pulse post by @${post.authorUsername}")
            putExtra(Intent.EXTRA_TITLE, "@${post.authorUsername} · ${post.caption.take(48)}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Share post via").apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooserIntent)
    } catch (_: Throwable) {
    }
}

/**
 * Subtle tactile haptic feedback controller for likes, message sends, filter transitions,
 * bookmarks/collections, and Loop video playback/mute controls.
 */
class PulseHaptics(
    private val context: Context,
    private val view: View,
    private val composeHaptics: HapticFeedback
) {
    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun vibrateFallback(durationMs: Long, amplitude: Int) {
        try {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (_: Throwable) {
        }
    }

    fun performLikeHaptic() {
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            val performed = view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vibrateFallback(durationMs = 18L, amplitude = 140)
            }
        } catch (_: Throwable) {
        }
    }

    fun performBookmarkHaptic() {
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            val performed = view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vibrateFallback(durationMs = 22L, amplitude = 165)
            }
        } catch (_: Throwable) {
        }
    }

    fun performLoopPlayPauseHaptic() {
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                HapticFeedbackConstants.CONTEXT_CLICK
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            val performed = view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vibrateFallback(durationMs = 14L, amplitude = 120)
            }
        } catch (_: Throwable) {
        }
    }

    fun performLoopMuteToggleHaptic() {
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                HapticFeedbackConstants.CLOCK_TICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            val performed = view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vibrateFallback(durationMs = 12L, amplitude = 110)
            }
        } catch (_: Throwable) {
        }
    }

    fun performSendHaptic() {
        try {
            val performed = view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vibrateFallback(durationMs = 15L, amplitude = 130)
            }
        } catch (_: Throwable) {
        }
    }

    fun performFilterTickHaptic() {
        try {
            val performed = view.performHapticFeedback(
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vibrateFallback(durationMs = 10L, amplitude = 95)
            }
        } catch (_: Throwable) {
        }
    }

    fun performControlTapHaptic() {
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                HapticFeedbackConstants.CONTEXT_CLICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            val performed = view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vibrateFallback(durationMs = 12L, amplitude = 110)
            }
        } catch (_: Throwable) {
        }
    }
}

@Composable
fun rememberPulseHaptics(): PulseHaptics {
    val context = LocalContext.current
    val view = LocalView.current
    val composeHaptics = LocalHapticFeedback.current
    return remember(context, view, composeHaptics) {
        PulseHaptics(context.applicationContext, view, composeHaptics)
    }
}

@Composable
fun rememberPulseShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "pulse_skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_skeleton_translate"
    )
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val highlightColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 350f, translateAnim - 350f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun PulseSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val shimmerBrush = rememberPulseShimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush)
    )
}

@Composable
fun StoriesRowSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("stories_skeleton_row")
    ) {
        repeat(5) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(68.dp)
            ) {
                PulseSkeletonBox(
                    shape = CircleShape,
                    modifier = Modifier.size(62.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                PulseSkeletonBox(
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .width(48.dp)
                        .height(10.dp)
                )
            }
        }
    }
}

@Composable
fun PostCardSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("post_card_skeleton")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            PulseSkeletonBox(
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                PulseSkeletonBox(
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(13.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                PulseSkeletonBox(
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.28f)
                        .height(10.dp)
                )
            }
        }

        PulseSkeletonBox(
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            repeat(3) {
                PulseSkeletonBox(
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .width(52.dp)
                        .height(22.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PulseSkeletonBox(
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            PulseSkeletonBox(
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .height(12.dp)
            )
            PulseSkeletonBox(
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
            )
        }
    }
}

@Composable
fun FeedSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("feed_skeleton_loader")
    ) {
        PostCardSkeletonLoader()
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        PostCardSkeletonLoader()
    }
}

@Composable
fun ExploreSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("explore_skeleton_loader")
    ) {
        PulseSkeletonBox(
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PulseSkeletonBox(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f)
            )
            PulseSkeletonBox(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f)
            )
        }
    }
}

@Composable
private fun StudioAvatarFallback(
    label: String,
    modifier: Modifier = Modifier
) {
    val cleanInitial = remember(label) {
        label.trim().removePrefix("@").firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E293B),
                    Color(0xFF0F172A)
                )
            )
        )
    ) {
        if (!cleanInitial.isNullOrBlank()) {
            Text(
                text = cleanInitial,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxSize(0.55f)
            )
        }
    }
}

@Composable
fun PulseAvatarImage(
    avatarRes: Int,
    avatarUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val resolvedUrl = remember(avatarUrl) {
        SupabaseClient.resolveSupabaseMediaUrl(avatarUrl, defaultBucket = "avatars")
    }
    val context = LocalContext.current
    val directAvatarBitmap = rememberDecodedLocalOrDataBitmap(context, resolvedUrl ?: avatarUrl)
    val imageLoader = remember(context) { SupabaseClient.getImageLoader(context) }

    if (directAvatarBitmap != null) {
        Image(
            bitmap = directAvatarBitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (!resolvedUrl.isNullOrBlank()) {
        val imageRequest = remember(resolvedUrl, context) {
            ImageRequest.Builder(context)
                .data(resolvedUrl)
                .size(256, 256)
                .precision(Precision.INEXACT)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build()
        }
        DisposableEffect(imageRequest, imageLoader) {
            val preloadDisposable: Disposable? = try {
                imageLoader.enqueue(imageRequest)
            } catch (_: Throwable) {
                null
            }
            onDispose {
                try {
                    preloadDisposable?.dispose()
                } catch (_: Throwable) {
                }
            }
        }
        SubcomposeAsyncImage(
            model = imageRequest,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            loading = {
                PulseSkeletonBox(
                    shape = CircleShape,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                StudioAvatarFallback(
                    label = contentDescription,
                    modifier = Modifier.fillMaxSize()
                )
            },
            modifier = modifier
        )
    } else {
        StudioAvatarFallback(
            label = contentDescription,
            modifier = modifier
        )
    }
}

/**
 * Hardware-bitmap-safe software + hardware blur generator so extracted video frames or poster
 * thumbnails render as a smooth, frosted Gaussian-like backdrop behind vertical/letterboxed
 * videos across all physical Android phones and API levels without crashing on HARDWARE bitmaps.
 */
private fun createBlurredVideoBackdropBitmap(source: Bitmap?): ImageBitmap? {
    if (source == null) return null
    return try {
        if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
        val softwareSource = if (source.config == Bitmap.Config.ARGB_8888) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        }
        val smallW = 36
        val smallH = 48
        val downscaled = Bitmap.createScaledBitmap(softwareSource, smallW, smallH, true)
        val working = if (downscaled.isMutable && downscaled.config == Bitmap.Config.ARGB_8888) {
            downscaled
        } else {
            downscaled.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        }
        val pixels = IntArray(smallW * smallH)
        val temp = IntArray(smallW * smallH)
        working.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)

        // 3-pass box blur on the compact grid for a deep Gaussian-equivalent blur
        val radius = 3
        repeat(3) {
            for (y in 0 until smallH) {
                for (x in 0 until smallW) {
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    var count = 0
                    for (dy in -radius..radius) {
                        val ny = (y + dy).coerceIn(0, smallH - 1)
                        for (dx in -radius..radius) {
                            val nx = (x + dx).coerceIn(0, smallW - 1)
                            val c = pixels[ny * smallW + nx]
                            rSum += (c shr 16) and 0xFF
                            gSum += (c shr 8) and 0xFF
                            bSum += c and 0xFF
                            count++
                        }
                    }
                    val r = rSum / count
                    val g = gSum / count
                    val b = bSum / count
                    temp[y * smallW + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            System.arraycopy(temp, 0, pixels, 0, pixels.size)
        }
        working.setPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
        val upscaled = Bitmap.createScaledBitmap(working, 180, 240, true)
        upscaled.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PulseVideoPlayer(
    videoUri: String,
    fallbackImageRes: Int,
    fallbackImageUri: String? = null,
    contentDescription: String,
    autoPlay: Boolean = true,
    compactPreviewMode: Boolean = false,
    loopsControlMode: Boolean = false,
    externalIsMuted: Boolean? = null,
    onExternalToggleMute: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberPulseHaptics()
    val resolvedVideoUrl = remember(videoUri) {
        SupabaseClient.resolveSupabaseMediaUrl(videoUri, defaultBucket = "videos") ?: videoUri
    }
    var extractedCompactPosterUri by remember(resolvedVideoUrl) {
        mutableStateOf(
            synchronized(compactVideoThumbnailUriCache) {
                compactVideoThumbnailUriCache.get(resolvedVideoUrl)
            }
        )
    }
    val resolvedPosterUrl = remember(fallbackImageUri, extractedCompactPosterUri) {
        val rawPoster = fallbackImageUri?.takeIf { it.isNotBlank() && !isVideoUrl(it) }
            ?: extractedCompactPosterUri
        rawPoster?.let { SupabaseClient.resolveSupabaseMediaUrl(it, defaultBucket = "posts") ?: it }
    }

    // In compactPreviewMode (e.g. Profile 3-column grid), extract a static video thumbnail on Dispatchers.IO
    // instead of spawning multiple concurrent hardware ExoPlayer instances.
    LaunchedEffect(compactPreviewMode, resolvedVideoUrl, resolvedPosterUrl) {
        if (compactPreviewMode && resolvedPosterUrl.isNullOrBlank() && resolvedVideoUrl.isNotBlank()) {
            val extractedUri = withContext(Dispatchers.IO) {
                try {
                    val playableSource = SupabaseClient.preparePlayableVideoSource(context, resolvedVideoUrl)
                        ?: resolvedVideoUrl
                    val thumbFile = DarkroomProcessingService.extractVideoThumbnailFile(context, playableSource)
                    thumbFile?.let { android.net.Uri.fromFile(it).toString() }
                } catch (_: Throwable) {
                    null
                }
            }
            if (!extractedUri.isNullOrBlank()) {
                synchronized(compactVideoThumbnailUriCache) {
                    compactVideoThumbnailUriCache.put(resolvedVideoUrl, extractedUri)
                }
                extractedCompactPosterUri = extractedUri
            }
        }
    }

    var isPlaying by remember(resolvedVideoUrl) { mutableStateOf(autoPlay && !compactPreviewMode) }
    var internalIsMuted by remember { mutableStateOf(true) }
    val effectiveIsMuted = externalIsMuted ?: internalIsMuted
    var isBuffering by remember(resolvedVideoUrl, compactPreviewMode) { mutableStateOf(!compactPreviewMode) }
    var hasVideoError by remember(resolvedVideoUrl) { mutableStateOf(false) }
    var progressFraction by remember(resolvedVideoUrl) { mutableFloatStateOf(0f) }
    var durationMs by remember(resolvedVideoUrl) { mutableLongStateOf(0L) }
    var currentPositionMs by remember(resolvedVideoUrl) { mutableLongStateOf(0L) }
    var isScrubbing by remember(resolvedVideoUrl) { mutableStateOf(false) }
    var scrubFraction by remember(resolvedVideoUrl) { mutableFloatStateOf(0f) }
    var isFastForward2x by remember(resolvedVideoUrl) { mutableStateOf(false) }
    var lastFastForwardReleaseMs by remember(resolvedVideoUrl) { mutableLongStateOf(0L) }
    var videoAspectRatio by remember(resolvedVideoUrl) { mutableFloatStateOf(9f / 16f) }
    var blurredBackdropBitmap by remember(resolvedVideoUrl, resolvedPosterUrl) {
        mutableStateOf<ImageBitmap?>(null)
    }
    var activeTextureView by remember { mutableStateOf<TextureView?>(null) }
    var posterDisposable by remember { mutableStateOf<Disposable?>(null) }

    val toggleMuteAction = {
        if (loopsControlMode) {
            haptics.performLoopMuteToggleHaptic()
        } else {
            haptics.performControlTapHaptic()
        }
        if (onExternalToggleMute != null) {
            onExternalToggleMute()
        } else {
            internalIsMuted = !internalIsMuted
        }
    }

    LaunchedEffect(autoPlay, compactPreviewMode) {
        isPlaying = autoPlay && !compactPreviewMode
    }

    // Load poster image URL if provided with explicit Coil request disposal and off-main-thread blur
    DisposableEffect(resolvedPosterUrl, context) {
        if (!resolvedPosterUrl.isNullOrBlank() && blurredBackdropBitmap == null) {
            try {
                val loader = SupabaseClient.getImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(resolvedPosterUrl)
                    .size(360, 480)
                    .precision(Precision.INEXACT)
                    .allowHardware(false)
                    .allowRgb565(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .target { drawable ->
                        val bmp = (drawable as? BitmapDrawable)?.bitmap
                        if (bmp != null) {
                            coroutineScope.launch(Dispatchers.Default) {
                                val blurred = createBlurredVideoBackdropBitmap(bmp)
                                if (blurred != null) {
                                    withContext(Dispatchers.Main) {
                                        blurredBackdropBitmap = blurred
                                    }
                                }
                            }
                        }
                    }
                    .build()
                posterDisposable = loader.enqueue(req)
            } catch (_: Throwable) {
            }
        }
        onDispose {
            try {
                posterDisposable?.dispose()
                posterDisposable = null
            } catch (_: Throwable) {
            }
        }
    }

    // Create and manage Media3 ExoPlayer instance safely:
    // Never allocate ExoPlayer for compact grid thumbnails (prevents concurrent MediaCodec overload & stale buffer warnings)
    val shouldAllocatePlayer = !compactPreviewMode
    val exoPlayer = remember(context, resolvedVideoUrl, shouldAllocatePlayer) {
        if (!shouldAllocatePlayer) {
            null
        } else {
            try {
                val appCtx = context.applicationContext
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(SupabaseClient.BROWSER_USER_AGENT)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(12_000)
                    .setReadTimeoutMs(15_000)
                val dataSourceFactory = DefaultDataSource.Factory(appCtx, httpDataSourceFactory)
                val renderersFactory = DefaultRenderersFactory(appCtx)
                    .setEnableDecoderFallback(true)
                val trackSelector = DefaultTrackSelector(appCtx).apply {
                    setParameters(
                        buildUponParameters()
                            .setMaxVideoSize(720, 1280)
                            .setMaxVideoFrameRate(30)
                    )
                }
                ExoPlayer.Builder(appCtx, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                    .build()
                    .apply {
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = autoPlay
                        volume = if (effectiveIsMuted) 0f else 1f
                    }
            } catch (_: Throwable) {
                null
            }
        }
    }

    LaunchedEffect(resolvedVideoUrl, exoPlayer) {
        if (exoPlayer == null) {
            isBuffering = false
            hasVideoError = true
            return@LaunchedEffect
        }
        if (resolvedVideoUrl.isNotBlank()) {
            try {
                hasVideoError = false
                isBuffering = true
                val playableUri = SupabaseClient.preparePlayableVideoSource(context, resolvedVideoUrl)
                if (playableUri.isNullOrBlank()) {
                    isBuffering = false
                    hasVideoError = true
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    return@LaunchedEffect
                }
                val mediaItem = MediaItem.fromUri(Uri.parse(playableUri))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.volume = if (effectiveIsMuted || compactPreviewMode) 0f else 1f
                exoPlayer.playWhenReady = isPlaying
            } catch (_: Throwable) {
                isBuffering = false
                hasVideoError = true
            }
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        try {
            if (isPlaying && !hasVideoError) {
                exoPlayer.playWhenReady = true
                exoPlayer.play()
            } else {
                exoPlayer.playWhenReady = false
                exoPlayer.pause()
            }
        } catch (_: Throwable) {
        }
    }

    LaunchedEffect(effectiveIsMuted, exoPlayer, compactPreviewMode) {
        if (exoPlayer == null) return@LaunchedEffect
        try {
            exoPlayer.volume = if (effectiveIsMuted || compactPreviewMode) 0f else 1f
        } catch (_: Throwable) {
        }
    }

    LaunchedEffect(isFastForward2x, exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        try {
            exoPlayer.setPlaybackSpeed(if (isFastForward2x) 2.0f else 1.0f)
        } catch (_: Throwable) {
        }
    }

    // Poll playback position without calling TextureView.getBitmap (avoids W/HWUI GPU bitmap copy warning)
    LaunchedEffect(exoPlayer, isPlaying, compactPreviewMode, isScrubbing) {
        if (exoPlayer == null || compactPreviewMode) return@LaunchedEffect
        while (isActive && isPlaying) {
            try {
                val duration = exoPlayer.duration
                val position = exoPlayer.currentPosition
                if (duration > 0L) {
                    durationMs = duration
                    if (!isScrubbing) {
                        currentPositionMs = position.coerceIn(0L, duration)
                        progressFraction = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    }
                }
            } catch (_: Throwable) {
            }
            delay(200L)
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer, resolvedVideoUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    hasVideoError = false
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height.toFloat()
                    if (ratio.isFinite() && ratio > 0.1f) {
                        videoAspectRatio = ratio
                    }
                }
            }

            override fun onRenderedFirstFrame() {
                if (compactPreviewMode) {
                    isPlaying = false
                    try {
                        exoPlayer?.playWhenReady = false
                        exoPlayer?.pause()
                    } catch (_: Throwable) {
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                hasVideoError = true
            }
        }
        try {
            exoPlayer?.addListener(listener)
        } catch (_: Throwable) {
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                        exoPlayer?.playWhenReady = false
                        exoPlayer?.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        if (isPlaying && !hasVideoError && !compactPreviewMode) {
                            exoPlayer?.playWhenReady = true
                            exoPlayer?.play()
                        }
                    }
                    else -> Unit
                }
            } catch (_: Throwable) {
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            try {
                posterDisposable?.dispose()
                posterDisposable = null
                exoPlayer?.removeListener(listener)
                exoPlayer?.playWhenReady = false
                exoPlayer?.stop()
                activeTextureView?.let { tv ->
                    exoPlayer?.clearVideoTextureView(tv)
                }
                activeTextureView = null
                exoPlayer?.clearMediaItems()
                exoPlayer?.release()
            } catch (_: Throwable) {
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .background(Color(0xFF0B0E14))
            .pointerInput(onDoubleTap, onTap, compactPreviewMode, loopsControlMode) {
                detectTapGestures(
                    onPress = {
                        if (loopsControlMode && !compactPreviewMode) {
                            var engagedFastForward = false
                            val holdJob = coroutineScope.launch {
                                delay(260L)
                                if (isPlaying && !hasVideoError) {
                                    engagedFastForward = true
                                    isFastForward2x = true
                                    haptics.performControlTapHaptic()
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                holdJob.cancel()
                                if (engagedFastForward || isFastForward2x) {
                                    isFastForward2x = false
                                    lastFastForwardReleaseMs = System.currentTimeMillis()
                                }
                            }
                        }
                    },
                    onDoubleTap = {
                        if (onDoubleTap != null) {
                            haptics.performLikeHaptic()
                            onDoubleTap()
                        }
                    },
                    onTap = {
                        if (loopsControlMode && (System.currentTimeMillis() - lastFastForwardReleaseMs) < 350L) {
                            return@detectTapGestures
                        }
                        if (onTap != null && compactPreviewMode) {
                            onTap()
                        } else {
                            if (loopsControlMode) {
                                haptics.performLoopPlayPauseHaptic()
                            } else {
                                haptics.performControlTapHaptic()
                            }
                            isPlaying = !isPlaying
                        }
                    }
                )
            }
            .testTag("pulse_video_player")
    ) {
        // Layer 1: Blurred Video Frame / Thumbnail Backdrop (NEVER an unblurred fallback drawable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("video_blurred_backdrop")
        ) {
            val backdropBitmap = blurredBackdropBitmap
            if (backdropBitmap != null) {
                Image(
                    bitmap = backdropBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.18f)
                        .blur(radius = 28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
            } else if (!resolvedPosterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolvedPosterUrl)
                        .size(360, 480)
                        .precision(Precision.INEXACT)
                        .allowRgb565(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build(),
                    imageLoader = SupabaseClient.getImageLoader(context),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.18f)
                        .blur(radius = 28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
            } else {
                // Soft blurred ambient darkroom glow while first video frame is decoding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CobaltPulse.copy(alpha = 0.26f),
                                    ShutterAmber.copy(alpha = 0.14f),
                                    Color(0xFF0B0E14)
                                )
                            )
                        )
                        .blur(radius = 28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
            }

            // Dark glass cinema tint over the blurred backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0E14).copy(alpha = 0.45f))
            )
        }

        // Layer 2: Media3 ExoPlayer rendered via AspectRatioFrameLayout + TextureView so it
        // composites cleanly over the blurred backdrop inside LazyColumn & VerticalPager on physical phones
        if (!hasVideoError && resolvedVideoUrl.isNotBlank() && exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val aspectContainer = AspectRatioFrameLayout(ctx).apply {
                            resizeMode = if (compactPreviewMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            setAspectRatio(videoAspectRatio)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                Gravity.CENTER
                            )
                        }
                        val textureView = TextureView(ctx).apply {
                            isOpaque = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                Gravity.CENTER
                            )
                        }
                        aspectContainer.addView(textureView)
                        addView(aspectContainer)
                        activeTextureView = textureView
                        try {
                            exoPlayer.setVideoTextureView(textureView)
                        } catch (_: Throwable) {
                        }
                    }
                },
                update = { rootContainer ->
                    val aspectContainer = rootContainer.getChildAt(0) as? AspectRatioFrameLayout
                    aspectContainer?.resizeMode = if (compactPreviewMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    aspectContainer?.setAspectRatio(videoAspectRatio)
                    val textureView = aspectContainer?.getChildAt(0) as? TextureView
                    if (textureView != null && activeTextureView !== textureView) {
                        activeTextureView = textureView
                        try {
                            exoPlayer.setVideoTextureView(textureView)
                        } catch (_: Throwable) {
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (hasVideoError) {
            // Clean visual fallback when remote video URL is unavailable or restricted (HTTP 403/404)
            val directPosterBitmap = rememberDecodedLocalOrDataBitmap(context, resolvedPosterUrl ?: fallbackImageUri)
            if (directPosterBitmap != null) {
                Image(
                    bitmap = directPosterBitmap,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!resolvedPosterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolvedPosterUrl)
                        .size(720, 900)
                        .precision(Precision.INEXACT)
                        .allowRgb565(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build(),
                    imageLoader = SupabaseClient.getImageLoader(context),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = safePainterResource(fallbackImageRes),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (!compactPreviewMode) {
            if (!loopsControlMode) {
                // Top-Left Video Badge (Feed / Card mode)
                Surface(
                    color = Color(0xCC0B0E14),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = ShutterAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "VIDEO · PLAYING" else "VIDEO · PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Buffering Indicator
            if (isBuffering && !hasVideoError) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = CobaltPulse,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
            }

            // Center Paused Overlay:
            // In Loops mode, displays Mute/Unmute button directly above the center Play button (Instagram Reels style).
            // In Feed mode, displays the center Play button while keeping the bottom-left controls.
            if (!isPlaying) {
                if (loopsControlMode) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("loops_paused_controls_stack")
                    ) {
                        // Mute / Unmute Button positioned directly above the Play Button when paused
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(52.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                                .clickable { toggleMuteAction() }
                                .testTag("loops_paused_mute_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (effectiveIsMuted) {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    },
                                    contentDescription = if (effectiveIsMuted) "Unmute loop" else "Mute loop",
                                    tint = if (effectiveIsMuted) ShutterAmber else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Center Play Button below the Mute/Unmute button
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                                .clickable {
                                    haptics.performLoopPlayPauseHaptic()
                                    isPlaying = true
                                }
                                .testTag("loops_paused_play_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play video",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.Center)
                            .clickable {
                                haptics.performControlTapHaptic()
                                isPlaying = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Bottom-Left Play/Pause & Mute/Unmute Controls (ONLY in non-Loops mode)
            if (!loopsControlMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, end = 10.dp, bottom = 14.dp)
                ) {
                    Surface(
                        color = Color(0xCC0B0E14),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable {
                                haptics.performControlTapHaptic()
                                isPlaying = !isPlaying
                            }
                            .testTag("video_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause video" else "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xCC0B0E14),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { toggleMuteAction() }
                            .testTag("video_mute_toggle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (effectiveIsMuted) {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = if (effectiveIsMuted) "Unmute video" else "Mute video",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2x Speed Hold Floating Indicator (Instagram Reels style)
            if (loopsControlMode) {
                AnimatedVisibility(
                    visible = isFastForward2x,
                    enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.9f),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 74.dp)
                        .testTag("loops_2x_speed_badge")
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(
                            1.dp,
                            ShutterAmber.copy(alpha = 0.8f),
                            RoundedCornerShape(20.dp)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "2x Speed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Filled.FastForward,
                                contentDescription = "Fast forwarding at 2x speed",
                                tint = ShutterAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Playback Progress / Interactive Scrub Bar
            if (loopsControlMode) {
                val activeFraction = if (isScrubbing) scrubFraction else progressFraction
                val shownPositionMs = if (isScrubbing && durationMs > 0L) {
                    (scrubFraction * durationMs).toLong()
                } else {
                    currentPositionMs
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    AnimatedVisibility(visible = isScrubbing || (!isPlaying && durationMs > 0L)) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(bottom = 2.dp)
                                .testTag("loops_scrub_time_label")
                        ) {
                            Text(
                                text = "${formatLoopDurationMs(shownPositionMs)} / ${formatLoopDurationMs(durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Slider(
                        value = activeFraction.coerceIn(0f, 1f),
                        onValueChange = { nextFraction ->
                            isScrubbing = true
                            scrubFraction = nextFraction.coerceIn(0f, 1f)
                            progressFraction = scrubFraction
                        },
                        onValueChangeFinished = {
                            if (durationMs > 0L) {
                                val targetMs = (scrubFraction * durationMs).toLong().coerceIn(0L, durationMs)
                                currentPositionMs = targetMs
                                try {
                                    exoPlayer?.seekTo(targetMs)
                                } catch (_: Throwable) {
                                }
                            }
                            isScrubbing = false
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isScrubbing) ShutterAmber else Color.White,
                            activeTrackColor = if (isScrubbing) ShutterAmber else CobaltPulse,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .testTag("loops_scrub_slider")
                    )
                }
            } else {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = CobaltPulse,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .testTag("video_progress_indicator")
                )
            }
        }
    }
}

private fun formatLoopDurationMs(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

@Composable
fun PulseFilteredImage(
    imageRes: Int,
    imageUri: String?,
    videoUri: String? = null,
    filterPreset: FilmPreset,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showFilmBadge: Boolean = true,
    autoPlayVideo: Boolean = true,
    compactVideoPreview: Boolean = false,
    loopsControlMode: Boolean = false,
    externalIsMuted: Boolean? = null,
    onExternalToggleMute: (() -> Unit)? = null,
    brightness: Float = 0f,
    exposure: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f,
    warmth: Float = 0f,
    filmGrain: Float = 0f,
    vignette: Float = 0f,
    enablePinchZoom: Boolean = false,
    onDoubleTap: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null
) {
    var showHeartBurst by remember { mutableStateOf(false) }
    var heartBurstGeneration by remember { mutableIntStateOf(0) }
    var isFallbackLoopPaused by remember(imageUri, imageRes) { mutableStateOf(false) }
    var internalFallbackMuted by remember(imageUri, imageRes) { mutableStateOf(true) }
    val effectiveFallbackMuted = externalIsMuted ?: internalFallbackMuted
    val haptics = rememberPulseHaptics()

    // Pinch-to-Zoom with smooth spring snap-back when released
    var rawZoomScale by remember { mutableFloatStateOf(1f) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }
    var isPinching by remember { mutableStateOf(false) }

    val animatedZoomScale by animateFloatAsState(
        targetValue = if (isPinching) rawZoomScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pinch_zoom_scale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isPinching) rawOffsetX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pinch_zoom_offset_x"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isPinching) rawOffsetY else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pinch_zoom_offset_y"
    )

    val triggerDoubleTapHeart = {
        haptics.performLikeHaptic()
        showHeartBurst = true
        heartBurstGeneration++
        onDoubleTap?.invoke()
    }

    LaunchedEffect(heartBurstGeneration) {
        if (heartBurstGeneration > 0) {
            showHeartBurst = true
            delay(850)
            showHeartBurst = false
        }
    }

    val effectiveVideo = remember(videoUri, imageUri) {
        videoUri?.takeIf { it.isNotBlank() } ?: imageUri?.takeIf { isVideoUrl(it) }
    }

    val colorFilter = remember(filterPreset, brightness, exposure, contrast, saturation, warmth) {
        filterPreset.toAdjustedColorMatrix(
            exposure = exposure,
            contrast = contrast,
            warmth = warmth,
            brightness = brightness,
            saturation = saturation
        )?.let { ColorFilter.colorMatrix(it) }
    }

    val tapGestureModifier = if (effectiveVideo.isNullOrBlank() && (onDoubleTap != null || onTap != null || loopsControlMode)) {
        Modifier.pointerInput(onDoubleTap, onTap, loopsControlMode) {
            detectTapGestures(
                onDoubleTap = {
                    if (onDoubleTap != null) {
                        triggerDoubleTapHeart()
                    }
                },
                onTap = {
                    if (loopsControlMode) {
                        haptics.performLoopPlayPauseHaptic()
                        isFallbackLoopPaused = !isFallbackLoopPaused
                    } else {
                        onTap?.invoke()
                    }
                }
            )
        }
    } else {
        Modifier
    }

    val pinchGestureModifier = if (enablePinchZoom && effectiveVideo.isNullOrBlank()) {
        Modifier.pointerInput(enablePinchZoom) {
            awaitEachGesture {
                do {
                    val event = awaitPointerEvent()
                    val pressedCount = event.changes.count { it.pressed }
                    if (pressedCount >= 2) {
                        isPinching = true
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        rawZoomScale = (rawZoomScale * zoomChange).coerceIn(1f, 3.5f)
                        rawOffsetX = (rawOffsetX + panChange.x).coerceIn(-360f, 360f)
                        rawOffsetY = (rawOffsetY + panChange.y).coerceIn(-360f, 360f)
                        event.changes.forEach { it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                if (isPinching) {
                    isPinching = false
                    rawZoomScale = 1f
                    rawOffsetX = 0f
                    rawOffsetY = 0f
                }
            }
        }
    } else {
        Modifier
    }

    val zoomGraphicsModifier = if (enablePinchZoom && effectiveVideo.isNullOrBlank()) {
        Modifier.graphicsLayer {
            scaleX = animatedZoomScale
            scaleY = animatedZoomScale
            translationX = animatedOffsetX
            translationY = animatedOffsetY
        }
    } else {
        Modifier
    }

    val context = LocalContext.current
    val imageLoader = remember(context) { SupabaseClient.getImageLoader(context) }
    val resolvedImageUrl = remember(imageUri) {
        imageUri
            ?.takeIf { it.isNotBlank() && !isVideoUrl(it) }
            ?.let { SupabaseClient.resolveSupabaseMediaUrl(it, defaultBucket = "posts") }
    }
    val directLocalOrDataBitmap = rememberDecodedLocalOrDataBitmap(
        context = context,
        rawUriOrData = resolvedImageUrl ?: imageUri
    )

    Box(
        modifier = modifier
            .clipToBounds()
            .then(tapGestureModifier)
            .then(pinchGestureModifier)
    ) {
        if (!effectiveVideo.isNullOrBlank()) {
            PulseVideoPlayer(
                videoUri = effectiveVideo,
                fallbackImageRes = imageRes,
                fallbackImageUri = resolvedImageUrl ?: imageUri,
                contentDescription = contentDescription,
                autoPlay = autoPlayVideo,
                compactPreviewMode = compactVideoPreview,
                loopsControlMode = loopsControlMode,
                externalIsMuted = externalIsMuted,
                onExternalToggleMute = onExternalToggleMute,
                modifier = Modifier.fillMaxSize(),
                onTap = onTap,
                onDoubleTap = {
                    if (onDoubleTap != null) {
                        triggerDoubleTapHeart()
                    }
                }
            )
        } else if (directLocalOrDataBitmap != null) {
            Image(
                bitmap = directLocalOrDataBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .then(zoomGraphicsModifier)
            )
        } else if (!resolvedImageUrl.isNullOrBlank()) {
            val imageRequest = remember(resolvedImageUrl, context) {
                ImageRequest.Builder(context)
                    .data(resolvedImageUrl)
                    .size(720, 900)
                    .precision(Precision.INEXACT)
                    .allowRgb565(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build()
            }
            DisposableEffect(imageRequest, imageLoader) {
                val preloadDisposable: Disposable? = try {
                    imageLoader.enqueue(imageRequest)
                } catch (_: Throwable) {
                    null
                }
                onDispose {
                    try {
                        preloadDisposable?.dispose()
                    } catch (_: Throwable) {
                    }
                }
            }
            SubcomposeAsyncImage(
                model = imageRequest,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = colorFilter,
                loading = {
                    PulseSkeletonBox(
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                },
                error = {
                    Image(
                        painter = safePainterResource(imageRes),
                        contentDescription = contentDescription,
                        contentScale = contentScale,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(zoomGraphicsModifier)
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(zoomGraphicsModifier)
            )
        } else {
            Image(
                painter = safePainterResource(imageRes),
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .then(zoomGraphicsModifier)
            )
        }

        // Fine-Grain Darkroom Vignette & Film Grain Overlays
        val effectiveVignette = when {
            vignette > 0.01f -> vignette.coerceIn(0f, 1f) * 0.65f
            filterPreset != FilmPreset.NATURAL && effectiveVideo.isNullOrBlank() -> 0.22f
            else -> 0f
        }
        if (effectiveVignette > 0f && effectiveVideo.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = effectiveVignette)
                            )
                        )
                    )
            )
        }

        if (filmGrain > 0.01f && effectiveVideo.isNullOrBlank()) {
            val grainAlpha = (filmGrain.coerceIn(0f, 1f) * 0.22f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 18.dp.toPx().coerceAtLeast(8f)
                var y = 0f
                var rowIdx = 0
                while (y < size.height) {
                    var x = if (rowIdx % 2 == 0) 0f else step * 0.5f
                    var colIdx = 0
                    while (x < size.width) {
                        val hash = ((rowIdx * 37 + colIdx * 101) and 0xFF) / 255f
                        val c = if (hash > 0.5f) Color.White else Color.Black
                        drawCircle(
                            color = c.copy(alpha = grainAlpha * (0.4f + hash * 0.6f)),
                            radius = 1.3f,
                            center = Offset(x, y)
                        )
                        x += step
                        colIdx++
                    }
                    y += step
                    rowIdx++
                }
            }
        }

        if (isPinching && animatedZoomScale > 1.04f) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = String.format("%.1fx ZOOM", animatedZoomScale),
                    style = MaterialTheme.typography.labelSmall,
                    color = ShutterAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (effectiveVideo.isNullOrBlank() && loopsControlMode && isFallbackLoopPaused) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("loops_paused_controls_stack")
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable {
                            haptics.performLoopMuteToggleHaptic()
                            if (onExternalToggleMute != null) {
                                onExternalToggleMute()
                            } else {
                                internalFallbackMuted = !internalFallbackMuted
                            }
                        }
                        .testTag("loops_paused_mute_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (effectiveFallbackMuted) {
                                Icons.AutoMirrored.Filled.VolumeOff
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = if (effectiveFallbackMuted) "Unmute loop" else "Mute loop",
                            tint = if (effectiveFallbackMuted) ShutterAmber else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable {
                            haptics.performLoopPlayPauseHaptic()
                            isFallbackLoopPaused = false
                        }
                        .testTag("loops_paused_play_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        if (showFilmBadge) {
            Surface(
                color = Color(0xCC0B0E14),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (filterPreset == FilmPreset.NATURAL) CobaltPulse else ShutterAmber
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${filterPreset.code} · ${filterPreset.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showHeartBurst,
            enter = scaleIn(
                initialScale = 0.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(120)),
            exit = scaleOut(targetScale = 1.25f, animationSpec = tween(220)) + fadeOut(animationSpec = tween(220)),
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("double_tap_heart_overlay")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(116.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Liked",
                    tint = Color.White.copy(alpha = 0.32f),
                    modifier = Modifier.size(100.dp)
                )
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = SignalCrimson,
                    modifier = Modifier.size(88.dp)
                )
            }
        }
    }
}

@Composable
fun StoryAvatarRing(
    avatarRes: Int,
    avatarUrl: String? = null,
    contentDescription: String,
    size: Dp = 64.dp,
    isUnviewed: Boolean = true,
    showAddBadge: Boolean = false,
    onClick: () -> Unit
) {
    val ringBrush = remember(isUnviewed) {
        if (isUnviewed) {
            Brush.linearGradient(
                colors = listOf(CobaltPulse, ShutterAmber, SignalCrimson)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFF475569), Color(0xFF334155))
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size + 8.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = ringBrush,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        PulseAvatarImage(
            avatarRes = avatarRes,
            avatarUrl = avatarUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size - 2.dp)
                .clip(CircleShape)
        )
        if (showAddBadge) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(CobaltPulse)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PostContactCard(
    post: PostEntity,
    onLikeToggle: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onOpenComments: () -> Unit,
    onSharePost: () -> Unit,
    onCreatorClick: () -> Unit,
    onMoreOptions: () -> Unit,
    onTagClick: (String) -> Unit = {},
    onLocationClick: (String) -> Unit = {},
    comments: List<CommentEntity> = emptyList(),
    onAddComment: ((String) -> Unit)? = null,
    moodboards: List<String> = listOf("Favorites", "Visual Inspiration", "Architecture", "Monochrome"),
    onAssignToCollection: ((String) -> Unit)? = null,
    autoPlayVideo: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberPulseHaptics()
    var showCollectionPicker by remember(post.id) { mutableStateOf(false) }
    var inlineCommentDraft by remember(post.id) { mutableStateOf("") }

    val likeScale by animateFloatAsState(
        targetValue = if (post.isLiked) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "like_scale"
    )
    val bookmarkScale by animateFloatAsState(
        targetValue = if (post.isBookmarked) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bookmark_scale"
    )

    val carouselFrames = remember(post.id, post.imageUri, post.imageRes, post.carouselImageUrisCsv, post.carouselImageResCsv) {
        post.carouselFrames
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .testTag("post_card_${post.id}")
    ) {
        // Author Details Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            PulseAvatarImage(
                avatarRes = post.authorAvatarRes,
                avatarUrl = post.authorAvatarUrl,
                contentDescription = post.authorDisplayName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onCreatorClick)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onCreatorClick)
                ) {
                    Text(
                        text = post.authorDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "@${post.authorUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (post.authorVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified creator",
                            tint = CobaltPulse,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (post.isLoop || post.hasVideo) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (post.hasVideo) "Video" else "Loop",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (carouselFrames.size > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = ShutterAmber.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${carouselFrames.size} Frames",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShutterAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (post.location.isNotBlank() || post.cameraSpec.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(enabled = post.location.isNotBlank()) {
                                haptics.performControlTapHaptic()
                                onLocationClick(post.location)
                            }
                            .testTag("post_location_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = ShutterAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = listOf(post.location, post.cameraSpec).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(
                onClick = onMoreOptions,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("post_more_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "Post options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Full-Bleed Contact-Sheet Photograph, Multi-Photo/Video Carousel, or Inline Video Player
        val mediaContent: @Composable () -> Unit = {
            if (carouselFrames.size > 1) {
                val pagerState = rememberPagerState(pageCount = { carouselFrames.size })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(post.aspectRatio.ratio)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                        .testTag("post_media_${post.id}")
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("post_carousel_pager_${post.id}")
                    ) { page ->
                        val (frameRes, frameUri) = carouselFrames[page]
                        val frameVideoUri = frameUri?.takeIf { isVideoUrl(it) }
                        val frameImageUri = frameUri?.takeIf { !isVideoUrl(it) }
                        PulseFilteredImage(
                            imageRes = if (frameUri.isNullOrBlank()) frameRes else 0,
                            imageUri = frameImageUri,
                            videoUri = frameVideoUri,
                            filterPreset = post.filterPreset,
                            contentDescription = "${post.caption} (${page + 1}/${carouselFrames.size})",
                            autoPlayVideo = autoPlayVideo && (pagerState.currentPage == page),
                            brightness = post.brightness,
                            exposure = post.exposure,
                            contrast = post.contrast,
                            saturation = post.saturation,
                            warmth = post.warmth,
                            filmGrain = post.filmGrain,
                            vignette = post.vignette,
                            enablePinchZoom = frameVideoUri == null,
                            onDoubleTap = onDoubleTapLike,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Page Counter Pill (1/4)
                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .testTag("carousel_counter_pill_${post.id}")
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${carouselFrames.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Dot Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("carousel_dots_${post.id}")
                    ) {
                        repeat(carouselFrames.size) { idx ->
                            val isSelected = idx == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ShutterAmber else Color.White.copy(alpha = 0.45f))
                            )
                        }
                    }
                }
            } else {
                PulseFilteredImage(
                    imageRes = post.imageRes,
                    imageUri = post.imageUri,
                    videoUri = post.effectiveVideoUri,
                    filterPreset = post.filterPreset,
                    contentDescription = post.caption,
                    autoPlayVideo = autoPlayVideo,
                    brightness = post.brightness,
                    exposure = post.exposure,
                    contrast = post.contrast,
                    saturation = post.saturation,
                    warmth = post.warmth,
                    filmGrain = post.filmGrain,
                    vignette = post.vignette,
                    enablePinchZoom = !post.hasVideo,
                    onDoubleTap = onDoubleTapLike,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(post.aspectRatio.ratio)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        .testTag("post_media_${post.id}")
                )
            }
        }

        if (post.hasExifFrameBorder) {
            val isDarkroomBorder = post.exifFrameStyle.equals("DARKROOM", ignoreCase = true)
            val borderBg = if (isDarkroomBorder) Color(0xFF0E131B) else Color(0xFFF6F3EC)
            val borderFg = if (isDarkroomBorder) Color(0xFFE2E8F0) else Color(0xFF1E293B)
            val borderAccent = if (isDarkroomBorder) ShutterAmber else CobaltPulse
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(borderBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline)
                    .testTag("exif_border_stamp_${post.id}")
            ) {
                mediaContent()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = post.cameraSpec.ifBlank { "35mm · f/1.8 · ISO 400" },
                        style = MaterialTheme.typography.labelSmall,
                        color = borderFg,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${post.filterPreset.code} · ${post.filterPreset.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = borderAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            mediaContent()
        }

        // Interactive Action Buttons Row (Like, Comment, Share, Bookmark + Dedicated Collection Badge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = {
                    haptics.performLikeHaptic()
                    onLikeToggle()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("like_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (post.isLiked) "Unlike post" else "Like post",
                    tint = if (post.isLiked) SignalCrimson else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.scale(likeScale)
                )
            }
            Text(
                text = formatMetricCount(post.likeCount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (post.isLiked) FontWeight.Bold else FontWeight.Medium,
                color = if (post.isLiked) SignalCrimson else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable {
                        haptics.performLikeHaptic()
                        onLikeToggle()
                    }
                    .testTag("like_count_${post.id}")
            )
            if (post.isLiked) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = SignalCrimson.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("liked_badge_${post.id}")
                ) {
                    Text(
                        text = "Liked",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalCrimson,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onOpenComments,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("comment_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = post.commentCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable(onClick = onOpenComments)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    haptics.performSendHaptic()
                    sharePostExternally(context, post)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("share_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share post externally",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Share",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable {
                        haptics.performSendHaptic()
                        sharePostExternally(context, post)
                    }
                    .testTag("share_label_${post.id}")
            )

            Spacer(modifier = Modifier.weight(1f))

            if (post.isBookmarked) {
                val activeCollectionLabel = post.moodboardName.let {
                    if (it.isBlank() || it == "All Saved") "Favorites" else it
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clickable {
                            haptics.performBookmarkHaptic()
                            showCollectionPicker = !showCollectionPicker
                        }
                        .testTag("bookmark_collection_chip_${post.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CollectionsBookmark,
                            contentDescription = null,
                            tint = ShutterAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeCollectionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ShutterAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    haptics.performBookmarkHaptic()
                    val willBeBookmarked = !post.isBookmarked
                    onBookmarkToggle()
                    showCollectionPicker = willBeBookmarked && onAssignToCollection != null
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("bookmark_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (post.isBookmarked) "Remove from saved collection" else "Save to collection",
                    tint = if (post.isBookmarked) ShutterAmber else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.scale(bookmarkScale)
                )
            }
        }

        // Inline Dedicated Collection Picker Row when a post is bookmarked in the Feed
        AnimatedVisibility(
            visible = post.isBookmarked && showCollectionPicker && onAssignToCollection != null
        ) {
            val selectableCollections = remember(moodboards) {
                val list = moodboards.filter { it.isNotBlank() && it != "All Saved" }
                if (list.isEmpty()) listOf("Favorites", "Visual Inspiration", "Architecture", "Monochrome") else list
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("feed_post_collection_selector_${post.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CollectionsBookmark,
                            contentDescription = null,
                            tint = ShutterAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save to dedicated collection",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = CobaltPulse,
                        modifier = Modifier.clickable {
                            haptics.performControlTapHaptic()
                            showCollectionPicker = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectableCollections, key = { it }) { collectionName ->
                        val activeBoard = post.moodboardName.let {
                            if (it.isBlank() || it == "All Saved") "Favorites" else it
                        }
                        val isSelected = activeBoard.equals(collectionName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptics.performBookmarkHaptic()
                                onAssignToCollection?.invoke(collectionName)
                                showCollectionPicker = false
                            },
                            label = { Text(collectionName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CobaltPulse,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("save_collection_chip_${post.id}_${collectionName.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // Caption, Tags & Timestamp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(post.authorUsername)
                        append("  ")
                    }
                    append(post.caption)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(post.tags, key = { it }) { tag ->
                        val cleanTag = tag.trim().removePrefix("#")
                        Text(
                            text = "#$cleanTag",
                            style = MaterialTheme.typography.bodySmall,
                            color = CobaltPulse,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable {
                                    haptics.performControlTapHaptic()
                                    onTagClick("#$cleanTag")
                                }
                                .testTag("post_tag_${post.id}_$cleanTag")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            val totalCommentsCount = maxOf(post.commentCount, comments.size)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (totalCommentsCount > 0) {
                        "View all $totalCommentsCount comments"
                    } else {
                        "Add a comment…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(onClick = onOpenComments)
                        .testTag("view_all_comments_${post.id}")
                )
                Text(
                    text = post.createdAtLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Inline Comment List Preview (shows latest comments right under the photo card)
            if (comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_inline_comments_${post.id}")
                ) {
                    comments.takeLast(3).forEach { commentItem ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenComments)
                                .testTag("post_inline_comment_item_${commentItem.id}")
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                        append(commentItem.authorUsername)
                                        append("  ")
                                    }
                                    append(commentItem.text)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = commentItem.timeAgo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Inline Quick Comment Input Field & Post Button
            if (onAddComment != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_inline_comment_row_${post.id}")
                ) {
                    OutlinedTextField(
                        value = inlineCommentDraft,
                        onValueChange = { inlineCommentDraft = it },
                        placeholder = {
                            Text(
                                text = "Write a comment…",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("post_inline_comment_input_${post.id}")
                    )
                    Button(
                        onClick = {
                            val trimmed = inlineCommentDraft.trim()
                            if (trimmed.isNotEmpty()) {
                                haptics.performSendHaptic()
                                onAddComment(trimmed)
                                inlineCommentDraft = ""
                            }
                        },
                        enabled = inlineCommentDraft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("post_inline_comment_submit_${post.id}")
                    ) {
                        Text(
                            text = "Post",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: PostEntity,
    comments: List<CommentEntity>,
    onDismiss: () -> Unit,
    onAddComment: (text: String, parentCommentId: String?, replyToUsername: String?) -> Unit,
    onToggleLikeComment: (CommentEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentDraft by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<CommentEntity?>(null) }
    val haptics = rememberPulseHaptics()
    val quickReactions = remember { listOf("🔥", "🎞️", "👏", "❤️", "✨") }

    val submitCommentAction = {
        val trimmed = commentDraft.trim()
        if (trimmed.isNotEmpty()) {
            haptics.performSendHaptic()
            val targetReply = replyingToComment
            val finalText = if (targetReply != null && !trimmed.startsWith("@${targetReply.authorUsername}")) {
                "@${targetReply.authorUsername} $trimmed"
            } else {
                trimmed
            }
            onAddComment(finalText, targetReply?.id, targetReply?.authorUsername)
            commentDraft = ""
            replyingToComment = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("comments_bottom_sheet")
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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Comments (${comments.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@${post.authorUsername} · ${post.filterPreset.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_comments_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (post.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        PulseAvatarImage(
                            avatarRes = post.authorAvatarRes,
                            avatarUrl = post.authorAvatarUrl,
                            contentDescription = post.authorDisplayName,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                    append("@${post.authorUsername}  ")
                                }
                                append(post.caption)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 360.dp)
                    .testTag("comments_list")
            ) {
                if (comments.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No comments yet. Start the conversation below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        val isReply = !comment.parentCommentId.isNullOrBlank() || !comment.replyToUsername.isNullOrBlank()
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = if (isReply) 28.dp else 0.dp)
                                .testTag("comment_item_${comment.id}")
                        ) {
                            PulseAvatarImage(
                                avatarRes = comment.authorAvatarRes,
                                avatarUrl = comment.authorAvatarUrl,
                                contentDescription = comment.authorDisplayName,
                                modifier = Modifier
                                    .size(if (isReply) 28.dp else 34.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.authorUsername,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = comment.timeAgo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        val words = comment.text.split(" ")
                                        words.forEachIndexed { index, word ->
                                            if (word.startsWith("@") && word.length > 1) {
                                                withStyle(
                                                    SpanStyle(
                                                        color = CobaltPulse,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            } else {
                                                append(word)
                                            }
                                            if (index < words.lastIndex) append(" ")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable {
                                                haptics.performControlTapHaptic()
                                                replyingToComment = comment
                                                if (!commentDraft.startsWith("@${comment.authorUsername}")) {
                                                    commentDraft = "@${comment.authorUsername} "
                                                }
                                            }
                                            .testTag("reply_comment_button_${comment.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                            contentDescription = "Reply",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Reply",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        haptics.performLikeHaptic()
                                        onToggleLikeComment(comment)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Like comment",
                                        tint = if (comment.isLiked) SignalCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (comment.likeCount > 0) {
                                    Text(
                                        text = comment.likeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Active Reply Banner
            val activeReply = replyingToComment
            if (activeReply != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("replying_to_banner")
                ) {
                    Text(
                        text = "Replying to @${activeReply.authorUsername}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CobaltPulse,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            replyingToComment = null
                            if (commentDraft.trim() == "@${activeReply.authorUsername}") {
                                commentDraft = ""
                            }
                        }
                    )
                }
            }

            // Quick Emoji Reaction Chips above input bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("comment_emoji_chips_row")
            ) {
                quickReactions.forEach { emoji ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .clickable {
                                haptics.performControlTapHaptic()
                                commentDraft = if (commentDraft.isBlank()) emoji else "$commentDraft $emoji"
                            }
                            .testTag("comment_emoji_chip_$emoji")
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    placeholder = { Text("Add a comment for @${post.authorUsername}…") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field")
                )
                Button(
                    onClick = submitCommentAction,
                    enabled = commentDraft.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CobaltPulse,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("comment_submit_button")
                ) {
                    Text(
                        text = "Post",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostBottomSheet(
    post: PostEntity,
    creators: List<CreatorProfileEntity>,
    onDismiss: () -> Unit,
    onSendToCreator: (CreatorProfileEntity, String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val haptics = rememberPulseHaptics()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Share post",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp)
            ) {
                PulseFilteredImage(
                    imageRes = post.imageRes,
                    imageUri = post.imageUri,
                    filterPreset = post.filterPreset,
                    contentDescription = post.caption,
                    showFilmBadge = false,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${post.authorUsername}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    haptics.performSendHaptic()
                    sharePostExternally(context, post)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("external_system_share_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = CobaltPulse,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share externally via Android system sheet")
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Add a personal message…") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 260.dp)
            ) {
                items(creators, key = { it.id }) { creator ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        PulseAvatarImage(
                            avatarRes = creator.avatarRes,
                            avatarUrl = creator.avatarUrl,
                            contentDescription = creator.displayName,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = creator.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "@${creator.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                haptics.performSendHaptic()
                                onSendToCreator(creator, noteText)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: PostEntity,
    isOwnPost: Boolean,
    moodboards: List<String> = listOf("Visual Inspiration", "Architecture", "Monochrome"),
    onDismiss: () -> Unit,
    onSaveToMoodboard: (String) -> Unit = {},
    onPinToHighlight: (String) -> Unit = {},
    onTogglePinOnProfile: () -> Unit = {},
    onDeleteOwnPost: () -> Unit,
    onReportPost: (String) -> Unit,
    onBlockCreator: () -> Unit
) {
    val reportReasons = listOf(
        "Inappropriate or harmful content",
        "Copyright or uncredited photography",
        "Spam or misleading tags"
    )
    var highlightName by remember { mutableStateOf("Exhibitions") }
    val haptics = rememberPulseHaptics()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Post options",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "@${post.authorUsername}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isOwnPost) {
                Surface(
                    color = CobaltPulse.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.performBookmarkHaptic()
                            onTogglePinOnProfile()
                            onDismiss()
                        }
                        .testTag("pin_profile_post_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pin to Profile",
                            tint = ShutterAmber
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (post.isPinnedOnProfile) "Unpin from Profile Grid" else "Pin to top of Profile (Max 3)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Highlight this frame at the top of your 3-column studio grid",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = SignalCrimson.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDeleteOwnPost()
                            onDismiss()
                        }
                        .testTag("delete_post_option_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete post",
                            tint = SignalCrimson
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Delete post",
                                style = MaterialTheme.typography.titleMedium,
                                color = SignalCrimson,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Remove this post permanently",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Save to Named Moodboard Collection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CollectionsBookmark,
                    contentDescription = null,
                    tint = ShutterAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save to Collection",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(moodboards.filter { it != "All Saved" }, key = { it }) { board ->
                    val isCurrent = post.isBookmarked && post.moodboardName.equals(board, ignoreCase = true)
                    FilterChip(
                        selected = isCurrent,
                        onClick = {
                            haptics.performBookmarkHaptic()
                            onSaveToMoodboard(board)
                            onDismiss()
                        },
                        label = { Text(board) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CobaltPulse,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (isOwnPost) {
                Spacer(modifier = Modifier.height(14.dp))

                // Pin to Profile Story Highlight
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = CobaltPulse,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pin to Story Highlights",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = highlightName,
                        onValueChange = { highlightName = it },
                        label = { Text("Highlight title") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onPinToHighlight(highlightName)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pin")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Report post",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                reportReasons.forEach { reason ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onReportPost(reason)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = null,
                                tint = ShutterAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = SignalCrimson.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onBlockCreator()
                            onDismiss()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = SignalCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Block @${post.authorUsername}",
                            style = MaterialTheme.typography.titleSmall,
                            color = SignalCrimson
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfileBottomSheet(
    creator: CreatorProfileEntity,
    creatorPosts: List<PostEntity>,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessageCreator: () -> Unit,
    onBlockCreator: () -> Unit,
    onSelectPost: (PostEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                PulseAvatarImage(
                    avatarRes = creator.avatarRes,
                    avatarUrl = creator.avatarUrl,
                    contentDescription = creator.displayName,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(2.dp, CobaltPulse, CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = creator.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (creator.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = CobaltPulse,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${creator.username} · ${creator.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = creator.roleTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = ShutterAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = creator.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 10.dp)
            ) {
                ProfileStatColumn(label = "Posts", value = creatorPosts.size.toString())
                ProfileStatColumn(label = "Followers", value = formatMetricCount(creator.followersCount))
                ProfileStatColumn(label = "Following", value = creator.followingCount.toString())
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onToggleFollow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (creator.isFollowing) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            CobaltPulse
                        },
                        contentColor = if (creator.isFollowing) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Color.White
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (creator.isFollowing) "Following" else "Follow creator")
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onMessageCreator()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Direct message")
                }
                TextButton(
                    onClick = {
                        onBlockCreator()
                        onDismiss()
                    }
                ) {
                    Text("Block", color = SignalCrimson)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Posts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 300.dp)
            ) {
                items(creatorPosts, key = { it.id }) { post ->
                    PulseFilteredImage(
                        imageRes = post.imageRes,
                        imageUri = post.imageUri,
                        videoUri = post.effectiveVideoUri,
                        filterPreset = post.filterPreset,
                        contentDescription = post.caption,
                        showFilmBadge = false,
                        autoPlayVideo = false,
                        compactVideoPreview = true,
                        onTap = {
                            onDismiss()
                            onSelectPost(post)
                        },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProfileStatColumn(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatMetricCount(count: Int): String {
    return when {
        count >= 10_000 -> String.format("%.1fk", count / 1000f)
        count >= 1_000 -> String.format("%,d", count)
        else -> count.toString()
    }
}
