package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.local.PulseDao
import com.example.data.model.ActivityItemEntity
import com.example.data.model.CommentEntity
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.DarkroomAdjustments
import com.example.data.model.DirectMessageEntity
import com.example.data.model.FilmPreset
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.PostEntity
import com.example.data.model.STUDIO_AUTOSAVE_DRAFT_ID
import com.example.data.model.StoryEntity
import com.example.data.model.StudioDraftEntity
import com.example.data.model.SupabaseSession
import com.example.data.model.UserProfileState
import com.example.data.model.MAX_CAROUSEL_ITEMS
import com.example.data.model.isVideoUrl
import com.example.data.model.joinCarouselUris
import com.example.data.model.splitCarouselUris
import com.example.data.remote.SupabaseClient
import com.example.data.service.DarkroomProcessingService
import com.example.data.service.PulseNotificationManager
import com.example.database.Comment
import com.example.database.Post
import com.example.database.PostLike
import com.example.database.PulseFeedDao
import com.example.database.PulseFeedDatabase
import com.example.database.Story
import com.example.database.User
import com.example.database.UserInteraction
import com.example.database.toDatabaseComment
import com.example.database.toDatabasePost
import com.example.database.toDatabaseStory
import com.example.database.toDatabaseUser
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PulseRepository(
    private val dao: PulseDao,
    context: Context,
    private val feedDao: PulseFeedDao = PulseFeedDatabase.getInstance(context.applicationContext).pulseFeedDao()
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 10
        const val DEFAULT_CACHE_TTL_MS = 5 * 60 * 1000L
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("pulse_supabase_session", Context.MODE_PRIVATE)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val notificationManager: PulseNotificationManager = PulseNotificationManager.getInstance(appContext)

    private val _session = MutableStateFlow<SupabaseSession?>(loadSavedSession())
    val session: StateFlow<SupabaseSession?> = _session.asStateFlow()

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _moodboards = MutableStateFlow(loadMoodboards())
    val moodboards: StateFlow<List<String>> = _moodboards.asStateFlow()

    private val _hasMorePosts = MutableStateFlow(true)
    val hasMorePosts: StateFlow<Boolean> = _hasMorePosts.asStateFlow()

    @Volatile
    private var currentPostsOffset: Int = 0

    @Volatile
    private var cachedProfiles: List<CreatorProfileEntity> = emptyList()

    val posts: Flow<List<PostEntity>> = dao.observeAllPosts()
    val stories: Flow<List<StoryEntity>> = dao.observeStories()
    val comments: Flow<List<CommentEntity>> = dao.observeAllComments()
    val creators: Flow<List<CreatorProfileEntity>> = dao.observeCreators()
    val users: Flow<List<User>> = dao.observeUsers()
    val directMessages: Flow<List<DirectMessageEntity>> = dao.observeDirectMessages()
    val activities: Flow<List<ActivityItemEntity>> = dao.observeActivities()
    val studioDrafts: Flow<List<StudioDraftEntity>> = dao.observeStudioDrafts()

    val cachedPosts: Flow<List<Post>> = feedDao.observePosts()
    val cachedUsers: Flow<List<User>> = feedDao.observeUsers()
    val cachedStories: Flow<List<Story>> = feedDao.observeStories()
    val cachedComments: Flow<List<Comment>> = feedDao.observeAllComments()
    val postLikes: Flow<List<PostLike>> = feedDao.observePostLikes()
    val userInteractions: Flow<List<UserInteraction>> = feedDao.observeUserInteractions()

    fun observeCommentsForPost(postId: String): Flow<List<CommentEntity>> =
        dao.observeCommentsForPost(postId)

    fun observePostsByCategory(category: String): Flow<List<PostEntity>> =
        dao.observePostsByCategory(category)

    suspend fun getCachedPostsSnapshot(): List<PostEntity> =
        dao.getCachedPostsSnapshot()

    suspend fun isFeedCacheFresh(maxAgeMs: Long = DEFAULT_CACHE_TTL_MS): Boolean {
        val count = dao.getCachedPostCount()
        if (count <= 0) return false
        val latestCachedAt = dao.getLatestCacheTimestampMs() ?: return false
        return (System.currentTimeMillis() - latestCachedAt) in 0..maxAgeMs
    }

    init {
        repoScope.launch {
            try {
                val journalPosts = loadAuthoredPostsFromJournal()
                if (journalPosts.isNotEmpty()) {
                    dao.insertPosts(journalPosts)
                    feedDao.insertPosts(journalPosts.map { it.toDatabasePost() })
                }
                val journalComments = loadAuthoredCommentsFromJournal()
                if (journalComments.isNotEmpty()) {
                    dao.insertComments(journalComments)
                    feedDao.insertComments(journalComments.map { it.toDatabaseComment() })
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun loadSavedSession(): SupabaseSession? {
        val token = prefs.getString("access_token", null)
        val userId = prefs.getString("user_id", null)
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            SupabaseClient.setAccessToken(token)
            return SupabaseSession(
                accessToken = token,
                refreshToken = prefs.getString("refresh_token", "") ?: "",
                userId = userId,
                email = prefs.getString("email", "") ?: "",
                username = prefs.getString("username", "") ?: "",
                displayName = prefs.getString("display_name", "") ?: "",
                avatarUrl = SupabaseClient.resolveSupabaseMediaUrl(
                    prefs.getString("avatar_url", null),
                    defaultBucket = "avatars"
                )
            )
        }
        val explicitlySignedOut = prefs.getBoolean("explicitly_signed_out", false)
        if (!explicitlySignedOut) {
            val remembered = findMostRecentRememberedAccount()
            if (remembered != null) {
                SupabaseClient.setAccessToken(remembered.accessToken)
                prefs.edit()
                    .putString("access_token", remembered.accessToken)
                    .putString("refresh_token", remembered.refreshToken)
                    .putString("user_id", remembered.userId)
                    .putString("email", remembered.email)
                    .putString("username", remembered.username)
                    .putString("display_name", remembered.displayName)
                    .putString("avatar_url", remembered.avatarUrl)
                    .commit()
                return remembered
            }
        }
        return null
    }

    private fun findMostRecentRememberedAccount(): SupabaseSession? {
        return try {
            val lastEmail = prefs.getString("last_logged_in_email", null)?.trim()?.lowercase()
            if (!lastEmail.isNullOrBlank()) {
                findRememberedAccountForEmail(lastEmail)?.let { return it }
            }
            val raw = prefs.getString("registered_accounts_json", "{}") ?: "{}"
            val root = JSONObject(raw)
            val keys = root.keys()
            var candidateEmail: String? = null
            while (keys.hasNext()) {
                candidateEmail = keys.next()
            }
            if (!candidateEmail.isNullOrBlank()) {
                findRememberedAccountForEmail(candidateEmail)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun continueAsExistingUser(): SupabaseSession {
        _session.value?.let { return it }
        val remembered = findMostRecentRememberedAccount()
        if (remembered != null) {
            saveSession(remembered)
            return remembered
        }
        val fallbackEmail = prefs.getString("last_logged_in_email", null)
            ?.takeIf { it.isNotBlank() }
            ?: "creator@pulse.studio"
        val fallbackHandle = prefs.getString("last_logged_in_username", null)
            ?.takeIf { it.isNotBlank() }
            ?: fallbackEmail.substringBefore("@").ifBlank { "pulse.creator" }
        val fallbackDisplay = prefs.getString("last_logged_in_display_name", null)
            ?.takeIf { it.isNotBlank() }
            ?: fallbackHandle.replaceFirstChar { it.uppercase() }
        val uid = prefs.getString("last_logged_in_user_id", null)
            ?.takeIf { it.isNotBlank() }
            ?: UUID.nameUUIDFromBytes(fallbackEmail.lowercase().toByteArray()).toString()
        val existingSession = SupabaseSession(
            accessToken = "pulse_session_$uid",
            refreshToken = "",
            userId = uid,
            email = fallbackEmail,
            username = fallbackHandle,
            displayName = fallbackDisplay,
            avatarUrl = prefs.getString("last_logged_in_avatar_url", null)
        )
        saveSession(existingSession)
        return existingSession
    }

    private fun saveSession(session: SupabaseSession) {
        val resolvedAvatar = SupabaseClient.resolveSupabaseMediaUrl(session.avatarUrl, defaultBucket = "avatars")
        val normalized = session.copy(
            accessToken = session.accessToken.ifBlank { "pulse_session_${session.userId}" },
            avatarUrl = resolvedAvatar
        )
        SupabaseClient.setAccessToken(normalized.accessToken)
        prefs.edit()
            .putString("access_token", normalized.accessToken)
            .putString("refresh_token", normalized.refreshToken)
            .putString("user_id", normalized.userId)
            .putString("email", normalized.email)
            .putString("username", normalized.username)
            .putString("display_name", normalized.displayName)
            .putString("avatar_url", normalized.avatarUrl)
            .putString("last_logged_in_user_id", normalized.userId)
            .putString("last_logged_in_email", normalized.email)
            .putString("last_logged_in_username", normalized.username)
            .putString("last_logged_in_display_name", normalized.displayName)
            .putString("last_logged_in_avatar_url", normalized.avatarUrl)
            .putBoolean("has_logged_in_before", true)
            .putBoolean("explicitly_signed_out", false)
            .commit()
        rememberAccountMetadataForEmail(normalized)
        _session.value = normalized
        _userProfile.value = loadUserProfile()
    }

    private fun rememberAccountMetadataForEmail(session: SupabaseSession, password: String? = null) {
        val emailKey = session.email.trim().lowercase()
        if (emailKey.isBlank()) return
        try {
            val raw = prefs.getString("registered_accounts_json", "{}") ?: "{}"
            val root = JSONObject(raw)
            val existing = root.optJSONObject(emailKey) ?: JSONObject()
            existing.put("user_id", session.userId)
            existing.put("email", session.email.trim())
            if (session.username.isNotBlank()) existing.put("username", session.username)
            if (session.displayName.isNotBlank()) existing.put("display_name", session.displayName)
            if (!session.avatarUrl.isNullOrBlank()) existing.put("avatar_url", session.avatarUrl)
            if (!password.isNullOrBlank()) existing.put("password", password)
            root.put(emailKey, existing)
            prefs.edit().putString("registered_accounts_json", root.toString()).apply()
        } catch (_: Throwable) {
        }
    }

    private fun findRememberedAccountForEmail(email: String, password: String? = null): SupabaseSession? {
        val emailKey = email.trim().lowercase()
        if (emailKey.isBlank()) return null
        return try {
            val raw = prefs.getString("registered_accounts_json", "{}") ?: "{}"
            val root = JSONObject(raw)
            val obj = root.optJSONObject(emailKey) ?: return null
            val storedPass = obj.optString("password", "")
            if (password != null && storedPass.isNotBlank() && storedPass != password) {
                return null
            }
            val uid = obj.optString("user_id").takeIf { it.isNotBlank() } ?: return null
            val handle = obj.optString("username").ifBlank { emailKey.substringBefore("@") }
            val display = obj.optString("display_name").ifBlank { handle }
            SupabaseSession(
                accessToken = "pulse_session_$uid",
                refreshToken = "",
                userId = uid,
                email = obj.optString("email").ifBlank { email.trim() },
                username = handle,
                displayName = display,
                avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() }
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadUserProfile(): UserProfileState {
        val activeSession = _session.value ?: return UserProfileState()
        val uid = activeSession.userId
        val email = activeSession.email
        val handle = activeSession.username
            .takeIf { it.isNotBlank() }
            ?: prefs.getString("username", "")
            ?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
        val name = activeSession.displayName
            .takeIf { it.isNotBlank() }
            ?: prefs.getString("display_name", "")
            ?.takeIf { it.isNotBlank() }
            ?: handle
        val rawAvatar = activeSession.avatarUrl ?: prefs.getString("avatar_url", null)
        val resolvedAvatar = SupabaseClient.resolveSupabaseMediaUrl(rawAvatar, defaultBucket = "avatars")

        return UserProfileState(
            id = uid,
            email = email,
            username = handle,
            displayName = name,
            roleTag = prefs.getString("role_tag", "Visual Creator") ?: "Visual Creator",
            bio = prefs.getString("bio", "") ?: "",
            website = prefs.getString("website", "") ?: "",
            location = prefs.getString("location", "") ?: "",
            avatarRes = R.drawable.img_post_portrait_1794021773202,
            avatarUrl = resolvedAvatar,
            followersCount = prefs.getInt("followers_count", 0),
            followingCount = prefs.getInt("following_count", 0)
        )
    }

    private fun loadMoodboards(): List<String> {
        val defaultBoards = "All Saved,Favorites,Visual Inspiration,Architecture,Monochrome"
        val raw = prefs.getString("moodboards_csv", defaultBoards) ?: defaultBoards
        val loaded = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return if ("Favorites" !in loaded) {
            listOf("All Saved", "Favorites") + loaded.filter { it != "All Saved" }
        } else {
            loaded
        }
    }

    fun addMoodboard(boardName: String) {
        val clean = boardName.trim()
        if (clean.isEmpty()) return
        val updated = (_moodboards.value + clean).distinct()
        prefs.edit().putString("moodboards_csv", updated.joinToString(",")).apply()
        _moodboards.value = updated
    }

    private fun loadSavedBookmarksMap(): Map<String, String> {
        val raw = prefs.getString("saved_bookmarks_entries", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(";;")
            .mapNotNull { entry ->
                val parts = entry.split("::", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    parts[0] to parts[1].ifBlank { "Favorites" }
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun saveBookmarkEntry(postId: String, isBookmarked: Boolean, collectionName: String) {
        if (postId.isBlank()) return
        val current = loadSavedBookmarksMap().toMutableMap()
        if (isBookmarked) {
            current[postId] = collectionName.ifBlank { "Favorites" }
        } else {
            current.remove(postId)
        }
        val serialized = current.entries.joinToString(";;") { "${it.key}::${it.value}" }
        prefs.edit().putString("saved_bookmarks_entries", serialized).apply()
    }

    private fun loadPinnedPostIds(): Set<String> {
        val raw = prefs.getString("pinned_profile_post_ids", "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3).toSet()
    }

    private fun savePinnedPostIds(ids: Set<String>) {
        prefs.edit().putString("pinned_profile_post_ids", ids.take(3).joinToString(",")).apply()
    }

    private fun loadSavedLikesMap(): Map<String, Pair<Boolean, Int>> {
        val raw = prefs.getString("saved_post_likes_entries", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(";;")
            .mapNotNull { entry ->
                val parts = entry.split("::", limit = 3)
                if (parts.size == 3 && parts[0].isNotBlank()) {
                    val isLiked = parts[1].toBooleanStrictOrNull() ?: false
                    val count = parts[2].toIntOrNull() ?: 0
                    parts[0] to (isLiked to count)
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun saveLikeEntry(postId: String, isLiked: Boolean, likeCount: Int) {
        if (postId.isBlank()) return
        val current = loadSavedLikesMap().toMutableMap()
        current[postId] = isLiked to likeCount.coerceAtLeast(0)
        val serialized = current.entries.joinToString(";;") { "${it.key}::${it.value.first}::${it.value.second}" }
        prefs.edit().putString("saved_post_likes_entries", serialized).apply()
    }

    suspend fun signIn(email: String, password: String): Result<SupabaseSession> {
        val cleanEmail = email.trim()
        val remembered = findRememberedAccountForEmail(cleanEmail, password = null)
        val result = SupabaseClient.signInWithEmail(cleanEmail, password)
        if (result.isSuccess) {
            val remoteSession = result.getOrThrow()
            val mergedSession = if (remembered != null) {
                remoteSession.copy(
                    username = remoteSession.username.ifBlank { remembered.username },
                    displayName = remoteSession.displayName.ifBlank { remembered.displayName },
                    avatarUrl = remoteSession.avatarUrl ?: remembered.avatarUrl
                )
            } else {
                remoteSession
            }
            rememberAccountMetadataForEmail(mergedSession, password)
            saveSession(mergedSession)
            return Result.success(mergedSession)
        }
        val localMatch = findRememberedAccountForEmail(cleanEmail, password)
        if (localMatch != null) {
            saveSession(localMatch)
            return Result.success(localMatch)
        }
        val error = result.exceptionOrNull()
        if (isNetworkOrHostError(error)) {
            val deterministicId = UUID.nameUUIDFromBytes(cleanEmail.lowercase().toByteArray()).toString()
            val handle = cleanEmail.substringBefore("@").lowercase().ifBlank { "creator" }
            val offlineSession = SupabaseSession(
                accessToken = "pulse_offline_$deterministicId",
                refreshToken = "",
                userId = deterministicId,
                email = cleanEmail,
                username = handle,
                displayName = handle.replaceFirstChar { it.uppercase() }
            )
            rememberAccountMetadataForEmail(offlineSession, password)
            saveSession(offlineSession)
            return Result.success(offlineSession)
        }
        return result
    }

    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<SupabaseSession> {
        val cleanEmail = email.trim()
        val result = SupabaseClient.signUpWithEmail(cleanEmail, password, username, displayName)
        if (result.isSuccess) {
            val session = result.getOrThrow()
            rememberAccountMetadataForEmail(session, password)
            saveSession(session)
            return Result.success(session)
        }
        val error = result.exceptionOrNull()
        if (isNetworkOrHostError(error)) {
            val cleanHandle = username.trim().lowercase().replace(" ", "_")
                .ifBlank { cleanEmail.substringBefore("@").lowercase() }
                .ifBlank { "creator" }
            val cleanName = displayName.trim().ifBlank { cleanHandle }
            val deterministicId = UUID.nameUUIDFromBytes(cleanEmail.lowercase().toByteArray()).toString()
            val offlineSession = SupabaseSession(
                accessToken = "pulse_offline_$deterministicId",
                refreshToken = "",
                userId = deterministicId,
                email = cleanEmail,
                username = cleanHandle,
                displayName = cleanName
            )
            rememberAccountMetadataForEmail(offlineSession, password)
            saveSession(offlineSession)
            return Result.success(offlineSession)
        }
        return result
    }

    private fun isNetworkOrHostError(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        val msg = throwable.localizedMessage.orEmpty().lowercase()
        return throwable is java.io.IOException ||
            throwable is java.net.UnknownHostException ||
            throwable is java.net.ConnectException ||
            throwable is java.net.SocketTimeoutException ||
            msg.contains("unable to resolve host") ||
            msg.contains("no address associated") ||
            msg.contains("failed to connect") ||
            msg.contains("timeout") ||
            msg.contains("network")
    }

    suspend fun signOut() {
        SupabaseClient.signOut()
        SupabaseClient.setAccessToken(null)
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("user_id")
            .remove("email")
            .remove("username")
            .remove("display_name")
            .remove("avatar_url")
            .remove("role_tag")
            .remove("bio")
            .remove("website")
            .remove("location")
            .remove("followers_count")
            .remove("following_count")
            .putBoolean("explicitly_signed_out", true)
            .commit()
        _session.value = null
        _userProfile.value = UserProfileState()
        currentPostsOffset = 0
        _hasMorePosts.value = true
        dao.clearAllPosts()
        dao.clearAllStories()
        dao.clearAllComments()
        dao.clearAllCreators()
        dao.clearAllUsers()
        dao.clearAllDirectMessages()
        dao.clearAllActivities()
        feedDao.clearAllPosts()
        feedDao.clearAllUsers()
        feedDao.clearAllStories()
    }

    private fun enrichPostsWithProfiles(
        rawPosts: List<PostEntity>,
        profiles: List<CreatorProfileEntity>
    ): List<PostEntity> {
        val savedBookmarks = loadSavedBookmarksMap()
        val savedLikes = loadSavedLikesMap()
        val pinnedIds = loadPinnedPostIds()
        val byId = profiles.associateBy { it.id.lowercase() }
        val byUsername = profiles.associateBy { it.username.lowercase() }

        return rawPosts.map { post ->
            val matched = byId[post.authorId.lowercase()]
                ?: byUsername[post.authorUsername.lowercase()]
            val savedCollection = savedBookmarks[post.id]
            val savedLikeState = savedLikes[post.id]
            val resolvedIsLiked = savedLikeState?.first ?: post.isLiked
            val resolvedLikeCount = when {
                savedLikeState != null && savedLikeState.first -> maxOf(post.likeCount, savedLikeState.second, 1)
                savedLikeState != null && !savedLikeState.first -> savedLikeState.second
                else -> post.likeCount
            }
            val isSaved = post.isBookmarked || savedCollection != null
            val isPinned = post.isPinnedOnProfile || post.id in pinnedIds
            val resolvedMoodboard = savedCollection
                ?: post.moodboardName.takeIf { it.isNotBlank() }
                ?: "Favorites"

            val profileEnriched = if (matched != null) {
                post.copy(
                    authorUsername = if (post.authorUsername.startsWith("creator_") || post.authorUsername.isBlank()) {
                        matched.username
                    } else {
                        post.authorUsername
                    },
                    authorDisplayName = if (post.authorDisplayName.startsWith("creator_") || post.authorDisplayName.isBlank()) {
                        matched.displayName
                    } else {
                        post.authorDisplayName
                    },
                    authorAvatarUrl = post.authorAvatarUrl ?: matched.avatarUrl,
                    authorAvatarRes = matched.avatarRes,
                    authorVerified = post.authorVerified || matched.isVerified
                )
            } else {
                post
            }
            profileEnriched.copy(
                isLiked = resolvedIsLiked,
                likeCount = resolvedLikeCount,
                isBookmarked = isSaved,
                moodboardName = if (isSaved) resolvedMoodboard else "All Saved",
                isPinnedOnProfile = isPinned
            )
        }
    }

    private fun enrichStoriesWithProfiles(
        rawStories: List<StoryEntity>,
        profiles: List<CreatorProfileEntity>
    ): List<StoryEntity> {
        if (profiles.isEmpty()) return rawStories
        val byId = profiles.associateBy { it.id.lowercase() }
        val byUsername = profiles.associateBy { it.username.lowercase() }

        return rawStories.map { story ->
            val matched = byId[story.authorId.lowercase()]
                ?: byUsername[story.authorUsername.lowercase()]
            if (matched != null) {
                story.copy(
                    authorUsername = if (story.authorUsername.startsWith("creator_") || story.authorUsername.isBlank()) {
                        matched.username
                    } else {
                        story.authorUsername
                    },
                    authorDisplayName = if (story.authorDisplayName.startsWith("creator_") || story.authorDisplayName.isBlank()) {
                        matched.displayName
                    } else {
                        story.authorDisplayName
                    },
                    authorAvatarUrl = story.authorAvatarUrl ?: matched.avatarUrl,
                    authorAvatarRes = matched.avatarRes
                )
            } else {
                story
            }
        }
    }

    private fun enrichCommentsWithProfiles(
        rawComments: List<CommentEntity>,
        profiles: List<CreatorProfileEntity>
    ): List<CommentEntity> {
        if (profiles.isEmpty()) return rawComments
        val byUsername = profiles.associateBy { it.username.lowercase() }
        val byId = profiles.associateBy { it.id.take(6).lowercase() }

        return rawComments.map { comment ->
            val matched = byUsername[comment.authorUsername.lowercase()]
                ?: byId[comment.authorUsername.lowercase()]
            if (matched != null) {
                comment.copy(
                    authorUsername = matched.username,
                    authorDisplayName = matched.displayName,
                    authorAvatarUrl = comment.authorAvatarUrl ?: matched.avatarUrl,
                    authorAvatarRes = matched.avatarRes
                )
            } else {
                comment
            }
        }
    }

    suspend fun refreshFromSupabase(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        forceRefresh: Boolean = true
    ): Result<Int> {
        if (!forceRefresh && isFeedCacheFresh()) {
            return Result.success(dao.getCachedPostCount())
        }
        if (!SupabaseClient.isConfigured) {
            return Result.failure(
                IllegalStateException("Supabase URL / Key not configured in BuildConfig.")
            )
        }

        // Sync authenticated user metadata if an access token is active
        val authUser = SupabaseClient.fetchAuthenticatedUserSession()
        val currentSession = _session.value
        if (authUser != null && currentSession != null) {
            val updated = currentSession.copy(
                userId = authUser.userId.ifBlank { currentSession.userId },
                email = authUser.email.ifBlank { currentSession.email },
                username = authUser.username.ifBlank { currentSession.username },
                displayName = authUser.displayName.ifBlank { currentSession.displayName },
                avatarUrl = authUser.avatarUrl ?: currentSession.avatarUrl
            )
            if (updated != currentSession) {
                saveSession(updated)
            }
        }

        val uid = _session.value?.userId?.takeIf { it.isNotBlank() }
        val activeHandle = _session.value?.username.orEmpty()

        val profilesResult = SupabaseClient.fetchProfiles(uid)
        profilesResult.onSuccess { fetchedProfiles ->
            cachedProfiles = fetchedProfiles
            if (fetchedProfiles.isNotEmpty()) {
                val mappedUsers = fetchedProfiles.map { it.toDatabaseUser() }
                dao.clearAllCreators()
                dao.insertCreators(fetchedProfiles)
                dao.clearAllUsers()
                dao.insertUsers(mappedUsers)
                feedDao.clearAllUsers()
                feedDao.insertUsers(mappedUsers)

                if (!uid.isNullOrBlank()) {
                    val myRemoteProfile = fetchedProfiles.find {
                        it.id.equals(uid, ignoreCase = true) ||
                            (activeHandle.isNotBlank() && it.username.equals(activeHandle, ignoreCase = true))
                    }

                    if (myRemoteProfile != null) {
                        val resolvedAvatar = SupabaseClient.resolveSupabaseMediaUrl(
                            myRemoteProfile.avatarUrl,
                            defaultBucket = "avatars"
                        )
                        prefs.edit()
                            .putString("user_id", uid)
                            .putString("username", myRemoteProfile.username)
                            .putString("display_name", myRemoteProfile.displayName)
                            .putString("role_tag", myRemoteProfile.roleTag)
                            .putString("bio", myRemoteProfile.bio)
                            .putString("website", myRemoteProfile.website)
                            .putString("location", myRemoteProfile.location)
                            .putString("avatar_url", resolvedAvatar ?: _userProfile.value.avatarUrl)
                            .putInt("followers_count", myRemoteProfile.followersCount)
                            .putInt("following_count", myRemoteProfile.followingCount)
                            .apply()
                        _userProfile.value = loadUserProfile()
                    }
                }
            }
        }

        val storiesResult = SupabaseClient.fetchStories()
        val authoredStories = loadAuthoredStoriesFromJournal()
        if (storiesResult.isSuccess) {
            val remoteStories = storiesResult.getOrDefault(emptyList())
            val mergedRawStories = (authoredStories + remoteStories).distinctBy { it.id }
            val enrichedStories = enrichStoriesWithProfiles(mergedRawStories, cachedProfiles)
            dao.clearAllStories()
            feedDao.clearAllStories()
            if (enrichedStories.isNotEmpty()) {
                dao.insertStories(enrichedStories)
                feedDao.insertStories(enrichedStories.map { it.toDatabaseStory() })
            }
        } else if (authoredStories.isNotEmpty()) {
            val enrichedStories = enrichStoriesWithProfiles(authoredStories, cachedProfiles)
            dao.insertStories(enrichedStories)
            feedDao.insertStories(enrichedStories.map { it.toDatabaseStory() })
        }

        val commentsResult = SupabaseClient.fetchComments()
        val authoredComments = loadAuthoredCommentsFromJournal()
        if (commentsResult.isSuccess) {
            val remoteComments = commentsResult.getOrDefault(emptyList())
            val mergedRawComments = (authoredComments + remoteComments).distinctBy { it.id }
            val enrichedComments = enrichCommentsWithProfiles(mergedRawComments, cachedProfiles)
            dao.clearAllComments()
            feedDao.clearAllComments()
            if (enrichedComments.isNotEmpty()) {
                dao.insertComments(enrichedComments)
                feedDao.insertComments(enrichedComments.map { it.toDatabaseComment() })
            }
        } else if (authoredComments.isNotEmpty()) {
            val enrichedComments = enrichCommentsWithProfiles(authoredComments, cachedProfiles)
            dao.insertComments(enrichedComments)
            feedDao.insertComments(enrichedComments.map { it.toDatabaseComment() })
        }

        val messagesResult = SupabaseClient.fetchMessages(uid)
        messagesResult.onSuccess { fetchedMessages ->
            dao.clearAllDirectMessages()
            if (fetchedMessages.isNotEmpty()) {
                dao.insertDirectMessages(fetchedMessages)
                fetchedMessages.filter { !it.isFromMe && it.isUnread }.forEach { unreadMsg ->
                    notificationManager.notifyIncomingMessage(
                        senderUsername = unreadMsg.participantUsername,
                        senderDisplayName = unreadMsg.participantDisplayName,
                        messageText = unreadMsg.text,
                        threadId = unreadMsg.threadId,
                        dedupeKey = "dm_${unreadMsg.id}"
                    )
                }
            }
        }

        val activitiesResult = SupabaseClient.fetchNotifications(uid)
        activitiesResult.onSuccess { fetchedActivities ->
            dao.clearAllActivities()
            if (fetchedActivities.isNotEmpty()) {
                dao.insertActivities(fetchedActivities)
                fetchedActivities.filter { !it.isRead }.forEach { unreadActivity ->
                    notificationManager.notifyActivityItem(unreadActivity)
                }
            }
        }

        // Also check dedicated video/reel/loop tables in Supabase
        val videoReels = SupabaseClient.fetchVideoReels(uid).getOrDefault(emptyList())
        val authoredLocalPosts = loadAuthoredPostsFromJournal()
        val existingCachedById = dao.getCachedPostsSnapshot().associateBy { it.id }

        // Fetch first page of posts (offset = 0)
        currentPostsOffset = 0
        val postsResult = SupabaseClient.fetchPosts(
            currentUserId = uid,
            limit = pageSize,
            offset = 0
        )
        return postsResult.fold(
            onSuccess = { fetchedPosts ->
                val nowCacheMs = System.currentTimeMillis()
                val remoteCombined = (fetchedPosts + videoReels).distinctBy { it.id }
                // Merge locally authored posts with remote posts (deduplicating by id or matching author + caption + media)
                val mergedPosts = mergeAuthoredAndRemotePosts(authoredLocalPosts, remoteCombined)
                val enrichedPosts = enrichPostsWithProfiles(mergedPosts, cachedProfiles).map { post ->
                    val existing = existingCachedById[post.id]
                    if (existing != null) {
                        post.copy(
                            carouselImageUrisCsv = post.carouselImageUrisCsv.ifBlank { existing.carouselImageUrisCsv },
                            carouselImageResCsv = post.carouselImageResCsv.ifBlank { existing.carouselImageResCsv },
                            isBookmarked = post.isBookmarked || existing.isBookmarked,
                            moodboardName = if (existing.isBookmarked) existing.moodboardName else post.moodboardName,
                            isLiked = post.isLiked || existing.isLiked,
                            likedByUserIdsCsv = post.likedByUserIdsCsv.ifBlank { existing.likedByUserIdsCsv },
                            lastLikedAtMs = maxOf(post.lastLikedAtMs, existing.lastLikedAtMs),
                            cachedAtMs = nowCacheMs
                        )
                    } else {
                        post.copy(cachedAtMs = nowCacheMs)
                    }
                }

                if (enrichedPosts.isNotEmpty()) {
                    dao.clearAllPosts()
                    feedDao.clearAllPosts()
                    dao.insertPosts(enrichedPosts)
                    feedDao.insertPosts(enrichedPosts.map { it.toDatabasePost() })
                }
                currentPostsOffset = fetchedPosts.size
                _hasMorePosts.value = fetchedPosts.size >= pageSize
                Result.success(enrichedPosts.size)
            },
            onFailure = { err ->
                if (authoredLocalPosts.isNotEmpty()) {
                    val enrichedLocal = enrichPostsWithProfiles(authoredLocalPosts, cachedProfiles)
                    dao.insertPosts(enrichedLocal)
                    feedDao.insertPosts(enrichedLocal.map { it.toDatabasePost() })
                }
                Result.failure(err)
            }
        )
    }

    private fun mergeAuthoredAndRemotePosts(
        authored: List<PostEntity>,
        remote: List<PostEntity>
    ): List<PostEntity> {
        if (authored.isEmpty()) return remote
        val remoteById = remote.associateBy { it.id }
        val consumedRemoteIds = mutableSetOf<String>()

        val resolvedAuthored = authored.map { localPost ->
            val matchedRemote = remoteById[localPost.id] ?: remote.find { r ->
                r.caption == localPost.caption &&
                    (r.authorId.equals(localPost.authorId, ignoreCase = true) ||
                        r.authorUsername.equals(localPost.authorUsername, ignoreCase = true) ||
                        (localPost.caption.isNotBlank() &&
                            kotlin.math.abs(r.timestampMs - localPost.timestampMs) < 15 * 60 * 1000L))
            }
            if (matchedRemote != null) {
                consumedRemoteIds.add(matchedRemote.id)
                localPost.copy(
                    imageUri = localPost.imageUri?.takeIf { it.isNotBlank() } ?: matchedRemote.imageUri,
                    videoUri = localPost.videoUri?.takeIf { it.isNotBlank() } ?: matchedRemote.videoUri,
                    carouselImageUrisCsv = localPost.carouselImageUrisCsv.ifBlank { matchedRemote.carouselImageUrisCsv },
                    carouselImageResCsv = localPost.carouselImageResCsv.ifBlank { matchedRemote.carouselImageResCsv },
                    likeCount = maxOf(localPost.likeCount, matchedRemote.likeCount),
                    commentCount = maxOf(localPost.commentCount, matchedRemote.commentCount)
                )
            } else {
                localPost
            }
        }

        val remainingRemote = remote.filter { it.id !in consumedRemoteIds }
        return (resolvedAuthored + remainingRemote)
            .distinctBy { it.id }
            .sortedByDescending { it.timestampMs }
    }

    private fun loadAuthoredPostsFromJournal(): List<PostEntity> {
        val raw = prefs.getString("authored_posts_journal_json", null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val list = mutableListOf<PostEntity>()
        val activeProfile = _userProfile.value
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val savedAuthorId = obj.optString("authorId")
                val savedUsername = obj.optString("authorUsername")
                val loadedImageUri = obj.optString("imageUri").takeIf { it.isNotBlank() }
                val loadedVideoUri = obj.optString("videoUri").takeIf { it.isNotBlank() }
                val defaultImageRes = if (loadedImageUri != null || loadedVideoUri != null) {
                    0
                } else {
                    R.drawable.img_post_kyoto_1794021771843
                }
                list.add(
                    PostEntity(
                        id = obj.optString("id"),
                        authorId = if (savedAuthorId.isBlank() || savedAuthorId == "user_session") {
                            activeProfile.id.ifBlank { "user_session" }
                        } else {
                            savedAuthorId
                        },
                        authorUsername = if (savedUsername.isBlank() || savedUsername == "creator") {
                            activeProfile.username.ifBlank { "creator" }
                        } else {
                            savedUsername
                        },
                        authorDisplayName = obj.optString("authorDisplayName").let {
                            if (it.isBlank() || it == "Creator") activeProfile.displayName.ifBlank { "Creator" } else it
                        },
                        authorAvatarRes = obj.optInt("authorAvatarRes", R.drawable.img_post_portrait_1794021773202),
                        authorAvatarUrl = obj.optString("authorAvatarUrl").takeIf { it.isNotBlank() } ?: activeProfile.avatarUrl,
                        authorVerified = obj.optBoolean("authorVerified", true),
                        imageRes = obj.optInt("imageRes", defaultImageRes),
                        imageUri = loadedImageUri,
                        videoUri = loadedVideoUri,
                        carouselImageUrisCsv = obj.optString("carouselImageUrisCsv", ""),
                        carouselImageResCsv = obj.optString("carouselImageResCsv", ""),
                        aspectRatioName = obj.optString("aspectRatioName", PhotoAspectRatio.PORTRAIT_4_5.name),
                        filterPresetName = obj.optString("filterPresetName", FilmPreset.NATURAL.name),
                        brightness = obj.optDouble("brightness", 0.0).toFloat(),
                        exposure = obj.optDouble("exposure", 0.0).toFloat(),
                        contrast = obj.optDouble("contrast", 1.0).toFloat(),
                        saturation = obj.optDouble("saturation", 1.0).toFloat(),
                        warmth = obj.optDouble("warmth", 0.0).toFloat(),
                        filmGrain = obj.optDouble("filmGrain", 0.0).toFloat(),
                        vignette = obj.optDouble("vignette", 0.0).toFloat(),
                        hasExifFrameBorder = obj.optBoolean("hasExifFrameBorder", false),
                        exifFrameStyle = obj.optString("exifFrameStyle", "GALLERY_WHITE"),
                        caption = obj.optString("caption", ""),
                        location = obj.optString("location", ""),
                        category = obj.optString("category", "Photography"),
                        tagsCsv = obj.optString("tagsCsv", ""),
                        likeCount = obj.optInt("likeCount", 0),
                        isLiked = obj.optBoolean("isLiked", false),
                        isBookmarked = obj.optBoolean("isBookmarked", false),
                        moodboardName = obj.optString("moodboardName", "All Saved"),
                        isPinnedOnProfile = obj.optBoolean("isPinnedOnProfile", false),
                        commentCount = obj.optInt("commentCount", 0),
                        isLoop = obj.optBoolean("isLoop", false),
                        audioTrack = obj.optString("audioTrack", "Original Audio"),
                        cameraSpec = obj.optString("cameraSpec", "35mm · f/1.8 · ISO 400"),
                        createdAtLabel = obj.optString("createdAtLabel", "Just now"),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun saveAuthoredPostsToJournal(posts: List<PostEntity>) {
        val arr = JSONArray()
        posts.distinctBy { it.id }.forEach { post ->
            arr.put(
                JSONObject().apply {
                    put("id", post.id)
                    put("authorId", post.authorId)
                    put("authorUsername", post.authorUsername)
                    put("authorDisplayName", post.authorDisplayName)
                    put("authorAvatarRes", post.authorAvatarRes)
                    put("authorAvatarUrl", post.authorAvatarUrl.orEmpty())
                    put("authorVerified", post.authorVerified)
                    put("imageRes", post.imageRes)
                    put("imageUri", post.imageUri.orEmpty())
                    put("videoUri", post.videoUri.orEmpty())
                    put("carouselImageUrisCsv", post.carouselImageUrisCsv)
                    put("carouselImageResCsv", post.carouselImageResCsv)
                    put("aspectRatioName", post.aspectRatioName)
                    put("filterPresetName", post.filterPresetName)
                    put("brightness", post.brightness.toDouble())
                    put("exposure", post.exposure.toDouble())
                    put("contrast", post.contrast.toDouble())
                    put("saturation", post.saturation.toDouble())
                    put("warmth", post.warmth.toDouble())
                    put("filmGrain", post.filmGrain.toDouble())
                    put("vignette", post.vignette.toDouble())
                    put("hasExifFrameBorder", post.hasExifFrameBorder)
                    put("exifFrameStyle", post.exifFrameStyle)
                    put("caption", post.caption)
                    put("location", post.location)
                    put("category", post.category)
                    put("tagsCsv", post.tagsCsv)
                    put("likeCount", post.likeCount)
                    put("isLiked", post.isLiked)
                    put("isBookmarked", post.isBookmarked)
                    put("moodboardName", post.moodboardName)
                    put("isPinnedOnProfile", post.isPinnedOnProfile)
                    put("commentCount", post.commentCount)
                    put("isLoop", post.isLoop)
                    put("audioTrack", post.audioTrack)
                    put("cameraSpec", post.cameraSpec)
                    put("createdAtLabel", post.createdAtLabel)
                    put("timestampMs", post.timestampMs)
                }
            )
        }
        prefs.edit().putString("authored_posts_journal_json", arr.toString()).commit()
    }

    private fun upsertAuthoredPostInJournal(post: PostEntity) {
        val current = loadAuthoredPostsFromJournal().filterNot { it.id == post.id }
        saveAuthoredPostsToJournal(listOf(post) + current)
    }

    private fun removeAuthoredPostFromJournal(postId: String) {
        val current = loadAuthoredPostsFromJournal().filterNot { it.id == postId }
        saveAuthoredPostsToJournal(current)
    }

    private fun loadAuthoredCommentsFromJournal(): List<CommentEntity> {
        val raw = prefs.getString("authored_comments_journal_json", null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val list = mutableListOf<CommentEntity>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    CommentEntity(
                        id = obj.optString("id"),
                        postId = obj.optString("postId"),
                        authorId = obj.optString("authorId", ""),
                        authorUsername = obj.optString("authorUsername", "user"),
                        authorDisplayName = obj.optString("authorDisplayName", "User"),
                        authorAvatarRes = obj.optInt("authorAvatarRes", R.drawable.img_post_portrait_1794021773202),
                        authorAvatarUrl = obj.optString("authorAvatarUrl").takeIf { it.isNotBlank() },
                        text = obj.optString("text", ""),
                        parentCommentId = obj.optString("parentCommentId").takeIf { it.isNotBlank() },
                        replyToUsername = obj.optString("replyToUsername").takeIf { it.isNotBlank() },
                        likeCount = obj.optInt("likeCount", 0),
                        isLiked = obj.optBoolean("isLiked", false),
                        timeAgo = obj.optString("timeAgo", "Just now"),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun saveAuthoredCommentsToJournal(commentsList: List<CommentEntity>) {
        val arr = JSONArray()
        commentsList.distinctBy { it.id }.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("postId", c.postId)
                    put("authorId", c.authorId)
                    put("authorUsername", c.authorUsername)
                    put("authorDisplayName", c.authorDisplayName)
                    put("authorAvatarRes", c.authorAvatarRes)
                    put("authorAvatarUrl", c.authorAvatarUrl.orEmpty())
                    put("text", c.text)
                    put("parentCommentId", c.parentCommentId.orEmpty())
                    put("replyToUsername", c.replyToUsername.orEmpty())
                    put("likeCount", c.likeCount)
                    put("isLiked", c.isLiked)
                    put("timeAgo", c.timeAgo)
                    put("timestampMs", c.timestampMs)
                }
            )
        }
        prefs.edit().putString("authored_comments_journal_json", arr.toString()).commit()
    }

    private fun upsertAuthoredCommentInJournal(comment: CommentEntity) {
        val current = loadAuthoredCommentsFromJournal().filterNot { it.id == comment.id }
        saveAuthoredCommentsToJournal(current + comment)
    }

    private fun loadAuthoredStoriesFromJournal(): List<StoryEntity> {
        val raw = prefs.getString("authored_stories_journal_json", null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val list = mutableListOf<StoryEntity>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val loadedImageUri = obj.optString("imageUri").takeIf { it.isNotBlank() }
                val loadedVideoUri = obj.optString("videoUri").takeIf { it.isNotBlank() }
                val defaultImageRes = if (loadedImageUri != null || loadedVideoUri != null) {
                    0
                } else {
                    R.drawable.img_post_kyoto_1794021771843
                }
                list.add(
                    StoryEntity(
                        id = obj.optString("id"),
                        authorId = obj.optString("authorId"),
                        authorUsername = obj.optString("authorUsername"),
                        authorDisplayName = obj.optString("authorDisplayName"),
                        authorAvatarRes = obj.optInt("authorAvatarRes", R.drawable.img_post_portrait_1794021773202),
                        authorAvatarUrl = obj.optString("authorAvatarUrl").takeIf { it.isNotBlank() },
                        imageRes = obj.optInt("imageRes", defaultImageRes),
                        imageUri = loadedImageUri,
                        videoUri = loadedVideoUri,
                        filterPresetName = obj.optString("filterPresetName", FilmPreset.NATURAL.name),
                        captionOverlay = obj.optString("captionOverlay", ""),
                        locationTag = obj.optString("locationTag", ""),
                        isViewed = obj.optBoolean("isViewed", false),
                        isHighlight = obj.optBoolean("isHighlight", false),
                        highlightTitle = obj.optString("highlightTitle", ""),
                        createdAtLabel = obj.optString("createdAtLabel", "Just now"),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun upsertAuthoredStoryInJournal(story: StoryEntity) {
        val current = loadAuthoredStoriesFromJournal().filterNot { it.id == story.id }
        val updated = listOf(story) + current
        val arr = JSONArray()
        updated.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("authorId", s.authorId)
                    put("authorUsername", s.authorUsername)
                    put("authorDisplayName", s.authorDisplayName)
                    put("authorAvatarRes", s.authorAvatarRes)
                    put("authorAvatarUrl", s.authorAvatarUrl.orEmpty())
                    put("imageRes", s.imageRes)
                    put("imageUri", s.imageUri.orEmpty())
                    put("videoUri", s.videoUri.orEmpty())
                    put("filterPresetName", s.filterPresetName)
                    put("captionOverlay", s.captionOverlay)
                    put("locationTag", s.locationTag)
                    put("isViewed", s.isViewed)
                    put("isHighlight", s.isHighlight)
                    put("highlightTitle", s.highlightTitle)
                    put("createdAtLabel", s.createdAtLabel)
                    put("timestampMs", s.timestampMs)
                }
            )
        }
        prefs.edit().putString("authored_stories_journal_json", arr.toString()).apply()
    }

    /**
     * Fetches the next page of posts from Supabase for infinite scrolling pagination.
     */
    suspend fun loadNextPostsPage(pageSize: Int = DEFAULT_PAGE_SIZE): Result<Int> {
        if (!SupabaseClient.isConfigured) {
            _hasMorePosts.value = false
            return Result.success(0)
        }
        val uid = _session.value?.userId
        val result = SupabaseClient.fetchPosts(
            currentUserId = uid,
            limit = pageSize,
            offset = currentPostsOffset
        )
        return result.map { nextPosts ->
            if (nextPosts.isEmpty()) {
                _hasMorePosts.value = false
                0
            } else {
                val enriched = enrichPostsWithProfiles(nextPosts, cachedProfiles)
                dao.insertPosts(enriched)
                feedDao.insertPosts(enriched.map { it.toDatabasePost() })
                currentPostsOffset += nextPosts.size
                _hasMorePosts.value = nextPosts.size >= pageSize
                enriched.size
            }
        }
    }

    fun updateUserProfile(
        displayName: String,
        username: String,
        roleTag: String,
        bio: String,
        website: String,
        location: String,
        avatarUrl: String? = null
    ) {
        val cleanUsername = username.trim().lowercase().replace(" ", "_")
        val editor = prefs.edit()
            .putString("display_name", displayName.trim())
            .putString("username", cleanUsername)
            .putString("role_tag", roleTag.trim())
            .putString("bio", bio.trim())
            .putString("website", website.trim())
            .putString("location", location.trim())
        if (avatarUrl != null) {
            val resolved = SupabaseClient.resolveSupabaseMediaUrl(avatarUrl, defaultBucket = "avatars")
            editor.putString("avatar_url", resolved)
        }
        editor.apply()
        val updated = loadUserProfile()
        _userProfile.value = updated
    }

    suspend fun syncUserProfileToSupabase() {
        val currentProfile = _userProfile.value
        val currentAvatar = currentProfile.avatarUrl
        if (!currentAvatar.isNullOrBlank() &&
            (currentAvatar.startsWith("file://") || currentAvatar.startsWith("content://") || currentAvatar.startsWith("/"))
        ) {
            val uploadedAvatar = SupabaseClient.uploadMediaToSupabaseStorage(
                context = appContext,
                localUriOrUrl = currentAvatar,
                fallbackResId = 0,
                isVideo = false,
                bucketName = "avatars"
            ) ?: SupabaseClient.buildCompactDataUriFromLocalFile(
                context = appContext,
                localUriOrUrl = currentAvatar,
                fallbackResId = 0
            )
            if (!uploadedAvatar.isNullOrBlank()) {
                prefs.edit().putString("avatar_url", uploadedAvatar).apply()
                _userProfile.value = loadUserProfile()
            }
        }
        SupabaseClient.updateProfileInSupabase(_userProfile.value)
    }

    fun toggleTheme() {
        val next = !_isDarkTheme.value
        prefs.edit().putBoolean("is_dark_theme", next).apply()
        _isDarkTheme.value = next
    }

    suspend fun toggleLikePost(post: PostEntity, forceLike: Boolean = false) {
        val targetLiked = if (forceLike) true else !post.isLiked
        if (targetLiked == post.isLiked) return
        val delta = if (targetLiked) 1 else -1
        val newLikeCount = (post.likeCount + delta).coerceAtLeast(0)
        val nowMs = System.currentTimeMillis()
        val activeUserId = _userProfile.value.id
            .ifBlank { _session.value?.userId.orEmpty() }
            .ifBlank { "local_user" }
        val activeUsername = _userProfile.value.username
            .ifBlank { _session.value?.username.orEmpty() }
            .ifBlank { "creator" }

        val existingLikedUsers = post.likedByUserIdsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        if (targetLiked) {
            existingLikedUsers.add(activeUserId)
        } else {
            existingLikedUsers.remove(activeUserId)
        }
        val updatedLikedUsersCsv = existingLikedUsers.joinToString(",")

        val updated = post.copy(
            isLiked = targetLiked,
            likeCount = newLikeCount,
            likedByUserIdsCsv = updatedLikedUsersCsv,
            lastLikedAtMs = if (targetLiked) nowMs else post.lastLikedAtMs
        )
        saveLikeEntry(post.id, targetLiked, newLikeCount)
        dao.updatePost(updated)
        feedDao.updatePost(updated.toDatabasePost())

        if (targetLiked) {
            val postLike = PostLike(
                postId = post.id,
                userId = activeUserId,
                username = activeUsername,
                isLiked = true,
                timestampMs = nowMs
            )
            val interaction = UserInteraction(
                id = "like_${post.id}_$activeUserId",
                postId = post.id,
                userId = activeUserId,
                interactionType = "LIKE",
                isActive = true,
                timestampMs = nowMs
            )
            dao.upsertPostLike(postLike)
            feedDao.upsertPostLike(postLike)
            dao.insertUserInteraction(interaction)
            feedDao.insertUserInteraction(interaction)
            val likeActivity = ActivityItemEntity(
                id = "activity_like_${post.id}_$nowMs",
                type = "LIKE",
                actorId = activeUserId,
                actorUsername = activeUsername,
                actorDisplayName = _userProfile.value.displayName.ifBlank { activeUsername },
                actorAvatarRes = _userProfile.value.avatarRes,
                actorAvatarUrl = _userProfile.value.avatarUrl,
                targetPostId = post.id,
                targetPostImageRes = post.imageRes,
                targetPostImageUri = post.imageUri,
                message = "liked \"${post.caption.take(40)}\"",
                timeLabel = "Just now",
                isRead = false,
                timestampMs = nowMs
            )
            dao.insertActivities(listOf(likeActivity))
            notificationManager.notifyNewLike(
                actorUsername = activeUsername,
                postCaption = post.caption,
                postId = post.id,
                dedupeKey = likeActivity.id
            )
        } else {
            dao.deletePostLike(post.id, activeUserId)
            feedDao.deletePostLike(post.id, activeUserId)
            dao.deleteUserInteraction(post.id, activeUserId, "LIKE")
            feedDao.deleteUserInteraction(post.id, activeUserId, "LIKE")
        }

        if (loadAuthoredPostsFromJournal().any { it.id == updated.id }) {
            upsertAuthoredPostInJournal(updated)
        }
        val uid = _userProfile.value.id
        if (uid.isNotBlank()) {
            SupabaseClient.syncLikeInSupabase(updated, uid, targetLiked)
        }
    }

    suspend fun toggleBookmarkPost(post: PostEntity, moodboardName: String = "Favorites") {
        val nextBookmarked = !post.isBookmarked
        val targetCollection = if (moodboardName.isBlank() || moodboardName == "All Saved") {
            "Favorites"
        } else {
            moodboardName
        }
        saveBookmarkEntry(post.id, nextBookmarked, targetCollection)
        val updated = post.copy(
            isBookmarked = nextBookmarked,
            moodboardName = if (nextBookmarked) targetCollection else "All Saved"
        )
        dao.updatePost(updated)
        feedDao.updatePost(updated.toDatabasePost())
        if (loadAuthoredPostsFromJournal().any { it.id == updated.id }) {
            upsertAuthoredPostInJournal(updated)
        }
    }

    suspend fun assignPostToMoodboard(post: PostEntity, moodboardName: String) {
        val cleanCollection = moodboardName.trim().ifBlank { "Favorites" }
        addMoodboard(cleanCollection)
        saveBookmarkEntry(post.id, true, cleanCollection)
        val updated = post.copy(
            isBookmarked = true,
            moodboardName = cleanCollection
        )
        dao.updatePost(updated)
        feedDao.updatePost(updated.toDatabasePost())
        if (loadAuthoredPostsFromJournal().any { it.id == updated.id }) {
            upsertAuthoredPostInJournal(updated)
        }
    }

    suspend fun togglePinPostOnProfile(post: PostEntity): Result<Boolean> {
        val currentPinned = loadPinnedPostIds().toMutableSet()
        val willBePinned = !post.isPinnedOnProfile && post.id !in currentPinned
        if (willBePinned && currentPinned.size >= 3) {
            return Result.failure(IllegalStateException("You can pin up to 3 posts on your profile"))
        }
        if (willBePinned) {
            currentPinned.add(post.id)
        } else {
            currentPinned.remove(post.id)
        }
        savePinnedPostIds(currentPinned)
        val updated = post.copy(isPinnedOnProfile = willBePinned)
        dao.updatePost(updated)
        feedDao.updatePost(updated.toDatabasePost())
        if (loadAuthoredPostsFromJournal().any { it.id == updated.id }) {
            upsertAuthoredPostInJournal(updated)
        }
        return Result.success(willBePinned)
    }

    suspend fun deletePost(postId: String) {
        val currentPinned = loadPinnedPostIds().toMutableSet()
        if (currentPinned.remove(postId)) {
            savePinnedPostIds(currentPinned)
        }
        removeAuthoredPostFromJournal(postId)
        dao.deletePost(postId)
        feedDao.deletePost(postId)
        SupabaseClient.deletePostInSupabase(postId)
    }

    suspend fun addComment(
        post: PostEntity,
        text: String,
        parentCommentId: String? = null,
        replyToUsername: String? = null
    ) {
        val profile = _userProfile.value
        val sessionNow = _session.value
        val effectiveUserId = profile.id.ifBlank { sessionNow?.userId.orEmpty() }.ifBlank { "user_session" }
        val effectiveUsername = profile.username.ifBlank { sessionNow?.username.orEmpty() }.ifBlank { "user" }
        val effectiveDisplayName = profile.displayName.ifBlank { sessionNow?.displayName.orEmpty() }.ifBlank { effectiveUsername }

        val comment = CommentEntity(
            id = UUID.randomUUID().toString(),
            postId = post.id,
            authorId = effectiveUserId,
            authorUsername = effectiveUsername,
            authorDisplayName = effectiveDisplayName,
            authorAvatarRes = profile.avatarRes,
            authorAvatarUrl = profile.avatarUrl ?: sessionNow?.avatarUrl,
            text = text.trim(),
            parentCommentId = parentCommentId,
            replyToUsername = replyToUsername,
            likeCount = 0,
            isLiked = false,
            timeAgo = "Just now",
            timestampMs = System.currentTimeMillis()
        )
        dao.insertComment(comment)
        feedDao.insertComment(comment.toDatabaseComment())
        upsertAuthoredCommentInJournal(comment)

        val updatedPost = post.copy(commentCount = post.commentCount + 1)
        dao.updatePost(updatedPost)
        feedDao.updatePost(updatedPost.toDatabasePost())
        if (loadAuthoredPostsFromJournal().any { it.id == updatedPost.id }) {
            upsertAuthoredPostInJournal(updatedPost)
        }
        val commentActivity = ActivityItemEntity(
            id = "activity_comment_${comment.id}",
            type = "COMMENT",
            actorId = effectiveUserId,
            actorUsername = effectiveUsername,
            actorDisplayName = effectiveDisplayName,
            actorAvatarRes = profile.avatarRes,
            actorAvatarUrl = profile.avatarUrl ?: sessionNow?.avatarUrl,
            targetPostId = post.id,
            targetPostImageRes = post.imageRes,
            targetPostImageUri = post.imageUri,
            message = "commented: \"${comment.text.take(60)}\"",
            timeLabel = "Just now",
            isRead = false,
            timestampMs = comment.timestampMs
        )
        dao.insertActivities(listOf(commentActivity))
        notificationManager.notifyNewComment(
            actorUsername = effectiveUsername,
            commentText = comment.text,
            postId = post.id,
            dedupeKey = commentActivity.id
        )
        SupabaseClient.insertCommentInSupabase(post, comment, effectiveUserId)
    }

    suspend fun toggleLikeComment(comment: CommentEntity) {
        val nextLiked = !comment.isLiked
        val delta = if (nextLiked) 1 else -1
        val updated = comment.copy(
            isLiked = nextLiked,
            likeCount = (comment.likeCount + delta).coerceAtLeast(0)
        )
        dao.updateComment(updated)
        feedDao.updateComment(updated.toDatabaseComment())
        if (loadAuthoredCommentsFromJournal().any { it.id == updated.id }) {
            upsertAuthoredCommentInJournal(updated)
        }
    }

    suspend fun markStoryViewed(story: StoryEntity) {
        if (!story.isViewed) {
            val viewed = story.copy(isViewed = true)
            dao.updateStory(viewed)
            feedDao.updateStory(viewed.toDatabaseStory())
        }
    }

    suspend fun pinStoryHighlight(story: StoryEntity, highlightTitle: String) {
        val profile = _userProfile.value
        val updated = story.copy(
            authorId = profile.id.ifBlank { story.authorId },
            authorUsername = profile.username.ifBlank { story.authorUsername },
            authorDisplayName = profile.displayName.ifBlank { story.authorDisplayName },
            isHighlight = true,
            highlightTitle = highlightTitle.trim().ifBlank { "Featured" },
            createdAtLabel = "Pinned"
        )
        dao.insertStory(updated)
        feedDao.insertStory(updated.toDatabaseStory())
        upsertAuthoredStoryInJournal(updated)
    }

    suspend fun pinArchivedStoriesToHighlight(stories: List<StoryEntity>, highlightTitle: String) {
        if (stories.isEmpty()) return
        val profile = _userProfile.value
        val resolvedTitle = highlightTitle.trim().ifBlank { "Highlights" }
        stories.forEach { story ->
            val pinnedStory = story.copy(
                authorId = profile.id.ifBlank { story.authorId },
                authorUsername = profile.username.ifBlank { story.authorUsername },
                authorDisplayName = profile.displayName.ifBlank { story.authorDisplayName },
                authorAvatarRes = if (profile.avatarRes != 0) profile.avatarRes else story.authorAvatarRes,
                authorAvatarUrl = profile.avatarUrl ?: story.authorAvatarUrl,
                isHighlight = true,
                highlightTitle = resolvedTitle,
                createdAtLabel = "Pinned"
            )
            dao.insertStory(pinnedStory)
            feedDao.insertStory(pinnedStory.toDatabaseStory())
            upsertAuthoredStoryInJournal(pinnedStory)
        }
    }

    suspend fun unpinStoryHighlight(story: StoryEntity) {
        val updated = story.copy(
            isHighlight = false,
            highlightTitle = "",
            createdAtLabel = "Archived"
        )
        dao.insertStory(updated)
        feedDao.insertStory(updated.toDatabaseStory())
        upsertAuthoredStoryInJournal(updated)
    }

    suspend fun createHighlightFromPost(post: PostEntity, highlightTitle: String) {
        val profile = _userProfile.value
        val highlightStory = StoryEntity(
            id = UUID.randomUUID().toString(),
            authorId = profile.id,
            authorUsername = profile.username,
            authorDisplayName = profile.displayName,
            authorAvatarRes = profile.avatarRes,
            authorAvatarUrl = profile.avatarUrl,
            imageRes = post.imageRes,
            imageUri = post.imageUri,
            videoUri = post.videoUri,
            filterPresetName = post.filterPresetName,
            captionOverlay = post.caption,
            locationTag = post.location,
            isViewed = false,
            isHighlight = true,
            highlightTitle = highlightTitle.trim().ifBlank { "Highlights" },
            createdAtLabel = "Pinned",
            timestampMs = System.currentTimeMillis()
        )
        dao.insertStory(highlightStory)
        feedDao.insertStory(highlightStory.toDatabaseStory())
        upsertAuthoredStoryInJournal(highlightStory)
        SupabaseClient.createStoryInSupabase(highlightStory)
    }

    /**
     * Handles media selection from the device, stages the file locally, bakes Darkroom
     * filter adjustments (for photos), and uploads the file to a Supabase Storage bucket
     * ("posts" for photos or "videos" for videos), returning the URL to be saved in the database.
     */
    suspend fun handleMediaSelectionAndUpload(
        selectedUri: Uri,
        isVideo: Boolean = false,
        bucketName: String? = null,
        filterPreset: FilmPreset = FilmPreset.NATURAL,
        adjustments: DarkroomAdjustments = DarkroomAdjustments()
    ): Result<String> {
        val stagedLocalUri = DarkroomProcessingService.copyUriToPermanentInternalFile(
            context = appContext,
            sourceUri = selectedUri,
            isVideo = isVideo
        )
        val finalUploadSourceUri = if (!isVideo) {
            val bakedFile = DarkroomProcessingService.processAndBakePhoto(
                context = appContext,
                sourceUri = stagedLocalUri,
                fallbackResId = 0,
                preset = filterPreset,
                adjustments = adjustments
            )
            bakedFile?.let { Uri.fromFile(it).toString() } ?: stagedLocalUri
        } else {
            stagedLocalUri
        }
        val uploadResult = SupabaseClient.uploadSelectedMediaToSupabaseBucket(
            context = appContext,
            localUriOrUrl = finalUploadSourceUri,
            bucketName = bucketName,
            fallbackResId = 0,
            isVideo = isVideo
        )
        val resolvedUrl = uploadResult?.publicUrl
            ?.takeIf { it.isNotBlank() }
            ?: if (!isVideo) {
                SupabaseClient.buildCompactDataUriFromLocalFile(
                    context = appContext,
                    localUriOrUrl = finalUploadSourceUri,
                    fallbackResId = 0
                ) ?: finalUploadSourceUri
            } else {
                finalUploadSourceUri
            }
        return if (resolvedUrl.isNotBlank()) {
            Result.success(resolvedUrl)
        } else {
            Result.failure(IllegalStateException("Unable to process and upload selected device media"))
        }
    }

    suspend fun createPost(
        imageRes: Int,
        imageUri: String?,
        videoUri: String? = null,
        carouselImageUrisCsv: String = "",
        carouselImageResCsv: String = "",
        aspectRatio: PhotoAspectRatio,
        filterPreset: FilmPreset,
        adjustments: DarkroomAdjustments = DarkroomAdjustments(),
        caption: String,
        location: String,
        category: String,
        tagsCsv: String,
        isLoop: Boolean,
        audioTrack: String,
        cameraSpec: String
    ): Result<PostEntity> = withContext(Dispatchers.IO) {
        val profile = _userProfile.value
        val sessionNow = _session.value
        val effectiveAuthorId = profile.id.ifBlank { sessionNow?.userId.orEmpty() }.ifBlank { "user_session" }
        val effectiveUsername = profile.username.ifBlank { sessionNow?.username.orEmpty() }.ifBlank { "creator" }
        val effectiveDisplayName = profile.displayName.ifBlank { sessionNow?.displayName.orEmpty() }.ifBlank { effectiveUsername }

        // Stage all carousel items (up to 10 photos and/or videos) into permanent local files immediately
        val inputCarouselUris = splitCarouselUris(carouselImageUrisCsv).take(MAX_CAROUSEL_ITEMS)
        val stagedCarouselUris = inputCarouselUris.map { rawFrameUri ->
            val trimmed = rawFrameUri.trim()
            if (trimmed.startsWith("content://", ignoreCase = true)) {
                DarkroomProcessingService.copyUriToPermanentInternalFile(
                    context = appContext,
                    sourceUri = Uri.parse(trimmed),
                    isVideo = isVideoUrl(trimmed)
                )
            } else {
                trimmed
            }
        }

        val rawVideoCandidate = videoUri?.takeIf { it.isNotBlank() }
            ?: imageUri?.takeIf { isVideoUrl(it) }
            ?: stagedCarouselUris.firstOrNull { isVideoUrl(it) }

        val stagedVideoUri = rawVideoCandidate?.let { rawVid ->
            if (rawVid.startsWith("content://", ignoreCase = true)) {
                DarkroomProcessingService.copyUriToPermanentInternalFile(
                    context = appContext,
                    sourceUri = Uri.parse(rawVid),
                    isVideo = true
                )
            } else {
                rawVid
            }
        }

        val rawNonVideoImageCandidate = imageUri?.takeIf { it.isNotBlank() && !isVideoUrl(it) }
            ?: stagedCarouselUris.firstOrNull { !isVideoUrl(it) }

        val stagedImageUri = rawNonVideoImageCandidate?.let { rawImg ->
            if (rawImg.startsWith("content://", ignoreCase = true)) {
                DarkroomProcessingService.copyUriToPermanentInternalFile(
                    context = appContext,
                    sourceUri = Uri.parse(rawImg),
                    isVideo = false
                )
            } else {
                rawImg
            }
        }

        val hasCustomUserMedia = stagedImageUri != null || stagedVideoUri != null || stagedCarouselUris.isNotEmpty()
        val effectiveFallbackResId = if (hasCustomUserMedia) 0 else imageRes

        val resolvedPrimaryImageUri = stagedImageUri
            ?: if (stagedVideoUri != null) {
                DarkroomProcessingService.extractVideoThumbnailFile(
                    context = appContext,
                    videoUriStr = stagedVideoUri
                )?.let { Uri.fromFile(it).toString() }
            } else if (!hasCustomUserMedia && effectiveFallbackResId != 0) {
                DarkroomProcessingService.processAndBakePhoto(
                    context = appContext,
                    sourceUri = null,
                    fallbackResId = effectiveFallbackResId,
                    preset = filterPreset,
                    adjustments = adjustments
                )?.let { Uri.fromFile(it).toString() }
            } else {
                null
            }

        val initialCarouselCsv = if (stagedCarouselUris.isNotEmpty()) {
            joinCarouselUris(stagedCarouselUris)
        } else {
            ""
        }

        val initialPost = PostEntity(
            id = UUID.randomUUID().toString(),
            authorId = effectiveAuthorId,
            authorUsername = effectiveUsername,
            authorDisplayName = effectiveDisplayName,
            authorAvatarRes = profile.avatarRes,
            authorAvatarUrl = profile.avatarUrl ?: sessionNow?.avatarUrl,
            authorVerified = true,
            imageRes = effectiveFallbackResId,
            imageUri = resolvedPrimaryImageUri,
            videoUri = if (stagedCarouselUris.size > 1) {
                stagedCarouselUris.firstOrNull { isVideoUrl(it) }
            } else {
                stagedVideoUri
            },
            carouselImageUrisCsv = initialCarouselCsv,
            carouselImageResCsv = if (hasCustomUserMedia || initialCarouselCsv.isNotBlank()) "" else carouselImageResCsv,
            aspectRatioName = aspectRatio.name,
            filterPresetName = filterPreset.name,
            brightness = adjustments.brightness,
            exposure = adjustments.exposure,
            contrast = adjustments.contrast,
            saturation = adjustments.saturation,
            warmth = adjustments.warmth,
            filmGrain = adjustments.filmGrain,
            vignette = adjustments.vignette,
            hasExifFrameBorder = adjustments.hasExifFrameBorder,
            exifFrameStyle = adjustments.exifFrameStyle,
            caption = caption.trim(),
            location = location.trim(),
            category = category.trim().ifBlank { "Photography" },
            tagsCsv = tagsCsv.trim(),
            likeCount = 0,
            isLiked = false,
            isBookmarked = false,
            commentCount = 0,
            isLoop = isLoop || (stagedCarouselUris.size <= 1 && stagedVideoUri != null),
            audioTrack = audioTrack.ifBlank { "Original Audio" },
            cameraSpec = cameraSpec.ifBlank { "35mm · f/1.8 · ISO 400" },
            createdAtLabel = "Just now",
            timestampMs = System.currentTimeMillis()
        )

        // Immediately insert into local Room and durable journal so all 10 carousel items appear right away
        dao.insertPost(initialPost)
        feedDao.insertPost(initialPost.toDatabasePost())
        upsertAuthoredPostInJournal(initialPost)

        // Perform Darkroom baking and Supabase cloud upload asynchronously in the background
        // so 10-photo/video carousel publishing never blocks or hangs the UI
        repoScope.launch(Dispatchers.IO) {
            try {
                val bakedPhotoFile = if (resolvedPrimaryImageUri != null || (!hasCustomUserMedia && effectiveFallbackResId != 0)) {
                    DarkroomProcessingService.processAndBakePhoto(
                        context = appContext,
                        sourceUri = resolvedPrimaryImageUri,
                        fallbackResId = effectiveFallbackResId,
                        preset = filterPreset,
                        adjustments = adjustments
                    )
                } else {
                    null
                }
                val bakedLocalUri = bakedPhotoFile?.let { Uri.fromFile(it).toString() }
                val sourceImageForUpload = bakedLocalUri ?: resolvedPrimaryImageUri

                val imageUploadResult = SupabaseClient.uploadSelectedMediaToSupabaseBucket(
                    context = appContext,
                    localUriOrUrl = sourceImageForUpload,
                    bucketName = "posts",
                    fallbackResId = effectiveFallbackResId,
                    isVideo = false
                )
                val uploadedImageUri = imageUploadResult?.publicUrl
                val uploadedVideoUri = if (stagedVideoUri != null) {
                    SupabaseClient.uploadSelectedMediaToSupabaseBucket(
                        context = appContext,
                        localUriOrUrl = stagedVideoUri,
                        bucketName = "videos",
                        fallbackResId = 0,
                        isVideo = true
                    )?.publicUrl
                } else {
                    null
                }

                val remoteShareableImageUrl = if (imageUploadResult?.isCloudHosted == true) {
                    imageUploadResult.publicUrl
                } else {
                    SupabaseClient.buildCompactDataUriFromLocalFile(
                        context = appContext,
                        localUriOrUrl = bakedLocalUri ?: uploadedImageUri ?: resolvedPrimaryImageUri,
                        fallbackResId = effectiveFallbackResId
                    )
                }

                val finalCarouselUris = if (stagedCarouselUris.isNotEmpty()) {
                    stagedCarouselUris.map { frameUri ->
                        if (isVideoUrl(frameUri)) {
                            val uploadedClip = SupabaseClient.uploadSelectedMediaToSupabaseBucket(
                                context = appContext,
                                localUriOrUrl = frameUri,
                                bucketName = "videos",
                                fallbackResId = 0,
                                isVideo = true
                            )?.publicUrl
                            uploadedClip?.takeIf { it.isNotBlank() } ?: frameUri
                        } else {
                            val bakedFrame = DarkroomProcessingService.processAndBakePhoto(
                                context = appContext,
                                sourceUri = frameUri,
                                fallbackResId = 0,
                                preset = filterPreset,
                                adjustments = adjustments
                            )
                            val localFrameUri = bakedFrame?.let { Uri.fromFile(it).toString() } ?: frameUri
                            val uploadedPhoto = SupabaseClient.uploadSelectedMediaToSupabaseBucket(
                                context = appContext,
                                localUriOrUrl = localFrameUri,
                                bucketName = "posts",
                                fallbackResId = 0,
                                isVideo = false
                            )?.publicUrl
                            uploadedPhoto?.takeIf { it.isNotBlank() } ?: localFrameUri
                        }
                    }
                } else {
                    emptyList()
                }

                val updatedPost = initialPost.copy(
                    imageUri = uploadedImageUri?.takeIf { it.isNotBlank() }
                        ?: bakedLocalUri
                        ?: initialPost.imageUri,
                    videoUri = uploadedVideoUri?.takeIf { it.isNotBlank() }
                        ?: initialPost.videoUri,
                    carouselImageUrisCsv = if (finalCarouselUris.isNotEmpty()) {
                        joinCarouselUris(finalCarouselUris)
                    } else {
                        initialPost.carouselImageUrisCsv
                    },
                    filterPresetName = if (bakedPhotoFile != null) FilmPreset.NATURAL.name else initialPost.filterPresetName,
                    brightness = if (bakedPhotoFile != null) 0f else initialPost.brightness,
                    exposure = if (bakedPhotoFile != null) 0f else initialPost.exposure,
                    contrast = if (bakedPhotoFile != null) 1f else initialPost.contrast,
                    saturation = if (bakedPhotoFile != null) 1f else initialPost.saturation,
                    warmth = if (bakedPhotoFile != null) 0f else initialPost.warmth,
                    filmGrain = if (bakedPhotoFile != null) 0f else initialPost.filmGrain,
                    vignette = if (bakedPhotoFile != null) 0f else initialPost.vignette
                )

                dao.insertPost(updatedPost)
                feedDao.insertPost(updatedPost.toDatabasePost())
                upsertAuthoredPostInJournal(updatedPost)

                val remoteResult = SupabaseClient.createPostInSupabase(
                    post = updatedPost,
                    remoteShareableImageUrl = remoteShareableImageUrl
                )
                remoteResult.getOrNull()?.takeIf { it.isNotBlank() && it != updatedPost.id }?.let { remoteId ->
                    val synced = updatedPost.copy(id = remoteId)
                    removeAuthoredPostFromJournal(updatedPost.id)
                    dao.deletePost(updatedPost.id)
                    feedDao.deletePost(updatedPost.id)
                    dao.insertPost(synced)
                    feedDao.insertPost(synced.toDatabasePost())
                    upsertAuthoredPostInJournal(synced)
                }
            } catch (_: Throwable) {
            }
        }

        Result.success(initialPost)
    }

    suspend fun createStory(
        imageRes: Int,
        imageUri: String?,
        videoUri: String? = null,
        filterPreset: FilmPreset,
        captionOverlay: String,
        locationTag: String,
        pinToHighlight: String = ""
    ): StoryEntity {
        val profile = _userProfile.value
        val sessionNow = _session.value
        val resolvedVideo = videoUri?.takeIf { it.isNotBlank() }
            ?: imageUri?.takeIf { isVideoUrl(it) }
        val nonVideoImageUri = imageUri?.takeIf { it.isNotBlank() && !isVideoUrl(it) }
        val hasCustomUserMedia = nonVideoImageUri != null || resolvedVideo != null
        val effectiveFallbackResId = if (hasCustomUserMedia) 0 else imageRes

        val bakedStoryPhoto = if (resolvedVideo == null || nonVideoImageUri != null) {
            DarkroomProcessingService.processAndBakePhoto(
                context = appContext,
                sourceUri = nonVideoImageUri,
                fallbackResId = effectiveFallbackResId,
                preset = filterPreset
            )
        } else {
            DarkroomProcessingService.extractVideoThumbnailFile(
                context = appContext,
                videoUriStr = resolvedVideo
            )
        }
        val bakedLocalUri = bakedStoryPhoto?.let { Uri.fromFile(it).toString() }
        val imageUploadResult = SupabaseClient.uploadSelectedMediaToSupabaseBucket(
            context = appContext,
            localUriOrUrl = bakedLocalUri ?: nonVideoImageUri,
            bucketName = "posts",
            fallbackResId = effectiveFallbackResId,
            isVideo = false
        )
        val uploadedImageUri = imageUploadResult?.publicUrl
        val uploadedVideoUri = if (resolvedVideo != null) {
            SupabaseClient.uploadSelectedMediaToSupabaseBucket(
                context = appContext,
                localUriOrUrl = resolvedVideo,
                bucketName = "videos",
                fallbackResId = 0,
                isVideo = true
            )?.publicUrl
        } else {
            null
        }
        val compactDataUri = if (imageUploadResult?.isCloudHosted == true) {
            imageUploadResult.publicUrl
        } else {
            SupabaseClient.buildCompactDataUriFromLocalFile(
                context = appContext,
                localUriOrUrl = bakedLocalUri ?: uploadedImageUri ?: nonVideoImageUri,
                fallbackResId = effectiveFallbackResId
            )
        }
        val resolvedStoryImageUri = uploadedImageUri
            ?.takeIf { it.isNotBlank() }
            ?: bakedLocalUri
            ?: compactDataUri
            ?: nonVideoImageUri

        val newStory = StoryEntity(
            id = UUID.randomUUID().toString(),
            authorId = profile.id.ifBlank { sessionNow?.userId.orEmpty() }.ifBlank { "user_session" },
            authorUsername = profile.username.ifBlank { sessionNow?.username.orEmpty() }.ifBlank { "creator" },
            authorDisplayName = profile.displayName.ifBlank { sessionNow?.displayName.orEmpty() }.ifBlank { "Creator" },
            authorAvatarRes = profile.avatarRes,
            authorAvatarUrl = profile.avatarUrl ?: sessionNow?.avatarUrl,
            imageRes = effectiveFallbackResId,
            imageUri = resolvedStoryImageUri,
            videoUri = uploadedVideoUri ?: resolvedVideo,
            filterPresetName = if (bakedStoryPhoto != null) FilmPreset.NATURAL.name else filterPreset.name,
            captionOverlay = captionOverlay.trim(),
            locationTag = locationTag.trim(),
            isViewed = false,
            isHighlight = pinToHighlight.isNotBlank(),
            highlightTitle = pinToHighlight.trim(),
            createdAtLabel = "Just now",
            timestampMs = System.currentTimeMillis()
        )
        dao.insertStory(newStory)
        feedDao.insertStory(newStory.toDatabaseStory())
        upsertAuthoredStoryInJournal(newStory)
        SupabaseClient.createStoryInSupabase(
            newStory.copy(imageUri = compactDataUri ?: newStory.imageUri)
        )
        return newStory
    }

    suspend fun generateCaptionAndTagsWithOpenRouter(
        location: String,
        category: String,
        filmPreset: FilmPreset,
        notes: String
    ): Result<Pair<String, String>> {
        return SupabaseClient.generateCaptionAndTagsWithOpenRouter(
            location = location,
            category = category,
            filmPreset = filmPreset,
            draftNotes = notes
        )
    }

    suspend fun toggleFollowCreator(creator: CreatorProfileEntity) {
        val nextFollow = !creator.isFollowing
        val delta = if (nextFollow) 1 else -1
        dao.updateCreator(
            creator.copy(
                isFollowing = nextFollow,
                followersCount = (creator.followersCount + delta).coerceAtLeast(0)
            )
        )
    }

    suspend fun blockCreator(creator: CreatorProfileEntity) {
        dao.updateCreator(creator.copy(isBlocked = true, isFollowing = false))
    }

    suspend fun sendDirectMessage(
        participantId: String,
        participantUsername: String,
        participantDisplayName: String,
        participantAvatarRes: Int,
        text: String,
        sharedPost: PostEntity? = null,
        attachedPhotoUri: String? = null,
        voiceNoteDurationSec: Int = 0
    ) {
        val threadId = "thread_$participantId"
        val matchedCreator = cachedProfiles.find { it.id == participantId }
        val message = DirectMessageEntity(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            participantId = participantId,
            participantUsername = participantUsername,
            participantDisplayName = participantDisplayName,
            participantAvatarRes = participantAvatarRes,
            participantAvatarUrl = matchedCreator?.avatarUrl,
            isFromMe = true,
            text = text.trim(),
            sharedPostId = sharedPost?.id,
            sharedPostCaption = sharedPost?.caption,
            sharedPostImageRes = sharedPost?.imageRes,
            sharedPostImageUri = attachedPhotoUri ?: sharedPost?.imageUri,
            voiceNoteDurationSec = voiceNoteDurationSec,
            timeLabel = "Just now",
            isLiked = false,
            isUnread = false,
            timestampMs = System.currentTimeMillis()
        )
        dao.insertDirectMessage(message)
        notificationManager.notifyIncomingMessage(
            senderUsername = participantUsername,
            senderDisplayName = participantDisplayName,
            messageText = if (text.isNotBlank()) text.trim() else "Shared a post in chat",
            threadId = threadId,
            dedupeKey = "dm_sent_${message.id}"
        )
    }

    suspend fun receiveIncomingDirectMessage(
        participantId: String,
        participantUsername: String,
        participantDisplayName: String,
        participantAvatarRes: Int,
        text: String
    ): DirectMessageEntity {
        val threadId = "thread_$participantId"
        val matchedCreator = cachedProfiles.find { it.id == participantId }
        val incoming = DirectMessageEntity(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            participantId = participantId,
            participantUsername = participantUsername,
            participantDisplayName = participantDisplayName,
            participantAvatarRes = participantAvatarRes,
            participantAvatarUrl = matchedCreator?.avatarUrl,
            isFromMe = false,
            text = text.trim(),
            sharedPostId = null,
            sharedPostCaption = null,
            sharedPostImageRes = null,
            sharedPostImageUri = null,
            voiceNoteDurationSec = 0,
            timeLabel = "Just now",
            isLiked = false,
            isUnread = true,
            timestampMs = System.currentTimeMillis()
        )
        dao.insertDirectMessage(incoming)
        notificationManager.notifyIncomingMessage(
            senderUsername = participantUsername,
            senderDisplayName = participantDisplayName,
            messageText = incoming.text,
            threadId = threadId,
            dedupeKey = "dm_in_${incoming.id}"
        )
        return incoming
    }

    suspend fun toggleLikeMessage(message: DirectMessageEntity) {
        dao.updateDirectMessage(message.copy(isLiked = !message.isLiked))
    }

    suspend fun markThreadRead(threadId: String) {
        dao.markThreadRead(threadId)
    }

    suspend fun markAllActivitiesRead() {
        dao.markAllActivitiesRead()
    }

    suspend fun saveStudioDraft(draft: StudioDraftEntity): StudioDraftEntity = withContext(Dispatchers.IO) {
        val resolvedId = draft.id.ifBlank { "draft_${UUID.randomUUID()}" }
        val persisted = draft.copy(
            id = resolvedId,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertStudioDraft(persisted)
        if (!persisted.isAutoSave && persisted.id != STUDIO_AUTOSAVE_DRAFT_ID) {
            dao.deleteStudioDraft(STUDIO_AUTOSAVE_DRAFT_ID)
        }
        persisted
    }

    suspend fun getStudioDraftById(draftId: String): StudioDraftEntity? = withContext(Dispatchers.IO) {
        dao.getStudioDraftById(draftId)
    }

    suspend fun deleteStudioDraft(draftId: String) = withContext(Dispatchers.IO) {
        dao.deleteStudioDraft(draftId)
    }

    suspend fun clearAutoSaveStudioDrafts() = withContext(Dispatchers.IO) {
        dao.clearAutoSaveStudioDrafts()
    }
}
