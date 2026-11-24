package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.FilmPreset
import com.example.data.model.PostEntity
import com.example.data.model.UserProfileState
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson

/**
 * Creator Analytics / Studio Insights bottom sheet displaying total post views,
 * likes, saves, and top-performing film presets & hashtags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorInsightsBottomSheet(
    userProfile: UserProfileState,
    myPosts: List<PostEntity>,
    bookmarkedPosts: List<PostEntity>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalLikes = remember(myPosts) { myPosts.sumOf { it.likeCount } }
    val totalComments = remember(myPosts) { myPosts.sumOf { it.commentCount } }
    val totalSaves = remember(myPosts, bookmarkedPosts) {
        (myPosts.count { it.isBookmarked } + bookmarkedPosts.size).coerceAtLeast(myPosts.size * 2)
    }
    val estimatedViews = remember(myPosts, totalLikes, totalComments) {
        (myPosts.size * 420 + totalLikes * 14 + totalComments * 28).coerceAtLeast(1280)
    }

    val presetBreakdown = remember(myPosts) {
        val grouped = myPosts
            .groupBy { it.filterPreset }
            .mapValues { (_, list) -> list.sumOf { it.likeCount + it.commentCount * 2 + 15 } }
            .toList()
            .sortedByDescending { it.second }
        if (grouped.isNotEmpty()) {
            grouped.take(4)
        } else {
            listOf(
                FilmPreset.KODACHROME_64 to 145,
                FilmPreset.TRI_X_400 to 98,
                FilmPreset.CINESTILL_800T to 84,
                FilmPreset.NATURAL to 62
            )
        }
    }
    val maxPresetScore = remember(presetBreakdown) {
        (presetBreakdown.maxOfOrNull { it.second } ?: 100).coerceAtLeast(1)
    }

    val topTags = remember(myPosts) {
        val counts = mutableMapOf<String, Int>()
        myPosts.forEach { post ->
            val score = post.likeCount + post.commentCount + 5
            post.tags.forEach { tag ->
                val clean = tag.lowercase().removePrefix("#")
                if (clean.isNotBlank()) {
                    counts[clean] = (counts[clean] ?: 0) + score
                }
            }
        }
        val sorted = counts.entries.sortedByDescending { it.value }.map { "#${it.key}" to it.value }.take(5)
        sorted.ifEmpty {
            listOf(
                "#kodachrome" to 210,
                "#street" to 164,
                "#architecture" to 132,
                "#mediumformat" to 96
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("creator_insights_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Analytics,
                        contentDescription = null,
                        tint = CobaltPulse,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Studio Insights",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@${userProfile.username.ifBlank { "creator" }} · 30-day darkroom telemetry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close insights"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Primary Metric Cards: Views, Likes, Saves
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InsightMetricCard(
                    label = "Post Views",
                    value = formatMetricCount(estimatedViews),
                    subtext = "+18% vs last 30d",
                    accentColor = CobaltPulse,
                    icon = Icons.Filled.Visibility,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("insights_views_card")
                )
                InsightMetricCard(
                    label = "Total Likes",
                    value = formatMetricCount(totalLikes),
                    subtext = "${myPosts.size} published",
                    accentColor = SignalCrimson,
                    icon = Icons.Filled.Favorite,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("insights_likes_card")
                )
                InsightMetricCard(
                    label = "Collection Saves",
                    value = formatMetricCount(totalSaves),
                    subtext = "Moodboards",
                    accentColor = ShutterAmber,
                    icon = Icons.Filled.Bookmark,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("insights_saves_card")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Top-Performing Film Presets",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            presetBreakdown.forEach { (preset, score) ->
                val ratio = (score.toFloat() / maxPresetScore.toFloat()).coerceIn(0.15f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${preset.displayName} (${preset.code})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$score engagement index",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShutterAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        color = CobaltPulse,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Top-Performing Tags",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                topTags.take(4).forEach { (tag, score) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = CobaltPulse,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$score reach",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InsightMetricCard(
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Collaborative Collections / Moodboards Visual Collage Card Dialog.
 * Allows users to preview and export/share a curated Saved Collection as a visual collage card.
 */
@Composable
fun MoodboardCollageDialog(
    collectionName: String,
    curatorUsername: String,
    posts: List<PostEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberPulseHaptics()
    var isCollaborativeEnabled by remember { mutableStateOf(true) }

    val collagePosts = remember(posts) {
        if (posts.isNotEmpty()) {
            posts.take(4)
        } else {
            listOf(
                PostEntity(
                    id = "col_1",
                    authorId = "u1",
                    authorUsername = curatorUsername,
                    authorDisplayName = curatorUsername,
                    imageRes = R.drawable.img_post_kyoto_1794021771843,
                    caption = "Kyoto Study"
                ),
                PostEntity(
                    id = "col_2",
                    authorId = "u1",
                    authorUsername = curatorUsername,
                    authorDisplayName = curatorUsername,
                    imageRes = R.drawable.img_post_architecture_1794021771801,
                    caption = "Monolith Concrete"
                ),
                PostEntity(
                    id = "col_3",
                    authorId = "u1",
                    authorUsername = curatorUsername,
                    authorDisplayName = curatorUsername,
                    imageRes = R.drawable.img_post_portrait_1794021773202,
                    caption = "Studio Portrait"
                ),
                PostEntity(
                    id = "col_4",
                    authorId = "u1",
                    authorUsername = curatorUsername,
                    authorDisplayName = curatorUsername,
                    imageRes = R.drawable.img_post_alpine_1794021773292,
                    caption = "Dolomites Light"
                )
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0E131B),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CobaltPulse.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .testTag("moodboard_collage_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CollectionsBookmark,
                            contentDescription = null,
                            tint = ShutterAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MOODBOARD COLLAGE CARD",
                            style = MaterialTheme.typography.labelMedium,
                            color = ShutterAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close collage",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2x2 Visual Collage Frame
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF07090D))
                        .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    val topRow = collagePosts.take(2)
                    val bottomRow = collagePosts.drop(2).take(2).ifEmpty { topRow.reversed() }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        topRow.forEach { p ->
                            PulseFilteredImage(
                                imageRes = p.imageRes,
                                imageUri = p.imageUri,
                                filterPreset = p.filterPreset,
                                contentDescription = p.caption,
                                showFilmBadge = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        bottomRow.forEach { p ->
                            PulseFilteredImage(
                                imageRes = p.imageRes,
                                imageUri = p.imageUri,
                                filterPreset = p.filterPreset,
                                contentDescription = p.caption,
                                showFilmBadge = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Column {
                            Text(
                                text = collectionName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Curated by @${curatorUsername.ifBlank { "creator" }} · ${posts.size} prints",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                        Surface(
                            color = if (isCollaborativeEnabled) CobaltPulse else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                haptics.performBookmarkHaptic()
                                isCollaborativeEnabled = !isCollaborativeEnabled
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GroupAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCollaborativeEnabled) "Collaborative" else "Solo Board",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            haptics.performSendHaptic()
                            shareMoodboardCollageExternally(
                                context = context,
                                collectionName = collectionName,
                                curatorUsername = curatorUsername,
                                postCount = posts.size,
                                isCollaborative = isCollaborativeEnabled
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("share_collage_external_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Collage")
                    }
                }
            }
        }
    }
}

fun shareMoodboardCollageExternally(
    context: Context,
    collectionName: String,
    curatorUsername: String,
    postCount: Int,
    isCollaborative: Boolean
) {
    val modeLabel = if (isCollaborative) "Collaborative Moodboard" else "Curated Moodboard"
    val body = buildString {
        appendLine("$modeLabel · \"$collectionName\" on Pulse")
        appendLine("Curated by @${curatorUsername.ifBlank { "creator" }} ($postCount saved frames)")
        append("Explore & contribute to this visual collection on Pulse Studio.")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Pulse Moodboard: $collectionName")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    val chooser = Intent.createChooser(sendIntent, "Share moodboard collage").apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        context.startActivity(chooser)
    } catch (_: Throwable) {
    }
}

/**
 * Trending Hashtag & Location Hub Bottom Sheet.
 * Opens whenever a user taps any #tag or location pill on a post in Feed or Explore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagLocationHubBottomSheet(
    hubQuery: String,
    allPosts: List<PostEntity>,
    onDismiss: () -> Unit,
    onOpenInExplore: (String) -> Unit,
    onSelectPost: (PostEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isHashtag = hubQuery.trim().startsWith("#")
    val cleanToken = hubQuery.trim().removePrefix("#").removePrefix("@").trim()

    val matchingPosts = remember(allPosts, cleanToken, isHashtag) {
        allPosts.filter { post ->
            if (isHashtag) {
                post.tags.any { it.equals(cleanToken, ignoreCase = true) || it.contains(cleanToken, ignoreCase = true) } ||
                    post.category.contains(cleanToken, ignoreCase = true) ||
                    post.caption.contains(cleanToken, ignoreCase = true)
            } else {
                post.location.contains(cleanToken, ignoreCase = true) ||
                    post.caption.contains(cleanToken, ignoreCase = true)
            }
        }.ifEmpty { allPosts.take(6) }
    }

    val totalLikes = remember(matchingPosts) { matchingPosts.sumOf { it.likeCount } }
    val uniqueContributors = remember(matchingPosts) {
        matchingPosts.map { "@${it.authorUsername}" }.distinct().take(4)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("tag_location_hub_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = CobaltPulse.copy(alpha = 0.18f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isHashtag) Icons.Filled.Tag else Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = if (isHashtag) CobaltPulse else ShutterAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHashtag) "#$cleanToken" else cleanToken,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${matchingPosts.size} curated frames · ${formatMetricCount(totalLikes)} likes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        onOpenInExplore(if (isHashtag) "#$cleanToken" else cleanToken)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltPulse),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("hub_filter_in_explore_button")
                ) {
                    Text("Open in Explore")
                }
            }

            if (uniqueContributors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Featured contributors: ${uniqueContributors.joinToString(" · ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShutterAmber
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 340.dp)
                    .testTag("hub_posts_grid")
            ) {
                items(matchingPosts, key = { "hub_${it.id}" }) { post ->
                    PulseFilteredImage(
                        imageRes = post.imageRes,
                        imageUri = post.imageUri,
                        videoUri = post.effectiveVideoUri,
                        filterPreset = post.filterPreset,
                        contentDescription = post.caption,
                        showFilmBadge = false,
                        autoPlayVideo = false,
                        compactVideoPreview = true,
                        onTap = {
                            onDismiss()
                            onSelectPost(post)
                        },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
