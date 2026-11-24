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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StudioDraftEntity
import com.example.data.model.StudioPublishTarget
import com.example.ui.components.PulseFilteredImage
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.CobaltPulseSoft
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson

internal fun formatDraftRelativeTime(updatedAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val diffSec = ((nowMs - updatedAtMs).coerceAtLeast(0L)) / 1000L
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m ago"
        diffSec < 86_400L -> "${diffSec / 3600L}h ago"
        else -> "${diffSec / 86_400L}d ago"
    }
}

@Composable
internal fun StudioDraftsTray(
    drafts: List<StudioDraftEntity>,
    activeDraftId: String?,
    onResumeDraft: (StudioDraftEntity) -> Unit,
    onDeleteDraft: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("studio_drafts_tray")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Drafts,
                        contentDescription = null,
                        tint = ShutterAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Saved Drafts (${drafts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Tap to resume",
                    style = MaterialTheme.typography.labelSmall,
                    color = CobaltPulseSoft,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(drafts, key = { it.id }) { draft ->
                    StudioDraftCard(
                        draft = draft,
                        isActive = draft.id == activeDraftId,
                        onResume = { onResumeDraft(draft) },
                        onDelete = { onDeleteDraft(draft.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioDraftCard(
    draft: StudioDraftEntity,
    isActive: Boolean,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val stepLabel = StudioCreationStep.entries
        .find { it.name == draft.currentStepName }
        ?.shortLabel
        ?: "Media"

    val targetBadgeText = when (draft.publishTarget) {
        StudioPublishTarget.FEED_POST -> "Post"
        StudioPublishTarget.STORY_24H -> "Story"
        StudioPublishTarget.LOOP_REEL -> "Loop"
    }

    val titleLine = draft.caption.trim().ifBlank {
        "Step · $stepLabel · ${draft.filterPreset.displayName}"
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .width(232.dp)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) CobaltPulseSoft else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onResume)
            .testTag("studio_draft_card_${draft.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            ) {
                val thumbUri = draft.previewThumbnailUri
                if (!thumbUri.isNullOrBlank()) {
                    PulseFilteredImage(
                        imageRes = 0,
                        imageUri = thumbUri,
                        videoUri = null,
                        filterPreset = draft.filterPreset,
                        contentDescription = "Draft thumbnail",
                        showFilmBadge = false,
                        autoPlayVideo = false,
                        compactVideoPreview = true,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (draft.hasVideo) Icons.Filled.Videocam else Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = if (draft.hasVideo) ShutterAmber else CobaltPulseSoft,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (draft.hasVideo && !thumbUri.isNullOrBlank()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.78f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = "Video draft",
                            tint = ShutterAmber,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        color = CobaltPulse.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = targetBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = CobaltPulseSoft,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    if (draft.isAutoSave) {
                        Surface(
                            color = ShutterAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShutterAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = titleLine,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatDraftRelativeTime(draft.updatedAtMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("studio_delete_draft_${draft.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete draft",
                    tint = SignalCrimson,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
