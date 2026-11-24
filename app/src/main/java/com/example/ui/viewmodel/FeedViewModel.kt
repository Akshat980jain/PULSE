package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PulseDatabase
import com.example.data.remote.SupabaseClient
import com.example.data.repository.PulseRepository
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
import com.example.database.toPostEntity
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for the Pulse social feed backed by the Room local cache
 * ([Post], [User], [Story], [Comment], [PostLike], and [UserInteraction] entities)
 * and synchronized with Supabase.
 */
data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val users: List<User> = emptyList(),
    val stories: List<Story> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val commentsByPostId: Map<String, List<Comment>> = emptyMap(),
    val postLikes: List<PostLike> = emptyList(),
    val userInteractions: List<UserInteraction> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMorePosts: Boolean = true,
    val isServingFromLocalCache: Boolean = false,
    val lastCachedAtMs: Long? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel that manages the state of the Pulse social feed by fetching
 * [Post], [User], [Story], and [Comment] data from [SupabaseClient] and caching it in
 * the Room database ([PulseFeedDatabase] & [PulseDatabase]), while tracking
 * user like and comment interactions.
 */
class FeedViewModel(
    private val feedDao: PulseFeedDao,
    private val repository: PulseRepository? = null,
    autoRefreshOnInit: Boolean = repository != null
) : ViewModel() {

    private val isLoadingFlow = MutableStateFlow(false)
    private val isRefreshingFlow = MutableStateFlow(false)
    private val hasMorePostsFlow = MutableStateFlow(true)
    private val errorMessageFlow = MutableStateFlow<String?>(null)

    val posts: StateFlow<List<Post>> = feedDao.observePosts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val likedPosts: StateFlow<List<Post>> = feedDao.observeLikedPosts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val users: StateFlow<List<User>> = feedDao.observeUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val stories: StateFlow<List<Story>> = feedDao.observeStories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val comments: StateFlow<List<Comment>> = feedDao.observeAllComments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val postLikes: StateFlow<List<PostLike>> = feedDao.observePostLikes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val userInteractions: StateFlow<List<UserInteraction>> = feedDao.observeUserInteractions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val interactionDataFlow = combine(
        feedDao.observePostLikes(),
        feedDao.observeUserInteractions(),
        feedDao.observeAllComments()
    ) { likes, interactions, allComments ->
        Triple(likes, interactions, allComments)
    }

    val uiState: StateFlow<FeedUiState> = combine(
        feedDao.observePosts(),
        feedDao.observeUsers(),
        feedDao.observeStories(),
        interactionDataFlow,
        combine(isLoadingFlow, isRefreshingFlow, hasMorePostsFlow, errorMessageFlow) { l, r, m, err ->
            listOf(l, r, m, err)
        }
    ) { cachedPosts, cachedUsers, cachedStories, (likes, interactions, allComments), statusList ->
        val isLoading = statusList[0] as Boolean
        val isRefreshing = statusList[1] as Boolean
        val hasMore = statusList[2] as Boolean
        val errorMessage = statusList[3] as String?
        FeedUiState(
            posts = cachedPosts,
            users = cachedUsers,
            stories = cachedStories,
            comments = allComments,
            commentsByPostId = allComments.groupBy { it.postId },
            postLikes = likes,
            userInteractions = interactions,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            hasMorePosts = repository?.hasMorePosts?.value ?: hasMore,
            isServingFromLocalCache = cachedPosts.isNotEmpty(),
            lastCachedAtMs = cachedPosts.maxOfOrNull { it.cachedAtMs },
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FeedUiState()
    )

    init {
        if (autoRefreshOnInit) {
            refreshFeedFromSupabase(forceRefresh = false)
        }
    }

    /**
     * Fetches the latest social feed (Users, Stories, Comments, and Posts) from Supabase
     * and caches the results in the local Room database.
     * When [forceRefresh] is false and the local Room cache is still fresh (within [cacheMaxAgeMs]),
     * serves the cached posts directly without redundant network calls.
     */
    fun refreshFeedFromSupabase(
        currentUserId: String? = null,
        pageSize: Int = PulseRepository.DEFAULT_PAGE_SIZE,
        forceRefresh: Boolean = true,
        cacheMaxAgeMs: Long = PulseRepository.DEFAULT_CACHE_TTL_MS
    ) {
        if (isRefreshingFlow.value) return
        viewModelScope.launch {
            if (!forceRefresh) {
                val cachedCount = feedDao.getCachedPostCount()
                val latestCachedAt = feedDao.getLatestCacheTimestampMs()
                if (cachedCount > 0 && latestCachedAt != null &&
                    (System.currentTimeMillis() - latestCachedAt) in 0..cacheMaxAgeMs
                ) {
                    return@launch
                }
            }

            isLoadingFlow.value = uiState.value.posts.isEmpty() && feedDao.getCachedPostCount() == 0
            isRefreshingFlow.value = true
            errorMessageFlow.value = null
            try {
                if (repository != null) {
                    val result = repository.refreshFromSupabase(
                        pageSize = pageSize,
                        forceRefresh = forceRefresh
                    )
                    result.onFailure { err ->
                        errorMessageFlow.value = err.localizedMessage
                    }
                    hasMorePostsFlow.value = repository.hasMorePosts.value
                } else if (SupabaseClient.isConfigured) {
                    val profilesResult = SupabaseClient.fetchProfiles(currentUserId)
                    profilesResult.onSuccess { remoteProfiles ->
                        val mappedUsers = remoteProfiles.map { it.toDatabaseUser() }
                        if (mappedUsers.isNotEmpty()) {
                            feedDao.insertUsers(mappedUsers)
                        }
                    }

                    val storiesResult = SupabaseClient.fetchStories()
                    storiesResult.onSuccess { remoteStories ->
                        val mappedStories = remoteStories.map { it.toDatabaseStory() }
                        if (mappedStories.isNotEmpty()) {
                            feedDao.insertStories(mappedStories)
                        }
                    }

                    val commentsResult = SupabaseClient.fetchComments()
                    commentsResult.onSuccess { remoteComments ->
                        val mappedComments = remoteComments.map { it.toDatabaseComment() }
                        if (mappedComments.isNotEmpty()) {
                            feedDao.insertComments(mappedComments)
                        }
                    }

                    val postsResult = SupabaseClient.fetchPosts(
                        currentUserId = currentUserId,
                        limit = pageSize,
                        offset = 0
                    )
                    postsResult.fold(
                        onSuccess = { remotePosts ->
                            val nowCacheMs = System.currentTimeMillis()
                            val mappedPosts = remotePosts.map { it.toDatabasePost().copy(cachedAtMs = nowCacheMs) }
                            if (mappedPosts.isNotEmpty()) {
                                feedDao.insertPosts(mappedPosts)
                            }
                            hasMorePostsFlow.value = remotePosts.size >= pageSize
                        },
                        onFailure = { err ->
                            errorMessageFlow.value = err.localizedMessage
                        }
                    )
                }
            } catch (e: Throwable) {
                errorMessageFlow.value = e.localizedMessage
            } finally {
                isLoadingFlow.value = false
                isRefreshingFlow.value = false
            }
        }
    }

    /**
     * Caches a list of [Post], [User], [Story], and [Comment] entities directly into Room.
     */
    fun cacheFeedEntities(
        newPosts: List<Post> = emptyList(),
        newUsers: List<User> = emptyList(),
        newStories: List<Story> = emptyList(),
        newComments: List<Comment> = emptyList()
    ) {
        viewModelScope.launch {
            if (newUsers.isNotEmpty()) {
                feedDao.insertUsers(newUsers)
            }
            if (newStories.isNotEmpty()) {
                feedDao.insertStories(newStories)
            }
            if (newPosts.isNotEmpty()) {
                feedDao.insertPosts(newPosts)
            }
            if (newComments.isNotEmpty()) {
                feedDao.insertComments(newComments)
            }
        }
    }

    /**
     * Toggles the 'like' heart state on a [Post] and records the user interaction in Room.
     */
    fun toggleLikePost(post: Post, forceLike: Boolean = false, userId: String = "local_user") {
        viewModelScope.launch {
            toggleLikePostSuspend(post, forceLike, userId)
        }
    }

    suspend fun toggleLikePostSuspend(post: Post, forceLike: Boolean = false, userId: String = "local_user") {
        if (repository != null) {
            repository.toggleLikePost(post.toPostEntity(), forceLike)
        } else {
            val targetLiked = if (forceLike) true else !post.isLiked
            if (targetLiked == post.isLiked) return
            val delta = if (targetLiked) 1 else -1
            val newLikeCount = (post.likeCount + delta).coerceAtLeast(0)
            val nowMs = System.currentTimeMillis()
            val likedUsers = post.likedByUserIdsCsv
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableSet()
            if (targetLiked) {
                likedUsers.add(userId)
            } else {
                likedUsers.remove(userId)
            }
            val updated = post.copy(
                isLiked = targetLiked,
                likeCount = newLikeCount,
                likedByUserIdsCsv = likedUsers.joinToString(","),
                lastLikedAtMs = if (targetLiked) nowMs else post.lastLikedAtMs
            )
            feedDao.updatePost(updated)
            if (targetLiked) {
                feedDao.upsertPostLike(
                    PostLike(
                        postId = post.id,
                        userId = userId,
                        isLiked = true,
                        timestampMs = nowMs
                    )
                )
                feedDao.insertUserInteraction(
                    UserInteraction(
                        id = "like_${post.id}_$userId",
                        postId = post.id,
                        userId = userId,
                        interactionType = "LIKE",
                        isActive = true,
                        timestampMs = nowMs
                    )
                )
            } else {
                feedDao.deletePostLike(post.id, userId)
                feedDao.deleteUserInteraction(post.id, userId, "LIKE")
            }
        }
    }

    /**
     * Posts a new comment on a feed photo and updates both the Comment table and the Post commentCount.
     */
    fun addComment(
        post: Post,
        text: String,
        authorUsername: String = "creator",
        authorDisplayName: String = "Creator"
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            addCommentSuspend(post, text, authorUsername, authorDisplayName)
        }
    }

    suspend fun addCommentSuspend(
        post: Post,
        text: String,
        authorUsername: String = "creator",
        authorDisplayName: String = "Creator"
    ) {
        if (text.isBlank()) return
        if (repository != null) {
            repository.addComment(post.toPostEntity(), text)
        } else {
            val newComment = Comment(
                id = UUID.randomUUID().toString(),
                postId = post.id,
                authorUsername = authorUsername,
                authorDisplayName = authorDisplayName,
                text = text.trim(),
                timeAgo = "Just now",
                timestampMs = System.currentTimeMillis()
            )
            feedDao.insertComment(newComment)
            feedDao.updatePost(post.copy(commentCount = post.commentCount + 1))
        }
    }

    fun toggleBookmarkPost(post: Post, moodboardName: String = "Favorites") {
        viewModelScope.launch {
            if (repository != null) {
                repository.toggleBookmarkPost(post.toPostEntity(), moodboardName)
            } else {
                val nextSaved = !post.isBookmarked
                val updated = post.copy(
                    isBookmarked = nextSaved,
                    moodboardName = if (nextSaved) moodboardName else "All Saved"
                )
                feedDao.updatePost(updated)
            }
        }
    }

    fun markStoryViewed(story: Story) {
        viewModelScope.launch {
            if (!story.isViewed) {
                feedDao.updateStory(story.copy(isViewed = true))
            }
        }
    }

    fun clearError() {
        errorMessageFlow.value = null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            val pulseDb = PulseDatabase.getInstance(appContext)
            val feedDb = PulseFeedDatabase.getInstance(appContext)
            val feedDao = feedDb.pulseFeedDao()
            val repo = PulseRepository(pulseDb.pulseDao(), appContext, feedDao)
            return FeedViewModel(feedDao, repo, autoRefreshOnInit = true) as T
        }
    }
}
