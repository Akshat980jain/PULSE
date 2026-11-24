package com.example.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.DarkroomAdjustments
import com.example.data.model.FilmPreset
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Darkroom processing service that computes combined Brightness, Contrast, Saturation,
 * Exposure, Warmth, and FilmPreset color matrices, renders a live Jetpack Compose [Canvas]
 * preview, and bakes adjustments into a durable JPEG [File] in internal storage before upload.
 */
object DarkroomProcessingService {

    /**
     * Builds a Jetpack Compose [ColorMatrix] combining [preset], [brightness] (-1f..1f),
     * [contrast] (0.5f..1.5f), [saturation] (0f..2f), [exposure] (-1f..1f), and [warmth] (-1f..1f).
     */
    fun buildComposeColorMatrix(
        preset: FilmPreset = FilmPreset.NATURAL,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
        exposure: Float = 0f,
        warmth: Float = 0f
    ): ColorMatrix? {
        val base = preset.toColorMatrix()
        val hasAdjustments = abs(brightness) > 0.005f ||
            abs(contrast - 1f) > 0.005f ||
            abs(saturation - 1f) > 0.005f ||
            abs(exposure) > 0.005f ||
            abs(warmth) > 0.005f
        if (base == null && !hasAdjustments) return null

        val result = ColorMatrix()
        if (base != null) {
            result.set(base)
        }

        // 1. Saturation matrix (Rec. 709 luma weights)
        val sat = saturation.coerceIn(0f, 2f)
        if (abs(sat - 1f) > 0.005f) {
            val satMatrix = ColorMatrix().apply { setToSaturation(sat) }
            result.timesAssign(satMatrix)
        }

        // 2. Contrast + Brightness + Exposure + Warmth matrix
        val c = contrast.coerceIn(0.5f, 1.6f)
        val brightnessOffset = brightness.coerceIn(-1f, 1f) * 70f
        val exposureOffset = exposure.coerceIn(-1f, 1f) * 55f
        val totalLightOffset = brightnessOffset + exposureOffset
        val contrastTranslate = (1f - c) * 128f
        val warmR = warmth.coerceIn(-1f, 1f) * 22f
        val warmB = -warmth.coerceIn(-1f, 1f) * 22f

        val toneMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, totalLightOffset + contrastTranslate + warmR,
                0f, c, 0f, 0f, totalLightOffset + contrastTranslate + (warmR * 0.25f),
                0f, 0f, c, 0f, totalLightOffset + contrastTranslate + warmB,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.timesAssign(toneMatrix)
        return result
    }

    fun buildComposeColorMatrix(
        preset: FilmPreset,
        adjustments: DarkroomAdjustments
    ): ColorMatrix? = buildComposeColorMatrix(
        preset = preset,
        brightness = adjustments.brightness,
        contrast = adjustments.contrast,
        saturation = adjustments.saturation,
        exposure = adjustments.exposure,
        warmth = adjustments.warmth
    )

    /**
     * Builds an Android [android.graphics.ColorMatrix] for bitmap baking on [android.graphics.Canvas].
     */
    fun buildAndroidColorMatrix(
        preset: FilmPreset = FilmPreset.NATURAL,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
        exposure: Float = 0f,
        warmth: Float = 0f
    ): android.graphics.ColorMatrix {
        val composeMatrix = buildComposeColorMatrix(
            preset = preset,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            exposure = exposure,
            warmth = warmth
        )
        return if (composeMatrix != null) {
            android.graphics.ColorMatrix(composeMatrix.values.copyOf())
        } else {
            android.graphics.ColorMatrix()
        }
    }

    /**
     * Live Jetpack Compose [Canvas] preview that renders [imageBitmap] with real-time
     * Brightness, Contrast, Saturation, Exposure, Warmth, Vignette, and Film Grain adjustments.
     */
    @Composable
    fun DarkroomCanvasPreview(
        imageBitmap: ImageBitmap,
        preset: FilmPreset,
        adjustments: DarkroomAdjustments,
        modifier: Modifier = Modifier,
        contentDescriptionText: String = "Darkroom processed photo preview"
    ) {
        val colorFilter = remember(preset, adjustments) {
            buildComposeColorMatrix(preset, adjustments)?.let { ColorFilter.colorMatrix(it) }
        }
        val grainOffsets = remember {
            List(120) { index ->
                val x = ((index * 73 + 19) % 100) / 100f
                val y = ((index * 41 + 53) % 100) / 100f
                val bright = index % 2 == 0
                Triple(x, y, bright)
            }
        }

        Canvas(
            modifier = modifier
                .testTag("darkroom_canvas_preview")
                .semantics { contentDescription = contentDescriptionText }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            if (canvasW <= 0f || canvasH <= 0f) return@Canvas

            val imgW = imageBitmap.width.toFloat().coerceAtLeast(1f)
            val imgH = imageBitmap.height.toFloat().coerceAtLeast(1f)
            val scale = max(canvasW / imgW, canvasH / imgH)
            val dstW = (imgW * scale).roundToInt()
            val dstH = (imgH * scale).roundToInt()
            val offsetX = ((canvasW - dstW) / 2f).roundToInt()
            val offsetY = ((canvasH - dstH) / 2f).roundToInt()

            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(offsetX, offsetY),
                dstSize = IntSize(dstW, dstH),
                colorFilter = colorFilter
            )

            // Vignette overlay
            if (adjustments.vignette > 0.01f) {
                val radius = max(canvasW, canvasH) * 0.72f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = (adjustments.vignette * 0.72f).coerceIn(0f, 0.85f))
                        ),
                        center = Offset(canvasW / 2f, canvasH / 2f),
                        radius = radius
                    )
                )
            }

            // Analog Film Grain overlay
            if (adjustments.filmGrain > 0.01f) {
                val grainAlpha = (adjustments.filmGrain * 0.22f).coerceIn(0f, 0.35f)
                grainOffsets.forEach { (nx, ny, isBright) ->
                    drawCircle(
                        color = if (isBright) {
                            Color.White.copy(alpha = grainAlpha)
                        } else {
                            Color.Black.copy(alpha = grainAlpha * 0.85f)
                        },
                        radius = 1.4f,
                        center = Offset(nx * canvasW, ny * canvasH)
                    )
                }
            }
        }
    }

    /**
     * Bakes [preset] and [adjustments] (brightness, contrast, saturation, exposure, warmth,
     * vignette, and film grain) onto a new [Bitmap] using [android.graphics.Canvas].
     */
    fun applyFiltersToBitmap(
        source: Bitmap,
        preset: FilmPreset = FilmPreset.NATURAL,
        adjustments: DarkroomAdjustments = DarkroomAdjustments()
    ): Bitmap {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)

        val androidMatrix = buildAndroidColorMatrix(
            preset = preset,
            brightness = adjustments.brightness,
            contrast = adjustments.contrast,
            saturation = adjustments.saturation,
            exposure = adjustments.exposure,
            warmth = adjustments.warmth
        )
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(androidMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, imagePaint)

        // Bake optical vignette if enabled
        if (adjustments.vignette > 0.01f) {
            val radius = max(width, height) * 0.75f
            val edgeAlpha = (adjustments.vignette.coerceIn(0f, 1f) * 185).roundToInt().coerceIn(0, 225)
            val edgeColor = android.graphics.Color.argb(edgeAlpha, 0, 0, 0)
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width / 2f,
                    height / 2f,
                    radius,
                    intArrayOf(android.graphics.Color.TRANSPARENT, edgeColor),
                    floatArrayOf(0.45f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        }

        // Bake subtle silver-halide film grain if enabled
        if (adjustments.filmGrain > 0.01f) {
            val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val alpha = (adjustments.filmGrain.coerceIn(0f, 1f) * 52).roundToInt().coerceIn(0, 90)
            val count = 160
            val radius = max(1f, minOf(width, height) / 420f)
            for (i in 0 until count) {
                val gx = ((i * 73 + 19) % 100) / 100f * width
                val gy = ((i * 41 + 53) % 100) / 100f * height
                grainPaint.color = if (i % 2 == 0) {
                    android.graphics.Color.argb(alpha, 255, 255, 255)
                } else {
                    android.graphics.Color.argb(alpha, 12, 12, 12)
                }
                canvas.drawCircle(gx, gy, radius, grainPaint)
            }
        }

        return output
    }

    /**
     * Checks whether a given [Uri] points to a video based on ContentResolver MIME type or file extension.
     */
    fun isUriVideo(context: Context, uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)?.lowercase().orEmpty()
            if (mimeType.startsWith("video/")) {
                true
            } else {
                com.example.data.model.isVideoUrl(uri.toString())
            }
        } catch (_: Throwable) {
            com.example.data.model.isVideoUrl(uri.toString())
        }
    }

    /**
     * Immediately copies a picker/camera `content://` or `file://` [Uri] into permanent
     * internal storage (`context.filesDir/darkroom_uploads`) while temporary URI permissions
     * are active, returning a durable `file://` URI string.
     */
    fun copyUriToPermanentInternalFile(
        context: Context,
        sourceUri: Uri,
        isVideo: Boolean = false
    ): String {
        return try {
            val effectiveIsVideo = isVideo || isUriVideo(context, sourceUri)
            val scheme = sourceUri.scheme?.lowercase().orEmpty()
            if (scheme == "file") {
                val path = sourceUri.path
                if (!path.isNullOrBlank() && File(path).exists() && File(path).length() > 0L) {
                    return sourceUri.toString()
                }
            }
            val outputDir = File(context.filesDir, "darkroom_uploads").apply {
                if (!exists()) mkdirs()
            }
            val ext = if (effectiveIsVideo) "mp4" else "jpg"
            val prefix = if (effectiveIsVideo) "pulse_picked_video_" else "pulse_picked_photo_"
            val outFile = File(
                outputDir,
                "$prefix${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"
            )
            val copiedBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: 0L
            if (copiedBytes > 0L && outFile.exists() && outFile.length() > 0L) {
                Uri.fromFile(outFile).toString()
            } else {
                outFile.delete()
                sourceUri.toString()
            }
        } catch (_: Throwable) {
            sourceUri.toString()
        }
    }

    /**
     * Extracts a real poster frame from a user-selected video URI using [android.media.MediaMetadataRetriever]
     * and writes it to `context.filesDir/darkroom_uploads` so video posts display their actual frame
     * rather than a sample placeholder image.
     */
    fun extractVideoThumbnailFile(
        context: Context,
        videoUriStr: String?
    ): File? {
        if (videoUriStr.isNullOrBlank()) return null
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            when {
                videoUriStr.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(videoUriStr).path
                    if (!path.isNullOrBlank()) {
                        retriever.setDataSource(path)
                    } else {
                        return null
                    }
                }
                videoUriStr.startsWith("/") -> {
                    retriever.setDataSource(videoUriStr)
                }
                videoUriStr.startsWith("content://", ignoreCase = true) ||
                    videoUriStr.startsWith("android.resource://", ignoreCase = true) -> {
                    retriever.setDataSource(context, Uri.parse(videoUriStr))
                }
                else -> return null
            }
            val frameBitmap = retriever.getFrameAtTime(0L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return null
            val scaled = scaleDownIfNeeded(frameBitmap, 1080)
            val outputDir = File(context.filesDir, "darkroom_uploads").apply {
                if (!exists()) mkdirs()
            }
            val outFile = File(
                outputDir,
                "pulse_video_thumb_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            )
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                fos.flush()
            }
            outFile.takeIf { it.exists() && it.length() > 0L }
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Decodes a user-selected photo URI (`content://`, `file://`, or `data:image/`) or fallback drawable resource,
     * applies the Darkroom filters, and writes a permanent JPEG file inside `context.filesDir/darkroom_uploads`
     * so the processed photo survives app restarts and hard refreshes.
     */
    suspend fun processAndBakePhoto(
        context: Context,
        sourceUri: String?,
        fallbackResId: Int,
        preset: FilmPreset,
        adjustments: DarkroomAdjustments = DarkroomAdjustments()
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Only use fallbackResId when the user did NOT provide a sourceUri
            val effectiveFallbackRes = if (sourceUri.isNullOrBlank()) fallbackResId else 0
            val rawBitmap: Bitmap? = decodeSourceBitmap(context, sourceUri, effectiveFallbackRes)
            if (rawBitmap == null) return@withContext null

            val maxDimension = 1440
            val scaledBitmap = scaleDownIfNeeded(rawBitmap, maxDimension)
            val processedBitmap = applyFiltersToBitmap(scaledBitmap, preset, adjustments)

            val outputDir = File(context.filesDir, "darkroom_uploads").apply {
                if (!exists()) mkdirs()
            }
            val outFile = File(
                outputDir,
                "pulse_darkroom_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            )
            FileOutputStream(outFile).use { fos ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
            }
            outFile
        } catch (_: Throwable) {
            null
        }
    }

    fun decodeSourceBitmap(
        context: Context,
        sourceUri: String?,
        fallbackResId: Int = 0
    ): Bitmap? {
        if (!sourceUri.isNullOrBlank()) {
            val trimmed = sourceUri.trim()
            try {
                when {
                    trimmed.startsWith("data:image/", ignoreCase = true) -> {
                        val base64Part = trimmed.substringAfter("base64,", "")
                        if (base64Part.isNotBlank()) {
                            val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (decoded != null) return decoded
                        }
                    }
                    trimmed.startsWith("file://", ignoreCase = true) -> {
                        val path = Uri.parse(trimmed).path ?: trimmed.removePrefix("file://")
                        if (path.isNotBlank()) {
                            val decoded = BitmapFactory.decodeFile(path)
                            if (decoded != null) return decoded
                        }
                    }
                    trimmed.startsWith("/") -> {
                        val decoded = BitmapFactory.decodeFile(trimmed)
                        if (decoded != null) return decoded
                    }
                    trimmed.startsWith("content://", ignoreCase = true) ||
                        trimmed.startsWith("android.resource://", ignoreCase = true) -> {
                        val uri = Uri.parse(trimmed)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            if (bytes.isNotEmpty()) {
                                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (decoded != null) return decoded
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
        if (fallbackResId != 0) {
            return try {
                BitmapFactory.decodeResource(context.resources, fallbackResId)
            } catch (_: Throwable) {
                null
            }
        }
        return null
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / max(w, h).toFloat()
        val targetW = (w * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (h * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}

