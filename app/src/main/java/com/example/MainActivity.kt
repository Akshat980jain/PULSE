package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PostEntity
import com.example.data.model.StudioPublishTarget
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.PostOptionsBottomSheet
import com.example.ui.components.SharePostBottomSheet
import com.example.ui.components.TagLocationHubBottomSheet
import com.example.ui.screens.ActivityNotificationsSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateStudioScreen
import com.example.ui.screens.CreatorUserProfileDialog
import com.example.ui.screens.DirectMessagesDialog
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.FirebaseMessagesTabScreen
import com.example.ui.screens.LoopsScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.WelcomeChoiceScreen
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.PulseTheme
import com.example.ui.viewmodel.PulseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthGateStep {
    CHOICE_PAGE,
    LOGIN_PAGE,
    MAIN_APP
}

private const val PAGE_CREATE_STUDIO = 0

enum class PulseDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val pageIndex: Int
) {
    FEED("feed", "Feed", Icons.Filled.Home, Icons.Outlined.Home, 1),
    LOOPS("loops", "Loops", Icons.Filled.PlayCircleFilled, Icons.Outlined.PlayCircleOutline, 2),
    MESSAGES("messages", "Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, 3),
    EXPLORE("explore", "Explore", Icons.Filled.Explore, Icons.Outlined.Explore, 4),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person, 5)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: PulseViewModel = viewModel(factory = PulseViewModel.Factory(context))
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PulseTheme(darkTheme = uiState.isDarkTheme) {
                PulseAppShell(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PulseAppShell(viewModel: PulseViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 6-page HorizontalPager:
    // Page 0 = Create Studio (left of Feed, Instagram-style camera swipe)
    // Page 1 = Feed
    // Page 2 = Loops
    // Page 3 = Messages (middle bottom bar icon)
    // Page 4 = Explore
    // Page 5 = Profile
    val pagerState = rememberPagerState(
        initialPage = PulseDestination.FEED.pageIndex,
        pageCount = { 6 }
    )

    var studioTarget by remember { mutableStateOf(StudioPublishTarget.FEED_POST) }
    var studioInitialAudioTrack by remember { mutableStateOf("") }
    var authGateStep by remember {
        mutableStateOf(
            if (uiState.session != null) AuthGateStep.MAIN_APP else AuthGateStep.CHOICE_PAGE
        )
    }
    var initialSignUpMode by remember { mutableStateOf(false) }

    var activeCommentsPostId by remember { mutableStateOf<String?>(null) }
    var activeSharePost by remember { mutableStateOf<PostEntity?>(null) }
    var activeOptionsPost by remember { mutableStateOf<PostEntity?>(null) }
    var activeCreatorId by remember { mutableStateOf<String?>(null) }
    var activeHubQuery by remember { mutableStateOf<String?>(null) }
    var showActivitySheet by remember { mutableStateOf(false) }
    var showDmDialog by remember { mutableStateOf(false) }
    var dmTargetParticipantId by remember { mutableStateOf<String?>(null) }
    var dmTargetUsername by remember { mutableStateOf<String?>(null) }
    var firebaseUnreadDmCount by remember { mutableIntStateOf(0) }

    val navigateToPage: (Int) -> Unit = { targetPage ->
        coroutineScope.launch {
            pagerState.scrollToPage(targetPage.coerceIn(0, 5))
        }
    }

    // Automatically enter main app when a Supabase session becomes active
    LaunchedEffect(uiState.session) {
        if (uiState.session != null) {
            authGateStep = AuthGateStep.MAIN_APP
        }
    }

    // Custom state-switching back handler per Android navigation guidelines
    BackHandler(
        enabled = authGateStep == AuthGateStep.LOGIN_PAGE ||
            (authGateStep == AuthGateStep.MAIN_APP && pagerState.currentPage != PulseDestination.FEED.pageIndex)
    ) {
        if (authGateStep == AuthGateStep.LOGIN_PAGE) {
            viewModel.clearAuthError()
            authGateStep = AuthGateStep.CHOICE_PAGE
        } else {
            navigateToPage(PulseDestination.FEED.pageIndex)
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(2800)
            viewModel.clearBannerMessage()
        }
    }

    if (authGateStep == AuthGateStep.CHOICE_PAGE && uiState.session == null) {
        WelcomeChoiceScreen(
            onExistingUserClick = {
                viewModel.continueAsExistingUser()
                authGateStep = AuthGateStep.MAIN_APP
            },
            onNewUserClick = {
                viewModel.clearAuthError()
                initialSignUpMode = true
                authGateStep = AuthGateStep.LOGIN_PAGE
            },
            onContinueAsGuest = {
                authGateStep = AuthGateStep.MAIN_APP
            }
        )
        return
    }

    if (authGateStep == AuthGateStep.LOGIN_PAGE && uiState.session == null) {
        AuthScreen(
            isLoading = uiState.isAuthLoading,
            errorMessage = uiState.authErrorMessage,
            onSignIn = viewModel::signIn,
            onSignUp = viewModel::signUp,
            onDismissError = viewModel::clearAuthError,
            initialSignUpMode = initialSignUpMode,
            onBackToChoice = {
                viewModel.clearAuthError()
                authGateStep = AuthGateStep.CHOICE_PAGE
            },
            onCancel = {
                viewModel.clearAuthError()
                authGateStep = AuthGateStep.MAIN_APP
            }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (pagerState.currentPage != PAGE_CREATE_STUDIO) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    PulseDestination.entries.forEach { dest ->
                        val selected = pagerState.currentPage == dest.pageIndex
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navigateToPage(dest.pageIndex)
                            },
                            icon = {
                                if (dest == PulseDestination.MESSAGES && firebaseUnreadDmCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = CobaltPulse,
                                                contentColor = Color.White
                                            ) {
                                                Text(firebaseUnreadDmCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                }
                            },
                            label = null,
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = CobaltPulse,
                                indicatorColor = CobaltPulse,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${dest.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_horizontal_pager")
            ) { page ->
                when (page) {
                    PAGE_CREATE_STUDIO -> {
                        CreateStudioScreen(
                            initialTarget = studioTarget,
                            initialAudioTrack = studioInitialAudioTrack,
                            savedDrafts = uiState.studioDrafts,
                            onSaveDraft = viewModel::saveStudioDraft,
                            onDeleteDraft = { draftId ->
                                viewModel.deleteStudioDraft(draftId, showBanner = true)
                            },
                            onDraftPublished = { draftId ->
                                if (!draftId.isNullOrBlank()) {
                                    viewModel.deleteStudioDraft(draftId, showBanner = false)
                                }
                                viewModel.deleteStudioDraft(
                                    com.example.data.model.STUDIO_AUTOSAVE_DRAFT_ID,
                                    showBanner = false
                                )
                                studioInitialAudioTrack = ""
                            },
                            isGeneratingAiCaption = uiState.isGeneratingAiCaption,
                            isUploadingMedia = uiState.isUploadingMedia,
                            onGenerateAiCaptionAndTags = viewModel::generateCaptionAndTagsWithOpenRouter,
                            onBackToFeed = {
                                navigateToPage(PulseDestination.FEED.pageIndex)
                            },
                            onPublish = { target, imageRes, imageUri, videoUri, carouselUrisCsv, carouselResCsv, aspectRatio, filterPreset, adjustments, caption, location, category, tags, audioTrack, cameraSpec, highlightTitle ->
                                viewModel.publishFromStudio(
                                    target = target,
                                    imageRes = imageRes,
                                    imageUri = imageUri,
                                    videoUri = videoUri,
                                    carouselImageUrisCsv = carouselUrisCsv,
                                    carouselImageResCsv = carouselResCsv,
                                    aspectRatio = aspectRatio,
                                    filterPreset = filterPreset,
                                    adjustments = adjustments,
                                    caption = caption,
                                    location = location,
                                    category = category,
                                    tagsInput = tags,
                                    audioTrack = audioTrack,
                                    cameraSpec = cameraSpec,
                                    highlightTitle = highlightTitle,
                                    onComplete = {
                                        val nextPage = when (target) {
                                            StudioPublishTarget.LOOP_REEL -> PulseDestination.LOOPS.pageIndex
                                            else -> PulseDestination.FEED.pageIndex
                                        }
                                        navigateToPage(nextPage)
                                    }
                                )
                            }
                        )
                    }

                    PulseDestination.FEED.pageIndex -> {
                        FeedScreen(
                            userProfile = uiState.userProfile,
                            isDarkTheme = uiState.isDarkTheme,
                            isRefreshing = uiState.isRefreshingFeed,
                            isLoadingMore = uiState.isLoadingMorePosts,
                            hasMorePosts = uiState.hasMorePosts,
                            feedMode = uiState.feedMode,
                            posts = uiState.feedPosts,
                            stories = uiState.stories,
                            unreadActivityCount = uiState.unreadActivityCount,
                            unreadDmCount = firebaseUnreadDmCount,
                            moodboards = uiState.moodboards,
                            selectedMoodboard = uiState.selectedMoodboard,
                            onSelectMoodboard = viewModel::selectMoodboard,
                            onCreateMoodboard = viewModel::createMoodboard,
                            onAssignPostToMoodboard = viewModel::assignPostToMoodboard,
                            onFeedModeChange = viewModel::setFeedMode,
                            onRefreshFeed = viewModel::refreshFromSupabase,
                            onLoadMorePosts = viewModel::loadMorePosts,
                            onToggleTheme = viewModel::toggleTheme,
                            onOpenActivitySheet = {
                                viewModel.markAllActivitiesRead()
                                showActivitySheet = true
                            },
                            onOpenDmInbox = {
                                dmTargetParticipantId = null
                                dmTargetUsername = null
                                navigateToPage(PulseDestination.MESSAGES.pageIndex)
                            },
                            onCreateStoryClick = {
                                studioTarget = StudioPublishTarget.STORY_24H
                                navigateToPage(PAGE_CREATE_STUDIO)
                            },
                            onCreatePostClick = {
                                studioTarget = StudioPublishTarget.FEED_POST
                                navigateToPage(PAGE_CREATE_STUDIO)
                            },
                            onStoryViewed = viewModel::markStoryViewed,
                            onSendStoryReply = { story, replyText ->
                                viewModel.sendDirectMessage(
                                    participantId = story.authorId,
                                    participantUsername = story.authorUsername,
                                    participantDisplayName = story.authorDisplayName,
                                    participantAvatarRes = story.authorAvatarRes,
                                    text = replyText
                                )
                                viewModel.showBanner("Sent reply to @${story.authorUsername}")
                            },
                            onLikeToggle = { viewModel.toggleLikePost(it) },
                            onDoubleTapLike = { viewModel.toggleLikePost(it, forceLike = true) },
                            onBookmarkToggle = viewModel::toggleBookmarkPost,
                            onOpenComments = { activeCommentsPostId = it.id },
                            commentsByPostId = uiState.commentsByPostId,
                            onAddInlineComment = { post, text ->
                                viewModel.addComment(post = post, text = text)
                            },
                            onSharePost = { activeSharePost = it },
                            onCreatorClick = { authorId ->
                                if (authorId == uiState.userProfile.id && authorId.isNotBlank()) {
                                    navigateToPage(PulseDestination.PROFILE.pageIndex)
                                } else {
                                    activeCreatorId = authorId
                                }
                            },
                            onMoreOptions = { activeOptionsPost = it },
                            onTagClick = { tag -> activeHubQuery = tag },
                            onLocationClick = { loc -> activeHubQuery = loc },
                            isFeedActive = pagerState.currentPage == PulseDestination.FEED.pageIndex
                        )
                    }

                    PulseDestination.LOOPS.pageIndex -> {
                        LoopsScreen(
                            loopPosts = uiState.loopPosts,
                            creators = uiState.creators,
                            onLikeToggle = { viewModel.toggleLikePost(it) },
                            onDoubleTapLike = { viewModel.toggleLikePost(it, forceLike = true) },
                            onBookmarkToggle = viewModel::toggleBookmarkPost,
                            onOpenComments = { /* Handled in-place by LoopsCommentsOverlayDrawer */ },
                            onSharePost = { activeSharePost = it },
                            onCreatorClick = { authorId ->
                                if (authorId == uiState.userProfile.id && authorId.isNotBlank()) {
                                    navigateToPage(PulseDestination.PROFILE.pageIndex)
                                } else {
                                    activeCreatorId = authorId
                                }
                            },
                            onMoreOptions = { activeOptionsPost = it },
                            onCreateLoopClick = {
                                studioInitialAudioTrack = ""
                                studioTarget = StudioPublishTarget.LOOP_REEL
                                navigateToPage(PAGE_CREATE_STUDIO)
                            },
                            onUseAudioClick = { selectedAudioTrack ->
                                studioTarget = StudioPublishTarget.LOOP_REEL
                                studioInitialAudioTrack = selectedAudioTrack
                                navigateToPage(PAGE_CREATE_STUDIO)
                            },
                            onFollowToggle = viewModel::toggleFollowCreator,
                            commentsByPostId = uiState.commentsByPostId,
                            onAddComment = { post, text, parentId, replyToUser ->
                                viewModel.addComment(
                                    post = post,
                                    text = text,
                                    parentCommentId = parentId,
                                    replyToUsername = replyToUser
                                )
                            },
                            onToggleLikeComment = viewModel::toggleLikeComment,
                            isLoopsActive = pagerState.currentPage == PulseDestination.LOOPS.pageIndex
                        )
                    }

                    PulseDestination.MESSAGES.pageIndex -> {
                        FirebaseMessagesTabScreen(
                            supabaseProfile = uiState.userProfile,
                            initialTargetSupabaseId = dmTargetParticipantId,
                            initialTargetUsername = dmTargetUsername,
                            onUnreadCountChanged = { count -> firebaseUnreadDmCount = count }
                        )
                    }

                    PulseDestination.EXPLORE.pageIndex -> {
                        ExploreScreen(
                            searchQuery = uiState.searchQuery,
                            selectedCategory = uiState.selectedExploreCategory,
                            posts = uiState.explorePosts,
                            creators = uiState.exploreCreators,
                            isLoading = uiState.isRefreshingFeed,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectExploreCategory,
                            onToggleFollowCreator = viewModel::toggleFollowCreator,
                            onCreatorClick = { activeCreatorId = it },
                            onLikeToggle = { viewModel.toggleLikePost(it) },
                            onBookmarkToggle = viewModel::toggleBookmarkPost,
                            onOpenComments = { activeCommentsPostId = it.id },
                            onSharePost = { activeSharePost = it },
                            onMoreOptions = { activeOptionsPost = it },
                            onOpenHub = { hub -> activeHubQuery = hub }
                        )
                    }

                    PulseDestination.PROFILE.pageIndex -> {
                        val myActiveStories = uiState.stories.filter { story ->
                            (uiState.userProfile.id.isNotBlank() && story.authorId.equals(uiState.userProfile.id, ignoreCase = true)) ||
                                (uiState.userProfile.username.isNotBlank() && story.authorUsername.equals(uiState.userProfile.username, ignoreCase = true))
                        }
                        UserProfileScreen(
                            userProfile = uiState.userProfile,
                            isAuthenticated = uiState.session != null,
                            isOwnProfile = true,
                            isDarkTheme = uiState.isDarkTheme,
                            isLoading = uiState.isRefreshingFeed,
                            userPosts = uiState.myPosts,
                            bookmarkedPosts = uiState.bookmarkedPosts,
                            moodboards = uiState.moodboards,
                            selectedMoodboard = uiState.selectedMoodboard,
                            highlights = uiState.profileHighlights,
                            creators = uiState.creators,
                            archivedStories = uiState.archivedStories,
                            activeUserStories = myActiveStories,
                            onToggleTheme = viewModel::toggleTheme,
                            onSelectMoodboard = viewModel::selectMoodboard,
                            onCreateMoodboard = viewModel::createMoodboard,
                            onUpdateProfile = viewModel::updateUserProfile,
                            onLogout = {
                                viewModel.signOut()
                                authGateStep = AuthGateStep.LOGIN_PAGE
                            },
                            onCreateStoryClick = {
                                studioTarget = StudioPublishTarget.STORY_24H
                                navigateToPage(PAGE_CREATE_STUDIO)
                            },
                            onCreatePostClick = {
                                studioTarget = StudioPublishTarget.FEED_POST
                                navigateToPage(PAGE_CREATE_STUDIO)
                            },
                            onStoryViewed = viewModel::markStoryViewed,
                            onSendStoryReply = { story, replyText ->
                                viewModel.sendDirectMessage(
                                    participantId = story.authorId,
                                    participantUsername = story.authorUsername,
                                    participantDisplayName = story.authorDisplayName,
                                    participantAvatarRes = story.authorAvatarRes,
                                    text = replyText
                                )
                            },
                            onPinStoriesToHighlight = viewModel::pinArchivedStoriesToHighlight,
                            onUnpinStoryHighlight = viewModel::unpinStoryHighlight,
                            onToggleFollowCreator = viewModel::toggleFollowCreator,
                            onLikeToggle = { viewModel.toggleLikePost(it) },
                            onBookmarkToggle = viewModel::toggleBookmarkPost,
                            onOpenComments = { activeCommentsPostId = it.id },
                            onSharePost = { activeSharePost = it },
                            onMoreOptions = { activeOptionsPost = it },
                            onTogglePinPost = viewModel::togglePinPostOnProfile,
                            onDeletePost = viewModel::deletePost
                        )
                    }
                }
            }

            // Floating Status Toast Banner
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Surface(
                    color = CobaltPulse,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = uiState.statusBannerMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // Trending Hashtag & Location Hub Sheet
    val currentHubQuery = activeHubQuery
    if (!currentHubQuery.isNullOrBlank()) {
        TagLocationHubBottomSheet(
            hubQuery = currentHubQuery,
            allPosts = uiState.posts,
            onDismiss = { activeHubQuery = null },
            onOpenInExplore = { query ->
                activeHubQuery = null
                viewModel.updateSearchQuery(query)
                navigateToPage(PulseDestination.EXPLORE.pageIndex)
            },
            onSelectPost = { post ->
                activeCommentsPostId = post.id
            }
        )
    }

    // Comments Sheet
    val commentPost = uiState.posts.find { it.id == activeCommentsPostId }
    if (commentPost != null) {
        val postComments = uiState.commentsByPostId[commentPost.id].orEmpty()
        CommentsBottomSheet(
            post = commentPost,
            comments = postComments,
            onDismiss = { activeCommentsPostId = null },
            onAddComment = { text, parentId, replyToUser ->
                viewModel.addComment(
                    post = commentPost,
                    text = text,
                    parentCommentId = parentId,
                    replyToUsername = replyToUser
                )
            },
            onToggleLikeComment = viewModel::toggleLikeComment
        )
    }

    // Share Post to Direct Message Sheet
    val sharePost = activeSharePost
    if (sharePost != null) {
        SharePostBottomSheet(
            post = sharePost,
            creators = uiState.creators,
            onDismiss = { activeSharePost = null },
            onSendToCreator = { creator, note ->
                viewModel.sendDirectMessage(
                    participantId = creator.id,
                    participantUsername = creator.username,
                    participantDisplayName = creator.displayName,
                    participantAvatarRes = creator.avatarRes,
                    text = note,
                    sharedPost = sharePost
                )
            }
        )
    }

    // Post More / Moodboard / Story Highlight / Pin / Safety Options Sheet
    val optionsPost = activeOptionsPost
    if (optionsPost != null) {
        val isOwnPost = pagerState.currentPage == PulseDestination.PROFILE.pageIndex ||
            uiState.myPosts.any { it.id == optionsPost.id } ||
            (uiState.userProfile.id.isNotBlank() && optionsPost.authorId.equals(uiState.userProfile.id, ignoreCase = true)) ||
            (uiState.userProfile.username.isNotBlank() && optionsPost.authorUsername.equals(uiState.userProfile.username, ignoreCase = true))
        PostOptionsBottomSheet(
            post = optionsPost,
            isOwnPost = isOwnPost,
            moodboards = uiState.moodboards,
            onDismiss = { activeOptionsPost = null },
            onSaveToMoodboard = { board -> viewModel.assignPostToMoodboard(optionsPost, board) },
            onPinToHighlight = { title -> viewModel.createHighlightFromPost(optionsPost, title) },
            onTogglePinOnProfile = { viewModel.togglePinPostOnProfile(optionsPost) },
            onDeleteOwnPost = { viewModel.deletePost(optionsPost) },
            onReportPost = { reason -> viewModel.reportPost(optionsPost, reason) },
            onBlockCreator = {
                viewModel.blockCreatorById(optionsPost.authorId, optionsPost.authorUsername)
            }
        )
    }

    // Full-Screen UserProfile Dialog for Inspecting Any Creator
    val activeCreator = uiState.creators.find { it.id == activeCreatorId }
    if (activeCreator != null) {
        val creatorPosts = uiState.posts.filter { it.authorId == activeCreator.id }
        val creatorStories = uiState.stories.filter { it.authorId == activeCreator.id }
        CreatorUserProfileDialog(
            creator = activeCreator,
            creatorPosts = creatorPosts,
            creatorStories = creatorStories,
            onDismiss = { activeCreatorId = null },
            onToggleFollow = { viewModel.toggleFollowCreator(activeCreator) },
            onMessageCreator = {
                activeCreatorId = null
                dmTargetParticipantId = activeCreator.id
                dmTargetUsername = activeCreator.username
                navigateToPage(PulseDestination.MESSAGES.pageIndex)
            },
            onBlockCreator = {
                viewModel.blockCreatorById(activeCreator.id, activeCreator.username)
            },
            onStoryViewed = viewModel::markStoryViewed,
            onSendStoryReply = { story, replyText ->
                viewModel.sendDirectMessage(
                    participantId = story.authorId,
                    participantUsername = story.authorUsername,
                    participantDisplayName = story.authorDisplayName,
                    participantAvatarRes = story.authorAvatarRes,
                    text = replyText
                )
                viewModel.showBanner("Sent reply to @${story.authorUsername}")
            },
            onLikeToggle = { viewModel.toggleLikePost(it) },
            onBookmarkToggle = viewModel::toggleBookmarkPost,
            onOpenComments = { activeCommentsPostId = it.id },
            onSharePost = { activeSharePost = it },
            onMoreOptions = { activeOptionsPost = it }
        )
    }

    // Activity Notifications Sheet
    if (showActivitySheet) {
        ActivityNotificationsSheet(
            activities = uiState.activities,
            onDismiss = { showActivitySheet = false },
            onCreatorClick = { creatorId -> activeCreatorId = creatorId }
        )
    }

    // Direct Messages Dialog (fallback modal if invoked directly)
    if (showDmDialog) {
        DirectMessagesDialog(
            threads = uiState.threads,
            creators = uiState.creators,
            initialSelectedParticipantId = dmTargetParticipantId,
            onMarkThreadRead = viewModel::markThreadRead,
            onSendMessage = { id, username, displayName, avatarRes, text, attachedPhotoUri, voiceNoteDurationSec ->
                viewModel.sendDirectMessage(
                    participantId = id,
                    participantUsername = username,
                    participantDisplayName = displayName,
                    participantAvatarRes = avatarRes,
                    text = text,
                    attachedPhotoUri = attachedPhotoUri,
                    voiceNoteDurationSec = voiceNoteDurationSec
                )
            },
            onToggleLikeMessage = viewModel::toggleLikeMessage,
            onDismiss = {
                showDmDialog = false
                dmTargetParticipantId = null
            }
        )
    }
}
