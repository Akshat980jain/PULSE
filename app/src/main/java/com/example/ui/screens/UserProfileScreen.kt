package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.StoryHighlightGroup
import com.example.data.model.UserProfileState
import com.example.data.service.DarkroomProcessingService
import com.example.ui.components.CreatorInsightsBottomSheet
import com.example.ui.components.EphemeralStoryViewer
import com.example.ui.components.MoodboardCollageDialog
import com.example.ui.components.PostContactCard
import com.example.ui.components.ProfileStatColumn
import com.example.ui.components.PulseAvatarImage
import com.example.ui.components.PulseFilteredImage
import com.example.ui.components.PulseSkeletonBox
import com.example.ui.components.StoryAvatarRing
import com.example.ui.components.formatMetricCount
import com.example.ui.components.rememberPulseHaptics
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson

/**
 * Full-featured UserProfile screen component powered by a unified [LazyVerticalGrid].
 * Displays the user's circular avatar, bio, follower/following/posts statistics,
 * pinned Story Highlights, and a responsive 3-column lazy grid of posted photos & videos.
 *
 * Supports both the authenticated user's own profile (`isOwnProfile = true`) and
 * inspecting any other creator's profile (`isOwnProfile = false`).
 */
@Composable
fun UserProfileScreen(
    userProfile: UserProfileState,
    isAuthenticated: Boolean = true,
    isOwnProfile: Boolean = true,
    isFollowingUser: Boolean = false,
    isDarkTheme: Boolean = true,
    isLoading: Boolean = false,
    userPosts: List<PostEntity>,
    bookmarkedPosts: List<PostEntity> = emptyList(),
    moodboards: List<String> = emptyList(),
    selectedMoodboard: String = "All Saved",
    highlights: List<StoryHighlightGroup> = emptyList(),
    creators: List<CreatorProfileEntity> = emptyList(),
    archivedStories: List<StoryEntity> = emptyList(),
    activeUserStories: List<StoryEntity> = emptyList(),
    onBackClick: (() -> Unit)? = null,
    onToggleTheme: () -> Unit = {},
    onSelectMoodboard: (String) -> Unit = {},
    onCreateMoodboard: (String) -> Unit = {},
    onUpdateProfile: (String, String, String, String, String, String, String?) -> Unit = { _, _, _, _, _, _, _ -> },
    onLogout: () -> Unit = {},
    onToggleFollowThisUser: () -> Unit = {},
    onMessageThisUser: () -> Unit = {},
    onBlockThisUser: () -> Unit = {},
    onCreateStoryClick: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onStoryViewed: (StoryEntity) -> Unit = {},
    onSendStoryReply: (StoryEntity, String) -> Unit = { _, _ -> },
    onPinStoriesToHighlight: (List<StoryEntity>, String) -> Unit = { _, _ -> },
    onUnpinStoryHighlight: (StoryEntity) -> Unit = {},
    onToggleFollowCreator: (CreatorProfileEntity) -> Unit = {},
    onLikeToggle: (PostEntity) -> Unit = {},
    onBookmarkToggle: (PostEntity) -> Unit = {},
    onOpenComments: (PostEntity) -> Unit = {},
    onSharePost: (PostEntity) -> Unit = {},
    onMoreOptions: (PostEntity) -> Unit = {},
    onTogglePinPost: (PostEntity) -> Unit = {},
    onDeletePost: (PostEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberPulseHaptics()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isCardFeedLayout by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showFollowersSheet by remember { mutableStateOf(false) }
    var showCreateMoodboardDialog by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var showMoodboardCollageDialog by remember { mutableStateOf(false) }
    var showArchiveHighlightPicker by remember { mutableStateOf(false) }
    var showActiveStoriesViewer by remember { mutableStateOf(false) }
    var initialHighlightTitleDraft by remember { mutableStateOf("") }
    var inspectedPostId by remember { mutableStateOf<String?>(null) }
    var activeHighlightGroup by remember { mutableStateOf<StoryHighlightGroup?>(null) }

    val userLoopPosts = remember(userPosts) { userPosts.filter { it.isLoop || it.hasVideo } }
    val followingCount = remember(creators, userProfile.followingCount, isOwnProfile) {
        if (isOwnProfile) {
            val localCount = creators.count { it.isFollowing }
            if (localCount > 0) localCount else userProfile.followingCount
        } else {
            userProfile.followingCount
        }
    }

    val tabTitles = remember(isOwnProfile, userPosts.size, userLoopPosts.size, bookmarkedPosts.size) {
        if (isOwnProfile) {
            listOf(
                "Posts (${userPosts.size})",
                "Loops (${userLoopPosts.size})",
                "Saved (${bookmarkedPosts.size})"
            )
        } else {
            listOf(
                "Posts (${userPosts.size})",
                "Loops (${userLoopPosts.size})"
            )
        }
    }

    val safeTabIndex = selectedTabIndex.coerceIn(0, tabTitles.lastIndex)
    val activeGridPosts = when (safeTabIndex) {
        0 -> userPosts
        1 -> userLoopPosts
        else -> bookmarkedPosts
    }

    val resolvedDisplayName = userProfile.displayName.ifBlank { "Pulse Creator" }
    val resolvedUsername = userProfile.username.ifBlank { "creator" }
    val hasActiveStories = activeUserStories.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(if (isOwnProfile) "profile_screen" else "user_profile_screen_${userProfile.id}")
    ) {
        // Top Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("user_profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "@$resolvedUsername",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Verified studio",
                    tint = CobaltPulse,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isOwnProfile) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Switch studio theme",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("profile_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log out",
                            tint = SignalCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Log Out",
                            style = MaterialTheme.typography.labelMedium,
                            color = SignalCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                TextButton(
                    onClick = onBlockThisUser,
                    modifier = Modifier.testTag("user_profile_block_button")
                ) {
                    Text(
                        text = "Block",
                        style = MaterialTheme.typography.labelMedium,
                        color = SignalCrimson,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        // Unified Scrollable LazyVerticalGrid: Header + Bio + Stats + Highlights + Tabs + 3-Column Photo Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("profile_posts_grid")
        ) {
            // 1. Profile Header: Avatar, Follower/Following Stats, Bio, Actions, Story Highlights
            item(
                key = "profile_header_section",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Circular Profile Avatar (with active gradient story ring if user has ephemeral stories)
                        val avatarRingBrush = remember(hasActiveStories) {
                            if (hasActiveStories) {
                                Brush.linearGradient(listOf(CobaltPulse, ShutterAmber, SignalCrimson))
                            } else {
                                Brush.linearGradient(listOf(CobaltPulse, CobaltPulse.copy(alpha = 0.65f)))
                            }
                        }
                        PulseAvatarImage(
                            avatarRes = userProfile.avatarRes,
                            avatarUrl = userProfile.avatarUrl,
                            contentDescription = resolvedDisplayName,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, avatarRingBrush, CircleShape)
                                .then(
                                    if (hasActiveStories) {
                                        Modifier.clickable {
                                            haptics.performControlTapHaptic()
                                            showActiveStoriesViewer = true
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("profile_avatar")
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Follower / Following / Posts Statistics Card
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("profile_stats_bar")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Box(modifier = Modifier.testTag("profile_post_count")) {
                                    ProfileStatColumn(
                                        label = "Posts",
                                        value = userPosts.size.toString()
                                    )
                                }
                                VerticalDivider(
                                    modifier = Modifier.height(26.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                Box(modifier = Modifier.testTag("profile_followers_count")) {
                                    ProfileStatColumn(
                                        label = "Followers",
                                        value = formatMetricCount(userProfile.followersCount),
                                        onClick = if (isOwnProfile) ({ showFollowersSheet = true }) else null
                                    )
                                }
                                VerticalDivider(
                                    modifier = Modifier.height(26.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                Box(modifier = Modifier.testTag("profile_following_count")) {
                                    ProfileStatColumn(
                                        label = "Following",
                                        value = followingCount.toString(),
                                        onClick = if (isOwnProfile) ({ showFollowersSheet = true }) else null
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Display Name & Specialty Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = resolvedDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = ShutterAmber.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = userProfile.roleTag.ifBlank { "Visual Creator" },
                                style = MaterialTheme.typography.labelSmall,
                                color = ShutterAmber,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // User Bio
                    Text(
                        text = userProfile.bio.ifBlank {
                            "Visual storyteller sharing curated photography & cinema loops on Pulse."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag("profile_bio")
                    )

                    if (userProfile.location.isNotBlank() || userProfile.website.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (userProfile.location.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = userProfile.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (userProfile.website.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Link,
                                        contentDescription = null,
                                        tint = CobaltPulse,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = userProfile.website,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CobaltPulse,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Profile Action Bar
                    if (isOwnProfile) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit profile", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Button(
                                onClick = onCreateStoryClick,
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_story_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Story", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            OutlinedButton(
                                onClick = { showInsightsSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("profile_insights_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Insights,
                                    contentDescription = "Creator Insights",
                                    tint = ShutterAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insights", maxLines = 1)
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    haptics.performControlTapHaptic()
                                    onToggleFollowThisUser()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowingUser) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        CobaltPulse
                                    },
                                    contentColor = if (isFollowingUser) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.White
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("user_profile_follow_button")
                            ) {
                                Text(
                                    text = if (isFollowingUser) "Following" else "Follow",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = onMessageThisUser,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("user_profile_message_button")
                            ) {
                                Text("Message")
                            }
                        }
                    }

                    // Pinned Story Highlights Section (Own Profile or when creator has highlights)
                    if (isOwnProfile || highlights.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Story Highlights",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val totalPinnedStories = highlights.sumOf { it.stories.size }
                                    Text(
                                        text = "${highlights.size} ${if (highlights.size == 1) "Reel" else "Reels"} · $totalPinnedStories Pinned",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ShutterAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (isOwnProfile) {
                                Surface(
                                    color = CobaltPulse.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .clickable {
                                            initialHighlightTitleDraft = ""
                                            showArchiveHighlightPicker = true
                                        }
                                        .testTag("pin_archived_story_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PushPin,
                                            contentDescription = "Pin archived story to profile",
                                            tint = CobaltPulse,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Pin Archive",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CobaltPulse,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(end = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_highlights_row")
                        ) {
                            if (isOwnProfile) {
                                item(key = "add_new_highlight") {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(72.dp)
                                            .clickable {
                                                initialHighlightTitleDraft = ""
                                                showArchiveHighlightPicker = true
                                            }
                                            .testTag("add_new_highlight_button")
                                    ) {
                                        StoryAvatarRing(
                                            avatarRes = userProfile.avatarRes,
                                            avatarUrl = userProfile.avatarUrl,
                                            contentDescription = "New Highlight from Story Archive",
                                            size = 60.dp,
                                            isUnviewed = true,
                                            showAddBadge = true,
                                            onClick = {
                                                initialHighlightTitleDraft = ""
                                                showArchiveHighlightPicker = true
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Text(
                                            text = "New",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Archive",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            items(highlights, key = { it.id }) { group ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable { activeHighlightGroup = group }
                                        .testTag("highlight_${group.id}")
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        StoryAvatarRing(
                                            avatarRes = group.coverImageRes,
                                            avatarUrl = group.coverImageUri,
                                            contentDescription = group.title,
                                            size = 60.dp,
                                            isUnviewed = true,
                                            showAddBadge = false,
                                            onClick = { activeHighlightGroup = group }
                                        )
                                        Surface(
                                            color = Color(0xFF0E131D).copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.border(
                                                width = 1.dp,
                                                color = ShutterAmber.copy(alpha = 0.7f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        ) {
                                            Text(
                                                text = "${group.stories.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ShutterAmber,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = group.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${group.stories.size} ${if (group.stories.size == 1) "story" else "stories"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Content Tabs + Grid / Player Layout Switcher
            item(
                key = "profile_tabs_section",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = safeTabIndex,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = CobaltPulse
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = safeTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (safeTabIndex == index) {
                                            MaterialTheme.colorScheme.onBackground
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                },
                                modifier = Modifier.testTag("profile_tab_$index")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (safeTabIndex) {
                                0 -> "Published Photos & Videos (${activeGridPosts.size})"
                                1 -> "Video Loops & Reels (${activeGridPosts.size})"
                                else -> "Moodboard · $selectedMoodboard (${activeGridPosts.size})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !isCardFeedLayout,
                                onClick = { isCardFeedLayout = false },
                                label = { Text("Grid") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.GridView,
                                        contentDescription = "Grid view",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CobaltPulse,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = isCardFeedLayout,
                                onClick = { isCardFeedLayout = true },
                                label = { Text("Player") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ViewAgenda,
                                        contentDescription = "Card player view",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CobaltPulse,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 3. Saved Moodboard Collection Chips (visible in Saved tab on own profile)
            if (isOwnProfile && safeTabIndex == 2) {
                item(
                    key = "profile_moodboard_chips",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("moodboard_filter_row")
                    ) {
                        items(moodboards, key = { it }) { board ->
                            val selected = board.equals(selectedMoodboard, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectMoodboard(board) },
                                label = { Text(board) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CobaltPulse,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item(key = "create_new_moodboard_chip") {
                            OutlinedButton(
                                onClick = { showCreateMoodboardDialog = true },
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Board",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        item(key = "share_moodboard_collage_chip") {
                            OutlinedButton(
                                onClick = { showMoodboardCollageDialog = true },
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("share_moodboard_collage_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    tint = ShutterAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Share Collage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShutterAmber
                                )
                            }
                        }
                    }
                }
            }

            // 4. Lazy Grid of Posted Photos & Videos (or Card Feed Layout / Empty / Skeleton)
            if (activeGridPosts.isEmpty()) {
                if (isLoading) {
                    items(6, key = { "profile_skeleton_$it" }) {
                        PulseSkeletonBox(
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(horizontal = 1.dp)
                        )
                    }
                } else {
                    item(
                        key = "profile_empty_state",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Text(
                                text = when (safeTabIndex) {
                                    0 -> if (isOwnProfile) {
                                        "No published posts on your profile yet"
                                    } else {
                                        "No published posts from @$resolvedUsername yet"
                                    }
                                    1 -> "No kinetic Loops published yet"
                                    else -> "No posts saved to '$selectedMoodboard' yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (isOwnProfile) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onCreatePostClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse)
                                ) {
                                    Text("Create a post in Camera")
                                }
                            }
                        }
                    }
                }
            } else if (isCardFeedLayout || safeTabIndex == 1) {
                items(
                    items = activeGridPosts,
                    key = { "profile_card_${it.id}" },
                    span = { GridItemSpan(maxLineSpan) }
                ) { post ->
                    PostContactCard(
                        post = post,
                        onLikeToggle = { onLikeToggle(post) },
                        onDoubleTapLike = { onLikeToggle(post) },
                        onBookmarkToggle = { onBookmarkToggle(post) },
                        onOpenComments = { onOpenComments(post) },
                        onSharePost = { onSharePost(post) },
                        onCreatorClick = {},
                        onMoreOptions = { onMoreOptions(post) }
                    )
                }
            } else {
                items(
                    items = activeGridPosts,
                    key = { "profile_grid_${it.id}" }
                ) { post ->
                    UserProfilePhotoGridTile(
                        post = post,
                        showPinnedBadge = post.isPinnedOnProfile && safeTabIndex == 0,
                        onClick = { inspectedPostId = post.id }
                    )
                }
            }
        }
    }

    // Active Ephemeral Stories Viewer triggered by tapping the user's circular avatar
    if (showActiveStoriesViewer && activeUserStories.isNotEmpty()) {
        EphemeralStoryViewer(
            stories = activeUserStories,
            initialIndex = 0,
            onStorySeen = onStoryViewed,
            onSendReply = onSendStoryReply,
            onAddNewStory = {
                showActiveStoriesViewer = false
                onCreateStoryClick()
            },
            onDismiss = { showActiveStoriesViewer = false }
        )
    }

    if (showInsightsSheet) {
        CreatorInsightsBottomSheet(
            userProfile = userProfile,
            myPosts = userPosts,
            bookmarkedPosts = bookmarkedPosts,
            onDismiss = { showInsightsSheet = false }
        )
    }

    if (showMoodboardCollageDialog) {
        MoodboardCollageDialog(
            collectionName = selectedMoodboard,
            curatorUsername = resolvedUsername,
            posts = bookmarkedPosts,
            onDismiss = { showMoodboardCollageDialog = false }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, handle, role, bio, web, loc, avatarUrl ->
                onUpdateProfile(name, handle, role, bio, web, loc, avatarUrl)
                showEditProfileDialog = false
            }
        )
    }

    if (showCreateMoodboardDialog) {
        var newBoardName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateMoodboardDialog = false },
            title = { Text("New Moodboard Collection") },
            text = {
                OutlinedTextField(
                    value = newBoardName,
                    onValueChange = { newBoardName = it },
                    label = { Text("Collection title") },
                    placeholder = { Text("e.g. Brutalist Concrete, Night Neon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBoardName.isNotBlank()) {
                            onCreateMoodboard(newBoardName)
                            showCreateMoodboardDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateMoodboardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFollowersSheet) {
        FollowersBottomSheet(
            creators = creators,
            onDismiss = { showFollowersSheet = false },
            onToggleFollow = onToggleFollowCreator
        )
    }

    val highlightToView = activeHighlightGroup
    if (highlightToView != null && highlightToView.stories.isNotEmpty()) {
        EphemeralStoryViewer(
            stories = highlightToView.stories,
            initialIndex = 0,
            onStorySeen = onStoryViewed,
            onSendReply = onSendStoryReply,
            onAddNewStory = {
                val targetTitle = highlightToView.title
                activeHighlightGroup = null
                initialHighlightTitleDraft = targetTitle
                showArchiveHighlightPicker = true
            },
            onToggleHighlight = if (isOwnProfile) {
                { story ->
                    if (story.isHighlight) {
                        onUnpinStoryHighlight(story)
                        val remaining = highlightToView.stories.filterNot { it.id == story.id }
                        activeHighlightGroup = if (remaining.isEmpty()) {
                            null
                        } else {
                            highlightToView.copy(stories = remaining)
                        }
                    } else {
                        onPinStoriesToHighlight(listOf(story), highlightToView.title)
                    }
                }
            } else {
                null
            },
            onDismiss = { activeHighlightGroup = null }
        )
    }

    if (showArchiveHighlightPicker) {
        ArchiveStoryHighlightPickerDialog(
            archivedStories = archivedStories,
            existingHighlightTitles = highlights.map { it.title },
            initialTitle = initialHighlightTitleDraft,
            onDismiss = { showArchiveHighlightPicker = false },
            onPinSelected = { selectedStories, title ->
                onPinStoriesToHighlight(selectedStories, title)
                showArchiveHighlightPicker = false
            },
            onCaptureNewStory = {
                showArchiveHighlightPicker = false
                onCreateStoryClick()
            }
        )
    }

    val allCandidatePosts = remember(userPosts, bookmarkedPosts) {
        (userPosts + bookmarkedPosts).distinctBy { it.id }
    }
    val inspectedPost = allCandidatePosts.find { it.id == inspectedPostId }
    if (inspectedPost != null) {
        Dialog(
            onDismissRequest = { inspectedPostId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { inspectedPostId = null }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close post"
                                )
                            }
                            Text(
                                text = "Post detail",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        if (isOwnProfile) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { onTogglePinPost(inspectedPost) },
                                    modifier = Modifier.testTag("profile_pin_post_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PushPin,
                                        contentDescription = "Pin post",
                                        tint = ShutterAmber,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (inspectedPost.isPinnedOnProfile) "Unpin" else "Pin (Max 3)",
                                        color = ShutterAmber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        onDeletePost(inspectedPost)
                                        inspectedPostId = null
                                    },
                                    modifier = Modifier.testTag("profile_delete_post_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete post",
                                        tint = SignalCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Delete post",
                                        color = SignalCrimson,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            PostContactCard(
                                post = inspectedPost,
                                onLikeToggle = { onLikeToggle(inspectedPost) },
                                onDoubleTapLike = { onLikeToggle(inspectedPost) },
                                onBookmarkToggle = { onBookmarkToggle(inspectedPost) },
                                onOpenComments = { onOpenComments(inspectedPost) },
                                onSharePost = { onSharePost(inspectedPost) },
                                onCreatorClick = {},
                                onMoreOptions = { onMoreOptions(inspectedPost) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfilePhotoGridTile(
    post: PostEntity,
    showPinnedBadge: Boolean,
    onClick: () -> Unit
) {
    val frameCount = remember(post.id, post.carouselImageUrisCsv, post.carouselImageResCsv) {
        post.carouselFrames.size
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("profile_grid_item_${post.id}")
    ) {
        PulseFilteredImage(
            imageRes = post.imageRes,
            imageUri = post.imageUri,
            videoUri = post.effectiveVideoUri,
            filterPreset = post.filterPreset,
            contentDescription = post.caption,
            showFilmBadge = false,
            autoPlayVideo = false,
            compactVideoPreview = true,
            onTap = onClick,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle bottom scrim + like count for quick portfolio scanning
        if (post.likeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = formatMetricCount(post.likeCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (showPinnedBadge) {
            Surface(
                color = CobaltPulse.copy(alpha = 0.92f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .testTag("pinned_badge_${post.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned post",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "PINNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        if (frameCount > 1) {
            Surface(
                color = Color(0xCC0B0E14),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Collections,
                        contentDescription = "Multi-frame carousel",
                        tint = ShutterAmber,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = frameCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else if (post.hasVideo || post.isLoop) {
            Surface(
                color = Color(0xCC0B0E14),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Video post",
                        tint = ShutterAmber,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "VIDEO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Full-screen dialog hosting [UserProfileScreen] for viewing any creator's profile,
 * bio, follower/following statistics, active ephemeral stories, and 3-column lazy photo grid.
 */
@Composable
fun CreatorUserProfileDialog(
    creator: CreatorProfileEntity,
    creatorPosts: List<PostEntity>,
    creatorStories: List<StoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessageCreator: () -> Unit,
    onBlockCreator: () -> Unit,
    onStoryViewed: (StoryEntity) -> Unit = {},
    onSendStoryReply: (StoryEntity, String) -> Unit = { _, _ -> },
    onLikeToggle: (PostEntity) -> Unit = {},
    onBookmarkToggle: (PostEntity) -> Unit = {},
    onOpenComments: (PostEntity) -> Unit = {},
    onSharePost: (PostEntity) -> Unit = {},
    onMoreOptions: (PostEntity) -> Unit = {}
) {
    val mappedProfile = remember(creator) {
        UserProfileState(
            id = creator.id,
            username = creator.username,
            displayName = creator.displayName,
            roleTag = creator.roleTag,
            bio = creator.bio,
            website = creator.website,
            location = creator.location,
            avatarRes = creator.avatarRes,
            avatarUrl = creator.avatarUrl,
            followersCount = creator.followersCount,
            followingCount = creator.followingCount
        )
    }

    val creatorHighlights = remember(creatorStories) {
        creatorStories
            .filter { it.isHighlight }
            .groupBy { it.highlightTitle.ifBlank { "Highlights" } }
            .map { (title, list) ->
                val first = list.first()
                StoryHighlightGroup(
                    id = "creator_hl_${creator.id}_${title.lowercase().replace(" ", "_")}",
                    title = title,
                    coverImageUri = first.imageUri,
                    coverImageRes = first.imageRes,
                    stories = list
                )
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            UserProfileScreen(
                userProfile = mappedProfile,
                isOwnProfile = false,
                isFollowingUser = creator.isFollowing,
                userPosts = creatorPosts,
                highlights = creatorHighlights,
                activeUserStories = creatorStories,
                onBackClick = onDismiss,
                onToggleFollowThisUser = onToggleFollow,
                onMessageThisUser = {
                    onDismiss()
                    onMessageCreator()
                },
                onBlockThisUser = {
                    onBlockCreator()
                    onDismiss()
                },
                onStoryViewed = onStoryViewed,
                onSendStoryReply = onSendStoryReply,
                onLikeToggle = onLikeToggle,
                onBookmarkToggle = onBookmarkToggle,
                onOpenComments = onOpenComments,
                onSharePost = onSharePost,
                onMoreOptions = onMoreOptions
            )
        }
    }
}

/**
 * Backward-compatible alias delegating to [UserProfileScreen].
 */
@Composable
fun ProfileScreen(
    userProfile: UserProfileState,
    isAuthenticated: Boolean,
    isDarkTheme: Boolean = true,
    isLoading: Boolean = false,
    myPosts: List<PostEntity>,
    bookmarkedPosts: List<PostEntity>,
    moodboards: List<String>,
    selectedMoodboard: String,
    highlights: List<StoryHighlightGroup>,
    creators: List<CreatorProfileEntity>,
    archivedStories: List<StoryEntity> = emptyList(),
    activeUserStories: List<StoryEntity> = emptyList(),
    onToggleTheme: () -> Unit = {},
    onSelectMoodboard: (String) -> Unit,
    onCreateMoodboard: (String) -> Unit,
    onUpdateProfile: (String, String, String, String, String, String, String?) -> Unit,
    onLogout: () -> Unit,
    onCreateStoryClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onStoryViewed: (StoryEntity) -> Unit = {},
    onSendStoryReply: (StoryEntity, String) -> Unit = { _, _ -> },
    onPinStoriesToHighlight: (List<StoryEntity>, String) -> Unit = { _, _ -> },
    onUnpinStoryHighlight: (StoryEntity) -> Unit = {},
    onToggleFollowCreator: (CreatorProfileEntity) -> Unit,
    onLikeToggle: (PostEntity) -> Unit,
    onBookmarkToggle: (PostEntity) -> Unit,
    onOpenComments: (PostEntity) -> Unit,
    onSharePost: (PostEntity) -> Unit,
    onMoreOptions: (PostEntity) -> Unit,
    onTogglePinPost: (PostEntity) -> Unit = {},
    onDeletePost: (PostEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    UserProfileScreen(
        userProfile = userProfile,
        isAuthenticated = isAuthenticated,
        isOwnProfile = true,
        isDarkTheme = isDarkTheme,
        isLoading = isLoading,
        userPosts = myPosts,
        bookmarkedPosts = bookmarkedPosts,
        moodboards = moodboards,
        selectedMoodboard = selectedMoodboard,
        highlights = highlights,
        creators = creators,
        archivedStories = archivedStories,
        activeUserStories = activeUserStories,
        onToggleTheme = onToggleTheme,
        onSelectMoodboard = onSelectMoodboard,
        onCreateMoodboard = onCreateMoodboard,
        onUpdateProfile = onUpdateProfile,
        onLogout = onLogout,
        onCreateStoryClick = onCreateStoryClick,
        onCreatePostClick = onCreatePostClick,
        onStoryViewed = onStoryViewed,
        onSendStoryReply = onSendStoryReply,
        onPinStoriesToHighlight = onPinStoriesToHighlight,
        onUnpinStoryHighlight = onUnpinStoryHighlight,
        onToggleFollowCreator = onToggleFollowCreator,
        onLikeToggle = onLikeToggle,
        onBookmarkToggle = onBookmarkToggle,
        onOpenComments = onOpenComments,
        onSharePost = onSharePost,
        onMoreOptions = onMoreOptions,
        onTogglePinPost = onTogglePinPost,
        onDeletePost = onDeletePost,
        modifier = modifier
    )
}

@Composable
private fun ArchiveStoryHighlightPickerDialog(
    archivedStories: List<StoryEntity>,
    existingHighlightTitles: List<String>,
    initialTitle: String,
    onDismiss: () -> Unit,
    onPinSelected: (List<StoryEntity>, String) -> Unit,
    onCaptureNewStory: () -> Unit
) {
    val presetTitles = remember(existingHighlightTitles) {
        (existingHighlightTitles + listOf("Analog 35mm", "Street", "Darkroom", "Travel", "Portraits"))
            .filter { it.isNotBlank() }
            .distinct()
    }
    var highlightTitle by remember(initialTitle) {
        mutableStateOf(initialTitle.ifBlank { presetTitles.firstOrNull() ?: "Highlights" })
    }
    var selectedStoryIds by remember(archivedStories) {
        val defaultFirstId = archivedStories.firstOrNull()?.id
        mutableStateOf(if (defaultFirstId != null) setOf(defaultFirstId) else emptySet())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("story_archive_highlight_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pin Archived Stories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Select past stories from your archive to pin to your profile Highlights",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_archive_highlight_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close archive picker"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = highlightTitle,
                    onValueChange = { highlightTitle = it },
                    label = { Text("Highlight Reel Title") },
                    placeholder = { Text("e.g., Analog 35mm, Street, Travel") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("highlight_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetTitles, key = { it }) { preset ->
                        val isSelected = highlightTitle.equals(preset, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { highlightTitle = preset },
                            label = {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CobaltPulse.copy(alpha = 0.22f),
                                selectedLabelColor = CobaltPulse
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Story Archive (${archivedStories.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onCaptureNewStory,
                        modifier = Modifier.testTag("archive_capture_new_story_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = CobaltPulse,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New 24h Story",
                            style = MaterialTheme.typography.labelMedium,
                            color = CobaltPulse
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (archivedStories.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Your story archive is empty",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onCaptureNewStory,
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse)
                            ) {
                                Text("Capture First Story")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .testTag("archive_stories_grid")
                    ) {
                        items(archivedStories, key = { it.id }) { story ->
                            val isSelected = story.id in selectedStoryIds
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) ShutterAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedStoryIds = if (isSelected) {
                                            selectedStoryIds - story.id
                                        } else {
                                            selectedStoryIds + story.id
                                        }
                                    }
                                    .testTag("archive_story_item_${story.id}")
                            ) {
                                PulseFilteredImage(
                                    imageRes = story.imageRes,
                                    imageUri = story.imageUri,
                                    videoUri = null,
                                    filterPreset = story.filterPreset,
                                    contentDescription = story.captionOverlay,
                                    showFilmBadge = false,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.35f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.78f)
                                                )
                                            )
                                        )
                                )

                                Surface(
                                    color = if (isSelected) ShutterAmber else Color.Black.copy(alpha = 0.55f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = Color(0xFF0B0E14),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(6.dp)
                                ) {
                                    if (story.isHighlight && story.highlightTitle.isNotBlank()) {
                                        Text(
                                            text = "Pinned · ${story.highlightTitle}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ShutterAmber,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = story.createdAtLabel.ifBlank { "Archived" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = story.captionOverlay.ifBlank { story.locationTag.ifBlank { "Story frame" } },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val chosen = archivedStories.filter { it.id in selectedStoryIds }
                            if (chosen.isNotEmpty()) {
                                onPinSelected(chosen, highlightTitle.trim().ifBlank { "Highlights" })
                            }
                        },
                        enabled = selectedStoryIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("confirm_pin_highlight_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pin to Profile (${selectedStoryIds.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    userProfile: UserProfileState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String?) -> Unit
) {
    var displayName by remember { mutableStateOf(userProfile.displayName) }
    var username by remember { mutableStateOf(userProfile.username) }
    var roleTag by remember { mutableStateOf(userProfile.roleTag) }
    var bio by remember { mutableStateOf(userProfile.bio) }
    var website by remember { mutableStateOf(userProfile.website) }
    var location by remember { mutableStateOf(userProfile.location) }
    var avatarUrl by remember { mutableStateOf(userProfile.avatarUrl.orEmpty()) }

    val context = LocalContext.current
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUrl = DarkroomProcessingService.copyUriToPermanentInternalFile(
                context = context,
                sourceUri = uri,
                isVideo = false
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit studio profile",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PulseAvatarImage(
                        avatarRes = userProfile.avatarRes,
                        avatarUrl = avatarUrl.ifBlank { userProfile.avatarUrl },
                        contentDescription = displayName.ifBlank { "Profile avatar" },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, CobaltPulse, CircleShape)
                    )
                    OutlinedButton(
                        onClick = {
                            avatarPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Profile Photo")
                    }
                }
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = { Text("Profile photo URL / path") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Handle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roleTag,
                    onValueChange = { roleTag = it },
                    label = { Text("Discipline / specialty") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Artist statement / bio") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Base city") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Portfolio URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        displayName,
                        username,
                        roleTag,
                        bio,
                        website,
                        location,
                        avatarUrl.trim().ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse)
            ) {
                Text("Save changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowersBottomSheet(
    creators: List<CreatorProfileEntity>,
    onDismiss: () -> Unit,
    onToggleFollow: (CreatorProfileEntity) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Studio network & following",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 340.dp)
            ) {
                items(creators, key = { it.id }) { creator ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PulseAvatarImage(
                            avatarRes = creator.avatarRes,
                            avatarUrl = creator.avatarUrl,
                            contentDescription = creator.displayName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = creator.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "@${creator.username} · ${creator.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onToggleFollow(creator) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (creator.isFollowing) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    CobaltPulse
                                },
                                contentColor = if (creator.isFollowing) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Color.White
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (creator.isFollowing) "Following" else "Follow")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
