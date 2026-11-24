package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CommentEntity
import com.example.data.model.FeedMode
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileState
import com.example.ui.components.EphemeralStoryViewer
import com.example.ui.components.FeedSkeletonLoader
import com.example.ui.components.PostCardSkeletonLoader
import com.example.ui.components.PostContactCard
import com.example.ui.components.StoriesAvatarTray
import com.example.ui.components.rememberPulseHaptics
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    userProfile: UserProfileState,
    isDarkTheme: Boolean,
    isRefreshing: Boolean,
    isLoadingMore: Boolean = false,
    hasMorePosts: Boolean = true,
    feedMode: FeedMode,
    posts: List<PostEntity>,
    stories: List<StoryEntity>,
    commentsByPostId: Map<String, List<CommentEntity>> = emptyMap(),
    unreadActivityCount: Int,
    unreadDmCount: Int,
    moodboards: List<String> = listOf("All Saved", "Favorites", "Visual Inspiration", "Architecture", "Monochrome"),
    selectedMoodboard: String = "All Saved",
    onSelectMoodboard: (String) -> Unit = {},
    onCreateMoodboard: (String) -> Unit = {},
    onAssignPostToMoodboard: (PostEntity, String) -> Unit = { _, _ -> },
    onFeedModeChange: (FeedMode) -> Unit,
    onRefreshFeed: () -> Unit,
    onLoadMorePosts: () -> Unit = {},
    onToggleTheme: () -> Unit,
    onOpenActivitySheet: () -> Unit,
    onOpenDmInbox: () -> Unit,
    onCreateStoryClick: () -> Unit,
    onCreatePostClick: () -> Unit = {},
    onStoryViewed: (StoryEntity) -> Unit,
    onSendStoryReply: (StoryEntity, String) -> Unit,
    onLikeToggle: (PostEntity) -> Unit,
    onDoubleTapLike: (PostEntity) -> Unit,
    onBookmarkToggle: (PostEntity) -> Unit,
    onOpenComments: (PostEntity) -> Unit,
    onAddInlineComment: (PostEntity, String) -> Unit = { _, _ -> },
    onSharePost: (PostEntity) -> Unit,
    onCreatorClick: (String) -> Unit,
    onMoreOptions: (PostEntity) -> Unit,
    onTagClick: (String) -> Unit = {},
    onLocationClick: (String) -> Unit = {},
    isFeedActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    var activeStoryIndex by remember { mutableStateOf<Int?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionDraft by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val haptics = rememberPulseHaptics()

    // Trigger infinite pagination when scrolling within 3 items of the end of the feed
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 2 && lastVisibleItemIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore, posts.size, hasMorePosts, isLoadingMore, isRefreshing, feedMode) {
        if (feedMode != FeedMode.SAVED && shouldLoadMore && posts.isNotEmpty() && hasMorePosts && !isLoadingMore && !isRefreshing) {
            onLoadMorePosts()
        }
    }

    var showFeedModeDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Studio Header Bar: Pulse brand + adjacent down-arrow dropdown toggle (no text/logo pill, no reload button)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "Pulse",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clickable {
                                haptics.performControlTapHaptic()
                                showFeedModeDropdown = !showFeedModeDropdown
                            }
                            .testTag("pulse_brand_header")
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    Icon(
                        imageVector = if (showFeedModeDropdown) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = "Select feed mode",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                haptics.performControlTapHaptic()
                                showFeedModeDropdown = !showFeedModeDropdown
                            }
                            .testTag("feed_mode_dropdown_button")
                    )
                }

                DropdownMenu(
                    expanded = showFeedModeDropdown,
                    onDismissRequest = { showFeedModeDropdown = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("feed_mode_dropdown_menu")
                ) {
                    listOf(FeedMode.CURATED, FeedMode.FOLLOWING).forEach { mode ->
                        val selected = feedMode == mode || (feedMode == FeedMode.SAVED && mode == FeedMode.CURATED)
                        val label = when (mode) {
                            FeedMode.CURATED -> "Curated"
                            FeedMode.FOLLOWING -> "Following"
                            else -> ""
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) CobaltPulse else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = CobaltPulse,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                haptics.performFilterTickHaptic()
                                onFeedModeChange(mode)
                                showFeedModeDropdown = false
                            },
                            modifier = Modifier.testTag("feed_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onOpenActivitySheet,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("activity_notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadActivityCount > 0) {
                            Badge(
                                containerColor = SignalCrimson,
                                contentColor = Color.White
                            ) {
                                Text(unreadActivityCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Activity notifications",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Dedicated Saved Collections Filter Bar when FeedMode.SAVED is active
        if (feedMode == FeedMode.SAVED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(vertical = 8.dp)
                    .testTag("feed_saved_collections_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CollectionsBookmark,
                            contentDescription = null,
                            tint = ShutterAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Saved Collection · $selectedMoodboard (${posts.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(
                        onClick = {
                            haptics.performBookmarkHaptic()
                            showCreateCollectionDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("feed_create_collection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreateNewFolder,
                            contentDescription = "New collection",
                            tint = CobaltPulse,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Collection",
                            style = MaterialTheme.typography.labelSmall,
                            color = CobaltPulse
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_saved_collections_row")
                ) {
                    items(moodboards, key = { it }) { board ->
                        val selected = board == selectedMoodboard
                        FilterChip(
                            selected = selected,
                            onClick = {
                                haptics.performBookmarkHaptic()
                                onSelectMoodboard(board)
                            },
                            label = { Text(board) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShutterAmber,
                                selectedLabelColor = Color(0xFF0B0E14)
                            ),
                            modifier = Modifier.testTag("feed_collection_chip_${board.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )

        // Material3 Swipe-to-Refresh Mechanism wrapping the Vertical LazyColumn Feed
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefreshFeed,
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = CobaltPulse,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("feed_swipe_refresh")
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("feed_lazy_column")
            ) {
                // Horizontal Scrolling Circular Avatar Stories Tray
                item(key = "stories_tray") {
                    StoriesAvatarTray(
                        userProfile = userProfile,
                        stories = stories,
                        isRefreshing = isRefreshing,
                        onOpenStoryAtIndex = { idx -> activeStoryIndex = idx },
                        onCreateStoryClick = onCreateStoryClick
                    )
                }

                if (posts.isEmpty()) {
                    if (isRefreshing) {
                        item(key = "feed_loading_skeleton") {
                            FeedSkeletonLoader()
                        }
                    } else {
                        item(key = "empty_feed_state") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(36.dp)
                            ) {
                                if (feedMode == FeedMode.SAVED) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        tint = ShutterAmber,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No bookmarked posts in '$selectedMoodboard'",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap the bookmark icon on any post in your Feed to save your favorite content to a dedicated collection.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            haptics.performFilterTickHaptic()
                                            onFeedModeChange(FeedMode.CURATED)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse)
                                    ) {
                                        Text("Browse Curated Feed")
                                    }
                                } else {
                                    Text(
                                        text = "No posts in this feed yet",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Pull down to refresh or share a new photo or video from the Camera tab.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = onRefreshFeed,
                                            colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Refresh feed")
                                        }
                                        OutlinedButton(onClick = onCreatePostClick) {
                                            Icon(
                                                imageVector = Icons.Filled.AddPhotoAlternate,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Create post")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val primaryVideoPostId = posts.firstOrNull { it.hasVideo }?.id
                    items(posts, key = { it.id }) { post ->
                        val postComments = commentsByPostId[post.id].orEmpty()
                        PostContactCard(
                            post = post,
                            onLikeToggle = { onLikeToggle(post) },
                            onDoubleTapLike = { onDoubleTapLike(post) },
                            onBookmarkToggle = { onBookmarkToggle(post) },
                            onOpenComments = { onOpenComments(post) },
                            onSharePost = { onSharePost(post) },
                            onCreatorClick = { onCreatorClick(post.authorId) },
                            onMoreOptions = { onMoreOptions(post) },
                            onTagClick = onTagClick,
                            onLocationClick = onLocationClick,
                            comments = postComments,
                            onAddComment = { commentText ->
                                onAddInlineComment(post, commentText)
                            },
                            moodboards = moodboards,
                            onAssignToCollection = { collectionName ->
                                onAssignPostToMoodboard(post, collectionName)
                            },
                            autoPlayVideo = isFeedActive && (post.id == primaryVideoPostId)
                        )
                    }

                    // Infinite Pagination Footer Item
                    if (feedMode != FeedMode.SAVED) {
                        item(key = "feed_pagination_footer") {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .testTag("feed_pagination_footer")
                            ) {
                                when {
                                    isLoadingMore -> {
                                        PostCardSkeletonLoader()
                                    }
                                    hasMorePosts -> {
                                        TextButton(
                                            onClick = onLoadMorePosts,
                                            modifier = Modifier.testTag("load_more_posts_button")
                                        ) {
                                            Text(
                                                text = "Load more posts",
                                                color = CobaltPulse
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = "You're all caught up",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            title = { Text("Create Saved Collection") },
            text = {
                OutlinedTextField(
                    value = newCollectionDraft,
                    onValueChange = { newCollectionDraft = it },
                    label = { Text("Collection name") },
                    placeholder = { Text("e.g., Street Favorites") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_new_collection_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionDraft.isNotBlank()) {
                            haptics.performBookmarkHaptic()
                            onCreateMoodboard(newCollectionDraft.trim())
                            newCollectionDraft = ""
                            showCreateCollectionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                    modifier = Modifier.testTag("feed_confirm_create_collection_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val currentStoryIndex = activeStoryIndex
    if (currentStoryIndex != null && stories.isNotEmpty()) {
        val safeIndex = currentStoryIndex.coerceIn(0, stories.lastIndex)
        EphemeralStoryViewer(
            stories = stories,
            initialIndex = safeIndex,
            onStorySeen = onStoryViewed,
            onSendReply = { story, replyText ->
                onSendStoryReply(story, replyText)
            },
            onAddNewStory = {
                activeStoryIndex = null
                onCreateStoryClick()
            },
            onCreatorClick = onCreatorClick,
            onDismiss = { activeStoryIndex = null }
        )
    }
}

@Composable
fun StoryViewerDialog(
    stories: List<StoryEntity>,
    initialIndex: Int,
    onStorySeen: (StoryEntity) -> Unit,
    onSendReply: (StoryEntity, String) -> Unit,
    onAddNewStory: () -> Unit,
    onDismiss: () -> Unit,
    onToggleHighlight: ((StoryEntity) -> Unit)? = null,
    storyDurationMs: Long = 5000L
) {
    EphemeralStoryViewer(
        stories = stories,
        initialIndex = initialIndex,
        onStorySeen = onStorySeen,
        onSendReply = onSendReply,
        onAddNewStory = onAddNewStory,
        onDismiss = onDismiss,
        onToggleHighlight = onToggleHighlight,
        imageDurationMs = storyDurationMs
    )
}


