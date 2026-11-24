package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserProfileState
import com.example.ui.components.EphemeralStoryViewer
import com.example.ui.components.StoriesAvatarTray
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.PulseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserProfileAndStoriesRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `userProfileScreen displays bio, follower and following statistics, and lazy photo grid`() {
        val sampleUser = UserProfileState(
            id = "u_creator_sora",
            username = "sora.studio",
            displayName = "Sora Takahashi",
            roleTag = "Street Photographer",
            bio = "Chasing rain reflections and 35mm film grain across Kyoto.",
            location = "Kyoto, Japan",
            website = "sorastudio.photo",
            avatarRes = R.drawable.img_post_kyoto_1794021771843,
            followersCount = 2480,
            followingCount = 312
        )
        val samplePosts = listOf(
            PostEntity(
                id = "grid_p1",
                authorId = sampleUser.id,
                authorUsername = sampleUser.username,
                authorDisplayName = sampleUser.displayName,
                imageRes = R.drawable.img_post_kyoto_1794021771843,
                caption = "Pontocho neon rain",
                likeCount = 128
            ),
            PostEntity(
                id = "grid_p2",
                authorId = sampleUser.id,
                authorUsername = sampleUser.username,
                authorDisplayName = sampleUser.displayName,
                imageRes = R.drawable.img_post_portrait_1794021773202,
                caption = "Golden hour window light",
                likeCount = 245
            ),
            PostEntity(
                id = "grid_p3",
                authorId = sampleUser.id,
                authorUsername = sampleUser.username,
                authorDisplayName = sampleUser.displayName,
                imageRes = R.drawable.img_post_kyoto_1794021771843,
                caption = "Analog contact sheet",
                likeCount = 91
            )
        )

        var followed by mutableStateOf(false)

        composeTestRule.setContent {
            PulseTheme(darkTheme = true) {
                UserProfileScreen(
                    userProfile = sampleUser,
                    isAuthenticated = true,
                    isOwnProfile = false,
                    isFollowingUser = followed,
                    userPosts = samplePosts,
                    onBackClick = {},
                    onToggleFollowThisUser = { followed = !followed }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("user_profile_screen_u_creator_sora").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_stats_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_post_count").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_followers_count").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_following_count").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_bio")
            .assertIsDisplayed()
            .assertTextContains("Chasing rain reflections and 35mm film grain across Kyoto.")
        // Follow button updates state on creator profile
        composeTestRule.onNodeWithTag("user_profile_follow_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(followed)
        composeTestRule.onNodeWithTag("user_profile_follow_button").assertTextContains("Following")

        // Scroll lazy photo grid to bring the 3-column photo tiles into viewport
        composeTestRule.onNodeWithTag("profile_posts_grid")
            .assertIsDisplayed()
            .performScrollToNode(hasTestTag("profile_grid_item_grid_p1"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_grid_item_grid_p1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_grid_item_grid_p2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_grid_item_grid_p3").assertIsDisplayed()
    }

    @Test
    fun `storiesAvatarTray opens full-screen ephemeral story viewer for image and video stories`() {
        val nowMs = System.currentTimeMillis()
        val currentUser = UserProfileState(
            id = "u_me",
            username = "pulse.creator",
            displayName = "Pulse Creator",
            avatarRes = R.drawable.img_post_kyoto_1794021771843
        )
        val stories = listOf(
            StoryEntity(
                id = "story_img_1",
                authorId = "u_sora",
                authorUsername = "sora.studio",
                authorDisplayName = "Sora Takahashi",
                authorAvatarRes = R.drawable.img_post_kyoto_1794021771843,
                imageRes = R.drawable.img_post_kyoto_1794021771843,
                captionOverlay = "Ephemeral 35mm frame in Gion",
                locationTag = "Kyoto",
                createdAtLabel = "2h ago",
                timestampMs = nowMs - (2L * 60L * 60L * 1000L),
                isViewed = false
            ),
            StoryEntity(
                id = "story_vid_2",
                authorId = "u_elena",
                authorUsername = "elena.voss",
                authorDisplayName = "Elena Voss",
                authorAvatarRes = R.drawable.img_post_portrait_1794021773202,
                imageRes = R.drawable.img_post_portrait_1794021773202,
                videoUri = "https://cdn.pulse.studio/stories/berlin_walk.mp4",
                captionOverlay = "Ephemeral video walk through Berlin",
                locationTag = "Berlin",
                createdAtLabel = "1h ago",
                timestampMs = nowMs - (1L * 60L * 60L * 1000L),
                isViewed = false
            )
        )

        var activeStoryIndex by mutableStateOf<Int?>(null)
        val seenStoryIds = mutableListOf<String>()

        composeTestRule.setContent {
            PulseTheme(darkTheme = true) {
                StoriesAvatarTray(
                    userProfile = currentUser,
                    stories = stories,
                    isRefreshing = false,
                    onOpenStoryAtIndex = { activeStoryIndex = it },
                    onCreateStoryClick = {}
                )
                val idx = activeStoryIndex
                if (idx != null) {
                    EphemeralStoryViewer(
                        stories = stories,
                        initialIndex = idx,
                        onStorySeen = { seenStoryIds.add(it.id) },
                        onSendReply = { _, _ -> },
                        onAddNewStory = {},
                        onDismiss = { activeStoryIndex = null },
                        imageDurationMs = 60_000L,
                        videoDurationMs = 60_000L
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Horizontal circular avatar tray renders image story & video story with video badge
        composeTestRule.onNodeWithTag("stories_avatar_tray").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stories_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_your_story_item").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_item_story_img_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_item_story_vid_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_video_badge_story_vid_2").assertIsDisplayed()

        // Pause Compose test clock auto-advance before opening the auto-advancing story viewer
        // so waitForIdle() does not fast-forward through the entire story deck and auto-dismiss it.
        composeTestRule.mainClock.autoAdvance = false

        // Tap the image story avatar -> opens full-screen EphemeralStoryViewer
        composeTestRule.onNodeWithTag("story_item_story_img_1").performClick()
        composeTestRule.mainClock.advanceTimeBy(160L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("story_viewer_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_progress_bars_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_progress_segment_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_progress_segment_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_ephemeral_ttl_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("story_position_counter").assertTextContains("1/2")
        assertTrue(seenStoryIds.contains("story_img_1"))

        // Tap second progress segment -> switches to ephemeral video story & reveals mute toggle
        composeTestRule.onNodeWithTag("story_progress_segment_1").performClick()
        composeTestRule.mainClock.advanceTimeBy(160L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("story_position_counter").assertTextContains("2/2")
        composeTestRule.onNodeWithTag("story_mute_toggle_button").assertIsDisplayed()
        assertTrue(seenStoryIds.contains("story_vid_2"))

        // Tap pause button -> displays PAUSED indicator
        composeTestRule.onNodeWithTag("story_pause_resume_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(64L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("story_paused_badge").assertIsDisplayed()

        // Tap close button -> dismisses full-screen story viewer
        composeTestRule.onNodeWithTag("story_close_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(64L)
        composeTestRule.waitForIdle()
        assertEquals(null, activeStoryIndex)
        composeTestRule.onNodeWithTag("story_viewer_dialog").assertDoesNotExist()
        composeTestRule.mainClock.autoAdvance = true
    }
}

