package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PulseDatabase
import com.example.data.model.FilmPreset
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.PostEntity
import com.example.data.model.STUDIO_AUTOSAVE_DRAFT_ID
import com.example.data.model.StudioDraftEntity
import com.example.data.model.StudioPublishTarget
import com.example.data.repository.PulseRepository
import com.example.database.PulseFeedDatabase
import com.example.ui.screens.CreateStudioScreen
import com.example.ui.screens.LoopsScreen
import com.example.ui.screens.StudioCreationStep
import com.example.ui.theme.PulseTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StudioDraftsAndLoopsRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `room persists auto-save and manual studio drafts, cleans up autosave on promote, and deletes cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pulseDb = Room.inMemoryDatabaseBuilder(context, PulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val feedDb = Room.inMemoryDatabaseBuilder(context, PulseFeedDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repository = PulseRepository(pulseDb.pulseDao(), context, feedDb.pulseFeedDao())

        runBlocking {
            // 1. Save an auto-save draft
            val autoDraft = StudioDraftEntity(
                id = STUDIO_AUTOSAVE_DRAFT_ID,
                targetName = StudioPublishTarget.LOOP_REEL.name,
                currentStepName = StudioCreationStep.EDIT.name,
                videoUri = "https://cdn.pulse.studio/videos/night_run.mp4",
                filterPresetName = FilmPreset.CINESTILL_800T.name,
                brightness = 0.15f,
                contrast = 1.2f,
                saturation = 1.15f,
                exposure = 0.1f,
                warmth = -0.2f,
                filmGrain = 0.35f,
                vignette = 0.4f,
                hasExifFrameBorder = true,
                exifFrameStyle = "DARKROOM",
                caption = "Midnight rain sprint",
                location = "Shibuya",
                audioTrack = "Neon Synth Loop",
                isAutoSave = true
            )
            repository.saveStudioDraft(autoDraft)

            val afterAutoSave = repository.studioDrafts.first()
            assertEquals(1, afterAutoSave.size)
            assertEquals(STUDIO_AUTOSAVE_DRAFT_ID, afterAutoSave.first().id)
            assertEquals("Midnight rain sprint", afterAutoSave.first().caption)
            assertEquals(FilmPreset.CINESTILL_800T, afterAutoSave.first().filterPreset)
            assertEquals(0.35f, afterAutoSave.first().filmGrain, 0.001f)

            // 2. Promote to a manual saved draft -> replaces transient auto-save entry
            val manualDraft = autoDraft.copy(
                id = "draft_manual_1",
                currentStepName = StudioCreationStep.DETAILS.name,
                isAutoSave = false
            )
            repository.saveStudioDraft(manualDraft)

            val afterManualSave = repository.studioDrafts.first()
            assertEquals(1, afterManualSave.size)
            assertEquals("draft_manual_1", afterManualSave.first().id)
            assertNull(repository.getStudioDraftById(STUDIO_AUTOSAVE_DRAFT_ID))
            assertNotNull(repository.getStudioDraftById("draft_manual_1"))

            // 3. Delete manual draft
            repository.deleteStudioDraft("draft_manual_1")
            assertTrue(repository.studioDrafts.first().isEmpty())
        }

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `studio drafts tray displays saved Room drafts, resumes full state at saved step, and deletes on tap`() {
        val savedDraft = StudioDraftEntity(
            id = "draft_resume_1",
            targetName = StudioPublishTarget.LOOP_REEL.name,
            currentStepName = StudioCreationStep.DETAILS.name,
            videoUri = "https://cdn.pulse.studio/videos/kyoto_clip.mp4",
            aspectRatioName = PhotoAspectRatio.PORTRAIT_4_5.name,
            filterPresetName = FilmPreset.KODACHROME_64.name,
            caption = "Resumed Kyoto short loop",
            location = "Kyoto",
            audioTrack = "Rain on Tatami",
            isAutoSave = false
        )

        var deletedDraftId: String? = null
        var manualSavedDraft: StudioDraftEntity? = null

        composeTestRule.setContent {
            PulseTheme(darkTheme = true) {
                CreateStudioScreen(
                    savedDrafts = listOf(savedDraft),
                    onSaveDraft = { draft, isManual ->
                        if (isManual) manualSavedDraft = draft
                    },
                    onDeleteDraft = { deletedDraftId = it },
                    onPublish = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.waitForIdle()
        // Step 1 shows Saved Drafts tray with the persisted draft card
        composeTestRule.onNodeWithTag("studio_drafts_tray").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_draft_card_draft_resume_1").assertIsDisplayed()

        // Tap the draft card -> resumes directly to Step 3 (DETAILS) with saved caption & audio track
        composeTestRule.onNodeWithTag("studio_draft_card_draft_resume_1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("studio_page_details").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_caption_input").performScrollTo().assertTextContains("Resumed Kyoto short loop")
        composeTestRule.onNodeWithTag("studio_audio_input").performScrollTo().assertTextContains("Rain on Tatami")

        // Header Save Draft button persists updated draft manually
        composeTestRule.onNodeWithTag("studio_save_draft_button").performClick()
        composeTestRule.waitForIdle()
        assertNotNull(manualSavedDraft)
        assertEquals("draft_resume_1", manualSavedDraft?.id)
        assertEquals("Resumed Kyoto short loop", manualSavedDraft?.caption)

        // Navigate back to Step 1 and tap delete on the draft card
        composeTestRule.onNodeWithTag("studio_step_pill_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_delete_draft_draft_resume_1").performClick()
        composeTestRule.waitForIdle()
        assertEquals("draft_resume_1", deletedDraftId)
    }

    @Test
    fun `loops screen filters out static images completely and shows clean Record a Loop empty state`() {
        val photoOnlyPosts = listOf(
            PostEntity(
                id = "photo_1",
                authorId = "u1",
                authorUsername = "sora.studio",
                authorDisplayName = "Sora Takahashi",
                imageUri = "https://cdn.pulse.studio/posts/kyoto_rain.jpg",
                videoUri = null,
                caption = "Static street photograph",
                isLoop = true // Even if flagged as loop, must NOT render in video-only Loops
            ),
            PostEntity(
                id = "photo_2",
                authorId = "u2",
                authorUsername = "elena.voss",
                authorDisplayName = "Elena Voss",
                imageUri = "https://cdn.pulse.studio/posts/berlin_concrete.png",
                videoUri = null,
                caption = "Brutalist still photo",
                isLoop = false
            )
        )

        var recordLoopClicked = false
        composeTestRule.setContent {
            PulseTheme(darkTheme = true) {
                LoopsScreen(
                    loopPosts = photoOnlyPosts,
                    creators = emptyList(),
                    onLikeToggle = {},
                    onDoubleTapLike = {},
                    onBookmarkToggle = {},
                    onOpenComments = {},
                    onSharePost = {},
                    onCreatorClick = {},
                    onMoreOptions = {},
                    onCreateLoopClick = { recordLoopClicked = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("loops_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("loops_vertical_pager").assertDoesNotExist()
        composeTestRule.onNodeWithTag("loops_empty_record_button").assertIsDisplayed().performClick()
        assertTrue(recordLoopClicked)
    }

    @Test
    fun `loops screen displays only video clips with scrub bar and interactive Audio Sheet Use Audio flow`() {
        val mixedPosts = listOf(
            PostEntity(
                id = "photo_excluded",
                authorId = "u1",
                authorUsername = "photo.only",
                authorDisplayName = "Photo Only",
                imageUri = "https://cdn.pulse.studio/posts/still.jpg",
                videoUri = null,
                caption = "Still image should not appear in Loops",
                isLoop = true
            ),
            PostEntity(
                id = "video_loop_1",
                authorId = "u2",
                authorUsername = "motion.lab",
                authorDisplayName = "Motion Lab",
                imageUri = "https://cdn.pulse.studio/posts/thumb.jpg",
                videoUri = "https://cdn.pulse.studio/videos/subway_drift.mp4",
                caption = "Late night Tokyo subway drift",
                audioTrack = "Shibuya Lo-Fi Beat",
                isLoop = true
            )
        )

        var selectedAudioForStudio: String? = null
        composeTestRule.setContent {
            PulseTheme(darkTheme = true) {
                LoopsScreen(
                    loopPosts = mixedPosts,
                    creators = emptyList(),
                    onLikeToggle = {},
                    onDoubleTapLike = {},
                    onBookmarkToggle = {},
                    onOpenComments = {},
                    onSharePost = {},
                    onCreatorClick = {},
                    onMoreOptions = {},
                    onCreateLoopClick = {},
                    onUseAudioClick = { selectedAudioForStudio = it }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("loops_empty_state").assertDoesNotExist()
        composeTestRule.onNodeWithTag("loops_vertical_pager").assertIsDisplayed()
        composeTestRule.onNodeWithTag("loops_scrub_slider").assertIsDisplayed()
        composeTestRule.onNodeWithTag("video_mute_toggle_button").assertDoesNotExist()

        // Excluded photo post must not exist in Loops
        composeTestRule.onNodeWithTag("loops_audio_pill_photo_excluded").assertDoesNotExist()

        // Tapping the audio pill opens the Audio Sheet and Use Audio passes the soundtrack name
        composeTestRule.onNodeWithTag("loops_audio_pill_video_loop_1").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("loops_audio_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("loops_use_audio_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals("Shibuya Lo-Fi Beat", selectedAudioForStudio)
    }

    @Test
    fun `migration 11 to 12 creates studio_drafts table cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_11_to_12.db"
        context.deleteDatabase(dbName)

        // 1. Seed a real PulseDatabase with an existing PostEntity, then downgrade it to a v11 state (without studio_drafts)
        val initialDb = Room.databaseBuilder(context, PulseDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            initialDb.pulseDao().insertPost(
                PostEntity(
                    id = "preserved_v11_post",
                    authorId = "u1",
                    authorUsername = "sora.studio",
                    authorDisplayName = "Sora Takahashi",
                    caption = "Survives v11 to v12 migration"
                )
            )
        }
        initialDb.close()

        val rawDbFile = context.getDatabasePath(dbName)
        val sqliteDb = SQLiteDatabase.openDatabase(rawDbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        sqliteDb.execSQL("DROP TABLE IF EXISTS `studio_drafts`")
        sqliteDb.version = 11
        sqliteDb.close()

        // 2. Re-open with MIGRATION_11_12 and NO destructive fallback to prove exact schema & data preservation
        val upgradedDb = Room.databaseBuilder(context, PulseDatabase::class.java, dbName)
            .addMigrations(PulseDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val preservedPosts = upgradedDb.pulseDao().getCachedPostsSnapshot()
            assertEquals(1, preservedPosts.size)
            assertEquals("preserved_v11_post", preservedPosts.first().id)

            val draft = StudioDraftEntity(
                id = "migrated_draft_1",
                caption = "Persisted after v12 upgrade"
            )
            upgradedDb.pulseDao().upsertStudioDraft(draft)
            val loaded = upgradedDb.pulseDao().getStudioDraftById("migrated_draft_1")
            assertNotNull(loaded)
            assertEquals("Persisted after v12 upgrade", loaded?.caption)
        }

        upgradedDb.close()
        context.deleteDatabase(dbName)
    }
}
