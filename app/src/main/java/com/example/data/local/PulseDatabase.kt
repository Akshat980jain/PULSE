package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ActivityItemEntity
import com.example.data.model.CommentEntity
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.DirectMessageEntity
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.StudioDraftEntity
import com.example.database.PostLike
import com.example.database.User
import com.example.database.UserInteraction
import kotlinx.coroutines.flow.Flow

@Dao
interface PulseDao {
    @Query("SELECT * FROM posts ORDER BY timestampMs DESC")
    fun observeAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE category = :category ORDER BY timestampMs DESC")
    fun observePostsByCategory(category: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY timestampMs DESC")
    fun observePostsByAuthor(authorId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY timestampMs DESC")
    suspend fun getCachedPostsSnapshot(): List<PostEntity>

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getCachedPostCount(): Int

    @Query("SELECT MAX(cachedAtMs) FROM posts")
    suspend fun getLatestCacheTimestampMs(): Long?

    @Query("SELECT * FROM posts WHERE isLiked = 1 ORDER BY lastLikedAtMs DESC, timestampMs DESC")
    fun observeLikedPosts(): Flow<List<PostEntity>>

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

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
    suspend fun deletePost(postId: String)

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

    @Query("SELECT * FROM stories ORDER BY isViewed ASC, timestampMs DESC")
    fun observeStories(): Flow<List<StoryEntity>>

    @Query("DELETE FROM stories")
    suspend fun clearAllStories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Update
    suspend fun updateStory(story: StoryEntity)

    @Query("SELECT * FROM comments ORDER BY timestampMs ASC")
    fun observeAllComments(): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestampMs ASC")
    fun observeCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Query("DELETE FROM comments")
    suspend fun clearAllComments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Update
    suspend fun updateComment(comment: CommentEntity)

    @Query("SELECT * FROM creators ORDER BY isFollowing DESC, followersCount DESC")
    fun observeCreators(): Flow<List<CreatorProfileEntity>>

    @Query("DELETE FROM creators")
    suspend fun clearAllCreators()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreators(creators: List<CreatorProfileEntity>)

    @Update
    suspend fun updateCreator(creator: CreatorProfileEntity)

    @Query("SELECT * FROM users ORDER BY isFollowing DESC, followersCount DESC")
    fun observeUsers(): Flow<List<User>>

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM direct_messages ORDER BY timestampMs ASC")
    fun observeDirectMessages(): Flow<List<DirectMessageEntity>>

    @Query("DELETE FROM direct_messages")
    suspend fun clearAllDirectMessages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessages(messages: List<DirectMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessage(message: DirectMessageEntity)

    @Update
    suspend fun updateDirectMessage(message: DirectMessageEntity)

    @Query("UPDATE direct_messages SET isUnread = 0 WHERE threadId = :threadId")
    suspend fun markThreadRead(threadId: String)

    @Query("SELECT * FROM activities ORDER BY timestampMs DESC")
    fun observeActivities(): Flow<List<ActivityItemEntity>>

    @Query("DELETE FROM activities")
    suspend fun clearAllActivities()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityItemEntity)

    @Query("UPDATE activities SET isRead = 1")
    suspend fun markAllActivitiesRead()

    @Query("SELECT * FROM studio_drafts ORDER BY updatedAtMs DESC")
    fun observeStudioDrafts(): Flow<List<StudioDraftEntity>>

    @Query("SELECT * FROM studio_drafts WHERE id = :draftId LIMIT 1")
    suspend fun getStudioDraftById(draftId: String): StudioDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudioDraft(draft: StudioDraftEntity)

    @Query("DELETE FROM studio_drafts WHERE id = :draftId")
    suspend fun deleteStudioDraft(draftId: String)

    @Query("DELETE FROM studio_drafts WHERE isAutoSave = 1")
    suspend fun clearAutoSaveStudioDrafts()
}

@Database(
    entities = [
        PostEntity::class,
        StoryEntity::class,
        CommentEntity::class,
        CreatorProfileEntity::class,
        User::class,
        PostLike::class,
        UserInteraction::class,
        DirectMessageEntity::class,
        ActivityItemEntity::class,
        StudioDraftEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun pulseDao(): PulseDao

    companion object {
        @Volatile
        private var INSTANCE: PulseDatabase? = null

        val MIGRATION_9_10 = object : Migration(9, 10) {
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

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_timestampMs` ON `posts` (`timestampMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_authorId` ON `posts` (`authorId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_category` ON `posts` (`category`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `studio_drafts` (
                        `id` TEXT NOT NULL,
                        `targetName` TEXT NOT NULL,
                        `currentStepName` TEXT NOT NULL,
                        `imageUri` TEXT,
                        `videoUri` TEXT,
                        `carouselImageUrisCsv` TEXT NOT NULL,
                        `aspectRatioName` TEXT NOT NULL,
                        `filterPresetName` TEXT NOT NULL,
                        `brightness` REAL NOT NULL,
                        `exposure` REAL NOT NULL,
                        `contrast` REAL NOT NULL,
                        `saturation` REAL NOT NULL,
                        `warmth` REAL NOT NULL,
                        `filmGrain` REAL NOT NULL,
                        `vignette` REAL NOT NULL,
                        `hasExifFrameBorder` INTEGER NOT NULL,
                        `exifFrameStyle` TEXT NOT NULL,
                        `caption` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `tagsInput` TEXT NOT NULL,
                        `audioTrack` TEXT NOT NULL,
                        `cameraSpec` TEXT NOT NULL,
                        `highlightTitle` TEXT NOT NULL,
                        `isAutoSave` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_studio_drafts_updatedAtMs` ON `studio_drafts` (`updatedAtMs`)"
                )
            }
        }

        fun getInstance(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabaseSafely(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabaseSafely(appContext: Context): PulseDatabase {
            return try {
                Room.databaseBuilder(
                    appContext,
                    PulseDatabase::class.java,
                    "pulse_supabase_cache.db"
                )
                    .addMigrations(MIGRATION_11_12)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { db ->
                        db.openHelper.writableDatabase
                    }
            } catch (_: Throwable) {
                try {
                    appContext.deleteDatabase("pulse_supabase_cache.db")
                } catch (_: Throwable) {
                }
                Room.databaseBuilder(
                    appContext,
                    PulseDatabase::class.java,
                    "pulse_supabase_cache.db"
                )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
            }
        }
    }
}
