package com.example.data.model

import androidx.compose.ui.graphics.ColorMatrix
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.R

fun isVideoUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val clean = url.lowercase().substringBefore("?").substringBefore("#")
    return clean.endsWith(".mp4") ||
        clean.endsWith(".mov") ||
        clean.endsWith(".webm") ||
        clean.endsWith(".m4v") ||
        clean.endsWith(".m3u8") ||
        clean.endsWith(".mkv") ||
        url.lowercase().contains("/videos/") ||
        url.lowercase().contains("media_type=video")
}

const val CAROUSEL_URI_DELIMITER = "|"
const val MAX_CAROUSEL_ITEMS = 10

/**
 * Splits a serialized carousel URI string into up to [MAX_CAROUSEL_ITEMS] individual photo or video URIs,
 * supporting both the pipe delimiter (`|`) and legacy comma-separated lists while preserving commas
 * inside `data:image/...;base64,...` URIs.
 */
fun splitCarouselUris(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    val normalized = raw.trim()
    if (normalized.contains(CAROUSEL_URI_DELIMITER)) {
        return normalized.split(CAROUSEL_URI_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_CAROUSEL_ITEMS)
    }
    val base64CommaPlaceholder = "__PULSE_BASE64_COMMA__"
    val protectedStr = normalized.replace(
        Regex("(data:[^,;]+;base64),", RegexOption.IGNORE_CASE),
        "$1$base64CommaPlaceholder"
    )
    return protectedStr.split(",")
        .map { it.replace(base64CommaPlaceholder, ",").trim() }
        .filter { it.isNotEmpty() }
        .take(MAX_CAROUSEL_ITEMS)
}

/**
 * Joins up to [MAX_CAROUSEL_ITEMS] photo or video URIs using [CAROUSEL_URI_DELIMITER] (`|`)
 * so base64 data URIs or query parameters containing commas are never split prematurely.
 */
fun joinCarouselUris(uris: List<String>): String {
    return uris
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(MAX_CAROUSEL_ITEMS)
        .joinToString(CAROUSEL_URI_DELIMITER)
}

enum class PhotoAspectRatio(val label: String, val ratio: Float) {
    PORTRAIT_4_5("4:5 Portrait", 0.8f),
    SQUARE_1_1("1:1 Contact", 1.0f),
    CINEMA_16_9("16:9 Cinema", 1.77f);

    companion object {
        fun fromName(name: String): PhotoAspectRatio =
            entries.find { it.name == name } ?: PORTRAIT_4_5
    }
}

enum class FilmPreset(
    val displayName: String,
    val code: String,
    val description: String
) {
    NATURAL("Studio Neutral", "STD-100", "True-to-life optical color balance"),
    KODACHROME_64("Kodachrome 64", "KOD-64", "Rich warm highlights and deep archival shadows"),
    TRI_X_400("Tri-X 400", "TX-400", "Classic fine-grain panchromatic street stock"),
    CINESTILL_800T("CineStill 800T", "CS-800T", "Tungsten cinema halation with cool shadows"),
    SILVER_HALIDE_BW("Silver Halide", "AGX-400", "High-contrast panchromatic monochrome print"),
    CYANOTYPE("Prussian Blue", "CYN-20", "Cool cobalt shadow toning with crisp highlights"),
    GOLDEN_HOUR("Amber Dusk", "AMB-800", "Luminous tungsten warmth and lifted mids"),
    HIGH_CONTRAST("Chrome Push +2", "CHR-1600", "Punchy micro-contrast and saturated primaries");

    fun toColorMatrix(): ColorMatrix? {
        return when (this) {
            NATURAL -> null
            KODACHROME_64 -> ColorMatrix(
                floatArrayOf(
                    1.18f, 0.04f, -0.02f, 0f, 8f,
                    0.02f, 1.06f, -0.03f, 0f, 3f,
                    -0.04f, -0.02f, 0.88f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            TRI_X_400 -> ColorMatrix(
                floatArrayOf(
                    0.42f, 0.48f, 0.10f, 0f, -8f,
                    0.42f, 0.48f, 0.10f, 0f, -8f,
                    0.42f, 0.48f, 0.10f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            CINESTILL_800T -> ColorMatrix(
                floatArrayOf(
                    1.12f, -0.04f, 0.08f, 0f, 4f,
                    0.0f, 1.04f, 0.06f, 0f, 2f,
                    -0.05f, 0.08f, 1.24f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            SILVER_HALIDE_BW -> ColorMatrix(
                floatArrayOf(
                    0.38f, 0.52f, 0.10f, 0f, 6f,
                    0.38f, 0.52f, 0.10f, 0f, 6f,
                    0.38f, 0.52f, 0.10f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            CYANOTYPE -> ColorMatrix(
                floatArrayOf(
                    0.78f, 0.10f, 0.12f, 0f, -10f,
                    0.06f, 0.96f, 0.14f, 0f, 2f,
                    0.12f, 0.22f, 1.28f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            GOLDEN_HOUR -> ColorMatrix(
                floatArrayOf(
                    1.22f, 0.08f, 0.0f, 0f, 14f,
                    0.04f, 1.08f, 0.0f, 0f, 6f,
                    -0.06f, -0.04f, 0.82f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            HIGH_CONTRAST -> ColorMatrix(
                floatArrayOf(
                    1.32f, -0.06f, -0.06f, 0f, -18f,
                    -0.06f, 1.28f, -0.06f, 0f, -16f,
                    -0.06f, -0.06f, 1.32f, 0f, -16f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }

    fun toAdjustedColorMatrix(
        exposure: Float = 0f,
        contrast: Float = 1f,
        warmth: Float = 0f,
        brightness: Float = 0f,
        saturation: Float = 1f
    ): ColorMatrix? {
        val base = toColorMatrix()
        val hasSliders = kotlin.math.abs(exposure) > 0.005f ||
            kotlin.math.abs(contrast - 1f) > 0.005f ||
            kotlin.math.abs(warmth) > 0.005f ||
            kotlin.math.abs(brightness) > 0.005f ||
            kotlin.math.abs(saturation - 1f) > 0.005f
        if (base == null && !hasSliders) return null

        val result = ColorMatrix()
        if (base != null) {
            result.set(base)
        }

        val sat = saturation.coerceIn(0f, 2f)
        if (kotlin.math.abs(sat - 1f) > 0.005f) {
            val satMatrix = ColorMatrix().apply { setToSaturation(sat) }
            result.timesAssign(satMatrix)
        }

        val c = contrast.coerceIn(0.5f, 1.6f)
        val expOffset = exposure.coerceIn(-1f, 1f) * 55f
        val brightOffset = brightness.coerceIn(-1f, 1f) * 70f
        val totalLightOffset = expOffset + brightOffset
        val contrastTranslate = (1f - c) * 128f
        val warmR = warmth.coerceIn(-1f, 1f) * 22f
        val warmB = -warmth.coerceIn(-1f, 1f) * 22f

        val adjustmentMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, totalLightOffset + contrastTranslate + warmR,
                0f, c, 0f, 0f, totalLightOffset + contrastTranslate + (warmR * 0.25f),
                0f, 0f, c, 0f, totalLightOffset + contrastTranslate + warmB,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.timesAssign(adjustmentMatrix)
        return result
    }

    companion object {
        fun fromName(name: String): FilmPreset =
            entries.find { it.name == name } ?: NATURAL
    }
}

data class DarkroomAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val warmth: Float = 0f,
    val filmGrain: Float = 0f,
    val vignette: Float = 0f,
    val hasExifFrameBorder: Boolean = false,
    val exifFrameStyle: String = "CONTACT_SHEET"
)

enum class FeedMode(val label: String) {
    CURATED("Pulse curated"),
    FOLLOWING("Following"),
    SAVED("Saved")
}

enum class StudioPublishTarget(val label: String) {
    FEED_POST("Feed Post"),
    STORY_24H("24h Story"),
    LOOP_REEL("Pulse Loop")
}

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String = "",
    val userId: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null
)

@Entity(
    tableName = "posts",
    indices = [
        Index(value = ["timestampMs"]),
        Index(value = ["authorId"]),
        Index(value = ["category"])
    ]
)
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarRes: Int = R.drawable.img_post_portrait_1794021773202,
    val authorAvatarUrl: String? = null,
    val authorVerified: Boolean = false,
    val imageRes: Int = R.drawable.img_post_kyoto_1794021771843,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val carouselImageUrisCsv: String = "",
    val carouselImageResCsv: String = "",
    val aspectRatioName: String = PhotoAspectRatio.PORTRAIT_4_5.name,
    val filterPresetName: String = FilmPreset.NATURAL.name,
    val brightness: Float = 0f,
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val filmGrain: Float = 0f,
    val vignette: Float = 0f,
    val hasExifFrameBorder: Boolean = false,
    val exifFrameStyle: String = "CONTACT_SHEET",
    val isPinnedOnProfile: Boolean = false,
    val caption: String,
    val location: String = "",
    val category: String = "Photography",
    val tagsCsv: String = "",
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val likedByUserIdsCsv: String = "",
    val lastLikedAtMs: Long = 0L,
    val isBookmarked: Boolean = false,
    val moodboardName: String = "All Saved",
    val commentCount: Int = 0,
    val isLoop: Boolean = false,
    val audioTrack: String = "Original Studio Audio",
    val cameraSpec: String = "Pulse Optical",
    val createdAtLabel: String = "Recent",
    val timestampMs: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val cachedAtMs: Long = System.currentTimeMillis()
) {
    val aspectRatio: PhotoAspectRatio
        get() = PhotoAspectRatio.fromName(aspectRatioName)

    val filterPreset: FilmPreset
        get() = FilmPreset.fromName(filterPresetName)

    val tags: List<String>
        get() = tagsCsv.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }

    val hasVideo: Boolean
        get() = !videoUri.isNullOrBlank() || isVideoUrl(imageUri)

    val effectiveVideoUri: String?
        get() = videoUri?.takeIf { it.isNotBlank() } ?: imageUri?.takeIf { isVideoUrl(it) }

    val carouselFrames: List<Pair<Int, String?>>
        get() {
            val uriItems = splitCarouselUris(carouselImageUrisCsv)
            if (uriItems.size >= 2) {
                return uriItems.map { imageRes to it }
            }
            val resItems = carouselImageResCsv
                .split(CAROUSEL_URI_DELIMITER, ",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it != 0 }
                .take(MAX_CAROUSEL_ITEMS)
            if (resItems.size >= 2) {
                return resItems.map { it to null }
            }
            return listOf(imageRes to imageUri)
        }
}

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarRes: Int = R.drawable.img_post_portrait_1794021773202,
    val authorAvatarUrl: String? = null,
    val imageRes: Int = R.drawable.img_post_kyoto_1794021771843,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val filterPresetName: String = FilmPreset.NATURAL.name,
    val captionOverlay: String,
    val locationTag: String = "",
    val isViewed: Boolean = false,
    val isHighlight: Boolean = false,
    val highlightTitle: String = "",
    val createdAtLabel: String = "Recent",
    val timestampMs: Long = System.currentTimeMillis()
) {
    val filterPreset: FilmPreset
        get() = FilmPreset.fromName(filterPresetName)

    val hasVideo: Boolean
        get() = !videoUri.isNullOrBlank() || isVideoUrl(imageUri)

    val effectiveVideoUri: String?
        get() = videoUri?.takeIf { it.isNotBlank() } ?: imageUri?.takeIf { isVideoUrl(it) }
}

data class StoryHighlightGroup(
    val id: String,
    val title: String,
    val coverImageUri: String?,
    val coverImageRes: Int,
    val stories: List<StoryEntity>
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String = "",
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarRes: Int = R.drawable.img_post_portrait_1794021773202,
    val authorAvatarUrl: String? = null,
    val text: String,
    val parentCommentId: String? = null,
    val replyToUsername: String? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val timeAgo: String = "Recent",
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "creators")
data class CreatorProfileEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val roleTag: String = "Creator",
    val bio: String = "",
    val website: String = "",
    val location: String = "",
    val avatarRes: Int = R.drawable.img_post_portrait_1794021773202,
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isVerified: Boolean = false,
    val isBlocked: Boolean = false
)

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val participantId: String,
    val participantUsername: String,
    val participantDisplayName: String,
    val participantAvatarRes: Int = R.drawable.img_post_portrait_1794021773202,
    val participantAvatarUrl: String? = null,
    val isFromMe: Boolean,
    val text: String,
    val sharedPostId: String? = null,
    val sharedPostCaption: String? = null,
    val sharedPostImageRes: Int? = null,
    val sharedPostImageUri: String? = null,
    val voiceNoteDurationSec: Int = 0,
    val timeLabel: String = "Recent",
    val isLiked: Boolean = false,
    val isUnread: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class DirectMessageThreadSummary(
    val threadId: String,
    val participantId: String,
    val participantUsername: String,
    val participantDisplayName: String,
    val participantAvatarRes: Int,
    val participantAvatarUrl: String? = null,
    val lastMessageText: String,
    val lastTimeLabel: String,
    val unreadCount: Int,
    val messages: List<DirectMessageEntity>
)

@Entity(tableName = "activities")
data class ActivityItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val actorId: String,
    val actorUsername: String,
    val actorDisplayName: String,
    val actorAvatarRes: Int = R.drawable.img_post_portrait_1794021773202,
    val actorAvatarUrl: String? = null,
    val message: String,
    val targetPostId: String? = null,
    val targetPostImageRes: Int? = null,
    val targetPostImageUri: String? = null,
    val timeLabel: String = "Recent",
    val isRead: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class UserProfileState(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val roleTag: String = "Visual Creator",
    val bio: String = "",
    val website: String = "",
    val location: String = "",
    val avatarRes: Int = R.drawable.img_post_portrait_1794021773202,
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

const val STUDIO_AUTOSAVE_DRAFT_ID = "studio_autosave_active"

@Entity(
    tableName = "studio_drafts",
    indices = [Index(value = ["updatedAtMs"])]
)
data class StudioDraftEntity(
    @PrimaryKey val id: String,
    val targetName: String = StudioPublishTarget.FEED_POST.name,
    val currentStepName: String = "MEDIA",
    val imageUri: String? = null,
    val videoUri: String? = null,
    val carouselImageUrisCsv: String = "",
    val aspectRatioName: String = PhotoAspectRatio.PORTRAIT_4_5.name,
    val filterPresetName: String = FilmPreset.NATURAL.name,
    val brightness: Float = 0f,
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val filmGrain: Float = 0f,
    val vignette: Float = 0f,
    val hasExifFrameBorder: Boolean = false,
    val exifFrameStyle: String = "GALLERY",
    val caption: String = "",
    val location: String = "",
    val category: String = "",
    val tagsInput: String = "",
    val audioTrack: String = "",
    val cameraSpec: String = "",
    val highlightTitle: String = "",
    val isAutoSave: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    val publishTarget: StudioPublishTarget
        get() = StudioPublishTarget.entries.find { it.name == targetName } ?: StudioPublishTarget.FEED_POST

    val aspectRatio: PhotoAspectRatio
        get() = PhotoAspectRatio.fromName(aspectRatioName)

    val filterPreset: FilmPreset
        get() = FilmPreset.fromName(filterPresetName)

    val carouselUris: List<String>
        get() = splitCarouselUris(carouselImageUrisCsv)

    val hasVideo: Boolean
        get() = !videoUri.isNullOrBlank() || isVideoUrl(imageUri) || carouselUris.any { isVideoUrl(it) }

    val previewThumbnailUri: String?
        get() = imageUri?.takeIf { it.isNotBlank() && !isVideoUrl(it) }
            ?: carouselUris.firstOrNull { !isVideoUrl(it) }

    val hasMedia: Boolean
        get() = carouselUris.isNotEmpty() || !videoUri.isNullOrBlank() || !imageUri.isNullOrBlank()
}

