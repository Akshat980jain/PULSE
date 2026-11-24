package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.DarkroomAdjustments
import com.example.data.model.FilmPreset
import com.example.data.model.MAX_CAROUSEL_ITEMS
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.STUDIO_AUTOSAVE_DRAFT_ID
import com.example.data.model.StudioDraftEntity
import com.example.data.model.StudioPublishTarget
import com.example.data.model.isVideoUrl
import com.example.data.model.joinCarouselUris
import com.example.data.service.DarkroomProcessingService
import com.example.ui.components.rememberPulseHaptics
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper for runtime permission management (`CAMERA` and `WRITE_EXTERNAL_STORAGE`) and
 * launching the system camera app via `ActivityResultContracts.TakePicture` and
 * `ActivityResultContracts.CaptureVideo` without CameraX.
 */
object BuiltInPhoneCameraHelper {

    data class PendingCameraTarget(
        val file: File,
        val contentUri: Uri,
        val isVideo: Boolean
    )

    /**
     * Returns the runtime permissions required for camera and file system access.
     * Requests `CAMERA` on all API levels and `WRITE_EXTERNAL_STORAGE` on API <= 28
     * (or both when explicitly requested via `includeLegacyWriteStorage = true`).
     */
    fun getRequiredCameraAndStoragePermissions(
        sdkInt: Int = Build.VERSION.SDK_INT,
        includeLegacyWriteStorage: Boolean = false
    ): Array<String> {
        return if (sdkInt <= Build.VERSION_CODES.P || includeLegacyWriteStorage) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
    }

    /**
     * Checks whether `CAMERA` and (where applicable on API <= 28) `WRITE_EXTERNAL_STORAGE`
     * permissions are currently granted.
     */
    fun hasCameraAndStoragePermissions(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Boolean {
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasStorage = if (sdkInt <= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return hasCamera && hasStorage
    }

    /**
     * Evaluates the permission result map returned by `ActivityResultContracts.RequestMultiplePermissions()`.
     */
    fun evaluatePermissionGrantMap(
        permissions: Map<String, Boolean>,
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Boolean {
        val cameraGranted = permissions[Manifest.permission.CAMERA]
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED)
        val storageGranted = if (sdkInt <= Build.VERSION_CODES.P) {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED)
        } else {
            true
        }
        return cameraGranted && storageGranted
    }

    /**
     * Creates a target file in external media directories (with internal fallback) and
     * generates a secure `FileProvider` `Uri` for `ActivityResultContracts.TakePicture`
     * or `ActivityResultContracts.CaptureVideo`.
     */
    fun createCaptureTarget(context: Context, isVideo: Boolean): PendingCameraTarget {
        val preferredExternalDir = try {
            val dirType = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            context.getExternalFilesDir(dirType)?.let { File(it, "darkroom_uploads") }
        } catch (_: Throwable) {
            null
        }
        val outDir = (preferredExternalDir ?: File(context.filesDir, "darkroom_uploads")).apply {
            if (!exists()) mkdirs()
        }
        val extension = if (isVideo) "mp4" else "jpg"
        val prefix = if (isVideo) "pulse_story_video_" else "pulse_story_photo_"
        val file = File(outDir, "$prefix${System.currentTimeMillis()}.$extension")
        val authority = "${context.packageName}.fileprovider"
        val contentUri = try {
            FileProvider.getUriForFile(context, authority, file)
        } catch (_: Throwable) {
            Uri.fromFile(file)
        }
        return PendingCameraTarget(
            file = file,
            contentUri = contentUri,
            isVideo = isVideo
        )
    }

    fun buildBuiltInPhotoIntent(context: Context, outputUri: Uri, useFrontCamera: Boolean = false): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (useFrontCamera) {
                putExtra("android.intent.extras.CAMERA_FACING", 1)
                putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
        }
    }

    fun buildBuiltInVideoSnippetIntent(
        context: Context,
        outputUri: Uri,
        maxDurationSeconds: Int = 15,
        useFrontCamera: Boolean = false
    ): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, maxDurationSeconds)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (useFrontCamera) {
                putExtra("android.intent.extras.CAMERA_FACING", 1)
                putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
        }
    }

    fun saveBitmapToInternalFile(context: Context, bitmap: Bitmap): String? {
        return try {
            val outDir = File(context.filesDir, "darkroom_uploads").apply {
                if (!exists()) mkdirs()
            }
            val outFile = File(outDir, "pulse_story_photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            Uri.fromFile(outFile).toString()
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * State & launcher wrapper that uses `ActivityResultContracts.TakePicture()` to trigger the
 * system camera app and return the captured `Uri`, replacing the need for CameraX.
 */
class SystemCameraTakePictureState(
    val takePictureLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val onPrepareTarget: () -> BuiltInPhoneCameraHelper.PendingCameraTarget
) {
    var lastOutputUri: Uri? = null
        private set

    fun launchSystemCamera(): Uri? {
        return try {
            val target = onPrepareTarget()
            lastOutputUri = target.contentUri
            takePictureLauncher.launch(target.contentUri)
            target.contentUri
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * Creates an `ActivityResultLauncher` using `ActivityResultContracts.TakePicture` to trigger
 * the system camera app and invoke [onPictureUriReady] with the captured `Uri` upon completion.
 */
@Composable
fun rememberSystemCameraTakePictureLauncher(
    context: Context,
    onPictureUriReady: (Uri) -> Unit
): SystemCameraTakePictureState {
    val coroutineScope = rememberCoroutineScope()
    var pendingTarget by remember {
        mutableStateOf<BuiltInPhoneCameraHelper.PendingCameraTarget?>(null)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        val target = pendingTarget
        if (isSuccess && target != null) {
            coroutineScope.launch {
                val resolvedUri = withContext(Dispatchers.IO) {
                    if (target.file.exists() && target.file.length() > 0L) {
                        Uri.fromFile(target.file)
                    } else {
                        target.contentUri
                    }
                }
                onPictureUriReady(resolvedUri)
            }
        }
    }

    return remember(context, takePictureLauncher) {
        SystemCameraTakePictureState(
            takePictureLauncher = takePictureLauncher,
            onPrepareTarget = {
                val created = BuiltInPhoneCameraHelper.createCaptureTarget(context, isVideo = false)
                pendingTarget = created
                created
            }
        )
    }
}

@Composable
fun CreateStudioScreen(
    initialTarget: StudioPublishTarget = StudioPublishTarget.FEED_POST,
    initialAudioTrack: String = "",
    savedDrafts: List<StudioDraftEntity> = emptyList(),
    onSaveDraft: (draft: StudioDraftEntity, isManualSave: Boolean) -> Unit = { _, _ -> },
    onDeleteDraft: (draftId: String) -> Unit = {},
    onDraftPublished: (draftId: String?) -> Unit = {},
    isGeneratingAiCaption: Boolean = false,
    isUploadingMedia: Boolean = false,
    onGenerateAiCaptionAndTags: (
        location: String,
        category: String,
        filmPreset: FilmPreset,
        currentDraft: String,
        onGenerated: (caption: String, tags: String) -> Unit
    ) -> Unit = { _, _, _, _, _ -> },
    onPublish: (
        target: StudioPublishTarget,
        imageRes: Int,
        imageUri: String?,
        videoUri: String?,
        carouselImageUrisCsv: String,
        carouselImageResCsv: String,
        aspectRatio: PhotoAspectRatio,
        filterPreset: FilmPreset,
        adjustments: DarkroomAdjustments,
        caption: String,
        location: String,
        category: String,
        tagsInput: String,
        audioTrack: String,
        cameraSpec: String,
        highlightTitle: String
    ) -> Unit,
    onBackToFeed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberPulseHaptics()

    // 3-Step Connected Creation Flow (Zero Sample Data)
    var currentStep by remember { mutableStateOf(StudioCreationStep.MEDIA) }
    var activeDarkroomTab by remember { mutableStateOf(DarkroomToolTab.PRESETS) }

    // Local Room Draft Tracking State
    var activeDraftId by remember { mutableStateOf<String?>(null) }
    var draftStatusLabel by remember { mutableStateOf<String?>(null) }

    var selectedTarget by remember(initialTarget) { mutableStateOf(initialTarget) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var selectedVideoUri by remember { mutableStateOf<String?>(null) }
    var carouselUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var isStagingCarouselMedia by remember { mutableStateOf(false) }
    var showVideoUrlField by remember { mutableStateOf(false) }
    var videoUrlDraft by remember { mutableStateOf("") }

    // Darkroom Grading & Framing State (Neutral Defaults)
    var selectedAspectRatio by remember { mutableStateOf(PhotoAspectRatio.PORTRAIT_4_5) }
    var selectedFilter by remember { mutableStateOf(FilmPreset.NATURAL) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var exposure by remember { mutableFloatStateOf(0f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var filmGrain by remember { mutableFloatStateOf(0f) }
    var vignette by remember { mutableFloatStateOf(0f) }
    var hasExifFrameBorder by remember { mutableStateOf(false) }
    var exifFrameStyle by remember { mutableStateOf("GALLERY") }

    // Built-In Phone Camera Viewfinder & Runtime Permission State
    var isLiveCameraOpen by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            BuiltInPhoneCameraHelper.hasCameraAndStoragePermissions(context)
        )
    }
    var permissionStatusMessage by remember { mutableStateOf<String?>(null) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var isRecordingLoopClip by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var pendingVideoCaptureTarget by remember {
        mutableStateOf<BuiltInPhoneCameraHelper.PendingCameraTarget?>(null)
    }
    var pendingCameraLaunchMode by remember { mutableStateOf<String?>(null) }

    // Publish Details State (Clean Empty Defaults — No Hardcoded Sample Text)
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var audioTrack by remember(initialAudioTrack) { mutableStateOf(initialAudioTrack) }
    var cameraSpec by remember { mutableStateOf("") }
    var highlightTitle by remember { mutableStateOf("") }

    LaunchedEffect(initialAudioTrack) {
        if (initialAudioTrack.isNotBlank()) {
            audioTrack = initialAudioTrack
        }
    }

    val resetDarkroomAdjustments = {
        brightness = 0f
        contrast = 1f
        saturation = 1f
        exposure = 0f
        warmth = 0f
        filmGrain = 0f
        vignette = 0f
    }

    val dismissKeyboard = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    val systemTakePictureState = rememberSystemCameraTakePictureLauncher(
        context = context,
        onPictureUriReady = { capturedUri: Uri ->
            selectedPhotoUri = capturedUri.toString()
            selectedVideoUri = null
            carouselUris = emptyList()
            permissionStatusMessage = null
            isLiveCameraOpen = false
        }
    )

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { isSuccess: Boolean ->
        val target = pendingVideoCaptureTarget
        if (isSuccess && target != null) {
            coroutineScope.launch {
                val (videoUriString, thumbUriString) = withContext(Dispatchers.IO) {
                    val savedVideoUri = if (target.file.exists() && target.file.length() > 0L) {
                        Uri.fromFile(target.file).toString()
                    } else {
                        target.contentUri.toString()
                    }
                    val extractedThumb = DarkroomProcessingService.extractVideoThumbnailFile(context, savedVideoUri)
                        ?.let { Uri.fromFile(it).toString() }
                    savedVideoUri to extractedThumb
                }
                selectedVideoUri = videoUriString
                if (!thumbUriString.isNullOrBlank()) {
                    selectedPhotoUri = thumbUriString
                }
                carouselUris = emptyList()
                if (selectedTarget == StudioPublishTarget.FEED_POST) {
                    selectedTarget = StudioPublishTarget.LOOP_REEL
                }
                permissionStatusMessage = null
                isRecordingLoopClip = false
                isLiveCameraOpen = false
            }
        } else {
            isRecordingLoopClip = false
        }
    }

    val launchBuiltInPhonePhotoCamera = {
        systemTakePictureState.launchSystemCamera()
        Unit
    }

    val launchBuiltInPhoneVideoCamera = {
        try {
            val target = BuiltInPhoneCameraHelper.createCaptureTarget(context, isVideo = true)
            pendingVideoCaptureTarget = target
            captureVideoLauncher.launch(target.contentUri)
        } catch (_: Throwable) {
        }
    }

    val cameraAndStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = BuiltInPhoneCameraHelper.evaluatePermissionGrantMap(permissionsMap, context)
        hasCameraPermission = granted
        permissionStatusMessage = if (granted) {
            null
        } else {
            "Camera access is required to capture photos and video clips."
        }
        when (pendingCameraLaunchMode) {
            "PHOTO" -> {
                pendingCameraLaunchMode = null
                if (granted) launchBuiltInPhonePhotoCamera() else isLiveCameraOpen = true
            }
            "VIDEO" -> {
                pendingCameraLaunchMode = null
                if (granted) launchBuiltInPhoneVideoCamera() else isLiveCameraOpen = true
            }
            else -> {
                pendingCameraLaunchMode = null
                isLiveCameraOpen = true
            }
        }
    }

    val requestCameraAndStoragePermissions = { mode: String ->
        pendingCameraLaunchMode = mode
        cameraAndStoragePermissionLauncher.launch(
            BuiltInPhoneCameraHelper.getRequiredCameraAndStoragePermissions()
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isStagingCarouselMedia = true
                try {
                    val permanentUri = withContext(Dispatchers.IO) {
                        DarkroomProcessingService.copyUriToPermanentInternalFile(
                            context = context,
                            sourceUri = uri,
                            isVideo = false
                        )
                    }
                    val updatedCarousel = (carouselUris + permanentUri).distinct().take(MAX_CAROUSEL_ITEMS)
                    carouselUris = updatedCarousel
                    selectedPhotoUri = updatedCarousel.firstOrNull { !isVideoUrl(it) } ?: permanentUri
                    selectedVideoUri = null
                    if (updatedCarousel.size > 1) {
                        selectedTarget = StudioPublishTarget.FEED_POST
                    }
                    isLiveCameraOpen = false
                } finally {
                    isStagingCarouselMedia = false
                }
            }
        }
    }

    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_CAROUSEL_ITEMS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isStagingCarouselMedia = true
                try {
                    val (pickedUris, firstExtractedThumb) = withContext(Dispatchers.IO) {
                        val copied = uris.take(MAX_CAROUSEL_ITEMS).map { uri ->
                            async {
                                val isVid = DarkroomProcessingService.isUriVideo(context, uri)
                                DarkroomProcessingService.copyUriToPermanentInternalFile(
                                    context = context,
                                    sourceUri = uri,
                                    isVideo = isVid
                                )
                            }
                        }.awaitAll()
                        val firstNonVideo = copied.firstOrNull { !isVideoUrl(it) }
                        val thumb = if (firstNonVideo == null && copied.isNotEmpty()) {
                            DarkroomProcessingService.extractVideoThumbnailFile(context, copied.first())
                                ?.let { Uri.fromFile(it).toString() }
                        } else {
                            null
                        }
                        copied to thumb
                    }
                    val mergedCarousel = if (carouselUris.isNotEmpty() && pickedUris.size < MAX_CAROUSEL_ITEMS) {
                        (carouselUris + pickedUris).distinct().take(MAX_CAROUSEL_ITEMS)
                    } else {
                        pickedUris.take(MAX_CAROUSEL_ITEMS)
                    }
                    carouselUris = mergedCarousel
                    selectedPhotoUri = mergedCarousel.firstOrNull { !isVideoUrl(it) }
                        ?: firstExtractedThumb
                        ?: mergedCarousel.firstOrNull()
                    selectedVideoUri = if (mergedCarousel.size == 1 && isVideoUrl(mergedCarousel.first())) {
                        mergedCarousel.first()
                    } else {
                        null
                    }
                    selectedTarget = StudioPublishTarget.FEED_POST
                    isLiveCameraOpen = false
                } finally {
                    isStagingCarouselMedia = false
                }
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isStagingCarouselMedia = true
                try {
                    val (permanentVideoUri, thumbUri) = withContext(Dispatchers.IO) {
                        val copiedVideo = DarkroomProcessingService.copyUriToPermanentInternalFile(
                            context = context,
                            sourceUri = uri,
                            isVideo = true
                        )
                        val extractedThumb = DarkroomProcessingService.extractVideoThumbnailFile(context, copiedVideo)
                            ?.let { Uri.fromFile(it).toString() }
                        copiedVideo to extractedThumb
                    }
                    val updatedCarousel = (carouselUris + permanentVideoUri).distinct().take(MAX_CAROUSEL_ITEMS)
                    carouselUris = updatedCarousel
                    if (updatedCarousel.size > 1) {
                        selectedVideoUri = null
                        if (selectedPhotoUri.isNullOrBlank() && thumbUri != null) {
                            selectedPhotoUri = thumbUri
                        }
                        selectedTarget = StudioPublishTarget.FEED_POST
                    } else {
                        selectedVideoUri = permanentVideoUri
                        if (thumbUri != null) {
                            selectedPhotoUri = thumbUri
                        }
                    }
                    isLiveCameraOpen = false
                } finally {
                    isStagingCarouselMedia = false
                }
            }
        }
    }

    LaunchedEffect(isRecordingLoopClip) {
        if (isRecordingLoopClip) {
            recordingSeconds = 0
            while (isRecordingLoopClip && recordingSeconds < 15) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    val activeMediaUris: List<String> = remember(carouselUris, selectedPhotoUri, selectedVideoUri) {
        when {
            carouselUris.isNotEmpty() -> carouselUris
            !selectedVideoUri.isNullOrBlank() -> listOf(selectedVideoUri!!)
            !selectedPhotoUri.isNullOrBlank() -> listOf(selectedPhotoUri!!)
            else -> emptyList()
        }
    }
    val hasMedia = activeMediaUris.isNotEmpty()
    val hasDraftableContent = hasMedia || caption.isNotBlank() || location.isNotBlank()

    fun buildDraftSnapshot(id: String, isAutoSave: Boolean): StudioDraftEntity {
        return StudioDraftEntity(
            id = id,
            targetName = selectedTarget.name,
            currentStepName = currentStep.name,
            imageUri = selectedPhotoUri,
            videoUri = selectedVideoUri,
            carouselImageUrisCsv = joinCarouselUris(carouselUris),
            aspectRatioName = selectedAspectRatio.name,
            filterPresetName = selectedFilter.name,
            brightness = brightness,
            exposure = exposure,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth,
            filmGrain = filmGrain,
            vignette = vignette,
            hasExifFrameBorder = hasExifFrameBorder,
            exifFrameStyle = exifFrameStyle,
            caption = caption,
            location = location,
            category = category,
            tagsInput = tagsInput,
            audioTrack = audioTrack,
            cameraSpec = cameraSpec,
            highlightTitle = highlightTitle,
            isAutoSave = isAutoSave,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    // Debounced auto-save to Room whenever user has staged media or entered post details
    LaunchedEffect(
        hasDraftableContent,
        selectedTarget,
        currentStep,
        selectedPhotoUri,
        selectedVideoUri,
        carouselUris,
        selectedAspectRatio,
        selectedFilter,
        brightness,
        exposure,
        contrast,
        saturation,
        warmth,
        filmGrain,
        vignette,
        hasExifFrameBorder,
        exifFrameStyle,
        caption,
        location,
        category,
        tagsInput,
        audioTrack,
        cameraSpec,
        highlightTitle
    ) {
        if (!hasDraftableContent) return@LaunchedEffect
        delay(650L)
        val targetDraftId = activeDraftId ?: STUDIO_AUTOSAVE_DRAFT_ID
        val isAuto = targetDraftId == STUDIO_AUTOSAVE_DRAFT_ID
        onSaveDraft(buildDraftSnapshot(id = targetDraftId, isAutoSave = isAuto), false)
        draftStatusLabel = if (isAuto) "Auto-saved" else "Draft updated"
    }

    // Step-aware system BackHandler: DETAILS -> EDIT -> MEDIA
    BackHandler(enabled = currentStep != StudioCreationStep.MEDIA) {
        dismissKeyboard()
        currentStep = when (currentStep) {
            StudioCreationStep.DETAILS -> StudioCreationStep.EDIT
            StudioCreationStep.EDIT -> StudioCreationStep.MEDIA
            StudioCreationStep.MEDIA -> StudioCreationStep.MEDIA
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("create_studio_screen")
    ) {
        StudioStepHeaderBar(
            currentStep = currentStep,
            hasMedia = hasMedia,
            canSaveDraft = hasDraftableContent,
            draftStatusLabel = draftStatusLabel,
            onSaveDraftClick = {
                dismissKeyboard()
                haptics.performBookmarkHaptic()
                val manualDraftId = if (activeDraftId == null || activeDraftId == STUDIO_AUTOSAVE_DRAFT_ID) {
                    "draft_${UUID.randomUUID()}"
                } else {
                    activeDraftId!!
                }
                activeDraftId = manualDraftId
                onSaveDraft(buildDraftSnapshot(id = manualDraftId, isAutoSave = false), true)
                draftStatusLabel = "Draft saved"
            },
            onStepSelected = { destination ->
                dismissKeyboard()
                if (destination == StudioCreationStep.MEDIA || hasMedia) {
                    haptics.performControlTapHaptic()
                    currentStep = destination
                }
            },
            onBackClick = when (currentStep) {
                StudioCreationStep.MEDIA -> null
                StudioCreationStep.EDIT -> ({
                    dismissKeyboard()
                    haptics.performControlTapHaptic()
                    currentStep = StudioCreationStep.MEDIA
                })
                StudioCreationStep.DETAILS -> ({
                    dismissKeyboard()
                    haptics.performControlTapHaptic()
                    currentStep = StudioCreationStep.EDIT
                })
            },
            onBackToFeed = onBackToFeed?.let { feedCallback ->
                {
                    dismissKeyboard()
                    feedCallback()
                }
            }
        )

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState.stepNumber >= initialState.stepNumber) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "create_studio_step_pager",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { step ->
            when (step) {
                StudioCreationStep.MEDIA -> {
                    StudioStep1MediaPage(
                        selectedTarget = selectedTarget,
                        onSelectTarget = { selectedTarget = it },
                        savedDrafts = savedDrafts,
                        activeDraftId = activeDraftId,
                        onResumeDraft = { draft ->
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            activeDraftId = draft.id
                            selectedTarget = draft.publishTarget
                            selectedPhotoUri = draft.imageUri
                            selectedVideoUri = draft.videoUri
                            carouselUris = draft.carouselUris
                            selectedAspectRatio = draft.aspectRatio
                            selectedFilter = draft.filterPreset
                            brightness = draft.brightness
                            exposure = draft.exposure
                            contrast = draft.contrast
                            saturation = draft.saturation
                            warmth = draft.warmth
                            filmGrain = draft.filmGrain
                            vignette = draft.vignette
                            hasExifFrameBorder = draft.hasExifFrameBorder
                            exifFrameStyle = draft.exifFrameStyle
                            caption = draft.caption
                            location = draft.location
                            category = draft.category
                            tagsInput = draft.tagsInput
                            audioTrack = draft.audioTrack
                            cameraSpec = draft.cameraSpec
                            highlightTitle = draft.highlightTitle
                            isLiveCameraOpen = false
                            showVideoUrlField = false
                            draftStatusLabel = "Resumed draft"
                            val restoredStep = StudioCreationStep.entries.find { it.name == draft.currentStepName }
                                ?: StudioCreationStep.MEDIA
                            currentStep = if (draft.hasMedia) restoredStep else StudioCreationStep.MEDIA
                        },
                        onDeleteDraft = { draftId ->
                            haptics.performControlTapHaptic()
                            if (activeDraftId == draftId) {
                                activeDraftId = null
                                draftStatusLabel = null
                            }
                            onDeleteDraft(draftId)
                        },
                        isLiveCameraOpen = isLiveCameraOpen,
                        hasCameraPermission = hasCameraPermission,
                        permissionStatusMessage = permissionStatusMessage,
                        useFrontCamera = useFrontCamera,
                        isRecordingLoopClip = isRecordingLoopClip,
                        recordingSeconds = recordingSeconds,
                        selectedFilter = selectedFilter,
                        cameraSpec = cameraSpec,
                        hasMedia = hasMedia,
                        isStagingMedia = isStagingCarouselMedia,
                        activeMediaUris = activeMediaUris,
                        selectedPhotoUri = selectedPhotoUri,
                        selectedVideoUri = selectedVideoUri,
                        carouselUris = carouselUris,
                        selectedAspectRatio = selectedAspectRatio,
                        showVideoUrlField = showVideoUrlField,
                        onToggleVideoUrlField = { showVideoUrlField = !showVideoUrlField },
                        videoUrlDraft = videoUrlDraft,
                        onVideoUrlDraftChange = { videoUrlDraft = it },
                        onLoadVideoUrl = {
                            val trimmed = videoUrlDraft.trim()
                            if (trimmed.isNotBlank()) {
                                dismissKeyboard()
                                haptics.performControlTapHaptic()
                                selectedVideoUri = trimmed
                                selectedPhotoUri = null
                                carouselUris = emptyList()
                                showVideoUrlField = false
                                isLiveCameraOpen = false
                            }
                        },
                        onOpenLiveCamera = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            if (!hasCameraPermission) {
                                requestCameraAndStoragePermissions("VIEWFINDER")
                            } else {
                                isLiveCameraOpen = true
                            }
                        },
                        onCloseLiveCamera = {
                            isLiveCameraOpen = false
                        },
                        onSnapPhoto = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            if (!hasCameraPermission) {
                                requestCameraAndStoragePermissions("PHOTO")
                            } else {
                                launchBuiltInPhonePhotoCamera()
                            }
                        },
                        onRecordVideo = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            if (!hasCameraPermission) {
                                requestCameraAndStoragePermissions("VIDEO")
                            } else {
                                launchBuiltInPhoneVideoCamera()
                            }
                        },
                        onPickPhoto = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onPickVideo = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onPickCarousel = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            multiPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onRequestPermission = { requestCameraAndStoragePermissions("VIEWFINDER") },
                        onFlipLens = { useFrontCamera = !useFrontCamera },
                        onPhotoCaptured = { capturedPath ->
                            haptics.performSendHaptic()
                            if (!capturedPath.isNullOrBlank()) {
                                selectedPhotoUri = capturedPath
                                selectedVideoUri = null
                                carouselUris = emptyList()
                                isLiveCameraOpen = false
                            } else {
                                launchBuiltInPhonePhotoCamera()
                            }
                        },
                        onToggleRecordLoop = {
                            if (!isRecordingLoopClip) {
                                isRecordingLoopClip = true
                                launchBuiltInPhoneVideoCamera()
                            } else {
                                isRecordingLoopClip = false
                            }
                        },
                        onRemoveCarouselItem = { idx ->
                            val updated = carouselUris.filterIndexed { i, _ -> i != idx }
                            carouselUris = updated
                            selectedPhotoUri = updated.firstOrNull { !isVideoUrl(it) }
                            selectedVideoUri = if (updated.size == 1 && isVideoUrl(updated.first())) {
                                updated.first()
                            } else {
                                null
                            }
                        },
                        onClearMedia = {
                            haptics.performControlTapHaptic()
                            selectedPhotoUri = null
                            selectedVideoUri = null
                            carouselUris = emptyList()
                            videoUrlDraft = ""
                        },
                        onNextToEdit = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            if (hasMedia) {
                                currentStep = StudioCreationStep.EDIT
                            }
                        }
                    )
                }

                StudioCreationStep.EDIT -> {
                    StudioStep2EditPage(
                        activeMediaUris = activeMediaUris,
                        selectedPhotoUri = selectedPhotoUri,
                        selectedVideoUri = selectedVideoUri,
                        selectedAspectRatio = selectedAspectRatio,
                        onSelectAspectRatio = { selectedAspectRatio = it },
                        selectedFilter = selectedFilter,
                        onSelectFilter = {
                            haptics.performControlTapHaptic()
                            selectedFilter = it
                        },
                        activeToolTab = activeDarkroomTab,
                        onSelectToolTab = {
                            haptics.performControlTapHaptic()
                            activeDarkroomTab = it
                        },
                        brightness = brightness,
                        onBrightnessChange = { brightness = it },
                        contrast = contrast,
                        onContrastChange = { contrast = it },
                        saturation = saturation,
                        onSaturationChange = { saturation = it },
                        exposure = exposure,
                        onExposureChange = { exposure = it },
                        warmth = warmth,
                        onWarmthChange = { warmth = it },
                        filmGrain = filmGrain,
                        onFilmGrainChange = { filmGrain = it },
                        vignette = vignette,
                        onVignetteChange = { vignette = it },
                        onResetAdjustments = {
                            haptics.performControlTapHaptic()
                            resetDarkroomAdjustments()
                        },
                        hasExifFrameBorder = hasExifFrameBorder,
                        onToggleExifFrameBorder = { hasExifFrameBorder = it },
                        exifFrameStyle = exifFrameStyle,
                        onSelectExifFrameStyle = { exifFrameStyle = it },
                        cameraSpec = cameraSpec,
                        onBackToMedia = {
                            haptics.performControlTapHaptic()
                            currentStep = StudioCreationStep.MEDIA
                        },
                        onNextToDetails = {
                            haptics.performControlTapHaptic()
                            currentStep = StudioCreationStep.DETAILS
                        }
                    )
                }

                StudioCreationStep.DETAILS -> {
                    val isStudioBusy = isUploadingMedia || isStagingCarouselMedia
                    StudioStep3DetailsPage(
                        selectedTarget = selectedTarget,
                        activeMediaUris = activeMediaUris,
                        selectedPhotoUri = selectedPhotoUri,
                        selectedVideoUri = selectedVideoUri,
                        selectedAspectRatio = selectedAspectRatio,
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
                        caption = caption,
                        onCaptionChange = { caption = it },
                        location = location,
                        onLocationChange = { location = it },
                        category = category,
                        onCategoryChange = { category = it },
                        tagsInput = tagsInput,
                        onTagsChange = { tagsInput = it },
                        cameraSpec = cameraSpec,
                        onCameraSpecChange = { cameraSpec = it },
                        audioTrack = audioTrack,
                        onAudioTrackChange = { audioTrack = it },
                        highlightTitle = highlightTitle,
                        onHighlightTitleChange = { highlightTitle = it },
                        isGeneratingAiCaption = isGeneratingAiCaption,
                        onGenerateAiCaption = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            onGenerateAiCaptionAndTags(
                                location,
                                category,
                                selectedFilter,
                                caption
                            ) { aiCaption, aiTags ->
                                caption = aiCaption
                                if (aiTags.isNotBlank()) {
                                    tagsInput = aiTags
                                }
                            }
                        },
                        isStudioBusy = isStudioBusy,
                        isStagingMedia = isStagingCarouselMedia,
                        onBackToEdit = {
                            dismissKeyboard()
                            haptics.performControlTapHaptic()
                            currentStep = StudioCreationStep.EDIT
                        },
                        onPublishClick = {
                            dismissKeyboard()
                            haptics.performSendHaptic()
                            val adjustments = DarkroomAdjustments(
                                brightness = brightness,
                                exposure = exposure,
                                contrast = contrast,
                                saturation = saturation,
                                warmth = warmth,
                                filmGrain = filmGrain,
                                vignette = vignette,
                                hasExifFrameBorder = hasExifFrameBorder,
                                exifFrameStyle = exifFrameStyle
                            )
                            val effectiveCarouselUris = carouselUris
                            val joinedCarouselCsv = joinCarouselUris(effectiveCarouselUris)
                            val publishPhotoUri = selectedPhotoUri
                                ?: effectiveCarouselUris.firstOrNull { !isVideoUrl(it) }
                            val publishVideoUri = selectedVideoUri
                                ?: effectiveCarouselUris.firstOrNull { isVideoUrl(it) }
                            onPublish(
                                selectedTarget,
                                0,
                                publishPhotoUri,
                                publishVideoUri,
                                joinedCarouselCsv,
                                "",
                                selectedAspectRatio,
                                selectedFilter,
                                adjustments,
                                caption,
                                location,
                                category,
                                tagsInput,
                                audioTrack,
                                cameraSpec,
                                highlightTitle
                            )
                            val publishedDraftId = activeDraftId
                            activeDraftId = null
                            draftStatusLabel = null
                            onDraftPublished(publishedDraftId)
                            carouselUris = emptyList()
                            selectedPhotoUri = null
                            selectedVideoUri = null
                            videoUrlDraft = ""
                            showVideoUrlField = false
                            caption = ""
                            location = ""
                            category = ""
                            tagsInput = ""
                            audioTrack = ""
                            cameraSpec = ""
                            highlightTitle = ""
                            selectedFilter = FilmPreset.NATURAL
                            hasExifFrameBorder = false
                            resetDarkroomAdjustments()
                            activeDarkroomTab = DarkroomToolTab.PRESETS
                            currentStep = StudioCreationStep.MEDIA
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun DarkroomSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    tag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = ShutterAmber,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ShutterAmber,
                activeTrackColor = CobaltPulse
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag(tag)
        )
    }
}

/**
 * Built-In Mobile Phone Camera Viewfinder Component.
 * Uses the phone's built-in Android camera hardware (`android.hardware.camera2.CameraManager` + `TextureView`)
 * and `ActivityResultContracts.TakePicture` / `ActivityResultContracts.CaptureVideo`
 * — completely free of CameraX.
 */
@SuppressLint("MissingPermission")
@Composable
internal fun BuiltInPhoneCameraViewfinderCard(
    hasCameraPermission: Boolean,
    useFrontCamera: Boolean,
    isRecordingLoopClip: Boolean,
    recordingSeconds: Int,
    selectedFilter: FilmPreset,
    cameraSpec: String,
    onRequestPermission: () -> Unit,
    onFlipLens: () -> Unit,
    onLaunchSystemPhotoCamera: () -> Unit,
    onLaunchSystemVideoCamera: () -> Unit,
    onPhotoCaptured: (String?) -> Unit,
    onToggleRecordLoop: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textureView = remember(context) {
        TextureView(context).apply {
            isOpaque = false
        }
    }
    var isNativePreviewStreaming by remember { mutableStateOf(false) }

    DisposableEffect(hasCameraPermission, useFrontCamera, textureView) {
        var cameraDevice: CameraDevice? = null
        var captureSession: CameraCaptureSession? = null
        var previewSurface: Surface? = null
        var cameraThread: HandlerThread? = null
        var cameraHandler: Handler? = null

        fun closeNativeCamera() {
            isNativePreviewStreaming = false
            try {
                captureSession?.close()
            } catch (_: Throwable) {
            }
            captureSession = null
            try {
                cameraDevice?.close()
            } catch (_: Throwable) {
            }
            cameraDevice = null
            try {
                previewSurface?.release()
            } catch (_: Throwable) {
            }
            previewSurface = null
            try {
                cameraThread?.quitSafely()
            } catch (_: Throwable) {
            }
            cameraThread = null
            cameraHandler = null
        }

        fun openNativeCamera(surfaceTexture: SurfaceTexture) {
            if (!hasCameraPermission) return
            try {
                val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
                val desiredFacing = if (useFrontCamera) {
                    CameraCharacteristics.LENS_FACING_FRONT
                } else {
                    CameraCharacteristics.LENS_FACING_BACK
                }
                val cameraId = manager.cameraIdList.firstOrNull { id ->
                    val chars = manager.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.LENS_FACING) == desiredFacing
                } ?: manager.cameraIdList.firstOrNull() ?: return

                val thread = HandlerThread("PulseBuiltInCameraThread").apply { start() }
                val handler = Handler(thread.looper)
                cameraThread = thread
                cameraHandler = handler

                surfaceTexture.setDefaultBufferSize(720, 960)
                val surface = Surface(surfaceTexture)
                previewSurface = surface

                manager.openCamera(
                    cameraId,
                    object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraDevice = camera
                            try {
                                val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                    addTarget(surface)
                                    set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                    )
                                }
                                @Suppress("DEPRECATION")
                                camera.createCaptureSession(
                                    listOf(surface),
                                    object : CameraCaptureSession.StateCallback() {
                                        override fun onConfigured(session: CameraCaptureSession) {
                                            if (cameraDevice == null) return
                                            captureSession = session
                                            try {
                                                session.setRepeatingRequest(requestBuilder.build(), null, handler)
                                                isNativePreviewStreaming = true
                                            } catch (_: Throwable) {
                                            }
                                        }

                                        override fun onConfigureFailed(session: CameraCaptureSession) {
                                            isNativePreviewStreaming = false
                                        }
                                    },
                                    handler
                                )
                            } catch (_: Throwable) {
                            }
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            closeNativeCamera()
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            closeNativeCamera()
                        }
                    },
                    handler
                )
            } catch (_: Throwable) {
                isNativePreviewStreaming = false
            }
        }

        if (hasCameraPermission) {
            if (textureView.isAvailable && textureView.surfaceTexture != null) {
                openNativeCamera(textureView.surfaceTexture!!)
            }
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    openNativeCamera(surface)
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    closeNativeCamera()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
            }
        }

        onDispose {
            textureView.surfaceTextureListener = null
            closeNativeCamera()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0B0E14))
            .border(1.5.dp, CobaltPulse, RoundedCornerShape(14.dp))
            .testTag("live_camerax_viewfinder")
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { textureView },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("builtin_camera_viewfinder")
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = ShutterAmber,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Built-In Phone Camera",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Grant camera access to capture photos and video clips with your phone's built-in camera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                    modifier = Modifier.testTag("camerax_grant_permission_button")
                ) {
                    Text("Grant Camera Permission")
                }
            }
        }

        // Optical HUD Overlay
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isRecordingLoopClip) {
                        "● REC 00:${recordingSeconds.toString().padStart(2, '0')} · STORY VIDEO"
                    } else {
                        "BUILT-IN CAMERA · ${selectedFilter.code}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRecordingLoopClip) SignalCrimson else ShutterAmber,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable { onLaunchSystemPhotoCamera() }
                        .testTag("open_system_camera_app_button")
                ) {
                    Text(
                        text = "System Camera App",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onFlipLens,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("camerax_flip_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Flip camera lens",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Shutter & Video Snippet Record Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = cameraSpec.ifBlank { "Auto" }.take(18),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )

            // Photo Shutter Button
            Surface(
                color = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(58.dp)
                    .border(3.dp, ShutterAmber, CircleShape)
                    .clickable {
                        if (hasCameraPermission && isNativePreviewStreaming) {
                            val previewBmp = try {
                                textureView.bitmap
                            } catch (_: Throwable) {
                                null
                            }
                            if (previewBmp != null) {
                                coroutineScope.launch {
                                    val savedUri = withContext(Dispatchers.IO) {
                                        BuiltInPhoneCameraHelper.saveBitmapToInternalFile(context, previewBmp)
                                    }
                                    onPhotoCaptured(savedUri)
                                }
                            } else {
                                onLaunchSystemPhotoCamera()
                            }
                        } else {
                            onPhotoCaptured(null)
                        }
                    }
                    .testTag("camerax_capture_photo_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Capture photo",
                        tint = Color(0xFF0B0E14),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Short Video Snippet / Loop Record Button
            OutlinedButton(
                onClick = onToggleRecordLoop,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("camerax_record_loop_button")
            ) {
                Icon(
                    imageVector = if (isRecordingLoopClip) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                    contentDescription = "Record video snippet",
                    tint = SignalCrimson,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isRecordingLoopClip) "Stop Clip" else "Rec Video",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}
