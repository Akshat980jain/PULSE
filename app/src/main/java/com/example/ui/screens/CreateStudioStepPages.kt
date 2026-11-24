package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.FilmPreset
import com.example.data.model.MAX_CAROUSEL_ITEMS
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.StudioDraftEntity
import com.example.data.model.StudioPublishTarget
import com.example.data.model.isVideoUrl
import com.example.ui.components.PulseFilteredImage
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.CobaltPulseSoft
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson

enum class StudioCreationStep(
    val stepNumber: Int,
    val shortLabel: String,
    val title: String,
    val subtitle: String
) {
    MEDIA(
        stepNumber = 1,
        shortLabel = "Media",
        title = "Select Media",
        subtitle = "Capture or choose your own frames"
    ),
    EDIT(
        stepNumber = 2,
        shortLabel = "Edit",
        title = "Darkroom Edit",
        subtitle = "Grade, tune & frame live preview"
    ),
    DETAILS(
        stepNumber = 3,
        shortLabel = "Publish",
        title = "Details & Publish",
        subtitle = "Verify final preview & share"
    )
}

enum class DarkroomToolTab(val label: String, val icon: ImageVector) {
    PRESETS("Presets", Icons.Filled.PhotoFilter),
    ADJUST("Adjust", Icons.Filled.Tune),
    FRAME("Frame", Icons.Filled.FilterFrames)
}

@Composable
internal fun StudioStepHeaderBar(
    currentStep: StudioCreationStep,
    hasMedia: Boolean,
    canSaveDraft: Boolean = false,
    draftStatusLabel: String? = null,
    onSaveDraftClick: (() -> Unit)? = null,
    onStepSelected: (StudioCreationStep) -> Unit,
    onBackClick: (() -> Unit)?,
    onBackToFeed: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (currentStep != StudioCreationStep.MEDIA && onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("studio_step_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous studio step",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Column {
                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("studio_step_title")
                    )
                    Text(
                        text = if (!draftStatusLabel.isNullOrBlank()) {
                            "${currentStep.subtitle} · $draftStatusLabel"
                        } else {
                            currentStep.subtitle
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!draftStatusLabel.isNullOrBlank()) {
                            ShutterAmber
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("studio_step_subtitle")
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canSaveDraft && onSaveDraftClick != null) {
                    OutlinedButton(
                        onClick = onSaveDraftClick,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, ShutterAmber.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ShutterAmber.copy(alpha = 0.16f),
                            contentColor = ShutterAmber
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("studio_save_draft_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkBorder,
                            contentDescription = "Save draft",
                            tint = ShutterAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save Draft",
                            style = MaterialTheme.typography.labelMedium,
                            color = ShutterAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (onBackToFeed != null) {
                    OutlinedButton(
                        onClick = onBackToFeed,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CobaltPulseSoft.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = CobaltPulseSoft
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("studio_back_to_feed_button")
                    ) {
                        Text(
                            text = "Feed →",
                            style = MaterialTheme.typography.labelMedium,
                            color = CobaltPulseSoft,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3-Step Connected Progress Bar
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_stepper_bar")
        ) {
            StudioCreationStep.entries.forEach { step ->
                val isCurrent = step == currentStep
                val isCompleted = step.stepNumber < currentStep.stepNumber
                val canJump = step == StudioCreationStep.MEDIA || hasMedia
                Surface(
                    color = when {
                        isCurrent -> CobaltPulse.copy(alpha = 0.24f)
                        isCompleted -> ShutterAmber.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = when {
                                isCurrent -> CobaltPulseSoft
                                isCompleted -> ShutterAmber
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (canJump && !isCurrent) {
                                Modifier.clickable { onStepSelected(step) }
                            } else {
                                Modifier
                            }
                        )
                        .testTag("studio_step_pill_${step.stepNumber}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> CobaltPulse
                                        isCompleted -> ShutterAmber
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Text(
                                    text = step.stepNumber.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = step.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.onSurface
                                isCompleted -> ShutterAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StudioLivePreviewStage(
    activeMediaUris: List<String>,
    selectedPhotoUri: String?,
    selectedVideoUri: String?,
    aspectRatio: PhotoAspectRatio,
    selectedFilter: FilmPreset,
    brightness: Float,
    exposure: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    filmGrain: Float,
    vignette: Float,
    hasExifFrameBorder: Boolean,
    exifFrameStyle: String,
    cameraSpec: String,
    compactPreview: Boolean = false,
    maxPreviewHeight: Dp? = null,
    showFilmBadge: Boolean = true,
    modifier: Modifier = Modifier
) {
    val previewModifier = when {
        compactPreview -> Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 200.dp)
            .clip(RoundedCornerShape(10.dp))
        maxPreviewHeight != null -> Modifier
            .heightIn(min = 170.dp, max = maxPreviewHeight)
            .aspectRatio(aspectRatio.ratio, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(10.dp))
        else -> Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.ratio)
            .clip(RoundedCornerShape(10.dp))
    }

    val mediaContent: @Composable () -> Unit = {
        if (activeMediaUris.size > 1) {
            val pagerState = rememberPagerState(pageCount = { activeMediaUris.size })
            Box(
                modifier = previewModifier.testTag("studio_carousel_preview_pager")
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val frameUri = activeMediaUris[page]
                    val frameVideoUri = frameUri.takeIf { isVideoUrl(it) }
                    val frameImageUri = frameUri.takeIf { !isVideoUrl(it) }
                    PulseFilteredImage(
                        imageRes = 0,
                        imageUri = frameImageUri,
                        videoUri = frameVideoUri,
                        filterPreset = selectedFilter,
                        contentDescription = "Carousel frame ${page + 1}",
                        showFilmBadge = showFilmBadge,
                        autoPlayVideo = pagerState.currentPage == page && !compactPreview,
                        brightness = brightness,
                        exposure = exposure,
                        contrast = contrast,
                        saturation = saturation,
                        warmth = warmth,
                        filmGrain = filmGrain,
                        vignette = vignette,
                        enablePinchZoom = frameVideoUri == null && !compactPreview,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${activeMediaUris.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            val singleUri = activeMediaUris.firstOrNull()
            val resolvedVideoUri = selectedVideoUri ?: singleUri?.takeIf { isVideoUrl(it) }
            val resolvedPhotoUri = selectedPhotoUri ?: singleUri?.takeIf { !isVideoUrl(it) }
            PulseFilteredImage(
                imageRes = 0,
                imageUri = resolvedPhotoUri,
                videoUri = resolvedVideoUri,
                filterPreset = selectedFilter,
                contentDescription = "Darkroom live preview",
                showFilmBadge = showFilmBadge,
                autoPlayVideo = !compactPreview,
                brightness = brightness,
                exposure = exposure,
                contrast = contrast,
                saturation = saturation,
                warmth = warmth,
                filmGrain = filmGrain,
                vignette = vignette,
                enablePinchZoom = resolvedVideoUri == null && !compactPreview,
                modifier = previewModifier
            )
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (hasExifFrameBorder) {
            val isDarkroomBorder = exifFrameStyle == "DARKROOM"
            val borderBg = if (isDarkroomBorder) Color(0xFF0E131B) else Color(0xFFF6F3EC)
            val borderFg = if (isDarkroomBorder) Color(0xFFE2E8F0) else Color(0xFF1E293B)
            val borderAccent = if (isDarkroomBorder) ShutterAmber else CobaltPulse
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(borderBg)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(10.dp)
                    .testTag("studio_exif_frame_preview")
            ) {
                mediaContent()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cameraSpec.ifBlank { "Pulse Darkroom" },
                        style = MaterialTheme.typography.labelSmall,
                        color = borderFg,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedFilter.code} · ${selectedFilter.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = borderAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(6.dp)
            ) {
                mediaContent()
            }
        }
    }
}

@Composable
internal fun StudioStep1MediaPage(
    selectedTarget: StudioPublishTarget,
    onSelectTarget: (StudioPublishTarget) -> Unit,
    savedDrafts: List<StudioDraftEntity> = emptyList(),
    activeDraftId: String? = null,
    onResumeDraft: (StudioDraftEntity) -> Unit = {},
    onDeleteDraft: (String) -> Unit = {},
    isLiveCameraOpen: Boolean,
    hasCameraPermission: Boolean,
    permissionStatusMessage: String?,
    useFrontCamera: Boolean,
    isRecordingLoopClip: Boolean,
    recordingSeconds: Int,
    selectedFilter: FilmPreset,
    cameraSpec: String,
    hasMedia: Boolean,
    isStagingMedia: Boolean,
    activeMediaUris: List<String>,
    selectedPhotoUri: String?,
    selectedVideoUri: String?,
    carouselUris: List<String>,
    selectedAspectRatio: PhotoAspectRatio,
    showVideoUrlField: Boolean,
    onToggleVideoUrlField: () -> Unit,
    videoUrlDraft: String,
    onVideoUrlDraftChange: (String) -> Unit,
    onLoadVideoUrl: () -> Unit,
    onOpenLiveCamera: () -> Unit,
    onCloseLiveCamera: () -> Unit,
    onSnapPhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onPickCarousel: () -> Unit,
    onRequestPermission: () -> Unit,
    onFlipLens: () -> Unit,
    onPhotoCaptured: (String?) -> Unit,
    onToggleRecordLoop: () -> Unit,
    onRemoveCarouselItem: (Int) -> Unit,
    onClearMedia: () -> Unit,
    onNextToEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("studio_page_media")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Compact Target Switcher (Feed Post / 24h Story / Pulse Loop)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_target_switcher")
            ) {
                StudioPublishTarget.entries.forEach { target ->
                    val isSelected = target == selectedTarget
                    Surface(
                        color = if (isSelected) CobaltPulse else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CobaltPulseSoft else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTarget(target) }
                            .testTag("studio_target_${target.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = target.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (permissionStatusMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = SignalCrimson.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SignalCrimson.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("camera_permission_status_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = permissionStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onRequestPermission,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Grant",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShutterAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (savedDrafts.isNotEmpty() && !isLiveCameraOpen) {
                Spacer(modifier = Modifier.height(10.dp))
                StudioDraftsTray(
                    drafts = savedDrafts,
                    activeDraftId = activeDraftId,
                    onResumeDraft = onResumeDraft,
                    onDeleteDraft = onDeleteDraft
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isLiveCameraOpen -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Live Camera Viewfinder",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = onCloseLiveCamera,
                            modifier = Modifier.testTag("studio_close_live_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = ShutterAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Close Camera", color = ShutterAmber, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    BuiltInPhoneCameraViewfinderCard(
                        hasCameraPermission = hasCameraPermission,
                        useFrontCamera = useFrontCamera,
                        isRecordingLoopClip = isRecordingLoopClip,
                        recordingSeconds = recordingSeconds,
                        selectedFilter = selectedFilter,
                        cameraSpec = cameraSpec,
                        onRequestPermission = onRequestPermission,
                        onFlipLens = onFlipLens,
                        onLaunchSystemPhotoCamera = onSnapPhoto,
                        onLaunchSystemVideoCamera = onRecordVideo,
                        onPhotoCaptured = onPhotoCaptured,
                        onToggleRecordLoop = onToggleRecordLoop
                    )
                }

                hasMedia -> {
                    // Page 1 Live Selection Preview (Real User Media Only)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_media_preview_stage")
                    ) {
                        Surface(
                            color = CobaltPulse.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, CobaltPulse.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .testTag("custom_device_media_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when {
                                        activeMediaUris.size > 1 -> {
                                            val videoCount = activeMediaUris.count { isVideoUrl(it) }
                                            val photoCount = activeMediaUris.size - videoCount
                                            "${activeMediaUris.size}/$MAX_CAROUSEL_ITEMS frames ready ($photoCount photo, $videoCount video)"
                                        }
                                        !selectedVideoUri.isNullOrBlank() -> "Video clip ready · Preview below"
                                        else -> "Photo ready · Tap Next to open Darkroom Edit"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = onClearMedia,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("clear_device_media_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Clear selected media",
                                        tint = SignalCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SignalCrimson,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        StudioLivePreviewStage(
                            activeMediaUris = activeMediaUris,
                            selectedPhotoUri = selectedPhotoUri,
                            selectedVideoUri = selectedVideoUri,
                            aspectRatio = selectedAspectRatio,
                            selectedFilter = selectedFilter,
                            brightness = 0f,
                            exposure = 0f,
                            contrast = 1f,
                            saturation = 1f,
                            warmth = 0f,
                            filmGrain = 0f,
                            vignette = 0f,
                            hasExifFrameBorder = false,
                            exifFrameStyle = "GALLERY",
                            cameraSpec = cameraSpec,
                            compactPreview = false,
                            showFilmBadge = false,
                            modifier = Modifier.testTag("studio_page1_live_preview")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Carousel Frame Strip (Add/Remove up to 10 mixed items)
                        if (carouselUris.isNotEmpty()) {
                            Text(
                                text = "Selected frames (${carouselUris.size}/$MAX_CAROUSEL_ITEMS)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_carousel_strip")
                            ) {
                                itemsIndexed(carouselUris, key = { idx, uri -> "${idx}_$uri" }) { idx, frameUri ->
                                    val frameIsVideo = isVideoUrl(frameUri)
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.5.dp, ShutterAmber, RoundedCornerShape(10.dp))
                                            .testTag("studio_carousel_item_$idx")
                                    ) {
                                        if (frameIsVideo) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF121824))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Videocam,
                                                    contentDescription = "Carousel video ${idx + 1}",
                                                    tint = ShutterAmber,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            PulseFilteredImage(
                                                imageRes = 0,
                                                imageUri = frameUri,
                                                videoUri = null,
                                                filterPreset = FilmPreset.NATURAL,
                                                contentDescription = "Carousel item ${idx + 1}",
                                                showFilmBadge = false,
                                                autoPlayVideo = false,
                                                compactVideoPreview = true,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.72f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = if (frameIsVideo) "${idx + 1}·VID" else "${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.78f),
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(20.dp)
                                                .clickable { onRemoveCarouselItem(idx) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove frame ${idx + 1}",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (carouselUris.size < MAX_CAROUSEL_ITEMS) {
                                    item(key = "add_more_carousel_frames") {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(1.dp, CobaltPulse, RoundedCornerShape(10.dp))
                                                .clickable(onClick = onPickCarousel)
                                                .testTag("studio_add_carousel_frame_button")
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Filled.Add,
                                                    contentDescription = "Add more media",
                                                    tint = CobaltPulse,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "${carouselUris.size}/$MAX_CAROUSEL_ITEMS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CobaltPulse
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Compact Add/Replace Action Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onOpenLiveCamera,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("studio_replace_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shoot New", style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = onPickCarousel,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("studio_add_more_gallery_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Collections,
                                    contentDescription = null,
                                    tint = ShutterAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add / Replace", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                else -> {
                    // Clean Empty Stage with Roomier Camera & Gallery Cards (Zero Sample Data)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_empty_media_stage")
                    ) {
                        // Crisp Elevated Dark Slate Prompt Banner
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            CobaltPulse.copy(alpha = 0.24f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CobaltPulse.copy(alpha = 0.26f))
                                    .border(1.dp, CobaltPulseSoft.copy(alpha = 0.75f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = ShutterAmber,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start with your own media",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Shoot live or pick up to $MAX_CAROUSEL_ITEMS frames to unlock Darkroom Edit.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Roomier High-Contrast Side-by-Side Camera & Gallery Cards
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Camera Card
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 4.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.25.dp,
                                        color = CobaltPulseSoft.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .testTag("studio_camera_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CobaltPulse.copy(alpha = 0.24f))
                                                .border(1.dp, CobaltPulseSoft.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CameraAlt,
                                                contentDescription = null,
                                                tint = CobaltPulseSoft,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Camera",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Shoot live",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = CobaltPulseSoft,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Live viewfinder, quick still photo, or video clip.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        minLines = 2,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Button(
                                        onClick = onOpenLiveCamera,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CobaltPulse,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("live_camerax_toggle_button")
                                    ) {
                                        Text(
                                            text = "Open Viewfinder",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 46.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(onClick = onSnapPhoto)
                                                .testTag("builtin_camera_photo_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PhotoCamera,
                                                    contentDescription = "Snap photo",
                                                    tint = CobaltPulseSoft,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Photo",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 46.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(onClick = onRecordVideo)
                                                .testTag("builtin_camera_video_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.FiberManualRecord,
                                                    contentDescription = "Record video",
                                                    tint = SignalCrimson,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Video",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Gallery Card
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 4.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.25.dp,
                                        color = ShutterAmber.copy(alpha = 0.78f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .testTag("studio_gallery_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ShutterAmber.copy(alpha = 0.22f))
                                                .border(1.dp, ShutterAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoLibrary,
                                                contentDescription = null,
                                                tint = ShutterAmber,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Gallery",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Up to 10 items",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = ShutterAmber,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Single frame or mixed photo & video carousel.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        minLines = 2,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Button(
                                        onClick = onPickCarousel,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ShutterAmber,
                                            contentColor = Color(0xFF0F172A)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("carousel_picker_button")
                                    ) {
                                        Text(
                                            text = "Pick Carousel",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 46.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(onClick = onPickPhoto)
                                                .testTag("photo_picker_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PhotoLibrary,
                                                    contentDescription = "Pick photo",
                                                    tint = CobaltPulseSoft,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Photo",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 46.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(onClick = onPickVideo)
                                                .testTag("video_picker_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Videocam,
                                                    contentDescription = "Pick video",
                                                    tint = ShutterAmber,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Video",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // High-Contrast Outlined Remote Video Link Trigger
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            OutlinedButton(
                                onClick = onToggleVideoUrlField,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("studio_toggle_video_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = CobaltPulseSoft,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (showVideoUrlField) "Hide video link input" else "Or paste a video URL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            AnimatedVisibility(visible = showVideoUrlField) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = videoUrlDraft,
                                        onValueChange = onVideoUrlDraftChange,
                                        label = { Text("Direct video URL (.mp4 / storage link)") },
                                        placeholder = { Text("https://example.com/clip.mp4") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("studio_video_url_input")
                                    )
                                    Button(
                                        onClick = onLoadVideoUrl,
                                        enabled = videoUrlDraft.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CobaltPulse,
                                            contentColor = Color.White,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                                        modifier = Modifier.testTag("studio_load_video_url_button")
                                    ) {
                                        Text("Load", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Step Bar (Elevated Slate Surface)
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = when {
                    isStagingMedia -> "Preparing media…"
                    hasMedia -> "Step 1 of 3 · Ready to edit"
                    else -> "Select media to continue"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onNextToEdit,
                enabled = hasMedia && !isStagingMedia,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CobaltPulse,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                modifier = Modifier.testTag("studio_next_to_edit_button")
            ) {
                if (isStagingMedia) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Next: Edit Preview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
internal fun StudioStep2EditPage(
    activeMediaUris: List<String>,
    selectedPhotoUri: String?,
    selectedVideoUri: String?,
    selectedAspectRatio: PhotoAspectRatio,
    onSelectAspectRatio: (PhotoAspectRatio) -> Unit,
    selectedFilter: FilmPreset,
    onSelectFilter: (FilmPreset) -> Unit,
    activeToolTab: DarkroomToolTab,
    onSelectToolTab: (DarkroomToolTab) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    exposure: Float,
    onExposureChange: (Float) -> Unit,
    warmth: Float,
    onWarmthChange: (Float) -> Unit,
    filmGrain: Float,
    onFilmGrainChange: (Float) -> Unit,
    vignette: Float,
    onVignetteChange: (Float) -> Unit,
    onResetAdjustments: () -> Unit,
    hasExifFrameBorder: Boolean,
    onToggleExifFrameBorder: (Boolean) -> Unit,
    exifFrameStyle: String,
    onSelectExifFrameStyle: (String) -> Unit,
    cameraSpec: String,
    onBackToMedia: () -> Unit,
    onNextToDetails: () -> Unit
) {
    val presetThumbImageUri = selectedPhotoUri
        ?: activeMediaUris.firstOrNull { !isVideoUrl(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("studio_page_edit")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Live Darkroom Preview Stage (height-capped so tabs stay visible directly underneath)
            StudioLivePreviewStage(
                activeMediaUris = activeMediaUris,
                selectedPhotoUri = selectedPhotoUri,
                selectedVideoUri = selectedVideoUri,
                aspectRatio = selectedAspectRatio,
                selectedFilter = selectedFilter,
                brightness = brightness,
                exposure = exposure,
                contrast = contrast,
                saturation = saturation,
                warmth = warmth,
                filmGrain = filmGrain,
                vignette = vignette,
                hasExifFrameBorder = hasExifFrameBorder,
                exifFrameStyle = exifFrameStyle,
                cameraSpec = cameraSpec,
                compactPreview = false,
                maxPreviewHeight = 200.dp,
                showFilmBadge = true,
                modifier = Modifier.testTag("studio_page2_live_preview")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Compact 3-Tab Darkroom Tray Selector: Presets | Adjust | Frame
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_darkroom_tabs_bar")
            ) {
                DarkroomToolTab.entries.forEach { tab ->
                    val isSelected = tab == activeToolTab
                    Surface(
                        color = if (isSelected) CobaltPulse else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CobaltPulseSoft else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectToolTab(tab) }
                            .testTag("darkroom_tab_${tab.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else CobaltPulseSoft,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Darkroom Tool Panel (Only 1 Focused Group Visible at a Time)
            when (activeToolTab) {
                DarkroomToolTab.PRESETS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("darkroom_presets_panel")
                    ) {
                        Text(
                            text = "${selectedFilter.displayName} · ${selectedFilter.description}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ShutterAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(FilmPreset.entries, key = { it.name }) { preset ->
                                val isSelected = preset == selectedFilter
                                Surface(
                                    color = if (isSelected) CobaltPulse.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(108.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) CobaltPulse else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectFilter(preset) }
                                        .testTag("preset_${preset.name.lowercase()}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        if (!presetThumbImageUri.isNullOrBlank()) {
                                            PulseFilteredImage(
                                                imageRes = 0,
                                                imageUri = presetThumbImageUri,
                                                videoUri = null,
                                                filterPreset = preset,
                                                contentDescription = preset.displayName,
                                                showFilmBadge = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            // Clean film-stock swatch when editing a video clip without static poster bitmap
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                CobaltPulse.copy(alpha = 0.35f),
                                                                Color(0xFF0E1420)
                                                            )
                                                        )
                                                    )
                                            ) {
                                                Text(
                                                    text = preset.code,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = ShutterAmber,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = preset.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) ShutterAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DarkroomToolTab.ADJUST -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .testTag("darkroom_sliders_panel")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Fine-Grain Adjustments",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(
                                    onClick = onResetAdjustments,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("reset_darkroom_sliders_button")
                                ) {
                                    Text("Reset", color = CobaltPulseSoft, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            DarkroomSliderRow(
                                label = "Brightness",
                                valueText = String.format("%+.0f%%", brightness * 100f),
                                value = brightness,
                                valueRange = -0.5f..0.5f,
                                onValueChange = onBrightnessChange,
                                tag = "slider_brightness"
                            )
                            DarkroomSliderRow(
                                label = "Contrast",
                                valueText = String.format("%.2fx", contrast),
                                value = contrast,
                                valueRange = 0.65f..1.45f,
                                onValueChange = onContrastChange,
                                tag = "slider_contrast"
                            )
                            DarkroomSliderRow(
                                label = "Saturation",
                                valueText = String.format("%.2fx", saturation),
                                value = saturation,
                                valueRange = 0f..2f,
                                onValueChange = onSaturationChange,
                                tag = "slider_saturation"
                            )
                            DarkroomSliderRow(
                                label = "Exposure",
                                valueText = String.format("%+.2f EV", exposure),
                                value = exposure,
                                valueRange = -0.5f..0.5f,
                                onValueChange = onExposureChange,
                                tag = "slider_exposure"
                            )
                            DarkroomSliderRow(
                                label = "Warmth",
                                valueText = String.format("%+.0f K", warmth * 100f),
                                value = warmth,
                                valueRange = -0.6f..0.6f,
                                onValueChange = onWarmthChange,
                                tag = "slider_warmth"
                            )
                            DarkroomSliderRow(
                                label = "Film Grain",
                                valueText = "${(filmGrain * 100).toInt()}%",
                                value = filmGrain,
                                valueRange = 0f..1f,
                                onValueChange = onFilmGrainChange,
                                tag = "slider_film_grain"
                            )
                            DarkroomSliderRow(
                                label = "Vignette",
                                valueText = "${(vignette * 100).toInt()}%",
                                value = vignette,
                                valueRange = 0f..1f,
                                onValueChange = onVignetteChange,
                                tag = "slider_vignette"
                            )
                        }
                    }
                }

                DarkroomToolTab.FRAME -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .testTag("exif_frame_stamp_panel")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Aspect Ratio",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(PhotoAspectRatio.entries, key = { it.name }) { ratio ->
                                        FilterChip(
                                            selected = selectedAspectRatio == ratio,
                                            onClick = { onSelectAspectRatio(ratio) },
                                            label = { Text(ratio.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CobaltPulse,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("aspect_ratio_${ratio.name.lowercase()}")
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "EXIF & Contact-Sheet Border",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Stamp stock code & camera notes onto the frame border",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = hasExifFrameBorder,
                                    onCheckedChange = onToggleExifFrameBorder,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CobaltPulse
                                    ),
                                    modifier = Modifier.testTag("exif_frame_stamp_toggle")
                                )
                            }

                            if (hasExifFrameBorder) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("GALLERY" to "Gallery Matte", "DARKROOM" to "Contact Sheet").forEach { (styleCode, styleLabel) ->
                                        FilterChip(
                                            selected = exifFrameStyle == styleCode,
                                            onClick = { onSelectExifFrameStyle(styleCode) },
                                            label = { Text(styleLabel) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CobaltPulse,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Sticky Bottom Navigation Bar: <- Media | Next: Details ->
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedButton(
                onClick = onBackToMedia,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.testTag("studio_back_to_media_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Media", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onNextToDetails,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CobaltPulse,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("studio_next_to_details_button")
            ) {
                Text("Next: Details & Publish", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
internal fun StudioStep3DetailsPage(
    selectedTarget: StudioPublishTarget,
    activeMediaUris: List<String>,
    selectedPhotoUri: String?,
    selectedVideoUri: String?,
    selectedAspectRatio: PhotoAspectRatio,
    selectedFilter: FilmPreset,
    brightness: Float,
    exposure: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    filmGrain: Float,
    vignette: Float,
    hasExifFrameBorder: Boolean,
    exifFrameStyle: String,
    caption: String,
    onCaptionChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    tagsInput: String,
    onTagsChange: (String) -> Unit,
    cameraSpec: String,
    onCameraSpecChange: (String) -> Unit,
    audioTrack: String,
    onAudioTrackChange: (String) -> Unit,
    highlightTitle: String,
    onHighlightTitleChange: (String) -> Unit,
    isGeneratingAiCaption: Boolean,
    onGenerateAiCaption: () -> Unit,
    isStudioBusy: Boolean,
    isStagingMedia: Boolean,
    onBackToEdit: () -> Unit,
    onPublishClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("studio_page_details")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Page 3 Final Publish Preview Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("studio_final_publish_preview_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = CobaltPulse.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = selectedTarget.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CobaltPulseSoft,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = selectedFilter.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = ShutterAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (activeMediaUris.size > 1) {
                            Text(
                                text = "${activeMediaUris.size} frames",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    StudioLivePreviewStage(
                        activeMediaUris = activeMediaUris,
                        selectedPhotoUri = selectedPhotoUri,
                        selectedVideoUri = selectedVideoUri,
                        aspectRatio = selectedAspectRatio,
                        selectedFilter = selectedFilter,
                        brightness = brightness,
                        exposure = exposure,
                        contrast = contrast,
                        saturation = saturation,
                        warmth = warmth,
                        filmGrain = filmGrain,
                        vignette = vignette,
                        hasExifFrameBorder = hasExifFrameBorder,
                        exifFrameStyle = exifFrameStyle,
                        cameraSpec = cameraSpec,
                        compactPreview = true,
                        showFilmBadge = true,
                        modifier = Modifier.testTag("studio_page3_live_preview")
                    )

                    if (caption.isNotBlank() || location.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (caption.isNotBlank()) {
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (location.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Caption + AI Assist Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedTarget == StudioPublishTarget.STORY_24H) {
                            "Story overlay"
                        } else {
                            "Caption & field notes"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onGenerateAiCaption,
                        enabled = !isGeneratingAiCaption,
                        modifier = Modifier.testTag("openrouter_ai_caption_button")
                    ) {
                        if (isGeneratingAiCaption) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = ShutterAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = ShutterAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Caption",
                            color = ShutterAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    placeholder = { Text("Write a caption or describe the moment…") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_caption_input")
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text("Location") },
                    placeholder = { Text("City or venue") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("studio_location_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = onCategoryChange,
                    label = { Text("Category") },
                    placeholder = { Text("e.g. Street, Travel") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("studio_category_input")
                )
            }

            if (selectedTarget == StudioPublishTarget.STORY_24H) {
                OutlinedTextField(
                    value = highlightTitle,
                    onValueChange = onHighlightTitleChange,
                    label = { Text("Pin to Profile Highlight (optional)") },
                    placeholder = { Text("e.g. Weekend Trip, Studio BTS") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_highlight_input")
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = onTagsChange,
                        label = { Text("Tags") },
                        placeholder = { Text("comma separated") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("studio_tags_input")
                    )
                    OutlinedTextField(
                        value = cameraSpec,
                        onValueChange = onCameraSpecChange,
                        label = { Text("Camera & lens") },
                        placeholder = { Text("optional") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("studio_camera_spec_input")
                    )
                }
            }

            if (selectedTarget == StudioPublishTarget.LOOP_REEL || !selectedVideoUri.isNullOrBlank()) {
                OutlinedTextField(
                    value = audioTrack,
                    onValueChange = onAudioTrackChange,
                    label = { Text("Soundtrack / audio note (optional)") },
                    placeholder = { Text("Original Audio") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_audio_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sticky Bottom Navigation Bar: <- Edit | Publish CTA
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedButton(
                onClick = onBackToEdit,
                enabled = !isStudioBusy,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.testTag("studio_back_to_edit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onPublishClick,
                enabled = !isStudioBusy && activeMediaUris.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CobaltPulse,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("studio_publish_button")
            ) {
                if (isStudioBusy) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isStagingMedia) {
                            "Preparing media…"
                        } else {
                            when (selectedTarget) {
                                StudioPublishTarget.FEED_POST -> "Publishing…"
                                StudioPublishTarget.STORY_24H -> "Sharing…"
                                StudioPublishTarget.LOOP_REEL -> "Publishing Loop…"
                            }
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedTarget) {
                            StudioPublishTarget.FEED_POST -> "Publish Post"
                            StudioPublishTarget.STORY_24H -> "Share 24h Story"
                            StudioPublishTarget.LOOP_REEL -> "Publish Loop"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
