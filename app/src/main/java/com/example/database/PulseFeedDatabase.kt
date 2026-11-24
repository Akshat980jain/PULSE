package com.example.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.R
import com.example.data.model.CommentEntity
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.FilmPreset
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room data entity representing a User in the Pulse social feed local cache.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val email: String = "",
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
    val isBlocked: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Room data entity representing a Post in the Pulse social feed local cache,
 * including user interaction tracking fields ([likeCount], [isLiked], [likedByUserIdsCsv], [lastLikedAtMs])
 * and Darkroom adjustments ([brightness], [contrast], [saturation], [exposure], [warmth]).
 */
@Entity(
    tableName = "posts",
    indices = [
        Index(value = ["timestampMs"]),
        Index(value = ["authorId"]),
        Index(value = ["category"])
    ]
)
data class Post(
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
)

/**
 * Room data entity representing a 24-hour Story or Highlight in the Pulse social feed local cache.
 */
@Entity(tableName = "stories")
data class Story(
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
)

/**
 * Room data entity representing a user comment on an individual photo in the Pulse feed.
 */
@Entity(
    tableName = "comments",
    indices = [Index(value = ["postId"])]
)
data class Comment(
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

/**
 * Room data entity tracking per-user post like interactions in the local database.
 */
@Entity(
    tableName = "post_likes",
    primaryKeys = ["postId", "userId"],
    indices = [Index(value = ["postId"]), Index(value = ["userId"])]
)
data class PostLike(
    val postId: String,
    val userId: String,
    val username: String = "",
    val isLiked: Boolean = true,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Room data entity tracking user interactions (such as LIKE, BOOKMARK, SHARE) on feed posts.
 */
@Entity(
    tableName = "user_interactions",
    indices = [Index(value = ["postId"]), Index(value = ["userId"])]
)
data class UserInteraction(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String,
    val interactionType: String = "LIKE",
    val isActive: Boolean = true,
    val timestampMs: Long = System.currentTimeMillis()
)

@Dao
interface PulseFeedDao {
    @Query("SELECT * FROM posts ORDER BY timestampMs DESC")
    fun observePosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE category = :category ORDER BY timestampMs DESC")
    fun observePostsByCategory(category: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY timestampMs DESC")
    fun observePostsByAuthor(authorId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts ORDER BY timestampMs DESC")
    suspend fun getCachedPostsSnapshot(): List<Post>

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getCachedPostCount(): Int

    @Query("SELECT MAX(cachedAtMs) FROM posts")
    suspend fun getLatestCacheTimestampMs(): Long?

    @Query("SELECT * FROM posts WHERE isLiked = 1 ORDER BY lastLikedAtMs DESC, timestampMs DESC")
    fun observeLikedPosts(): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Update
    suspend fun updatePost(post: Post)

    @Query(
        "UPDATE posts SET isLiked = :isLiked, likeCount = :likeCount, likedByUserIdsCsv = :likedByUserIdsCsv, lastLikedAtMs = :timestampMs WHERE id = :postId"
    )
    suspend fun updatePostLikeState(
        postId: String,
        isLiked: Boolean,
        likeCount: Int,
        likedByUserIdsCsv: String = "",
        timestampMs: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM posts")
    suspend fun clearPosts()

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()

    @Query("SELECT * FROM comments ORDER BY timestampMs ASC")
    fun observeAllComments(): Flow<List<Comment>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestampMs ASC")
    fun observeCommentsForPost(postId: String): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<Comment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    @Update
    suspend fun updateComment(comment: Comment)

    @Query("DELETE FROM comments")
    suspend fun clearAllComments()

    @Query("SELECT * FROM post_likes WHERE isLiked = 1 ORDER BY timestampMs DESC")
    fun observePostLikes(): Flow<List<PostLike>>

    @Query("SELECT * FROM post_likes WHERE postId = :postId AND isLiked = 1 ORDER BY timestampMs DESC")
    fun observeLikesForPost(postId: String): Flow<List<PostLike>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPostLike(postLike: PostLike)

    @Query("DELETE FROM post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun deletePostLike(postId: String, userId: String)

    @Query("DELETE FROM post_likes")
    suspend fun clearAllPostLikes()

    @Query("SELECT * FROM user_interactions WHERE isActive = 1 ORDER BY timestampMs DESC")
    fun observeUserInteractions(): Flow<List<UserInteraction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInteraction(interaction: UserInteraction)

    @Query(
        "DELETE FROM user_interactions WHERE postId = :postId AND userId = :userId AND interactionType = :interactionType"
    )
    suspend fun deleteUserInteraction(
        postId: String,
        userId: String,
        interactionType: String = "LIKE"
    )

    @Query("DELETE FROM user_interactions")
    suspend fun clearAllUserInteractions()

    @Query("SELECT * FROM users ORDER BY isFollowing DESC, followersCount DESC")
    fun observeUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()

    @Query("SELECT * FROM stories ORDER BY isViewed ASC, timestampMs DESC")
    fun observeStories(): Flow<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<Story>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Update
    suspend fun updateStory(story: Story)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: String)

    @Query("DELETE FROM stories")
    suspend fun clearStories()

    @Query("DELETE FROM stories")
    suspend fun clearAllStories()
}

@Database(
    entities = [
        Post::class,
        User::class,
        Story::class,
        Comment::class,
        PostLike::class,
        UserInteraction::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PulseFeedDatabase : RoomDatabase() {
    abstract fun pulseFeedDao(): PulseFeedDao

    companion object {
        @Volatile
        private var INSTANCE: PulseFeedDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `posts` ADD COLUMN `cachedAtMs` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Throwable) {
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_timestampMs` ON `posts` (`timestampMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_authorId` ON `posts` (`authorId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_category` ON `posts` (`category`)")
            }
        }

        fun getInstance(context: Context): PulseFeedDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabaseSafely(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabaseSafely(appContext: Context): PulseFeedDatabase {
            return try {
                Room.databaseBuilder(
                    appContext,
                    PulseFeedDatabase::class.java,
                    "pulse_feed_local.db"
                )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { db ->
                        db.openHelper.writableDatabase
                    }
            } catch (_: Throwable) {
                try {
                    appContext.deleteDatabase("pulse_feed_local.db")
                } catch (_: Throwable) {
                }
                Room.databaseBuilder(
                    appContext,
                    PulseFeedDatabase::class.java,
                    "pulse_feed_local.db"
                )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
            }
        }
    }
}

fun PostEntity.toDatabasePost(): Post = Post(
    id = id,
    authorId = authorId,
    authorUsername = authorUsername,
    authorDisplayName = authorDisplayName,
    authorAvatarRes = authorAvatarRes,
    authorAvatarUrl = authorAvatarUrl,
    authorVerified = authorVerified,
    imageRes = imageRes,
    imageUri = imageUri,
    videoUri = videoUri,
    carouselImageUrisCsv = carouselImageUrisCsv,
    carouselImageResCsv = carouselImageResCsv,
    aspectRatioName = aspectRatioName,
    filterPresetName = filterPresetName,
    brightness = brightness,
    exposure = exposure,
    contrast = contrast,
    saturation = saturation,
    warmth = warmth,
    filmGrain = filmGrain,
    vignette = vignette,
    hasExifFrameBorder = hasExifFrameBorder,
    exifFrameStyle = exifFrameStyle,
    isPinnedOnProfile = isPinnedOnProfile,
    caption = caption,
    location = location,
    category = category,
    tagsCsv = tagsCsv,
    likeCount = likeCount,
    isLiked = isLiked,
    likedByUserIdsCsv = likedByUserIdsCsv,
    lastLikedAtMs = lastLikedAtMs,
    isBookmarked = isBookmarked,
    moodboardName = moodboardName,
    commentCount = commentCount,
    isLoop = isLoop,
    audioTrack = audioTrack,
    cameraSpec = cameraSpec,
    createdAtLabel = createdAtLabel,
    timestampMs = timestampMs,
    cachedAtMs = cachedAtMs
)

fun Post.toPostEntity(): PostEntity = PostEntity(
    id = id,
    authorId = authorId,
    authorUsername = authorUsername,
    authorDisplayName = authorDisplayName,
    authorAvatarRes = authorAvatarRes,
    authorAvatarUrl = authorAvatarUrl,
    authorVerified = authorVerified,
    imageRes = imageRes,
    imageUri = imageUri,
    videoUri = videoUri,
    carouselImageUrisCsv = carouselImageUrisCsv,
    carouselImageResCsv = carouselImageResCsv,
    aspectRatioName = aspectRatioName,
    filterPresetName = filterPresetName,
    brightness = brightness,
    exposure = exposure,
    contrast = contrast,
    saturation = saturation,
    warmth = warmth,
    filmGrain = filmGrain,
    vignette = vignette,
    hasExifFrameBorder = hasExifFrameBorder,
    exifFrameStyle = exifFrameStyle,
    isPinnedOnProfile = isPinnedOnProfile,
    caption = caption,
    location = location,
    category = category,
    tagsCsv = tagsCsv,
    likeCount = likeCount,
    isLiked = isLiked,
    likedByUserIdsCsv = likedByUserIdsCsv,
    lastLikedAtMs = lastLikedAtMs,
    isBookmarked = isBookmarked,
    moodboardName = moodboardName,
    commentCount = commentCount,
    isLoop = isLoop,
    audioTrack = audioTrack,
    cameraSpec = cameraSpec,
    createdAtLabel = createdAtLabel,
    timestampMs = timestampMs,
    cachedAtMs = cachedAtMs
)

fun CommentEntity.toDatabaseComment(): Comment = Comment(
    id = id,
    postId = postId,
    authorId = authorId,
    authorUsername = authorUsername,
    authorDisplayName = authorDisplayName,
    authorAvatarRes = authorAvatarRes,
    authorAvatarUrl = authorAvatarUrl,
    text = text,
    parentCommentId = parentCommentId,
    replyToUsername = replyToUsername,
    likeCount = likeCount,
    isLiked = isLiked,
    timeAgo = timeAgo,
    timestampMs = timestampMs
)

fun Comment.toCommentEntity(): CommentEntity = CommentEntity(
    id = id,
    postId = postId,
    authorId = authorId,
    authorUsername = authorUsername,
    authorDisplayName = authorDisplayName,
    authorAvatarRes = authorAvatarRes,
    authorAvatarUrl = authorAvatarUrl,
    text = text,
    parentCommentId = parentCommentId,
    replyToUsername = replyToUsername,
    likeCount = likeCount,
    isLiked = isLiked,
    timeAgo = timeAgo,
    timestampMs = timestampMs
)

fun CreatorProfileEntity.toDatabaseUser(): User = User(
    id = id,
    username = username,
    displayName = displayName,
    roleTag = roleTag,
    bio = bio,
    website = website,
    location = location,
    avatarRes = avatarRes,
    avatarUrl = avatarUrl,
    followersCount = followersCount,
    followingCount = followingCount,
    isFollowing = isFollowing,
    isVerified = isVerified,
    isBlocked = isBlocked
)

fun User.toCreatorProfileEntity(): CreatorProfileEntity = CreatorProfileEntity(
    id = id,
    username = username,
    displayName = displayName,
    roleTag = roleTag,
    bio = bio,
    website = website,
    location = location,
    avatarRes = avatarRes,
    avatarUrl = avatarUrl,
    followersCount = followersCount,
    followingCount = followingCount,
    isFollowing = isFollowing,
    isVerified = isVerified,
    isBlocked = isBlocked
)

fun StoryEntity.toDatabaseStory(): Story = Story(
    id = id,
    authorId = authorId,
    authorUsername = authorUsername,
    authorDisplayName = authorDisplayName,
    authorAvatarRes = authorAvatarRes,
    authorAvatarUrl = authorAvatarUrl,
    imageRes = imageRes,
    imageUri = imageUri,
    videoUri = videoUri,
    filterPresetName = filterPresetName,
    captionOverlay = captionOverlay,
    locationTag = locationTag,
    isViewed = isViewed,
    isHighlight = isHighlight,
    highlightTitle = highlightTitle,
    createdAtLabel = createdAtLabel,
    timestampMs = timestampMs
)

fun Story.toStoryEntity(): StoryEntity = StoryEntity(
    id = id,
    authorId = authorId,
    authorUsername = authorUsername,
    authorDisplayName = authorDisplayName,
    authorAvatarRes = authorAvatarRes,
    authorAvatarUrl = authorAvatarUrl,
    imageRes = imageRes,
    imageUri = imageUri,
    videoUri = videoUri,
    filterPresetName = filterPresetName,
    captionOverlay = captionOverlay,
    locationTag = locationTag,
    isViewed = isViewed,
    isHighlight = isHighlight,
    highlightTitle = highlightTitle,
    createdAtLabel = createdAtLabel,
    timestampMs = timestampMs
)
