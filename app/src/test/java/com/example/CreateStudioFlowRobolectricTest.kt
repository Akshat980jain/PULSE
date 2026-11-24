package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.DarkroomAdjustments
import com.example.data.model.FilmPreset
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.StudioPublishTarget
import com.example.ui.screens.CreateStudioScreen
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
class CreateStudioFlowRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `studio starts empty with zero sample data and steps through Media, Edit Preview, and Details & Publish`() {
        var publishedCount = 0
        var publishedImageRes = -1
        var publishedVideoUri: String? = null
        var publishedCarouselResCsv = "sentinel"
        var publishedCaption = ""
        var publishedLocation = ""
        var publishedExifBorder = false

        composeTestRule.setContent {
            PulseTheme(darkTheme = true) {
                CreateStudioScreen(
                    onPublish = { _: StudioPublishTarget,
                                  imageRes: Int,
                                  _: String?,
                                  videoUri: String?,
                                  _: String,
                                  carouselImageResCsv: String,
                                  _: PhotoAspectRatio,
                                  _: FilmPreset,
                                  adjustments: DarkroomAdjustments,
                                  caption: String,
                                  location: String,
                                  _: String,
                                  _: String,
                                  _: String,
                                  _: String,
                                  _: String ->
                        publishedCount++
                        publishedImageRes = imageRes
                        publishedVideoUri = videoUri
                        publishedCarouselResCsv = carouselImageResCsv
                        publishedCaption = caption
                        publishedLocation = location
                        publishedExifBorder = adjustments.hasExifFrameBorder
                    }
                )
            }
        }

        // Step 1: Clean empty stage with zero sample data, Camera & Gallery cards, Next disabled
        composeTestRule.onNodeWithTag("studio_stepper_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_page_media").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_empty_media_stage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_camera_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_gallery_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_next_to_edit_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("darkroom_sliders_panel").assertDoesNotExist()
        composeTestRule.onNodeWithTag("exif_frame_stamp_panel").assertDoesNotExist()
        composeTestRule.onNodeWithTag("studio_publish_button").assertDoesNotExist()

        // Expand video URL field, enter a real clip URL, and load it to preview on Step 1
        composeTestRule.onNodeWithTag("studio_toggle_video_url_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_video_url_input")
            .performScrollTo()
            .performTextInput("https://cdn.example.com/user_clip.mp4")
        composeTestRule.onNodeWithTag("studio_load_video_url_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Step 1 now shows Live Preview and enables Next -> Edit Preview
        composeTestRule.onNodeWithTag("studio_media_preview_stage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_page1_live_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_next_to_edit_button").assertIsEnabled().performClick()
        composeTestRule.waitForIdle()

        // Step 2: Edit Preview page with compact Presets / Adjust / Frame tabs under live preview
        composeTestRule.onNodeWithTag("studio_page_edit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_page2_live_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_darkroom_tabs_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("darkroom_presets_panel").assertExists()
        composeTestRule.onNodeWithTag("darkroom_sliders_panel").assertDoesNotExist()

        // Switch to Adjust tab
        composeTestRule.onNodeWithTag("darkroom_tab_adjust").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("darkroom_sliders_panel").assertExists()
        composeTestRule.onNodeWithTag("darkroom_presets_panel").assertDoesNotExist()

        // Switch to Frame tab and enable EXIF border
        composeTestRule.onNodeWithTag("darkroom_tab_frame").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exif_frame_stamp_panel").assertExists()
        composeTestRule.onNodeWithTag("exif_frame_stamp_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_exif_frame_preview").assertExists()

        // Advance to Step 3: Details & Publish
        composeTestRule.onNodeWithTag("studio_next_to_details_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_page_details").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_final_publish_preview_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_page3_live_preview").assertIsDisplayed()

        // Ensure zero hardcoded sample text exists on Step 3
        composeTestRule.onNodeWithText("Kyoto · Studio").assertDoesNotExist()
        composeTestRule.onNodeWithText("mediumformat,analog,pulse").assertDoesNotExist()

        // Enter user caption & location and verify back/forward step persistence
        composeTestRule.onNodeWithTag("studio_caption_input")
            .performScrollTo()
            .performTextInput("Golden hour test clip")
        composeTestRule.onNodeWithTag("studio_location_input")
            .performScrollTo()
            .performTextInput("Lisbon")
        composeTestRule.waitForIdle()

        // Navigate back to Step 2 and forward again to Step 3
        composeTestRule.onNodeWithTag("studio_back_to_edit_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_page_edit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_next_to_details_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_page_details").assertIsDisplayed()

        // Publish and verify callback receives zero sample resource IDs and resets back to empty Step 1
        composeTestRule.onNodeWithTag("studio_publish_button").assertIsEnabled().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, publishedCount)
        assertEquals(0, publishedImageRes)
        assertEquals("https://cdn.example.com/user_clip.mp4", publishedVideoUri)
        assertEquals("", publishedCarouselResCsv)
        assertEquals("Golden hour test clip", publishedCaption)
        assertEquals("Lisbon", publishedLocation)
        assertTrue(publishedExifBorder)

        // Studio resets cleanly to empty Step 1
        composeTestRule.onNodeWithTag("studio_page_media").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_empty_media_stage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_next_to_edit_button").assertIsNotEnabled()
    }
}
