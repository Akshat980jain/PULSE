package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.PostEntity
import com.example.ui.components.ExploreSkeletonLoader
import com.example.ui.components.PostContactCard
import com.example.ui.components.PulseAvatarImage
import com.example.ui.components.PulseFilteredImage
import com.example.ui.components.formatMetricCount
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson

/**
 * Simple query filter matching posts by tags (#tag / tag name) or user handles (@username / displayName).
 */
fun filterPostsByTagOrHandle(posts: List<PostEntity>, rawQuery: String): List<PostEntity> {
    val trimmed = rawQuery.trim()
    val cleanQuery = trimmed.lowercase().removePrefix("@").removePrefix("#").trim()
    if (cleanQuery.isEmpty()) return posts
    val isHandleOnly = trimmed.startsWith("@")
    val isTagOnly = trimmed.startsWith("#")

    return posts.filter { post ->
        val matchesHandle = post.authorUsername.lowercase().contains(cleanQuery) ||
            post.authorDisplayName.lowercase().contains(cleanQuery)
        val matchesTag = post.tags.any { it.lowercase().removePrefix("#").contains(cleanQuery) } ||
            post.tagsCsv.lowercase().contains(cleanQuery) ||
            post.category.lowercase().contains(cleanQuery)
        val matchesCaptionOrLocation = post.caption.lowercase().contains(cleanQuery) ||
            post.location.lowercase().contains(cleanQuery)

        when {
            isHandleOnly -> matchesHandle
            isTagOnly -> matchesTag
            else -> matchesTag || matchesHandle || matchesCaptionOrLocation
        }
    }
}

/**
 * Computes a deterministic, visually varied masonry aspect ratio (width / height)
 * for each post in the Explore staggered grid so adjacent columns stagger naturally like Pinterest.
 */
fun calculateMasonryAspectRatio(post: PostEntity, index: Int): Float {
    val rhythmPresets = floatArrayOf(0.68f, 0.96f, 0.76f, 1.12f, 0.64f, 0.86f)
    val baseRhythm = rhythmPresets[index % rhythmPresets.size]
    return when {
        post.isLoop || post.hasVideo -> if (index % 2 == 0) 0.64f else 0.72f
        post.aspectRatio == PhotoAspectRatio.CINEMA_16_9 -> if (index % 3 == 0) 1.18f else 0.88f
        post.aspectRatio == PhotoAspectRatio.SQUARE_1_1 -> if (index % 2 == 0) 0.96f else 0.82f
        else -> baseRhythm
    }
}

@Composable
fun ExploreScreen(
    searchQuery: String,
    selectedCategory: String,
    posts: List<PostEntity>,
    creators: List<CreatorProfileEntity>,
    isLoading: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleFollowCreator: (CreatorProfileEntity) -> Unit,
    onCreatorClick: (String) -> Unit,
    onLikeToggle: (PostEntity) -> Unit,
    onBookmarkToggle: (PostEntity) -> Unit,
    onOpenComments: (PostEntity) -> Unit,
    onSharePost: (PostEntity) -> Unit,
    onMoreOptions: (PostEntity) -> Unit,
    onOpenHub: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Street", "Architecture", "Portraits", "Alpine", "Loops", "Kodachrome")
    var inspectedPostId by remember { mutableStateOf<String?>(null) }

    val filteredPosts = remember(posts, searchQuery) {
        filterPostsByTagOrHandle(posts, searchQuery)
    }

    val quickSuggestions = remember(posts, creators) {
        val tags = posts
            .flatMap { it.tags }
            .map { "#${it.trim().removePrefix("#")}" }
            .filter { it.length > 1 }
            .distinct()
            .take(4)
        val handles = (posts.map { it.authorUsername } + creators.map { it.username })
            .map { it.trim().removePrefix("@") }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
            .map { "@$it" }
        (tags + handles).ifEmpty {
            listOf("#street", "#analog", "#mediumformat", "#architecture")
        }
    }

    val trendingHubs = remember(posts) {
        val topCities = posts.map { it.location.substringBefore("·").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
        val topTags = posts.flatMap { it.tags }
            .map { "#${it.trim().removePrefix("#")}" }
            .filter { it.length > 1 }
            .distinct()
            .take(4)
        (topCities + topTags).ifEmpty {
            listOf("Kyoto", "Berlin", "#kodachrome", "#street", "#architecture")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Search Bar Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("explore_search_bar")
        ) {
            Text(
                text = "Explore exhibition",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by #tags or @user_handles…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search tags or user handles"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("explore_search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick #tag & @handle filter pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_quick_query_row")
            ) {
                items(quickSuggestions, key = { it }) { suggestion ->
                    val isSelected = searchQuery.trim().equals(suggestion, ignoreCase = true)
                    Surface(
                        color = if (isSelected) {
                            CobaltPulse
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable {
                                if (isSelected) {
                                    onSearchQueryChange("")
                                } else {
                                    onSearchQueryChange(suggestion)
                                }
                            }
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Category Filter Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it }) { category ->
                val selected = category == selectedCategory
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CobaltPulse,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("explore_chip_${category.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Trending Hashtag & Location Hubs Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("explore_trending_hubs_row")
        ) {
            items(trendingHubs, key = { "hub_$it" }) { hub ->
                val isTag = hub.startsWith("#")
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { onOpenHub(hub) }
                        .testTag("explore_hub_${hub.lowercase().removePrefix("#").replace(" ", "_")}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (!isTag) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = ShutterAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isTag) "$hub Hub" else "$hub City Hub",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTag) CobaltPulse else ShutterAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("explore_lazy_column")
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 156.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("explore_staggered_grid")
            ) {
                // Skeleton Loader when Explore data is initially loading
                if (isLoading && filteredPosts.isEmpty()) {
                    item(
                        key = "explore_loading_skeleton",
                        span = StaggeredGridItemSpan.FullLine
                    ) {
                        ExploreSkeletonLoader()
                    }
                } else {
                    // Featured Creators Carousel spanning full width above the staggered masonry grid
                    if (creators.isNotEmpty()) {
                        item(
                            key = "featured_creators_section",
                            span = StaggeredGridItemSpan.FullLine
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Featured visual storytellers",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(creators, key = { it.id }) { creator ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .width(190.dp)
                                                .border(
                                                    0.5.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clickable { onCreatorClick(creator.id) }
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(14.dp)
                                            ) {
                                                PulseAvatarImage(
                                                    avatarRes = creator.avatarRes,
                                                    avatarUrl = creator.avatarUrl,
                                                    contentDescription = creator.displayName,
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(CircleShape)
                                                        .border(1.5.dp, CobaltPulse, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = creator.displayName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (creator.isVerified) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Filled.CheckCircle,
                                                            contentDescription = null,
                                                            tint = CobaltPulse,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "@${creator.username}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                                if (creator.location.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = creator.location,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ShutterAmber,
                                                        maxLines = 1
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = { onToggleFollowCreator(creator) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (creator.isFollowing) {
                                                            MaterialTheme.colorScheme.surface
                                                        } else {
                                                            CobaltPulse
                                                        },
                                                        contentColor = if (creator.isFollowing) {
                                                            MaterialTheme.colorScheme.onSurface
                                                        } else {
                                                            Color.White
                                                        }
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(34.dp)
                                                ) {
                                                    Text(
                                                        text = if (creator.isFollowing) "Following" else "Follow",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredPosts.isEmpty()) {
                        item(
                            key = "explore_empty_state",
                            span = StaggeredGridItemSpan.FullLine
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                                    .testTag("explore_empty_state")
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        "No posts found for \"$searchQuery\""
                                    } else {
                                        "No posts in this category yet"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try searching another #tag or @user_handle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { onSearchQueryChange("") },
                                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse)
                                    ) {
                                        Text("Clear search")
                                    }
                                }
                            }
                        }
                    } else {
                        // Pinterest-like Staggered Masonry Feed Tiles
                        itemsIndexed(
                            items = filteredPosts,
                            key = { _, post -> "masonry_pin_${post.id}" }
                        ) { index, post ->
                            val pinAspectRatio = remember(post.id, post.aspectRatioName, post.isLoop, index) {
                                calculateMasonryAspectRatio(post, index)
                            }
                            ExploreMasonryPinCard(
                                post = post,
                                aspectRatio = pinAspectRatio,
                                onClick = { inspectedPostId = post.id },
                                onLikeToggle = { onLikeToggle(post) },
                                onBookmarkToggle = { onBookmarkToggle(post) },
                                onCreatorClick = { onCreatorClick(post.authorId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("explore_masonry_pin_${post.id}")
                            )
                        }
                    }
                }
            }
        }
    }

    val inspectedPost = filteredPosts.find { it.id == inspectedPostId }
        ?: posts.find { it.id == inspectedPostId }
    if (inspectedPost != null) {
        Dialog(
            onDismissRequest = { inspectedPostId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        IconButton(onClick = { inspectedPostId = null }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close print inspection"
                            )
                        }
                        Text(
                            text = "Exhibition print",
                            style = MaterialTheme.typography.headlineMedium
                        )
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
                                onCreatorClick = {
                                    inspectedPostId = null
                                    onCreatorClick(inspectedPost.authorId)
                                },
                                onMoreOptions = { onMoreOptions(inspectedPost) },
                                onTagClick = { tag ->
                                    inspectedPostId = null
                                    onOpenHub(tag)
                                },
                                onLocationClick = { loc ->
                                    inspectedPostId = null
                                    onOpenHub(loc)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreMasonryPinCard(
    post: PostEntity,
    aspectRatio: Float,
    onClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onCreatorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryTag = remember(post.tagsCsv, post.category) {
        post.tags.firstOrNull()?.removePrefix("#") ?: post.category.lowercase()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Masonry Image / Video Frame with dynamic staggered aspectRatio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                PulseFilteredImage(
                    imageRes = post.imageRes,
                    imageUri = post.imageUri,
                    videoUri = post.effectiveVideoUri,
                    filterPreset = post.filterPreset,
                    contentDescription = post.caption,
                    showFilmBadge = true,
                    autoPlayVideo = true,
                    compactVideoPreview = true,
                    onTap = onClick,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-right quick save / video badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    if (post.isLoop || post.hasVideo) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.68f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (post.hasVideo) Icons.Filled.Videocam else Icons.Filled.PlayArrow,
                                contentDescription = "Video Loop",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(14.dp)
                            )
                        }
                    }

                    Surface(
                        color = if (post.isBookmarked) {
                            ShutterAmber.copy(alpha = 0.9f)
                        } else {
                            Color.Black.copy(alpha = 0.58f)
                        },
                        shape = CircleShape,
                        modifier = Modifier.clickable(onClick = onBookmarkToggle)
                    ) {
                        Icon(
                            imageVector = if (post.isBookmarked) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Filled.BookmarkBorder
                            },
                            contentDescription = "Save Pin",
                            tint = if (post.isBookmarked) Color(0xFF090D16) else Color.White,
                            modifier = Modifier
                                .padding(5.dp)
                                .size(14.dp)
                        )
                    }
                }

                // Bottom-left tag & location overlay pill inside the frame
                if (primaryTag.isNotBlank() || post.location.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (post.location.isNotBlank()) {
                                "#$primaryTag · ${post.location.substringBefore("·").trim()}"
                            } else {
                                "#$primaryTag"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = ShutterAmber,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Pinterest-style Pin Footer: Caption snippet + Creator Avatar + Like action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                if (post.caption.isNotBlank()) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = if (post.caption.length > 48) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onCreatorClick)
                    ) {
                        PulseAvatarImage(
                            avatarRes = post.authorAvatarRes,
                            avatarUrl = post.authorAvatarUrl,
                            contentDescription = post.authorDisplayName,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "@${post.authorUsername}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onLikeToggle)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Like Pin",
                            tint = if (post.isLiked) SignalCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formatMetricCount(post.likeCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

