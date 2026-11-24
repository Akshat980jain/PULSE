package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import com.example.database.toDatabasePost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pulse", appName)
    }

    @Test
    fun `bottom navigation order has Messages in middle and Loops and Explore in place`() {
        val order = PulseDestination.entries.map { it.route }
        assertEquals(listOf("feed", "loops", "messages", "explore", "profile"), order)
    }

    @Test
    fun `mainActivity launches with choice page for existing or new user, redirects to login page, and renders feed and tabs`() {
        composeTestRule.waitForIdle()
        // Fresh install shows the Choice Page (WelcomeChoiceScreen) instead of auto-logging into an existing account
        composeTestRule.onNodeWithTag("welcome_choice_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_app_logo_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("choice_existing_user_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("choice_new_user_button").assertIsDisplayed()

        // Tapping New User takes the user to the Login / Create Account page
        composeTestRule.onNodeWithTag("choice_new_user_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_app_logo_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_display_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_username_input").assertIsDisplayed()

        // Navigate back to the Choice Page via the "Back" button and tap Existing User -> enters Feed directly without requiring login
        composeTestRule.onNodeWithTag("auth_back_to_choice_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_choice_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("choice_existing_user_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pulse_brand_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feed_mode_dropdown_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("activity_notifications_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("direct_messages_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("theme_toggle_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("feed_mode_saved").assertDoesNotExist()
        composeTestRule.onNodeWithTag("refresh_feed_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("feed_swipe_refresh").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feed_lazy_column").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stories_avatar_tray").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stories_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_feed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_loops").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_messages").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_explore").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_profile").assertIsDisplayed()

        // Open the Feed mode dropdown adjacent to Pulse and switch between Following and Curated
        composeTestRule.onNodeWithTag("feed_mode_dropdown_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("feed_mode_curated").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feed_mode_following").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feed_mode_following").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("feed_mode_dropdown_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("feed_mode_curated").performClick()
        composeTestRule.waitForIdle()

        // Left-of-Feed swipe (swipeRight on Home page) opens redesigned 3-step Create Studio (Page 0)
        composeTestRule.onNodeWithTag("main_horizontal_pager").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_studio_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_stepper_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_page_media").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_empty_media_stage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_camera_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_gallery_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_camerax_toggle_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_picker_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("photo_picker_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("video_picker_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("darkroom_sliders_panel").assertDoesNotExist()
        composeTestRule.onNodeWithTag("exif_frame_stamp_panel").assertDoesNotExist()

        // Return to Feed from Create Studio
        composeTestRule.onNodeWithTag("studio_back_to_feed_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pulse_brand_header").assertIsDisplayed()

        // Navigate to Loops (2nd option in bottom bar) and verify corner mute button is hidden while playing
        composeTestRule.onNodeWithTag("nav_tab_loops").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("video_mute_toggle_button").assertDoesNotExist()

        // Navigate to Messages (3rd / middle option in bottom bar) and verify MessagesScreen is displayed
        composeTestRule.onNodeWithTag("nav_tab_messages").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("messages_screen").assertIsDisplayed()

        // Navigate to Explore (4th option in bottom bar) and verify Search Bar & Trending Hubs row
        composeTestRule.onNodeWithTag("nav_tab_explore").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("explore_search_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("explore_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("explore_quick_query_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("explore_trending_hubs_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("explore_staggered_grid").assertIsDisplayed()

        // Navigate to Profile and verify Theme toggle button next to Log Out button, Insights button, avatar, bio, post count, and photo grid
        composeTestRule.onNodeWithTag("nav_tab_profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_logout_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_dedicated_logout_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("profile_avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_bio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_stats_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_post_count").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_followers_count").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_following_count").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_insights_button").assertExists()
        composeTestRule.onNodeWithTag("profile_highlights_row").assertExists()
        composeTestRule.onNodeWithTag("profile_posts_grid").assertIsDisplayed()

        // Clicking the top Log Out button opens the AuthScreen
        composeTestRule.onNodeWithTag("profile_logout_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
    }

    @Test
    fun `explore search query filters posts by tags and user handles`() {
        val samplePosts = listOf(
            com.example.data.model.PostEntity(
                id = "p1",
                authorId = "u1",
                authorUsername = "sora.studio",
                authorDisplayName = "Sora Takahashi",
                caption = "Midnight rain in Kyoto",
                location = "Kyoto",
                category = "Street",
                tagsCsv = "street,neon,kyoto",
                likeCount = 12,
                commentCount = 2
            ),
            com.example.data.model.PostEntity(
                id = "p2",
                authorId = "u2",
                authorUsername = "elena.voss",
                authorDisplayName = "Elena Voss",
                caption = "Concrete brutalist geometry",
                location = "Berlin",
                category = "Architecture",
                tagsCsv = "architecture,monochrome,minimal",
                likeCount = 24,
                commentCount = 4
            )
        )

        val byTag = com.example.ui.screens.filterPostsByTagOrHandle(samplePosts, "#monochrome")
        assertEquals(1, byTag.size)
        assertEquals("p2", byTag.first().id)

        val byHandle = com.example.ui.screens.filterPostsByTagOrHandle(samplePosts, "@sora.studio")
        assertEquals(1, byHandle.size)
        assertEquals("p1", byHandle.first().id)
    }

    @Test
    fun `supabase media url resolver handles full public urls and json-wrapped urls`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = com.example.data.remote.SupabaseClient.getImageLoader(context)
        org.junit.Assert.assertNotNull(loader)

        val fullUrl = "https://example.supabase.co/storage/v1/object/public/avatars/user 1.png"
        assertEquals(
            "https://example.supabase.co/storage/v1/object/public/avatars/user%201.png",
            com.example.data.remote.SupabaseClient.resolveSupabaseMediaUrl(fullUrl)
        )

        val jsonWrapped = "{\"publicUrl\":\"https://example.supabase.co/storage/v1/object/public/posts/photo.jpg\"}"
        assertEquals(
            "https://example.supabase.co/storage/v1/object/public/posts/photo.jpg",
            com.example.data.remote.SupabaseClient.resolveSupabaseMediaUrl(jsonWrapped, "posts")
        )
    }

    @Test
    fun `sharePostExternally launches native Android system share chooser intent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val samplePost = com.example.data.model.PostEntity(
            id = "share_test_1",
            authorId = "u1",
            authorUsername = "sora.studio",
            authorDisplayName = "Sora Takahashi",
            caption = "Midnight rain in Kyoto",
            location = "Kyoto",
            tagsCsv = "street,kyoto"
        )

        com.example.ui.components.sharePostExternally(context, samplePost)

        val startedIntent = org.robolectric.Shadows.shadowOf(
            context as android.app.Application
        ).nextStartedActivity
        org.junit.Assert.assertNotNull(startedIntent)
        assertEquals(android.content.Intent.ACTION_CHOOSER, startedIntent.action)
        @Suppress("DEPRECATION")
        val targetIntent = startedIntent.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)
        org.junit.Assert.assertNotNull(targetIntent)
        assertEquals(android.content.Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("text/plain", targetIntent?.type)
    }

    @Test
    fun `room database persists Post, User, and Story entities and FeedViewModel exposes cached feed state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = db.pulseFeedDao()

        val testUser = com.example.database.User(
            id = "user_kyoto_1",
            username = "sora.studio",
            displayName = "Sora Takahashi",
            roleTag = "Street Photographer",
            bio = "Midnight rain & analog grain",
            location = "Kyoto, Japan",
            followersCount = 1420
        )
        val testPost = com.example.database.Post(
            id = "post_kyoto_1",
            authorId = "user_kyoto_1",
            authorUsername = "sora.studio",
            authorDisplayName = "Sora Takahashi",
            caption = "Neon reflections on Pontocho Alley",
            location = "Kyoto",
            category = "Street",
            tagsCsv = "kyoto,neon,analog",
            likeCount = 48
        )
        val testStory = com.example.database.Story(
            id = "story_kyoto_1",
            authorId = "user_kyoto_1",
            authorUsername = "sora.studio",
            authorDisplayName = "Sora Takahashi",
            captionOverlay = "Behind the lens in Gion",
            locationTag = "Gion, Kyoto"
        )

        runBlocking {
            dao.insertUsers(listOf(testUser))
            dao.insertPosts(listOf(testPost))
            dao.insertStories(listOf(testStory))

            val storedUsers = dao.observeUsers().first()
            val storedPosts = dao.observePosts().first()
            val storedStories = dao.observeStories().first()

            assertEquals(1, storedUsers.size)
            assertEquals("sora.studio", storedUsers.first().username)
            assertEquals(1, storedPosts.size)
            assertEquals("Neon reflections on Pontocho Alley", storedPosts.first().caption)
            assertEquals(1, storedStories.size)
            assertEquals("Behind the lens in Gion", storedStories.first().captionOverlay)
        }

        val feedViewModel = com.example.ui.viewmodel.FeedViewModel(dao)

        runBlocking {
            feedViewModel.toggleLikePostSuspend(testPost, userId = "user_kyoto_1")

            val updatedPosts = dao.observePosts().first()
            val storedLikes = dao.observePostLikes().first()
            val storedInteractions = dao.observeUserInteractions().first()
            assertEquals(true, updatedPosts.first().isLiked)
            assertEquals(49, updatedPosts.first().likeCount)
            assertEquals("user_kyoto_1", updatedPosts.first().likedByUserIdsCsv)
            assertEquals(1, storedLikes.size)
            assertEquals("post_kyoto_1", storedLikes.first().postId)
            assertEquals("user_kyoto_1", storedLikes.first().userId)
            assertEquals(1, storedInteractions.size)
            assertEquals("LIKE", storedInteractions.first().interactionType)

            // Toggle unlike and verify interaction is removed and count decrements
            feedViewModel.toggleLikePostSuspend(updatedPosts.first(), userId = "user_kyoto_1")

            val unlikedPosts = dao.observePosts().first()
            val remainingLikes = dao.observePostLikes().first()
            val remainingInteractions = dao.observeUserInteractions().first()
            assertEquals(false, unlikedPosts.first().isLiked)
            assertEquals(48, unlikedPosts.first().likeCount)
            assertEquals(0, remainingLikes.size)
            assertEquals(0, remainingInteractions.size)

            // Post a comment on the feed photo and verify Comment entity and commentCount update
            feedViewModel.addCommentSuspend(
                post = unlikedPosts.first(),
                text = "Love the Kodachrome reflections in the rain!",
                authorUsername = "sora.studio",
                authorDisplayName = "Sora Takahashi"
            )
            val commentedPosts = dao.observePosts().first()
            val postComments = dao.observeCommentsForPost("post_kyoto_1").first()
            assertEquals(1, commentedPosts.first().commentCount)
            assertEquals(1, postComments.size)
            assertEquals("Love the Kodachrome reflections in the rain!", postComments.first().text)
            assertEquals("sora.studio", postComments.first().authorUsername)
        }

        db.close()
    }

    @Test
    fun `darkroomProcessingService applies brightness, contrast, and saturation filters and bakes output file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceBitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
        sourceBitmap.eraseColor(android.graphics.Color.rgb(120, 100, 80))

        val adjustments = com.example.data.model.DarkroomAdjustments(
            brightness = 0.2f,
            contrast = 1.25f,
            saturation = 1.4f,
            exposure = 0.1f,
            warmth = 0.15f,
            filmGrain = 0.1f,
            vignette = 0.2f
        )

        val filteredBitmap = com.example.data.service.DarkroomProcessingService.applyFiltersToBitmap(
            source = sourceBitmap,
            preset = com.example.data.model.FilmPreset.KODACHROME_64,
            adjustments = adjustments
        )
        org.junit.Assert.assertNotNull(filteredBitmap)
        assertEquals(64, filteredBitmap.width)
        assertEquals(64, filteredBitmap.height)

        runBlocking {
            val bakedFile = com.example.data.service.DarkroomProcessingService.processAndBakePhoto(
                context = context,
                sourceUri = null,
                fallbackResId = R.drawable.img_post_kyoto_1794021771843,
                preset = com.example.data.model.FilmPreset.KODACHROME_64,
                adjustments = adjustments
            )
            org.junit.Assert.assertNotNull(bakedFile)
            org.junit.Assert.assertTrue(bakedFile!!.exists())
            org.junit.Assert.assertTrue(bakedFile.length() > 0L)
        }
    }

    @Test
    fun `newly created post persists in Room and survives hard refresh`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pulseDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PulseDatabase::class.java
        ).allowMainThreadQueries().build()
        val feedDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()

        val repository = com.example.data.repository.PulseRepository(
            dao = pulseDb.pulseDao(),
            context = context,
            feedDao = feedDb.pulseFeedDao()
        )

        runBlocking {
            repository.createPost(
                imageRes = R.drawable.img_post_kyoto_1794021771843,
                imageUri = null,
                videoUri = null,
                carouselImageUrisCsv = "",
                carouselImageResCsv = "",
                aspectRatio = com.example.data.model.PhotoAspectRatio.PORTRAIT_4_5,
                filterPreset = com.example.data.model.FilmPreset.KODACHROME_64,
                adjustments = com.example.data.model.DarkroomAdjustments(
                    brightness = 0.15f,
                    contrast = 1.2f,
                    saturation = 1.3f
                ),
                caption = "Persistent Darkroom Test Frame #analog",
                location = "Tokyo Darkroom",
                category = "Street",
                tagsCsv = "analog,darkroom,test",
                isLoop = false,
                audioTrack = "Original Audio",
                cameraSpec = "Leica M6 · 35mm"
            )

            val postsBeforeRefresh = pulseDb.pulseDao().observeAllPosts().first()
            org.junit.Assert.assertTrue(
                postsBeforeRefresh.any { it.caption == "Persistent Darkroom Test Frame #analog" }
            )

            // Trigger a hard refresh from Supabase and verify the post still remains in Room
            repository.refreshFromSupabase()

            val postsAfterRefresh = pulseDb.pulseDao().observeAllPosts().first()
            val savedPost = postsAfterRefresh.firstOrNull {
                it.caption == "Persistent Darkroom Test Frame #analog"
            }
            org.junit.Assert.assertNotNull(savedPost)
            org.junit.Assert.assertFalse(savedPost!!.imageUri.isNullOrBlank())
        }

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `user posted custom photo stores actual photo in database instead of sample image`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pulseDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PulseDatabase::class.java
        ).allowMainThreadQueries().build()
        val feedDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()

        val repository = com.example.data.repository.PulseRepository(
            dao = pulseDb.pulseDao(),
            context = context,
            feedDao = feedDb.pulseFeedDao()
        )

        // Create a distinct custom user photo (bright green 80x80 bitmap)
        val customUserBmp = android.graphics.Bitmap.createBitmap(
            80,
            80,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        customUserBmp.eraseColor(android.graphics.Color.rgb(10, 240, 60))
        val customInputFile = java.io.File(context.cacheDir, "user_picked_test_photo.jpg")
        java.io.FileOutputStream(customInputFile).use { fos ->
            customUserBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos)
        }
        val permanentCopyUri = com.example.data.service.DarkroomProcessingService.copyUriToPermanentInternalFile(
            context = context,
            sourceUri = android.net.Uri.fromFile(customInputFile),
            isVideo = false
        )
        org.junit.Assert.assertTrue(permanentCopyUri.startsWith("file://"))

        runBlocking {
            val result = repository.createPost(
                imageRes = 0,
                imageUri = permanentCopyUri,
                videoUri = null,
                carouselImageUrisCsv = "",
                carouselImageResCsv = "",
                aspectRatio = com.example.data.model.PhotoAspectRatio.SQUARE_1_1,
                filterPreset = com.example.data.model.FilmPreset.NATURAL,
                adjustments = com.example.data.model.DarkroomAdjustments(),
                caption = "Actual User Photo Post Test",
                location = "User Device",
                category = "Street",
                tagsCsv = "custom,userphoto",
                isLoop = false,
                audioTrack = "Original Audio",
                cameraSpec = "Device Camera"
            )
            val createdPost = result.getOrThrow()
            assertEquals(0, createdPost.imageRes)
            org.junit.Assert.assertFalse(createdPost.imageUri.isNullOrBlank())

            // Decode the stored imageUri and verify it is the user's 80x80 green photo, NOT a sample drawable
            val decodedStoredBmp = com.example.data.service.DarkroomProcessingService.decodeSourceBitmap(
                context = context,
                sourceUri = createdPost.imageUri,
                fallbackResId = 0
            )
            org.junit.Assert.assertNotNull(decodedStoredBmp)
            assertEquals(80, decodedStoredBmp!!.width)
            assertEquals(80, decodedStoredBmp.height)
            val centerPixel = decodedStoredBmp.getPixel(40, 40)
            org.junit.Assert.assertTrue(android.graphics.Color.green(centerPixel) > 180)

            // Also verify data:image/jpeg;base64 URI round-trip decoding for cloud database storage
            val dataUri = com.example.data.remote.SupabaseClient.buildCompactDataUriFromLocalFile(
                context = context,
                localUriOrUrl = createdPost.imageUri,
                fallbackResId = 0
            )
            org.junit.Assert.assertNotNull(dataUri)
            org.junit.Assert.assertTrue(dataUri!!.startsWith("data:image/jpeg;base64,"))
            val decodedFromDataUri = com.example.data.service.DarkroomProcessingService.decodeSourceBitmap(
                context = context,
                sourceUri = dataUri,
                fallbackResId = 0
            )
            org.junit.Assert.assertNotNull(decodedFromDataUri)
            assertEquals(80, decodedFromDataUri!!.width)
            assertEquals(80, decodedFromDataUri.height)
        }

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `handleMediaSelectionAndUpload uploads selected device image and video to Supabase bucket and returns URL for database`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pulseDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PulseDatabase::class.java
        ).allowMainThreadQueries().build()
        val feedDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()

        val repository = com.example.data.repository.PulseRepository(
            dao = pulseDb.pulseDao(),
            context = context,
            feedDao = feedDb.pulseFeedDao()
        )

        // 1. Create a device-selected photo file and invoke handleMediaSelectionAndUploadToSupabase
        val bmp = android.graphics.Bitmap.createBitmap(72, 72, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.rgb(30, 120, 250))
        val photoFile = java.io.File(context.cacheDir, "device_selected_photo.jpg")
        java.io.FileOutputStream(photoFile).use { fos ->
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, fos)
        }
        val selectedPhotoUri = android.net.Uri.fromFile(photoFile)

        // 2. Create a device-selected video file
        val videoFile = java.io.File(context.cacheDir, "device_selected_video.mp4")
        videoFile.writeBytes(ByteArray(256) { (it % 127).toByte() })
        val selectedVideoUri = android.net.Uri.fromFile(videoFile)

        runBlocking {
            val directClientPhotoUrl = com.example.data.remote.SupabaseClient.handleMediaSelectionAndUploadToSupabase(
                context = context,
                selectedMediaUri = selectedPhotoUri,
                bucketName = "posts",
                isVideo = false
            )
            org.junit.Assert.assertNotNull(directClientPhotoUrl)
            org.junit.Assert.assertTrue(directClientPhotoUrl!!.isNotBlank())

            val repoPhotoResult = repository.handleMediaSelectionAndUpload(
                selectedUri = selectedPhotoUri,
                isVideo = false,
                bucketName = "posts",
                filterPreset = com.example.data.model.FilmPreset.KODACHROME_64
            )
            org.junit.Assert.assertTrue(repoPhotoResult.isSuccess)
            val returnedPhotoUrl = repoPhotoResult.getOrThrow()
            org.junit.Assert.assertTrue(returnedPhotoUrl.isNotBlank())

            val repoVideoResult = repository.handleMediaSelectionAndUpload(
                selectedUri = selectedVideoUri,
                isVideo = true,
                bucketName = "videos"
            )
            org.junit.Assert.assertTrue(repoVideoResult.isSuccess)
            val returnedVideoUrl = repoVideoResult.getOrThrow()
            org.junit.Assert.assertTrue(returnedVideoUrl.isNotBlank())

            // Save a post using the returned URL and verify it is stored in the database with imageRes = 0
            val savedPost = repository.createPost(
                imageRes = R.drawable.img_post_kyoto_1794021771843, // Even if caller passes a sample resId, custom media forces imageRes = 0
                imageUri = returnedPhotoUrl,
                videoUri = null,
                carouselImageUrisCsv = "",
                carouselImageResCsv = "",
                aspectRatio = com.example.data.model.PhotoAspectRatio.PORTRAIT_4_5,
                filterPreset = com.example.data.model.FilmPreset.NATURAL,
                adjustments = com.example.data.model.DarkroomAdjustments(),
                caption = "Uploaded to Supabase Storage bucket",
                location = "Supabase Cloud",
                category = "Street",
                tagsCsv = "supabase,storage",
                isLoop = false,
                audioTrack = "Original Audio",
                cameraSpec = "35mm · f/1.8"
            ).getOrThrow()

            assertEquals(0, savedPost.imageRes)
            org.junit.Assert.assertFalse(savedPost.imageUri.isNullOrBlank())
        }

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `built-in phone camera helper handles CAMERA and WRITE_EXTERNAL_STORAGE permissions and TakePicture Uri contract`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify permission arrays for API <= 28 (includes WRITE_EXTERNAL_STORAGE) and API >= 29
        val legacyPerms = com.example.ui.screens.BuiltInPhoneCameraHelper.getRequiredCameraAndStoragePermissions(
            sdkInt = android.os.Build.VERSION_CODES.P
        )
        org.junit.Assert.assertTrue(legacyPerms.contains(android.Manifest.permission.CAMERA))
        org.junit.Assert.assertTrue(legacyPerms.contains(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))

        val scopedPerms = com.example.ui.screens.BuiltInPhoneCameraHelper.getRequiredCameraAndStoragePermissions(
            sdkInt = android.os.Build.VERSION_CODES.TIRAMISU
        )
        org.junit.Assert.assertTrue(scopedPerms.contains(android.Manifest.permission.CAMERA))

        // 2. Verify evaluatePermissionGrantMap for CAMERA and WRITE_EXTERNAL_STORAGE
        val grantedOnLegacy = com.example.ui.screens.BuiltInPhoneCameraHelper.evaluatePermissionGrantMap(
            permissions = mapOf(
                android.Manifest.permission.CAMERA to true,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE to true
            ),
            context = context,
            sdkInt = android.os.Build.VERSION_CODES.P
        )
        org.junit.Assert.assertTrue(grantedOnLegacy)

        val deniedStorageOnLegacy = com.example.ui.screens.BuiltInPhoneCameraHelper.evaluatePermissionGrantMap(
            permissions = mapOf(
                android.Manifest.permission.CAMERA to true,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE to false
            ),
            context = context,
            sdkInt = android.os.Build.VERSION_CODES.P
        )
        org.junit.Assert.assertFalse(deniedStorageOnLegacy)

        // 3. Verify ActivityResultContracts.TakePicture intent creation with FileProvider Uri
        val photoTarget = com.example.ui.screens.BuiltInPhoneCameraHelper.createCaptureTarget(
            context = context,
            isVideo = false
        )
        org.junit.Assert.assertTrue(photoTarget.file.name.endsWith(".jpg"))
        val takePictureContract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
        val takePictureIntent = takePictureContract.createIntent(context, photoTarget.contentUri)
        assertEquals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE, takePictureIntent.action)
        @Suppress("DEPRECATION")
        val extraOutputUri = takePictureIntent.getParcelableExtra<android.net.Uri>(android.provider.MediaStore.EXTRA_OUTPUT)
        assertEquals(photoTarget.contentUri, extraOutputUri)
    }

    @Test
    fun `room feed cache schema supports category queries, cache freshness TTL, and offline fallback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pulseDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PulseDatabase::class.java
        ).allowMainThreadQueries().build()
        val feedDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()

        val pulseDao = pulseDb.pulseDao()
        val feedDao = feedDb.pulseFeedDao()
        val repository = com.example.data.repository.PulseRepository(
            dao = pulseDao,
            context = context,
            feedDao = feedDao
        )

        val nowMs = System.currentTimeMillis()
        val cachedPosts = listOf(
            com.example.data.model.PostEntity(
                id = "offline_cache_1",
                authorId = "u_arch",
                authorUsername = "elena.voss",
                authorDisplayName = "Elena Voss",
                caption = "Cached Architecture Frame",
                category = "Architecture",
                timestampMs = nowMs - 1000L,
                cachedAtMs = nowMs
            ),
            com.example.data.model.PostEntity(
                id = "offline_cache_2",
                authorId = "u_street",
                authorUsername = "sora.studio",
                authorDisplayName = "Sora Takahashi",
                caption = "Cached Street Frame",
                category = "Street",
                timestampMs = nowMs,
                cachedAtMs = nowMs
            )
        )

        runBlocking {
            pulseDao.insertPosts(cachedPosts)
            feedDao.insertPosts(cachedPosts.map { it.toDatabasePost() })

            assertEquals(2, pulseDao.getCachedPostCount())
            assertEquals(2, feedDao.getCachedPostCount())
            assertEquals(nowMs, pulseDao.getLatestCacheTimestampMs())
            assertEquals(nowMs, feedDao.getLatestCacheTimestampMs())
            org.junit.Assert.assertTrue(repository.isFeedCacheFresh())

            val streetPosts = feedDao.observePostsByCategory("Street").first()
            assertEquals(1, streetPosts.size)
            assertEquals("offline_cache_2", streetPosts.first().id)

            // Calling refreshFromSupabase with forceRefresh = false returns cached count immediately without wiping cache
            val cacheHitResult = repository.refreshFromSupabase(forceRefresh = false)
            org.junit.Assert.assertTrue(cacheHitResult.isSuccess)
            assertEquals(2, cacheHitResult.getOrThrow())
            assertEquals(2, repository.getCachedPostsSnapshot().size)
        }

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `room database upgrades cleanly from version 9 and 10 and falls back destructively from older versions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (oldVersion in listOf(8, 9, 10)) {
            val dbName = "test_upgrade_v${oldVersion}_to_v11.db"
            context.deleteDatabase(dbName)

            val rawDbFile = context.getDatabasePath(dbName)
            rawDbFile.parentFile?.mkdirs()
            val sqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(rawDbFile, null)
            sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS `posts` (`id` TEXT NOT NULL PRIMARY KEY)")
            sqliteDb.version = oldVersion
            sqliteDb.close()

            val upgradedDb = androidx.room.Room.databaseBuilder(
                context,
                com.example.data.local.PulseDatabase::class.java,
                dbName
            )
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .allowMainThreadQueries()
                .build()

            runBlocking {
                assertEquals(0, upgradedDb.pulseDao().getCachedPostCount())
            }
            upgradedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `explore masonry aspect ratio calculator produces varied Pinterest-like heights across staggered tiles`() {
        val samplePost = com.example.data.model.PostEntity(
            id = "masonry_test_1",
            authorId = "u1",
            authorUsername = "sora.studio",
            authorDisplayName = "Sora Takahashi",
            caption = "Staggered Masonry Pin",
            aspectRatioName = com.example.data.model.PhotoAspectRatio.PORTRAIT_4_5.name
        )

        val ratio0 = com.example.ui.screens.calculateMasonryAspectRatio(samplePost, 0)
        val ratio1 = com.example.ui.screens.calculateMasonryAspectRatio(samplePost, 1)
        val ratio2 = com.example.ui.screens.calculateMasonryAspectRatio(samplePost, 2)
        val ratio3 = com.example.ui.screens.calculateMasonryAspectRatio(samplePost, 3)

        org.junit.Assert.assertNotEquals(ratio0, ratio1, 0.01f)
        org.junit.Assert.assertNotEquals(ratio1, ratio2, 0.01f)
        org.junit.Assert.assertNotEquals(ratio2, ratio3, 0.01f)
    }

    @Test
    fun `pinArchivedStoriesToHighlight pins archived stories to user profile highlights and unpinStoryHighlight removes them`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pulseDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PulseDatabase::class.java
        ).allowMainThreadQueries().build()
        val feedDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()

        val pulseDao = pulseDb.pulseDao()
        val feedDao = feedDb.pulseFeedDao()
        val repository = com.example.data.repository.PulseRepository(pulseDao, context, feedDao)

        val archivedStory1 = com.example.data.model.StoryEntity(
            id = "arch_story_1",
            authorId = "u_studio",
            authorUsername = "studio.creator",
            authorDisplayName = "Studio Creator",
            captionOverlay = "Leica M6 roll in Shinjuku",
            locationTag = "Shinjuku, Tokyo",
            isHighlight = false,
            highlightTitle = "",
            createdAtLabel = "2d ago"
        )
        val archivedStory2 = com.example.data.model.StoryEntity(
            id = "arch_story_2",
            authorId = "u_studio",
            authorUsername = "studio.creator",
            authorDisplayName = "Studio Creator",
            captionOverlay = "Darkroom contact sheet #12",
            locationTag = "Tokyo Darkroom",
            isHighlight = false,
            highlightTitle = "",
            createdAtLabel = "5d ago"
        )

        runBlocking {
            repository.pinArchivedStoriesToHighlight(
                stories = listOf(archivedStory1, archivedStory2),
                highlightTitle = "Analog 35mm"
            )

            val storedStories = pulseDao.observeStories().first()
            val pinnedStories = storedStories.filter { it.isHighlight && it.highlightTitle == "Analog 35mm" }
            assertEquals(2, pinnedStories.size)
            assertEquals("Pinned", pinnedStories.first().createdAtLabel)

            // Unpin one story from the highlight reel
            repository.unpinStoryHighlight(pinnedStories.first())
            val afterUnpin = pulseDao.observeStories().first()
            val remainingPinned = afterUnpin.filter { it.isHighlight && it.highlightTitle == "Analog 35mm" }
            assertEquals(1, remainingPinned.size)
        }

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `carousel upload preserves up to 10 mixed photo and video items including base64 data URIs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pulseDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PulseDatabase::class.java
        ).allowMainThreadQueries().build()
        val feedDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()

        val pulseDao = pulseDb.pulseDao()
        val feedDao = feedDb.pulseFeedDao()
        val repository = com.example.data.repository.PulseRepository(pulseDao, context, feedDao)

        val tenCarouselItems = listOf(
            "data:image/jpeg;base64,frame1base64payload==",
            "https://cdn.pulse.studio/videos/clip2.mp4",
            "data:image/jpeg;base64,frame3base64payload==",
            "https://cdn.pulse.studio/photos/photo4.jpg",
            "https://cdn.pulse.studio/videos/clip5.mp4",
            "https://cdn.pulse.studio/photos/photo6.jpg",
            "https://cdn.pulse.studio/photos/photo7.jpg",
            "https://cdn.pulse.studio/videos/clip8.mp4",
            "https://cdn.pulse.studio/photos/photo9.jpg",
            "https://cdn.pulse.studio/photos/photo10.jpg"
        )
        val joined = com.example.data.model.joinCarouselUris(tenCarouselItems)
        val parsed = com.example.data.model.splitCarouselUris(joined)
        assertEquals(10, parsed.size)
        assertEquals(tenCarouselItems, parsed)

        runBlocking {
            val createResult = repository.createPost(
                imageRes = 0,
                imageUri = tenCarouselItems.first(),
                videoUri = tenCarouselItems[1],
                carouselImageUrisCsv = joined,
                carouselImageResCsv = "",
                aspectRatio = com.example.data.model.PhotoAspectRatio.PORTRAIT_4_5,
                filterPreset = com.example.data.model.FilmPreset.NATURAL,
                caption = "10-frame mixed photo & video carousel roll",
                location = "Tokyo, Japan",
                category = "Street",
                tagsCsv = "carousel,analog,video",
                isLoop = false,
                audioTrack = "Original Audio",
                cameraSpec = "Leica M6 · 35mm"
            )
            org.junit.Assert.assertTrue(createResult.isSuccess)
            val createdPost = createResult.getOrThrow()
            assertEquals(10, createdPost.carouselFrames.size)
            assertEquals("https://cdn.pulse.studio/videos/clip2.mp4", createdPost.carouselFrames[1].second)
            assertEquals("https://cdn.pulse.studio/photos/photo10.jpg", createdPost.carouselFrames[9].second)

            val cachedPosts = pulseDao.getCachedPostsSnapshot()
            val storedPost = cachedPosts.first { it.id == createdPost.id }
            assertEquals(10, storedPost.carouselFrames.size)
        }

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `local notification manager dispatches notifications for new likes, comments, and incoming messages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = com.example.data.service.PulseNotificationManager.getInstance(context)
        notificationManager.clearHistory()

        val pulseDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PulseDatabase::class.java
        ).allowMainThreadQueries().build()
        val feedDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()

        val pulseDao = pulseDb.pulseDao()
        val feedDao = feedDb.pulseFeedDao()
        val repository = com.example.data.repository.PulseRepository(pulseDao, context, feedDao)

        val testPost = com.example.data.model.PostEntity(
            id = "notif_post_1",
            authorId = "u_creator",
            authorUsername = "sora.studio",
            authorDisplayName = "Sora Takahashi",
            caption = "Nocturne in Kyoto",
            likeCount = 4,
            isLiked = false,
            commentCount = 1
        )

        runBlocking {
            pulseDao.insertPost(testPost)
            feedDao.insertPost(testPost.toDatabasePost())

            // 1. Trigger a like notification
            repository.toggleLikePost(testPost)

            // 2. Trigger a comment notification
            repository.addComment(testPost, "Incredible shadow detail on Portra!")

            // 3. Trigger an incoming direct message notification
            repository.receiveIncomingDirectMessage(
                participantId = "u_elena",
                participantUsername = "elena.voss",
                participantDisplayName = "Elena Voss",
                participantAvatarRes = R.drawable.img_post_portrait_1794021773202,
                text = "Hey! Are you shooting in Berlin this weekend?"
            )
        }

        val dispatched = notificationManager.dispatchedNotifications.value
        assertEquals(3, dispatched.size)
        val channels = dispatched.map { it.channelId }.toSet()
        org.junit.Assert.assertTrue(channels.contains(com.example.data.service.PulseNotificationManager.CHANNEL_LIKES))
        org.junit.Assert.assertTrue(channels.contains(com.example.data.service.PulseNotificationManager.CHANNEL_COMMENTS))
        org.junit.Assert.assertTrue(channels.contains(com.example.data.service.PulseNotificationManager.CHANNEL_MESSAGES))

        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNotificationManager = org.robolectric.Shadows.shadowOf(systemNotificationManager)
        org.junit.Assert.assertTrue(shadowNotificationManager.allNotifications.size >= 3)

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `ten local photo and video files in carousel upload immediately without blocking and existing user session persists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pulseDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PulseDatabase::class.java
        ).allowMainThreadQueries().build()
        val feedDb = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.database.PulseFeedDatabase::class.java
        ).allowMainThreadQueries().build()

        val pulseDao = pulseDb.pulseDao()
        val feedDao = feedDb.pulseFeedDao()
        val repository = com.example.data.repository.PulseRepository(pulseDao, context, feedDao)

        // Verify Existing User continuation sets a persistent session so user is not required to log in
        val session = repository.continueAsExistingUser()
        org.junit.Assert.assertTrue(session.userId.isNotBlank())
        org.junit.Assert.assertNotNull(repository.session.value)

        // Create 10 local photo and video files to simulate selecting 10 items from device picker
        val localUris = (1..10).map { idx ->
            if (idx % 3 == 0) {
                val videoFile = java.io.File(context.cacheDir, "carousel_test_video_$idx.mp4")
                videoFile.writeBytes(ByteArray(512) { (it % 120).toByte() })
                android.net.Uri.fromFile(videoFile).toString()
            } else {
                val bmp = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.rgb(20 * idx, 100, 180))
                val imgFile = java.io.File(context.cacheDir, "carousel_test_photo_$idx.jpg")
                java.io.FileOutputStream(imgFile).use { fos ->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
                }
                android.net.Uri.fromFile(imgFile).toString()
            }
        }

        val joinedLocalUris = com.example.data.model.joinCarouselUris(localUris)
        runBlocking {
            val startMs = System.currentTimeMillis()
            val result = repository.createPost(
                imageRes = 0,
                imageUri = localUris.first(),
                videoUri = localUris[2],
                carouselImageUrisCsv = joinedLocalUris,
                carouselImageResCsv = "",
                aspectRatio = com.example.data.model.PhotoAspectRatio.PORTRAIT_4_5,
                filterPreset = com.example.data.model.FilmPreset.KODACHROME_64,
                caption = "10 local photos and videos fast upload test",
                location = "Tokyo Studio",
                category = "Street",
                tagsCsv = "carousel,fast,ten",
                isLoop = false,
                audioTrack = "Original Audio",
                cameraSpec = "35mm · f/1.8"
            )
            val elapsedMs = System.currentTimeMillis() - startMs
            org.junit.Assert.assertTrue(result.isSuccess)
            val post = result.getOrThrow()
            assertEquals(10, post.carouselFrames.size)
            org.junit.Assert.assertTrue("Expected instant carousel creation (< 3000ms), took ${elapsedMs}ms", elapsedMs < 3000L)
        }

        pulseDb.close()
        feedDb.close()
    }

    @Test
    fun `formatEphemeralRemainingLabel computes 24h TTL countdown and highlight labels`() {
        val nowMs = 1_750_000_000_000L
        val sixHoursAgo = nowMs - (6L * 60L * 60L * 1000L)
        val freshStory = com.example.data.model.StoryEntity(
            id = "s_fresh",
            authorId = "u1",
            authorUsername = "sora.studio",
            authorDisplayName = "Sora Takahashi",
            captionOverlay = "Fresh frame",
            timestampMs = sixHoursAgo,
            isHighlight = false
        )
        assertEquals("18h left", com.example.ui.components.formatEphemeralRemainingLabel(freshStory, nowMs))

        val expiringSoonStory = freshStory.copy(
            id = "s_soon",
            timestampMs = nowMs - (23L * 60L * 60L * 1000L + 30L * 60L * 1000L)
        )
        assertEquals("<1h left", com.example.ui.components.formatEphemeralRemainingLabel(expiringSoonStory, nowMs))

        val highlightStory = freshStory.copy(id = "s_hl", isHighlight = true)
        assertEquals("Highlight", com.example.ui.components.formatEphemeralRemainingLabel(highlightStory, nowMs))
    }
}


