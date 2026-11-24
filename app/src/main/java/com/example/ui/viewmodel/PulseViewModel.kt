package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PulseDatabase
import com.example.data.model.ActivityItemEntity
import com.example.data.model.CommentEntity
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.DarkroomAdjustments
import com.example.data.model.DirectMessageEntity
import com.example.data.model.DirectMessageThreadSummary
import com.example.data.model.FeedMode
import com.example.data.model.FilmPreset
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.PostEntity
import com.example.data.model.STUDIO_AUTOSAVE_DRAFT_ID
import com.example.data.model.StoryEntity
import com.example.data.model.StoryHighlightGroup
import com.example.data.model.StudioDraftEntity
import com.example.data.model.StudioPublishTarget
import com.example.data.model.SupabaseSession
import com.example.data.model.UserProfileState
import com.example.data.repository.PulseRepository
import com.example.database.Post
import com.example.database.PostLike
import com.example.database.Story
import com.example.database.User
import com.example.database.UserInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PulseUiState(
    val session: SupabaseSession? = null,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val isRefreshingFeed: Boolean = false,
    val isLoadingMorePosts: Boolean = false,
    val hasMorePosts: Boolean = true,
    val isGeneratingAiCaption: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val userProfile: UserProfileState = UserProfileState(),
    val isDarkTheme: Boolean = true,
    val feedMode: FeedMode = FeedMode.CURATED,
    val posts: List<PostEntity> = emptyList(),
    val feedPosts: List<PostEntity> = emptyList(),
    val loopPosts: List<PostEntity> = emptyList(),
    val myPosts: List<PostEntity> = emptyList(),
    val bookmarkedPosts: List<PostEntity> = emptyList(),
    val moodboards: List<String> = listOf("All Saved", "Visual Inspiration", "Architecture", "Monochrome"),
    val selectedMoodboard: String = "All Saved",
    val stories: List<StoryEntity> = emptyList(),
    val archivedStories: List<StoryEntity> = emptyList(),
    val profileHighlights: List<StoryHighlightGroup> = emptyList(),
    val commentsByPostId: Map<String, List<CommentEntity>> = emptyMap(),
    val creators: List<CreatorProfileEntity> = emptyList(),
    val threads: List<DirectMessageThreadSummary> = emptyList(),
    val unreadDmCount: Int = 0,
    val activities: List<ActivityItemEntity> = emptyList(),
    val unreadActivityCount: Int = 0,
    val searchQuery: String = "",
    val selectedExploreCategory: String = "All",
    val explorePosts: List<PostEntity> = emptyList(),
    val exploreCreators: List<CreatorProfileEntity> = emptyList(),
    val studioDrafts: List<StudioDraftEntity> = emptyList(),
    val statusBannerMessage: String? = null
)

private data class CoreDataBundle(
    val posts: List<PostEntity>,
    val stories: List<StoryEntity>,
    val comments: List<CommentEntity>,
    val creators: List<CreatorProfileEntity>,
    val messages: List<DirectMessageEntity>
)

private data class FilterStateBundle(
    val feedMode: FeedMode,
    val searchQuery: String,
    val category: String,
    val selectedMoodboard: String,
    val hiddenPostIds: Set<String>,
    val banner: String?
)

private data class AuthRuntimeBundle(
    val session: SupabaseSession?,
    val isAuthLoading: Boolean,
    val authError: String?,
    val isRefreshing: Boolean,
    val isLoadingMore: Boolean,
    val hasMorePosts: Boolean,
    val isGeneratingAi: Boolean,
    val isUploadingMedia: Boolean,
    val userProfile: UserProfileState,
    val isDarkTheme: Boolean,
    val moodboards: List<String>
)

class PulseViewModel(private val repository: PulseRepository) : ViewModel() {

    val cachedPosts: StateFlow<List<Post>> = repository.cachedPosts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val cachedUsers: StateFlow<List<User>> = repository.cachedUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val cachedStories: StateFlow<List<Story>> = repository.cachedStories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val postLikes: StateFlow<List<PostLike>> = repository.postLikes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val userInteractions: StateFlow<List<UserInteraction>> = repository.userInteractions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val feedModeFlow = MutableStateFlow(FeedMode.CURATED)
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow("All")
    private val selectedMoodboardFlow = MutableStateFlow("All Saved")
    private val hiddenPostIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    private val bannerMessageFlow = MutableStateFlow<String?>(null)

    private val isAuthLoadingFlow = MutableStateFlow(false)
    private val authErrorFlow = MutableStateFlow<String?>(null)
    private val isRefreshingFlow = MutableStateFlow(false)
    private val isLoadingMoreFlow = MutableStateFlow(false)
    private val isGeneratingAiCaptionFlow = MutableStateFlow(false)
    private val isUploadingMediaFlow = MutableStateFlow(false)

    init {
        refreshFromSupabase(forceRefresh = false)
    }

    private val coreDataFlow = combine(
        repository.posts,
        repository.stories,
        repository.comments,
        repository.creators,
        repository.directMessages
    ) { posts, stories, comments, creators, messages ->
        CoreDataBundle(posts, stories, comments, creators, messages)
    }

    private val filterStateFlow = combine(
        feedModeFlow,
        searchQueryFlow,
        selectedCategoryFlow,
        selectedMoodboardFlow,
        combine(hiddenPostIdsFlow, bannerMessageFlow) { h, b -> h to b }
    ) { feedMode, searchQuery, category, moodboard, (hiddenPostIds, banner) ->
        FilterStateBundle(feedMode, searchQuery, category, moodboard, hiddenPostIds, banner)
    }

    private val paginationRuntimeFlow = combine(
        isRefreshingFlow,
        isLoadingMoreFlow,
        repository.hasMorePosts,
        isGeneratingAiCaptionFlow,
        isUploadingMediaFlow
    ) { isRefreshing, isLoadingMore, hasMore, isGenAi, isUploading ->
        listOf(isRefreshing, isLoadingMore, hasMore, isGenAi, isUploading)
    }

    private val authRuntimeFlow = combine(
        combine(repository.session, isAuthLoadingFlow, authErrorFlow) { s, l, e -> Triple(s, l, e) },
        paginationRuntimeFlow,
        repository.userProfile,
        repository.isDarkTheme,
        repository.moodboards
    ) { (session, isAuthLoading, authErr), pagFlags, userProfile, isDarkTheme, moodboards ->
        AuthRuntimeBundle(
            session = session,
            isAuthLoading = isAuthLoading,
            authError = authErr,
            isRefreshing = pagFlags[0],
            isLoadingMore = pagFlags[1],
            hasMorePosts = pagFlags[2],
            isGeneratingAi = pagFlags[3],
            isUploadingMedia = pagFlags[4],
            userProfile = userProfile,
            isDarkTheme = isDarkTheme,
            moodboards = moodboards
        )
    }

    val uiState: StateFlow<PulseUiState> = combine(
        coreDataFlow,
        filterStateFlow,
        repository.activities,
        authRuntimeFlow,
        repository.studioDrafts
    ) { core, filter, activities, runtime, studioDrafts ->
        val userProfile = runtime.userProfile
        val blockedCreatorIds = core.creators.filter { it.isBlocked }.map { it.id }.toSet()
        val followingCreatorIds = core.creators.filter { it.isFollowing && !it.isBlocked }.map { it.id }.toSet() + userProfile.id
        val visibleCreators = core.creators
            .filter { !it.isBlocked && it.id.isNotBlank() }
            .distinctBy { it.id }
            .distinctBy { it.username.lowercase().ifBlank { it.id } }

        val visiblePosts = core.posts
            .filter { post ->
                post.id.isNotBlank() && post.authorId !in blockedCreatorIds && post.id !in filter.hiddenPostIds
            }
            .distinctBy { it.id }

        val allBookmarked = visiblePosts.filter { it.isBookmarked }
        val filteredBookmarked = if (filter.selectedMoodboard == "All Saved") {
            allBookmarked
        } else {
            allBookmarked.filter { it.moodboardName.equals(filter.selectedMoodboard, ignoreCase = true) }
        }

        val feedPosts = when (filter.feedMode) {
            FeedMode.CURATED -> visiblePosts
            FeedMode.FOLLOWING -> visiblePosts.filter {
                followingCreatorIds.size <= 1 || it.authorId in followingCreatorIds
            }
            FeedMode.SAVED -> filteredBookmarked
        }

        // Strictly short videos only in Loops (Instagram Reels behavior — photos stay in Feed)
        val loopPosts = visiblePosts.filter { it.hasVideo }
        val matchedMyPosts = visiblePosts.filter {
            (userProfile.id.isNotBlank() && it.authorId.equals(userProfile.id, ignoreCase = true)) ||
                (userProfile.username.isNotBlank() && it.authorUsername.equals(userProfile.username, ignoreCase = true))
        }
        val myPosts = matchedMyPosts.sortedByDescending { it.isPinnedOnProfile }

        val visibleStories = core.stories
            .filter { it.id.isNotBlank() && it.authorId !in blockedCreatorIds }
            .distinctBy { it.id }

        val myOwnedStories = visibleStories.filter {
            (userProfile.id.isNotBlank() && it.authorId.equals(userProfile.id, ignoreCase = true)) ||
                (userProfile.username.isNotBlank() && it.authorUsername.equals(userProfile.username, ignoreCase = true)) ||
                it.createdAtLabel.equals("Pinned", ignoreCase = true)
        }
        val postDerivedArchiveStories = myPosts.map { post ->
            StoryEntity(
                id = "archive_post_${post.id}",
                authorId = userProfile.id.ifBlank { post.authorId },
                authorUsername = userProfile.username.ifBlank { post.authorUsername },
                authorDisplayName = userProfile.displayName.ifBlank { post.authorDisplayName },
                authorAvatarRes = userProfile.avatarRes,
                authorAvatarUrl = userProfile.avatarUrl ?: post.authorAvatarUrl,
                imageRes = post.imageRes,
                imageUri = post.imageUri,
                videoUri = post.videoUri,
                filterPresetName = post.filterPresetName,
                captionOverlay = post.caption.take(90),
                locationTag = post.location,
                isViewed = true,
                isHighlight = false,
                highlightTitle = post.category.ifBlank { "Studio" },
                createdAtLabel = post.createdAtLabel.ifBlank { "Archived" },
                timestampMs = post.timestampMs
            )
        }
        val archivedStories = (myOwnedStories + postDerivedArchiveStories + visibleStories)
            .distinctBy { it.id }
            .sortedByDescending { it.timestampMs }

        val myHighlightStories = myOwnedStories.filter { it.isHighlight }
        val highlightStories = if (myHighlightStories.isNotEmpty()) {
            myHighlightStories
        } else {
            visibleStories.filter { it.isHighlight }
        }
        val profileHighlights = highlightStories
            .groupBy { it.highlightTitle.ifBlank { "Highlights" } }
            .map { (title, items) ->
                val first = items.first()
                StoryHighlightGroup(
                    id = "hl_${title.lowercase().replace(" ", "_")}",
                    title = title,
                    coverImageUri = first.imageUri,
                    coverImageRes = first.imageRes,
                    stories = items.distinctBy { it.id }
                )
            }
            .distinctBy { it.id }

        val rawQuery = filter.searchQuery.trim()
        val normalizedQuery = rawQuery.lowercase().removePrefix("@").removePrefix("#").trim()
        val isHandleOnlyQuery = rawQuery.startsWith("@")
        val isTagOnlyQuery = rawQuery.startsWith("#")
        val cat = filter.category

        val explorePosts = visiblePosts.filter { post ->
            val matchesCategory = when (cat) {
                "All" -> true
                "Loops" -> post.hasVideo
                "Kodachrome" -> post.filterPreset == FilmPreset.KODACHROME_64
                else -> post.category.equals(cat, ignoreCase = true)
            }
            val matchesQuery = if (normalizedQuery.isEmpty()) {
                true
            } else {
                val matchesHandle = post.authorUsername.lowercase().contains(normalizedQuery) ||
                    post.authorDisplayName.lowercase().contains(normalizedQuery)
                val matchesTag = post.tags.any { it.lowercase().removePrefix("#").contains(normalizedQuery) } ||
                    post.tagsCsv.lowercase().contains(normalizedQuery) ||
                    post.category.lowercase().contains(normalizedQuery)
                val matchesCaptionOrLocation = post.caption.lowercase().contains(normalizedQuery) ||
                    post.location.lowercase().contains(normalizedQuery)

                when {
                    isHandleOnlyQuery -> matchesHandle
                    isTagOnlyQuery -> matchesTag
                    else -> matchesTag || matchesHandle || matchesCaptionOrLocation
                }
            }
            matchesCategory && matchesQuery
        }

        val exploreCreators = if (normalizedQuery.isEmpty()) {
            visibleCreators
        } else {
            visibleCreators.filter {
                it.username.lowercase().contains(normalizedQuery) ||
                    it.displayName.lowercase().contains(normalizedQuery) ||
                    it.roleTag.lowercase().contains(normalizedQuery) ||
                    it.location.lowercase().contains(normalizedQuery)
            }
        }

        val groupedMessages = core.messages
            .filter { it.id.isNotBlank() && it.participantId !in blockedCreatorIds }
            .distinctBy { it.id }
            .groupBy { it.threadId.ifBlank { "thread_${it.participantId}" } }

        val threads = groupedMessages.mapNotNull { (threadId, msgs) ->
            val sorted = msgs.sortedBy { it.timestampMs }
            val last = sorted.lastOrNull() ?: return@mapNotNull null
            val unreadCount = sorted.count { it.isUnread && !it.isFromMe }
            DirectMessageThreadSummary(
                threadId = threadId,
                participantId = last.participantId,
                participantUsername = last.participantUsername,
                participantDisplayName = last.participantDisplayName,
                participantAvatarRes = last.participantAvatarRes,
                participantAvatarUrl = last.participantAvatarUrl,
                lastMessageText = if (last.sharedPostId != null) "Shared a post · ${last.text}" else last.text,
                lastTimeLabel = last.timeLabel,
                unreadCount = unreadCount,
                messages = sorted
            )
        }
            .distinctBy { it.threadId }
            .sortedByDescending { it.messages.lastOrNull()?.timestampMs ?: 0L }

        val safeActivities = activities
            .filter { it.id.isNotBlank() && it.actorId !in blockedCreatorIds }
            .distinctBy { it.id }

        val unreadDmCount = threads.sumOf { it.unreadCount }
        val unreadActivityCount = safeActivities.count { !it.isRead }

        PulseUiState(
            session = runtime.session,
            isAuthLoading = runtime.isAuthLoading,
            authErrorMessage = runtime.authError,
            isRefreshingFeed = runtime.isRefreshing,
            isLoadingMorePosts = runtime.isLoadingMore,
            hasMorePosts = runtime.hasMorePosts,
            isGeneratingAiCaption = runtime.isGeneratingAi,
            isUploadingMedia = runtime.isUploadingMedia,
            userProfile = userProfile,
            isDarkTheme = runtime.isDarkTheme,
            feedMode = filter.feedMode,
            posts = visiblePosts,
            feedPosts = feedPosts,
            loopPosts = loopPosts,
            myPosts = myPosts,
            bookmarkedPosts = filteredBookmarked,
            moodboards = runtime.moodboards.filter { it.isNotBlank() }.distinct(),
            selectedMoodboard = filter.selectedMoodboard,
            stories = visibleStories,
            archivedStories = archivedStories,
            profileHighlights = profileHighlights,
            commentsByPostId = core.comments.filter { it.id.isNotBlank() }.distinctBy { it.id }.groupBy { it.postId },
            creators = visibleCreators,
            threads = threads,
            unreadDmCount = unreadDmCount,
            activities = safeActivities,
            unreadActivityCount = unreadActivityCount,
            searchQuery = filter.searchQuery,
            selectedExploreCategory = filter.category,
            explorePosts = explorePosts,
            exploreCreators = exploreCreators,
            studioDrafts = studioDrafts,
            statusBannerMessage = filter.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PulseUiState(
            session = repository.session.value,
            userProfile = repository.userProfile.value,
            isDarkTheme = repository.isDarkTheme.value,
            moodboards = repository.moodboards.value
        )
    )

    private fun formatAuthErrorMessage(err: Throwable?): String {
        val raw = err?.localizedMessage.orEmpty()
        val lower = raw.lowercase()
        return when {
            lower.contains("unable to resolve host") ||
            lower.contains("no address associated") ||
            lower.contains("failed to connect") ||
            lower.contains("network error") ||
            lower.contains("timed out") ||
            lower.contains("timeout") ->
                "Unable to connect to server. Please check your internet connection or continue as guest."
            lower.contains("invalid login credentials") ->
                "Invalid email or password. Please try again."
            lower.contains("email not confirmed") ->
                "Email confirmation pending. Please check your inbox or continue."
            raw.isNotBlank() -> raw
            else -> "Sign in failed. Please try again."
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            authErrorFlow.value = "Please enter both email and password."
            return
        }
        viewModelScope.launch {
            isAuthLoadingFlow.value = true
            authErrorFlow.value = null
            try {
                val res = repository.signIn(email, password)
                isAuthLoadingFlow.value = false
                res.fold(
                    onSuccess = {
                        refreshFromSupabase()
                    },
                    onFailure = { err ->
                        authErrorFlow.value = formatAuthErrorMessage(err)
                    }
                )
            } catch (e: Throwable) {
                authErrorFlow.value = formatAuthErrorMessage(e)
            } finally {
                isAuthLoadingFlow.value = false
            }
        }
    }

    fun signUp(email: String, password: String, username: String, displayName: String) {
        if (email.isBlank() || password.isBlank()) {
            authErrorFlow.value = "Please enter email and password."
            return
        }
        viewModelScope.launch {
            isAuthLoadingFlow.value = true
            authErrorFlow.value = null
            try {
                val res = repository.signUp(email, password, username, displayName)
                isAuthLoadingFlow.value = false
                res.fold(
                    onSuccess = {
                        refreshFromSupabase()
                    },
                    onFailure = { err ->
                        authErrorFlow.value = formatAuthErrorMessage(err)
                    }
                )
            } catch (e: Throwable) {
                authErrorFlow.value = formatAuthErrorMessage(e)
            } finally {
                isAuthLoadingFlow.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            isAuthLoadingFlow.value = false
            authErrorFlow.value = null
            repository.signOut()
            bannerMessageFlow.value = "Signed out of Pulse"
        }
    }

    fun continueAsExistingUser() {
        authErrorFlow.value = null
        repository.continueAsExistingUser()
        refreshFromSupabase(forceRefresh = false)
    }

    fun clearAuthError() {
        authErrorFlow.value = null
    }

    fun refreshFromSupabase(forceRefresh: Boolean = true) {
        if (isRefreshingFlow.value) return
        viewModelScope.launch {
            isRefreshingFlow.value = true
            try {
                repository.refreshFromSupabase(forceRefresh = forceRefresh)
            } finally {
                isRefreshingFlow.value = false
            }
        }
    }

    fun loadMorePosts() {
        if (isLoadingMoreFlow.value || isRefreshingFlow.value || !repository.hasMorePosts.value) {
            return
        }
        viewModelScope.launch {
            isLoadingMoreFlow.value = true
            try {
                repository.loadNextPostsPage()
            } finally {
                isLoadingMoreFlow.value = false
            }
        }
    }

    fun generateCaptionAndTagsWithOpenRouter(
        location: String,
        category: String,
        filmPreset: FilmPreset,
        currentDraft: String,
        onResult: (caption: String, tags: String) -> Unit
    ) {
        viewModelScope.launch {
            isGeneratingAiCaptionFlow.value = true
            val res = repository.generateCaptionAndTagsWithOpenRouter(
                location = location,
                category = category,
                filmPreset = filmPreset,
                notes = currentDraft
            )
            isGeneratingAiCaptionFlow.value = false
            res.fold(
                onSuccess = { (caption, tags) ->
                    onResult(caption, tags)
                    bannerMessageFlow.value = "AI caption & tags generated"
                },
                onFailure = { err ->
                    bannerMessageFlow.value = err.localizedMessage ?: "AI request failed"
                }
            )
        }
    }

    fun selectMoodboard(boardName: String) {
        selectedMoodboardFlow.value = boardName
    }

    fun createMoodboard(boardName: String) {
        val clean = boardName.trim()
        if (clean.isEmpty()) return
        repository.addMoodboard(clean)
        selectedMoodboardFlow.value = clean
        bannerMessageFlow.value = "Created collection '$clean'"
    }

    fun assignPostToMoodboard(post: PostEntity, boardName: String) {
        viewModelScope.launch {
            val clean = boardName.trim().ifBlank { "Favorites" }
            repository.assignPostToMoodboard(post, clean)
            bannerMessageFlow.value = "Saved to '$clean' collection"
        }
    }

    fun createHighlightFromPost(post: PostEntity, title: String) {
        viewModelScope.launch {
            repository.createHighlightFromPost(post, title)
            bannerMessageFlow.value = "Pinned to '$title' highlight"
        }
    }

    fun setFeedMode(mode: FeedMode) {
        feedModeFlow.value = mode
    }

    fun toggleTheme() {
        repository.toggleTheme()
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun selectExploreCategory(category: String) {
        selectedCategoryFlow.value = category
    }

    fun clearBannerMessage() {
        bannerMessageFlow.value = null
    }

    fun showBanner(message: String) {
        bannerMessageFlow.value = message
    }

    fun toggleLikePost(post: PostEntity, forceLike: Boolean = false) {
        viewModelScope.launch {
            repository.toggleLikePost(post, forceLike)
        }
    }

    fun toggleBookmarkPost(post: PostEntity) {
        viewModelScope.launch {
            val saving = !post.isBookmarked
            val activeCollection = selectedMoodboardFlow.value.let {
                if (it.isBlank() || it == "All Saved") "Favorites" else it
            }
            repository.toggleBookmarkPost(post, activeCollection)
            bannerMessageFlow.value = if (saving) {
                "Saved to '$activeCollection' collection"
            } else {
                "Removed from saved collection"
            }
        }
    }

    fun togglePinPostOnProfile(post: PostEntity) {
        viewModelScope.launch {
            val res = repository.togglePinPostOnProfile(post)
            res.fold(
                onSuccess = { isPinned ->
                    bannerMessageFlow.value = if (isPinned) {
                        "Pinned post to top of profile"
                    } else {
                        "Unpinned post from profile"
                    }
                },
                onFailure = { err ->
                    bannerMessageFlow.value = err.localizedMessage ?: "You can pin up to 3 posts"
                }
            )
        }
    }

    fun deletePost(post: PostEntity) {
        viewModelScope.launch {
            repository.deletePost(post.id)
            bannerMessageFlow.value = "Post deleted"
        }
    }

    fun addComment(
        post: PostEntity,
        text: String,
        parentCommentId: String? = null,
        replyToUsername: String? = null
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(
                post = post,
                text = text,
                parentCommentId = parentCommentId,
                replyToUsername = replyToUsername
            )
        }
    }

    fun toggleLikeComment(comment: CommentEntity) {
        viewModelScope.launch {
            repository.toggleLikeComment(comment)
        }
    }

    fun markStoryViewed(story: StoryEntity) {
        viewModelScope.launch {
            repository.markStoryViewed(story)
        }
    }

    suspend fun handleMediaSelectionAndUpload(
        selectedUri: android.net.Uri,
        isVideo: Boolean = false,
        bucketName: String? = null,
        filterPreset: FilmPreset = FilmPreset.NATURAL,
        adjustments: DarkroomAdjustments = DarkroomAdjustments()
    ): Result<String> {
        return repository.handleMediaSelectionAndUpload(
            selectedUri = selectedUri,
            isVideo = isVideo,
            bucketName = bucketName,
            filterPreset = filterPreset,
            adjustments = adjustments
        )
    }

    fun saveStudioDraft(
        draft: StudioDraftEntity,
        isManualSave: Boolean = true
    ) {
        viewModelScope.launch {
            repository.saveStudioDraft(draft)
            if (isManualSave) {
                bannerMessageFlow.value = "Draft saved locally"
            }
        }
    }

    fun deleteStudioDraft(
        draftId: String,
        showBanner: Boolean = true
    ) {
        viewModelScope.launch {
            repository.deleteStudioDraft(draftId)
            if (showBanner) {
                bannerMessageFlow.value = "Draft deleted"
            }
        }
    }

    fun publishFromStudio(
        target: StudioPublishTarget,
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
        tagsInput: String,
        audioTrack: String,
        cameraSpec: String,
        highlightTitle: String = "",
        activeDraftId: String? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            isUploadingMediaFlow.value = true
            try {
                when (target) {
                    StudioPublishTarget.STORY_24H -> {
                        repository.createStory(
                            imageRes = imageRes,
                            imageUri = imageUri,
                            videoUri = videoUri,
                            filterPreset = filterPreset,
                            captionOverlay = caption,
                            locationTag = location,
                            pinToHighlight = highlightTitle
                        )
                        bannerMessageFlow.value = "Published to your 24h story"
                    }
                    StudioPublishTarget.FEED_POST, StudioPublishTarget.LOOP_REEL -> {
                        val isMultiItemCarousel = com.example.data.model.splitCarouselUris(carouselImageUrisCsv).size > 1
                        val isLoop = target == StudioPublishTarget.LOOP_REEL ||
                            (!videoUri.isNullOrBlank() && !isMultiItemCarousel)
                        repository.createPost(
                            imageRes = imageRes,
                            imageUri = imageUri,
                            videoUri = videoUri,
                            carouselImageUrisCsv = carouselImageUrisCsv,
                            carouselImageResCsv = carouselImageResCsv,
                            aspectRatio = aspectRatio,
                            filterPreset = filterPreset,
                            adjustments = adjustments,
                            caption = caption,
                            location = location,
                            category = category,
                            tagsCsv = tagsInput,
                            isLoop = isLoop,
                            audioTrack = audioTrack,
                            cameraSpec = cameraSpec
                        )
                        bannerMessageFlow.value = if (isLoop) "Published new Loop" else "Published new post"
                    }
                }
                if (!activeDraftId.isNullOrBlank()) {
                    repository.deleteStudioDraft(activeDraftId)
                }
                repository.deleteStudioDraft(STUDIO_AUTOSAVE_DRAFT_ID)
                onComplete()
            } finally {
                isUploadingMediaFlow.value = false
            }
        }
    }

    fun toggleFollowCreator(creator: CreatorProfileEntity) {
        viewModelScope.launch {
            val nowFollowing = !creator.isFollowing
            repository.toggleFollowCreator(creator)
            bannerMessageFlow.value = if (nowFollowing) {
                "Following @${creator.username}"
            } else {
                "Unfollowed @${creator.username}"
            }
        }
    }

    fun reportPost(post: PostEntity, reason: String) {
        hiddenPostIdsFlow.value = hiddenPostIdsFlow.value + post.id
        bannerMessageFlow.value = "Reported post ($reason)"
    }

    fun blockCreatorById(authorId: String, authorUsername: String) {
        viewModelScope.launch {
            val creator = uiState.value.creators.find { it.id == authorId }
            if (creator != null) {
                repository.blockCreator(creator)
            }
            bannerMessageFlow.value = "Blocked @$authorUsername"
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
        repository.updateUserProfile(
            displayName = displayName,
            username = username,
            roleTag = roleTag,
            bio = bio,
            website = website,
            location = location,
            avatarUrl = avatarUrl
        )
        viewModelScope.launch {
            repository.syncUserProfileToSupabase()
        }
        bannerMessageFlow.value = "Profile updated"
    }

    fun sendDirectMessage(
        participantId: String,
        participantUsername: String,
        participantDisplayName: String,
        participantAvatarRes: Int,
        text: String,
        sharedPost: PostEntity? = null,
        attachedPhotoUri: String? = null,
        voiceNoteDurationSec: Int = 0
    ) {
        if (text.isBlank() && sharedPost == null && attachedPhotoUri.isNullOrBlank() && voiceNoteDurationSec <= 0) return
        viewModelScope.launch {
            val resolvedText = when {
                text.isNotBlank() -> text
                voiceNoteDurationSec > 0 -> "Voice note (0:${voiceNoteDurationSec.toString().padStart(2, '0')})"
                !attachedPhotoUri.isNullOrBlank() -> "Sent a studio photo"
                else -> "Shared a post from @${sharedPost?.authorUsername}"
            }
            repository.sendDirectMessage(
                participantId = participantId,
                participantUsername = participantUsername,
                participantDisplayName = participantDisplayName,
                participantAvatarRes = participantAvatarRes,
                text = resolvedText,
                sharedPost = sharedPost,
                attachedPhotoUri = attachedPhotoUri,
                voiceNoteDurationSec = voiceNoteDurationSec
            )
            if (sharedPost != null) {
                bannerMessageFlow.value = "Shared post with @$participantUsername"
            }
        }
    }

    fun toggleLikeMessage(message: DirectMessageEntity) {
        viewModelScope.launch {
            repository.toggleLikeMessage(message)
        }
    }

    fun markThreadRead(threadId: String) {
        viewModelScope.launch {
            repository.markThreadRead(threadId)
        }
    }

    fun markAllActivitiesRead() {
        viewModelScope.launch {
            repository.markAllActivitiesRead()
        }
    }

    fun pinArchivedStoriesToHighlight(stories: List<StoryEntity>, highlightTitle: String) {
        if (stories.isEmpty()) return
        val resolvedTitle = highlightTitle.trim().ifBlank { "Highlights" }
        viewModelScope.launch {
            repository.pinArchivedStoriesToHighlight(stories, resolvedTitle)
            bannerMessageFlow.value = "Pinned ${stories.size} ${if (stories.size == 1) "story" else "stories"} to '$resolvedTitle'"
        }
    }

    fun unpinStoryHighlight(story: StoryEntity) {
        viewModelScope.launch {
            repository.unpinStoryHighlight(story)
            bannerMessageFlow.value = "Removed story from Highlights"
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = PulseDatabase.getInstance(context.applicationContext)
            val repo = PulseRepository(db.pulseDao(), context.applicationContext)
            return PulseViewModel(repo) as T
        }
    }
}
