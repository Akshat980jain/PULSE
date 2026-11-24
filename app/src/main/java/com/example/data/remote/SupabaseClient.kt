package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import coil.ImageLoader
import com.example.BuildConfig
import com.example.R
import com.example.data.model.ActivityItemEntity
import com.example.data.model.CommentEntity
import com.example.data.model.CreatorProfileEntity
import com.example.data.model.DirectMessageEntity
import com.example.data.model.FilmPreset
import com.example.data.model.PhotoAspectRatio
import com.example.data.model.PostEntity
import com.example.data.model.StoryEntity
import com.example.data.model.SupabaseSession
import com.example.data.model.UserProfileState
import com.example.data.model.isVideoUrl
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Structured result returned after uploading a user-selected image or video file to a
 * Supabase Storage bucket (or encoding a durable database-ready fallback URL when RLS blocks bucket writes).
 */
data class SupabaseStorageUploadResult(
    val publicUrl: String,
    val bucketName: String,
    val objectPath: String,
    val localFileUri: String,
    val isCloudHosted: Boolean
)

/**
 * Singleton SupabaseClient configured strictly from BuildConfig.SUPABASE_URL and
 * BuildConfig.SUPABASE_PUBLISHABLE_KEY to handle authentication, paginated database queries,
 * video reels, and Supabase Storage URL resolution.
 */
object SupabaseClient {

    private fun sanitizeHeaderToken(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.filter { it in '\u0020'..'\u007E' }.trim()
    }

    val supabaseUrl: String by lazy {
        val rawUrl = sanitizeHeaderToken(BuildConfig.SUPABASE_URL).trimEnd('/')
        if (rawUrl.isNotEmpty() && !rawUrl.contains("YOUR_SUPABASE") && rawUrl.startsWith("http")) {
            rawUrl
        } else {
            val rawProjectId = sanitizeHeaderToken(BuildConfig.SUPABASE_PROJECT_ID)
            if (rawProjectId.isNotEmpty() && !rawProjectId.contains("YOUR_SUPABASE")) {
                "https://$rawProjectId.supabase.co"
            } else {
                ""
            }
        }
    }

    val publishableKey: String by lazy {
        val key = sanitizeHeaderToken(BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        if (key.isNotEmpty() && !key.contains("YOUR_SUPABASE")) key else ""
    }

    val openRouterApiKey: String by lazy {
        val key = sanitizeHeaderToken(BuildConfig.OPENROUTER_API_KEY)
        if (key.isNotEmpty() && !key.contains("YOUR_OPENROUTER")) key else ""
    }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotEmpty() && publishableKey.isNotEmpty()

    @Volatile
    private var currentAccessToken: String? = null

    @Volatile
    private var sharedCoilImageLoader: ImageLoader? = null

    fun setAccessToken(token: String?) {
        currentAccessToken = sanitizeHeaderToken(token).takeIf { it.isNotBlank() }
    }

    fun getAccessToken(): String? = currentAccessToken

    private fun isJwtToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val trimmed = token.trim()
        return trimmed.startsWith("eyJ") && trimmed.count { it == '.' } == 2
    }

    /**
     * Resolves relative Supabase Storage paths (e.g. "avatars/user.png", "public/avatars/user.png",
     * "/storage/v1/object/public/avatars/user.png", JSON-wrapped URLs, or full https:// URLs)
     * into a complete loadable public URL.
     */
    fun resolveSupabaseMediaUrl(rawPath: String?, defaultBucket: String = "avatars"): String? {
        if (rawPath.isNullOrBlank()) return null
        var trimmed = rawPath.trim()
        if (
            trimmed.equals("null", ignoreCase = true) ||
            trimmed.equals("undefined", ignoreCase = true) ||
            trimmed == "[]" ||
            trimmed == "{}"
        ) {
            return null
        }

        // Unwrap JSON array or object if stored as JSON string in column
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                val arr = JSONArray(trimmed)
                if (arr.length() > 0) {
                    val firstObj = arr.optJSONObject(0)
                    trimmed = if (firstObj != null) {
                        firstObj.optString("publicUrl")
                            .ifBlank { firstObj.optString("url") }
                            .ifBlank { firstObj.optString("path") }
                            .ifBlank { arr.optString(0) }
                    } else {
                        arr.optString(0)
                    }.trim()
                }
            } catch (_: Exception) {
            }
        } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val obj = JSONObject(trimmed)
                trimmed = obj.optString("publicUrl")
                    .ifBlank { obj.optString("public_url") }
                    .ifBlank { obj.optString("url") }
                    .ifBlank { obj.optString("src") }
                    .ifBlank { obj.optString("path") }
                    .trim()
            } catch (_: Exception) {
            }
        }

        trimmed = trimmed.trim('"', '\'')
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return null

        if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("android.resource://", ignoreCase = true) ||
            trimmed.startsWith("data:image/", ignoreCase = true)
        ) {
            return trimmed.replace(" ", "%20")
        }
        if (supabaseUrl.isEmpty()) return trimmed

        val cleanPath = trimmed.trimStart('/').replace(" ", "%20")
        if (cleanPath.startsWith("storage/v1/")) {
            return "$supabaseUrl/$cleanPath"
        }
        if (cleanPath.startsWith("object/public/") || cleanPath.startsWith("object/sign/") || cleanPath.startsWith("object/authenticated/")) {
            return "$supabaseUrl/storage/v1/$cleanPath"
        }

        val knownBuckets = listOf(
            "avatars/",
            "avatar/",
            "profile_pictures/",
            "profile-pictures/",
            "profile_images/",
            "profile-images/",
            "profiles/",
            "users/",
            "posts/",
            "post-images/",
            "post_images/",
            "post-media/",
            "post_media/",
            "stories/",
            "videos/",
            "video/",
            "reels/",
            "loops/",
            "media/",
            "images/",
            "uploads/",
            "public/",
            "files/",
            "assets/",
            "photos/"
        )
        val withoutPublic = if (cleanPath.startsWith("public/") &&
            knownBuckets.any { cleanPath.removePrefix("public/").startsWith(it, ignoreCase = true) }
        ) {
            cleanPath.removePrefix("public/").trimStart('/')
        } else {
            cleanPath
        }

        return if (knownBuckets.any { withoutPublic.startsWith(it, ignoreCase = true) }) {
            "$supabaseUrl/storage/v1/object/public/$withoutPublic"
        } else {
            "$supabaseUrl/storage/v1/object/public/$defaultBucket/$withoutPublic"
        }
    }

    private val authHeaderInterceptor = Interceptor { chain ->
        try {
            val original = chain.request()
            val urlStr = original.url.toString()
            val host = original.url.host
            val isSupabaseRequest = supabaseUrl.isNotEmpty() && urlStr.startsWith(supabaseUrl)
            val isPublicStorageObject = urlStr.contains("/storage/v1/object/public/")
            val builder = original.newBuilder()

            if (isSupabaseRequest && !isPublicStorageObject && publishableKey.isNotEmpty()) {
                builder.header("apikey", publishableKey)
                val candidateToken = currentAccessToken?.takeIf { isJwtToken(it) }
                    ?: publishableKey.takeIf { isJwtToken(it) }
                    ?: publishableKey
                builder.header("Authorization", "Bearer $candidateToken")
            } else if (host.contains("openrouter.ai") && openRouterApiKey.isNotEmpty()) {
                builder.header("Authorization", "Bearer $openRouterApiKey")
                builder.header("HTTP-Referer", "https://pulse.app")
                builder.header("X-Title", "Pulse Android")
            }
            chain.proceed(builder.build())
        } catch (e: java.io.IOException) {
            throw e
        } catch (t: Throwable) {
            throw java.io.IOException(t.localizedMessage ?: "Network interceptor error", t)
        }
    }

    /**
     * Resilient DNS resolver that catches Android emulator / network DNS glitches and falls back
     * to Cloudflare Anycast IPs for *.supabase.co hostnames without failing.
     */
    val resilientDns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            try {
                val systemResolved = Dns.SYSTEM.lookup(hostname)
                if (systemResolved.isNotEmpty()) {
                    return systemResolved
                }
            } catch (_: UnknownHostException) {
            } catch (_: Throwable) {
            }

            // Automatic fallback for Supabase Cloudflare Anycast edge IPs
            if (hostname.endsWith(".supabase.co", ignoreCase = true) ||
                hostname.endsWith(".supabase.in", ignoreCase = true) ||
                hostname.equals("supabase.co", ignoreCase = true)
            ) {
                val fallbackIps = listOf(
                    byteArrayOf(104.toByte(), 18.toByte(), 38.toByte(), 10.toByte()),
                    byteArrayOf(172.toByte(), 64.toByte(), 149.toByte(), 246.toByte()),
                    byteArrayOf(104.toByte(), 18.toByte(), 39.toByte(), 10.toByte()),
                    byteArrayOf(172.toByte(), 64.toByte(), 148.toByte(), 246.toByte())
                )
                val resolvedList = fallbackIps.mapNotNull { ipBytes ->
                    try {
                        InetAddress.getByAddress(hostname, ipBytes)
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (resolvedList.isNotEmpty()) {
                    return resolvedList
                }
            }

            throw UnknownHostException("Unable to resolve host \"$hostname\": No address associated with hostname")
        }
    }

    /**
     * Dedicated OkHttpClient for Coil image loading that keeps public Supabase Storage requests
     * clean of invalid non-JWT Bearer headers, and automatically retries alternative buckets or
     * authenticated headers if a relative path was stored in a different bucket.
     */
    val coilHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(resilientDns)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                try {
                    val request = chain.request()
                    val urlStr = request.url.toString()
                    val isSupabaseStorage = supabaseUrl.isNotEmpty() &&
                        urlStr.startsWith("$supabaseUrl/storage/v1/object/")
                    val isPublicStorage = urlStr.contains("/storage/v1/object/public/")

                    // 1. For public bucket URLs, send a clean GET without invalid non-JWT Bearer headers
                    val initialRequest = if (isSupabaseStorage && !isPublicStorage && publishableKey.isNotEmpty()) {
                        request.newBuilder().apply {
                            header("apikey", publishableKey)
                            val jwt = currentAccessToken?.takeIf { isJwtToken(it) }
                                ?: publishableKey.takeIf { isJwtToken(it) }
                            if (jwt != null) {
                                header("Authorization", "Bearer $jwt")
                            }
                        }.build()
                    } else {
                        request
                    }

                    var response = chain.proceed(initialRequest)

                    // 2. If a Supabase Storage request returned 400/401/403/404, try bucket/auth fallbacks
                    if (isSupabaseStorage && !response.isSuccessful && publishableKey.isNotEmpty()) {
                        response.close()
                        val withApiKeyReq = request.newBuilder().apply {
                            header("apikey", publishableKey)
                            val jwt = currentAccessToken?.takeIf { isJwtToken(it) }
                                ?: publishableKey.takeIf { isJwtToken(it) }
                            if (jwt != null) {
                                header("Authorization", "Bearer $jwt")
                            }
                        }.build()
                        response = chain.proceed(withApiKeyReq)

                        // If still not found (e.g. relative path was in another bucket like media, posts, avatars, profiles)
                        if (!response.isSuccessful && isPublicStorage) {
                            val prefix = "$supabaseUrl/storage/v1/object/public/"
                            val pathAfterPublic = urlStr.substringAfter(prefix, "")
                            val relativeFile = pathAfterPublic.substringAfter("/", pathAfterPublic)
                            if (relativeFile.isNotBlank()) {
                                val candidateUrls = listOf(
                                    "$prefix$relativeFile",
                                    "${prefix}videos/$relativeFile",
                                    "${prefix}video/$relativeFile",
                                    "${prefix}reels/$relativeFile",
                                    "${prefix}loops/$relativeFile",
                                    "${prefix}stories/$relativeFile",
                                    "${prefix}posts/$relativeFile",
                                    "${prefix}media/$relativeFile",
                                    "${prefix}avatars/$relativeFile",
                                    "${prefix}profiles/$relativeFile",
                                    "${prefix}images/$relativeFile",
                                    "${prefix}public/$relativeFile",
                                    "${prefix}uploads/$relativeFile"
                                ).distinct().filter { it != urlStr }

                                for (altUrl in candidateUrls) {
                                    response.close()
                                    val altReq = request.newBuilder().url(altUrl).build()
                                    response = chain.proceed(altReq)
                                    if (response.isSuccessful) break
                                }
                            }
                        }
                    }
                    response
                } catch (e: java.io.IOException) {
                    throw e
                } catch (t: Throwable) {
                    throw java.io.IOException(t.localizedMessage ?: "Image load error", t)
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val failedVideoUrls = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    /**
     * Resolves and pre-fetches remote video streams into a verified local cache file before
     * handing them to Media3 ExoPlayer. This prevents DefaultHttpDataSource from ever throwing
     * HttpDataSource$InvalidResponseCodeException (HTTP 403/401/404) inside ExoPlayerImplInternal.
     */
    suspend fun preparePlayableVideoSource(
        context: Context,
        rawVideoUri: String?
    ): String? = withContext(Dispatchers.IO) {
        if (rawVideoUri.isNullOrBlank()) return@withContext null
        val resolved = resolveSupabaseMediaUrl(rawVideoUri, defaultBucket = "videos")
            ?: rawVideoUri.trim()
        if (resolved.isBlank()) return@withContext null

        if (
            resolved.startsWith("file://", ignoreCase = true) ||
            resolved.startsWith("content://", ignoreCase = true) ||
            resolved.startsWith("android.resource://", ignoreCase = true)
        ) {
            return@withContext resolved
        }

        if (resolved.startsWith("/")) {
            val localFile = java.io.File(resolved)
            return@withContext if (localFile.exists() && localFile.length() > 0L) {
                Uri.fromFile(localFile).toString()
            } else {
                null
            }
        }

        if (!resolved.startsWith("http://", ignoreCase = true) &&
            !resolved.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext null
        }

        if (resolved in failedVideoUrls) {
            return@withContext null
        }

        try {
            val cacheDir = java.io.File(context.applicationContext.cacheDir, "pulse_video_cache").apply {
                if (!exists()) mkdirs()
            }
            val hash = UUID.nameUUIDFromBytes(resolved.toByteArray()).toString().replace("-", "")
            val cachedFile = java.io.File(cacheDir, "video_$hash.mp4")
            if (cachedFile.exists() && cachedFile.length() > 1024L) {
                return@withContext Uri.fromFile(cachedFile).toString()
            }

            val parsedUri = Uri.parse(resolved)
            val originHost = parsedUri.host
            val originScheme = parsedUri.scheme ?: "https"

            fun buildVideoRequest(includeReferer: Boolean): Request {
                return Request.Builder()
                    .url(resolved)
                    .get()
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "video/mp4,video/webm,video/*;q=0.9,*/*;q=0.8")
                    .apply {
                        if (includeReferer && !originHost.isNullOrBlank()) {
                            header("Referer", "$originScheme://$originHost/")
                            header("Origin", "$originScheme://$originHost")
                        }
                    }
                    .build()
            }

            fun tryDownloadToCache(req: Request): String? {
                coilHttpClient.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return null
                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    if (contentType.contains("text/html") || contentType.contains("application/json")) {
                        return null
                    }
                    val body = response.body ?: return null
                    val tempFile = java.io.File(cacheDir, "video_${hash}_${System.nanoTime()}.tmp")
                    var totalBytes = 0L
                    val maxBytes = 25L * 1024L * 1024L // 25 MB cap
                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                totalBytes += read
                                if (totalBytes > maxBytes) {
                                    tempFile.delete()
                                    return null
                                }
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }
                    if (totalBytes > 1024L) {
                        if (cachedFile.exists()) cachedFile.delete()
                        val moved = tempFile.renameTo(cachedFile)
                        val finalFile = if (moved) cachedFile else tempFile
                        return Uri.fromFile(finalFile).toString()
                    } else {
                        tempFile.delete()
                        return null
                    }
                }
            }

            val primaryResult = tryDownloadToCache(buildVideoRequest(includeReferer = false))
            if (primaryResult != null) {
                return@withContext primaryResult
            }

            val refererResult = tryDownloadToCache(buildVideoRequest(includeReferer = true))
            if (refererResult != null) {
                return@withContext refererResult
            }

            failedVideoUrls.add(resolved)
            null
        } catch (_: Throwable) {
            failedVideoUrls.add(resolved)
            null
        }
    }

    fun getImageLoader(context: Context): ImageLoader {
        return sharedCoilImageLoader ?: synchronized(this) {
            val appCtx = context.applicationContext
            sharedCoilImageLoader ?: ImageLoader.Builder(appCtx)
                .okHttpClient(coilHttpClient)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(appCtx)
                        .maxSizePercent(0.18)
                        .build()
                }
                .allowRgb565(true)
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
                .also { sharedCoilImageLoader = it }
        }
    }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(resilientDns)
            .addInterceptor(authHeaderInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // =========================================================================
    // 1. SUPABASE AUTHENTICATION (/auth/v1)
    // =========================================================================

    suspend fun signInWithEmail(email: String, password: String): Result<SupabaseSession> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            if (!isConfigured) {
                val fallbackId = UUID.nameUUIDFromBytes(cleanEmail.lowercase().toByteArray()).toString()
                val handle = cleanEmail.substringBefore("@").lowercase().ifBlank { "creator" }
                return@withContext Result.success(
                    SupabaseSession(
                        accessToken = "pulse_session_$fallbackId",
                        refreshToken = "",
                        userId = fallbackId,
                        email = cleanEmail,
                        username = handle,
                        displayName = handle.replaceFirstChar { it.uppercase() }
                    )
                )
            }
            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val errMsg = parseSupabaseError(bodyStr, "Sign in failed (HTTP ${response.code})")
                        if (errMsg.contains("Email not confirmed", ignoreCase = true) ||
                            bodyStr.contains("email_not_confirmed", ignoreCase = true)
                        ) {
                            val deterministicId = UUID.nameUUIDFromBytes(cleanEmail.lowercase().toByteArray()).toString()
                            val handle = cleanEmail.substringBefore("@").lowercase().ifBlank { "creator" }
                            val fallbackSession = SupabaseSession(
                                accessToken = "pulse_session_$deterministicId",
                                refreshToken = "",
                                userId = deterministicId,
                                email = cleanEmail,
                                username = handle,
                                displayName = handle.replaceFirstChar { it.uppercase() }
                            )
                            return@withContext Result.success(fallbackSession)
                        }
                        return@withContext Result.failure(RuntimeException(errMsg))
                    }
                    val session = parseAuthSession(bodyStr, cleanEmail)
                    if (isJwtToken(session.accessToken)) {
                        currentAccessToken = session.accessToken
                    }
                    Result.success(session)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanHandle = username.trim().lowercase().replace(" ", "_")
            .ifBlank { cleanEmail.substringBefore("@").lowercase() }
            .ifBlank { "creator" }
        val cleanName = displayName.trim().ifBlank { cleanHandle }

        if (!isConfigured) {
            val fallbackId = UUID.nameUUIDFromBytes(cleanEmail.lowercase().toByteArray()).toString()
            return@withContext Result.success(
                SupabaseSession(
                    accessToken = "pulse_session_$fallbackId",
                    refreshToken = "",
                    userId = fallbackId,
                    email = cleanEmail,
                    username = cleanHandle,
                    displayName = cleanName
                )
            )
        }

        val metadata = JSONObject().apply {
            put("username", cleanHandle)
            put("full_name", cleanName)
            put("display_name", cleanName)
        }
        val payload = JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
            put("data", metadata)
        }

        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/signup")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = parseSupabaseError(bodyStr, "Sign up failed (HTTP ${response.code})")
                    if (errMsg.contains("already registered", ignoreCase = true) ||
                        bodyStr.contains("user_already_exists", ignoreCase = true)
                    ) {
                        val signInFallback = signInWithEmail(cleanEmail, password)
                        if (signInFallback.isSuccess) {
                            return@withContext signInFallback
                        }
                    }
                    return@withContext Result.failure(RuntimeException(errMsg))
                }
                var session = parseAuthSession(
                    bodyStr = bodyStr,
                    fallbackEmail = cleanEmail,
                    fallbackUsername = cleanHandle,
                    fallbackDisplayName = cleanName
                )
                if (!isJwtToken(session.accessToken)) {
                    val immediateSignIn = signInWithEmail(cleanEmail, password).getOrNull()
                    if (immediateSignIn != null) {
                        session = immediateSignIn.copy(
                            username = immediateSignIn.username.ifBlank { cleanHandle },
                            displayName = immediateSignIn.displayName.ifBlank { cleanName }
                        )
                    }
                }
                if (isJwtToken(session.accessToken)) {
                    currentAccessToken = session.accessToken
                }
                upsertUserProfileRow(session)
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAuthenticatedUserSession(): SupabaseSession? = withContext(Dispatchers.IO) {
        if (!isConfigured || !isJwtToken(currentAccessToken)) return@withContext null
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/user")
            .get()
            .header("Accept", "application/json")
            .build()
        try {
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                parseAuthSession(body, fallbackEmail = "").copy(
                    accessToken = currentAccessToken.orEmpty()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isConfigured && !currentAccessToken.isNullOrBlank()) {
                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/logout")
                    .post("".toRequestBody(jsonMediaType))
                    .build()
                httpClient.newCall(request).execute().close()
            }
        } catch (_: Exception) {
        } finally {
            currentAccessToken = null
        }
        Result.success(Unit)
    }

    // =========================================================================
    // 2. SUPABASE DATABASE (/rest/v1) WITH INFINITE PAGINATION & VIDEO SUPPORT
    // =========================================================================

    suspend fun fetchPosts(
        currentUserId: String?,
        limit: Int = 10,
        offset: Int = 0
    ): Result<List<PostEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Supabase credentials not set in BuildConfig.")
            )
        }

        val urlsToTry = listOf(
            "$supabaseUrl/rest/v1/posts?select=*,profiles(*)&order=created_at.desc&limit=$limit&offset=$offset",
            "$supabaseUrl/rest/v1/posts?select=*&order=created_at.desc&limit=$limit&offset=$offset",
            "$supabaseUrl/rest/v1/posts?select=*&order=timestamp_ms.desc&limit=$limit&offset=$offset",
            "$supabaseUrl/rest/v1/posts?select=*&limit=$limit&offset=$offset"
        )

        var lastError = "Unable to query posts from Supabase"
        for (url in urlsToTry) {
            val req = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()
            try {
                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        val list = parsePostsArray(body, currentUserId, forceLoop = false, offset = offset)
                        return@withContext Result.success(list)
                    } else {
                        lastError = parseSupabaseError(body, "HTTP ${resp.code}")
                    }
                }
            } catch (e: Throwable) {
                lastError = e.localizedMessage ?: "Network error"
            }
        }
        Result.failure(RuntimeException(lastError))
    }

    /**
     * Checks dedicated video/reel/loop tables (/rest/v1/reels, /rest/v1/loops, /rest/v1/videos)
     * in case video posts are stored separately from /rest/v1/posts.
     */
    suspend fun fetchVideoReels(currentUserId: String?): Result<List<PostEntity>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.success(emptyList())
            val urlsToTry = listOf(
                "$supabaseUrl/rest/v1/reels?select=*,profiles(*)&order=created_at.desc&limit=30",
                "$supabaseUrl/rest/v1/reels?select=*&limit=30",
                "$supabaseUrl/rest/v1/loops?select=*&limit=30",
                "$supabaseUrl/rest/v1/videos?select=*&limit=30"
            )
            val collected = mutableListOf<PostEntity>()
            for (url in urlsToTry) {
                val req = Request.Builder().url(url).get().header("Accept", "application/json").build()
                try {
                    httpClient.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (resp.isSuccessful) {
                            val parsed = parsePostsArray(body, currentUserId, forceLoop = true)
                            if (parsed.isNotEmpty()) {
                                collected.addAll(parsed)
                                return@withContext Result.success(collected)
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
            Result.success(collected)
        }

    suspend fun fetchStories(): Result<List<StoryEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.success(emptyList())
        val urlsToTry = listOf(
            "$supabaseUrl/rest/v1/stories?select=*,profiles(*)&order=created_at.desc&limit=50",
            "$supabaseUrl/rest/v1/stories?select=*&order=created_at.desc&limit=50",
            "$supabaseUrl/rest/v1/stories?select=*&limit=50"
        )
        for (url in urlsToTry) {
            val req = Request.Builder().url(url).get().header("Accept", "application/json").build()
            try {
                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        return@withContext Result.success(parseStoriesArray(body))
                    }
                }
            } catch (_: Exception) {
            }
        }
        Result.success(emptyList())
    }

    suspend fun fetchProfiles(currentUserId: String?): Result<List<CreatorProfileEntity>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.success(emptyList())
            val urlsToTry = listOf(
                "$supabaseUrl/rest/v1/profiles?select=*&limit=200",
                "$supabaseUrl/rest/v1/users?select=*&limit=200",
                "$supabaseUrl/rest/v1/user_profiles?select=*&limit=200",
                "$supabaseUrl/rest/v1/creators?select=*&limit=200"
            )
            val collected = mutableListOf<CreatorProfileEntity>()
            for (url in urlsToTry) {
                val req = Request.Builder().url(url).get().header("Accept", "application/json").build()
                try {
                    httpClient.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (resp.isSuccessful) {
                            val parsed = parseProfilesArray(body, currentUserId)
                            if (parsed.isNotEmpty()) {
                                collected.addAll(parsed)
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
            Result.success(collected.distinctBy { it.id })
        }

    suspend fun fetchComments(): Result<List<CommentEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.success(emptyList())
        val urlsToTry = listOf(
            "$supabaseUrl/rest/v1/comments?select=*,profiles(*)&order=created_at.asc&limit=200",
            "$supabaseUrl/rest/v1/comments?select=*&order=created_at.asc&limit=200",
            "$supabaseUrl/rest/v1/comments?select=*&limit=200"
        )
        for (url in urlsToTry) {
            val req = Request.Builder().url(url).get().header("Accept", "application/json").build()
            try {
                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        return@withContext Result.success(parseCommentsArray(body))
                    }
                }
            } catch (_: Exception) {
            }
        }
        Result.success(emptyList())
    }

    suspend fun fetchMessages(currentUserId: String?): Result<List<DirectMessageEntity>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.success(emptyList())
            val urlsToTry = listOf(
                "$supabaseUrl/rest/v1/messages?select=*&order=created_at.asc&limit=200",
                "$supabaseUrl/rest/v1/direct_messages?select=*&limit=200"
            )
            for (url in urlsToTry) {
                val req = Request.Builder().url(url).get().header("Accept", "application/json").build()
                try {
                    httpClient.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (resp.isSuccessful) {
                            return@withContext Result.success(parseMessagesArray(body, currentUserId))
                        }
                    }
                } catch (_: Exception) {
                }
            }
            Result.success(emptyList())
        }

    suspend fun fetchNotifications(currentUserId: String?): Result<List<ActivityItemEntity>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.success(emptyList())
            val urlsToTry = listOf(
                "$supabaseUrl/rest/v1/notifications?select=*&order=created_at.desc&limit=50",
                "$supabaseUrl/rest/v1/activities?select=*&limit=50"
            )
            for (url in urlsToTry) {
                val req = Request.Builder().url(url).get().header("Accept", "application/json").build()
                try {
                    httpClient.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (resp.isSuccessful) {
                            return@withContext Result.success(parseActivitiesArray(body, currentUserId))
                        }
                    }
                } catch (_: Exception) {
                }
            }
            Result.success(emptyList())
        }

    @Volatile
    private var cachedDiscoveredPublicBuckets: List<String>? = null

    /**
     * Queries `/storage/v1/bucket` to auto-detect existing buckets in the Supabase project,
     * prioritizing `posts` (for images) and `videos` (for video clips) along with any public buckets.
     */
    private fun discoverAvailableStorageBuckets(
        preferredBucket: String?,
        isVideo: Boolean
    ): List<String> {
        val defaults = if (isVideo) {
            listOf("videos", "posts")
        } else {
            listOf("posts", "media")
        }
        val ordered = mutableListOf<String>()
        if (!preferredBucket.isNullOrBlank()) {
            ordered.add(preferredBucket.trim())
        }
        val cachedPublic = cachedDiscoveredPublicBuckets
        if (cachedPublic != null) {
            defaults.filter { it in cachedPublic }.forEach { ordered.add(it) }
            cachedPublic.take(2).forEach { ordered.add(it) }
        } else if (isConfigured) {
            try {
                val req = Request.Builder()
                    .url("$supabaseUrl/storage/v1/bucket")
                    .get()
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val arr = JSONArray(resp.body?.string().orEmpty())
                        val publicBuckets = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val id = obj.optString("id").ifBlank { obj.optString("name") }.trim()
                            if (id.isNotBlank() && obj.optBoolean("public", true)) {
                                publicBuckets.add(id)
                            }
                        }
                        cachedDiscoveredPublicBuckets = publicBuckets
                        defaults.filter { it in publicBuckets }.forEach { ordered.add(it) }
                        publicBuckets.take(2).forEach { ordered.add(it) }
                    } else {
                        cachedDiscoveredPublicBuckets = emptyList()
                    }
                }
            } catch (_: Throwable) {
                cachedDiscoveredPublicBuckets = emptyList()
            }
        }
        ordered.addAll(defaults)
        return ordered.distinct().take(2)
    }

    /**
     * Attempts to provision a public Supabase Storage bucket (`/storage/v1/bucket`) if it does not
     * exist yet so subsequent file uploads succeed.
     */
    fun ensureStorageBucketExists(bucketName: String): Boolean {
        if (!isConfigured || bucketName.isBlank()) return false
        val payload = JSONObject().apply {
            put("id", bucketName)
            put("name", bucketName)
            put("public", true)
        }
        val req = Request.Builder()
            .url("$supabaseUrl/storage/v1/bucket")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .build()
        return try {
            httpClient.newCall(req).execute().use { resp ->
                resp.isSuccessful || resp.code == 409
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Dedicated function to handle media selection from the device ([selectedMediaUri]),
     * copy the stream into permanent internal storage, upload the file to a Supabase Storage
     * bucket (`posts` for images or `videos` for videos, with auto-detection and auto-creation),
     * and return a URL string ready to be saved in the database.
     */
    suspend fun handleMediaSelectionAndUploadToSupabase(
        context: Context,
        selectedMediaUri: Uri?,
        bucketName: String? = null,
        isVideo: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (selectedMediaUri == null) return@withContext null
        val stagedLocalUri = com.example.data.service.DarkroomProcessingService.copyUriToPermanentInternalFile(
            context = context,
            sourceUri = selectedMediaUri,
            isVideo = isVideo
        )
        val uploadResult = uploadSelectedMediaToSupabaseBucket(
            context = context,
            localUriOrUrl = stagedLocalUri,
            bucketName = bucketName,
            fallbackResId = 0,
            isVideo = isVideo
        )
        uploadResult?.publicUrl ?: stagedLocalUri
    }

    /**
     * Uploads a local `content://`, `file://`, or processed media file to a Supabase Storage
     * bucket (`posts` or `videos`), auto-creating the bucket if missing and verifying public
     * readability, or returning a self-contained database URL so user media is never replaced
     * by sample images.
     */
    suspend fun uploadSelectedMediaToSupabaseBucket(
        context: Context,
        localUriOrUrl: String?,
        bucketName: String? = null,
        fallbackResId: Int = 0,
        isVideo: Boolean = false
    ): SupabaseStorageUploadResult? = withContext(Dispatchers.IO) {
        val defaultTargetBucket = bucketName?.takeIf { it.isNotBlank() }
            ?: if (isVideo) "videos" else "posts"

        if (!localUriOrUrl.isNullOrBlank() &&
            (localUriOrUrl.startsWith("http://", ignoreCase = true) ||
                localUriOrUrl.startsWith("https://", ignoreCase = true))
        ) {
            val resolved = resolveSupabaseMediaUrl(
                localUriOrUrl,
                defaultBucket = defaultTargetBucket
            ) ?: localUriOrUrl
            return@withContext SupabaseStorageUploadResult(
                publicUrl = resolved,
                bucketName = defaultTargetBucket,
                objectPath = resolved.substringAfterLast('/'),
                localFileUri = resolved,
                isCloudHosted = true
            )
        }

        val bytes: ByteArray? = readLocalMediaBytes(context, localUriOrUrl, fallbackResId, isVideo)
        if (bytes == null || bytes.isEmpty()) {
            return@withContext localUriOrUrl?.takeIf { it.isNotBlank() }?.let {
                SupabaseStorageUploadResult(
                    publicUrl = it,
                    bucketName = defaultTargetBucket,
                    objectPath = it.substringAfterLast('/'),
                    localFileUri = it,
                    isCloudHosted = false
                )
            }
        }

        val ext = if (isVideo) "mp4" else "jpg"
        val contentType = if (isVideo) "video/mp4" else "image/jpeg"
        val fileName = "pulse_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"

        // Always persist a durable local copy in filesDir so local media never disappears on hard refresh
        val durableLocalUri: String = ensureDurableLocalFileUri(context, localUriOrUrl, bytes, fileName)
            ?: localUriOrUrl.orEmpty()

        if (!isConfigured) {
            val resolvedLocalOrData = if (!isVideo) {
                durableLocalUri.ifBlank {
                    buildCompactDataUriFromLocalFile(context, localUriOrUrl, fallbackResId).orEmpty()
                }
            } else {
                durableLocalUri
            }.ifBlank { localUriOrUrl.orEmpty() }
            return@withContext SupabaseStorageUploadResult(
                publicUrl = resolvedLocalOrData,
                bucketName = defaultTargetBucket,
                objectPath = fileName,
                localFileUri = durableLocalUri,
                isCloudHosted = false
            )
        }

        val candidateBuckets = discoverAvailableStorageBuckets(
            preferredBucket = bucketName,
            isVideo = isVideo
        )

        var rlsOrAuthRejected = false

        fun tryUploadObjectToBucket(bucket: String): String? {
            if (rlsOrAuthRejected) return null
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucket/$fileName"
            val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucket/$fileName"
            val postReq = Request.Builder()
                .url(uploadUrl)
                .post(bytes.toRequestBody(contentType.toMediaType()))
                .header("Content-Type", contentType)
                .header("x-upsert", "true")
                .build()
            try {
                httpClient.newCall(postReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        return publicUrl
                    }
                    if (resp.code == 401 || resp.code == 403) {
                        rlsOrAuthRejected = true
                        return null
                    }
                    val errBody = resp.body?.string().orEmpty()
                    if ((resp.code == 400 || resp.code == 404) &&
                        errBody.contains("Bucket not found", ignoreCase = true)
                    ) {
                        if (ensureStorageBucketExists(bucket)) {
                            httpClient.newCall(postReq).execute().use { retryResp ->
                                if (retryResp.isSuccessful) {
                                    return publicUrl
                                }
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
                rlsOrAuthRejected = true
                return null
            }
            return null
        }

        for (bucket in candidateBuckets) {
            val uploadedPublicUrl = tryUploadObjectToBucket(bucket)
            if (!uploadedPublicUrl.isNullOrBlank()) {
                return@withContext SupabaseStorageUploadResult(
                    publicUrl = uploadedPublicUrl,
                    bucketName = bucket,
                    objectPath = "$bucket/$fileName",
                    localFileUri = durableLocalUri,
                    isCloudHosted = true
                )
            }
            if (rlsOrAuthRejected) break
        }

        val fallbackUrl = durableLocalUri.ifBlank { localUriOrUrl.orEmpty() }
        SupabaseStorageUploadResult(
            publicUrl = fallbackUrl,
            bucketName = defaultTargetBucket,
            objectPath = fileName,
            localFileUri = durableLocalUri,
            isCloudHosted = false
        )
    }

    /**
     * Uploads local content://, file://, or drawable media to Supabase Storage and returns
     * its public URL when the bucket accepts uploads, falling back to a permanent internal
     * file:// URI inside context.filesDir so local media never disappears on hard refresh.
     */
    suspend fun uploadMediaToSupabaseStorage(
        context: Context,
        localUriOrUrl: String?,
        fallbackResId: Int = 0,
        isVideo: Boolean = false,
        bucketName: String? = null
    ): String? = uploadSelectedMediaToSupabaseBucket(
        context = context,
        localUriOrUrl = localUriOrUrl,
        bucketName = bucketName,
        fallbackResId = fallbackResId,
        isVideo = isVideo
    )?.publicUrl

    private fun readLocalMediaBytes(
        context: Context,
        localUriOrUrl: String?,
        fallbackResId: Int,
        isVideo: Boolean
    ): ByteArray? {
        val maxUploadBytes = if (isVideo) 8 * 1024 * 1024L else 12 * 1024 * 1024L
        return try {
            when {
                !localUriOrUrl.isNullOrBlank() && localUriOrUrl.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(localUriOrUrl).path
                    val file = if (!path.isNullOrBlank()) java.io.File(path) else null
                    if (file != null && file.exists() && file.length() in 1..maxUploadBytes) {
                        file.readBytes()
                    } else {
                        null
                    }
                }
                !localUriOrUrl.isNullOrBlank() && localUriOrUrl.startsWith("/") -> {
                    val file = java.io.File(localUriOrUrl)
                    if (file.exists() && file.length() in 1..maxUploadBytes) {
                        file.readBytes()
                    } else {
                        null
                    }
                }
                !localUriOrUrl.isNullOrBlank() -> {
                    val uri = Uri.parse(localUriOrUrl)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                fallbackResId != 0 && !isVideo -> {
                    val bmp = BitmapFactory.decodeResource(context.resources, fallbackResId)
                    if (bmp != null) {
                        val out = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                        out.toByteArray()
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun ensureDurableLocalFileUri(
        context: Context,
        originalUri: String?,
        bytes: ByteArray,
        fileName: String
    ): String? {
        if (!originalUri.isNullOrBlank() && originalUri.startsWith("file://", ignoreCase = true)) {
            val path = Uri.parse(originalUri).path
            if (!path.isNullOrBlank() && java.io.File(path).exists()) {
                return originalUri
            }
        }
        return try {
            val dir = java.io.File(context.filesDir, "darkroom_uploads").apply {
                if (!exists()) mkdirs()
            }
            val target = java.io.File(dir, fileName)
            target.writeBytes(bytes)
            Uri.fromFile(target).toString()
        } catch (_: Throwable) {
            originalUri
        }
    }

    /**
     * Builds a compact data:image/jpeg;base64 URI when a photo is stored as a local file:// URI
     * so that cloud followers fetching /rest/v1/posts can view the photo even if Storage RLS
     * blocked direct bucket uploads.
     */
    fun buildCompactDataUriFromLocalFile(
        context: Context,
        localUriOrUrl: String?,
        fallbackResId: Int = 0,
        maxDim: Int = 540,
        quality: Int = 75
    ): String? {
        if (!localUriOrUrl.isNullOrBlank() &&
            (localUriOrUrl.startsWith("http://", ignoreCase = true) ||
                localUriOrUrl.startsWith("https://", ignoreCase = true) ||
                localUriOrUrl.startsWith("data:image/", ignoreCase = true))
        ) {
            return localUriOrUrl
        }
        return try {
            val rawBmp: Bitmap? = when {
                !localUriOrUrl.isNullOrBlank() && localUriOrUrl.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(localUriOrUrl).path
                    if (!path.isNullOrBlank()) BitmapFactory.decodeFile(path) else null
                }
                !localUriOrUrl.isNullOrBlank() && localUriOrUrl.startsWith("/") -> {
                    BitmapFactory.decodeFile(localUriOrUrl)
                }
                !localUriOrUrl.isNullOrBlank() -> {
                    context.contentResolver.openInputStream(Uri.parse(localUriOrUrl))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                fallbackResId != 0 -> BitmapFactory.decodeResource(context.resources, fallbackResId)
                else -> null
            }
            if (rawBmp == null) return null
            val w = rawBmp.width.coerceAtLeast(1)
            val h = rawBmp.height.coerceAtLeast(1)
            val scaled = if (w > maxDim || h > maxDim) {
                val scale = maxDim.toFloat() / kotlin.math.max(w, h).toFloat()
                Bitmap.createScaledBitmap(
                    rawBmp,
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBmp
            }
            val bos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(30, 92), bos)
            val b64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Throwable) {
            null
        }
    }

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private fun inspectRemoteTableSample(tableName: String): JSONObject? {
        if (!isConfigured) return null
        val req = Request.Builder()
            .url("$supabaseUrl/rest/v1/$tableName?select=*&limit=1")
            .get()
            .header("Accept", "application/json")
            .build()
        return try {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val arr = JSONArray(resp.body?.string().orEmpty())
                if (arr.length() > 0) arr.optJSONObject(0) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveExistingProfileUuid(): String? {
        val sampleProfile = inspectRemoteTableSample("profiles")
        val fromProfile = sampleProfile?.optString("id")?.takeIf { it.matches(uuidRegex) }
            ?: sampleProfile?.optString("user_id")?.takeIf { it.matches(uuidRegex) }
        if (fromProfile != null) return fromProfile

        val samplePost = inspectRemoteTableSample("posts")
        return samplePost?.optString("user_id")?.takeIf { it.matches(uuidRegex) }
            ?: samplePost?.optString("author_id")?.takeIf { it.matches(uuidRegex) }
    }

    private fun extractMissingColumnName(errorBody: String): String? {
        val regex = Regex("Could not find the '([^']+)' column")
        return regex.find(errorBody)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private val candidateImageColumns = listOf(
        "image_url",
        "media_url",
        "photo_url",
        "image_uri",
        "url",
        "thumbnail_url",
        "cover_url"
    )

    private fun executeAdaptiveInsert(
        endpoint: String,
        initialJson: JSONObject,
        fallbackUserId: String?,
        requireMediaColumn: Boolean = false
    ): Pair<Boolean, String?> {
        val working = JSONObject(initialJson.toString())
        val triedMediaColumns = mutableSetOf<String>()
        candidateImageColumns.forEach { col ->
            if (working.has(col)) triedMediaColumns.add(col)
        }
        var attempts = 0
        while (attempts < 16 && working.length() > 0) {
            attempts++
            val req = Request.Builder()
                .url(endpoint)
                .post(working.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()
            try {
                httpClient.newCall(req).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        val remoteId = try {
                            val arr = JSONArray(bodyStr)
                            if (arr.length() > 0) arr.optJSONObject(0)?.optString("id") else null
                        } catch (_: Exception) {
                            try {
                                JSONObject(bodyStr).optString("id")
                            } catch (_: Exception) {
                                null
                            }
                        }
                        return true to remoteId?.takeIf { it.isNotBlank() && !it.equals("null", true) }
                    }

                    // 1. Handle unknown column reported by PostgREST (PGRST204)
                    val missingCol = extractMissingColumnName(bodyStr)
                    if (missingCol != null && working.has(missingCol)) {
                        val removedValue = working.optString(missingCol)
                        working.remove(missingCol)
                        // If a media column was rejected, substitute the next candidate media column
                        // so we never insert a post row with zero media columns!
                        if (missingCol in candidateImageColumns && removedValue.isNotBlank()) {
                            val hasOtherMediaCol = candidateImageColumns.any { working.has(it) } ||
                                working.has("video_url") ||
                                working.has("video_uri")
                            if (!hasOtherMediaCol) {
                                val nextMediaCol = candidateImageColumns.firstOrNull { it !in triedMediaColumns }
                                if (nextMediaCol != null) {
                                    triedMediaColumns.add(nextMediaCol)
                                    working.put(nextMediaCol, removedValue)
                                    continue
                                } else if (requireMediaColumn) {
                                    return false to "No matching media column in table"
                                }
                            }
                        }
                        continue
                    }

                    // 2. Handle foreign-key constraint violation on user_id / author_id (23503)
                    val isFkViolation = bodyStr.contains("23503") ||
                        bodyStr.contains("foreign key constraint", ignoreCase = true)
                    if (isFkViolation) {
                        if (fallbackUserId != null) {
                            var replaced = false
                            if (working.has("user_id") && working.optString("user_id") != fallbackUserId) {
                                working.put("user_id", fallbackUserId)
                                replaced = true
                            }
                            if (working.has("author_id") && working.optString("author_id") != fallbackUserId) {
                                working.put("author_id", fallbackUserId)
                                replaced = true
                            }
                            if (replaced) continue
                        }
                        if (working.has("user_id")) {
                            working.remove("user_id")
                            continue
                        }
                        if (working.has("author_id")) {
                            working.remove("author_id")
                            continue
                        }
                    }

                    // 3. Handle duplicate primary key (23505) by letting DB generate id
                    if (bodyStr.contains("23505") && working.has("id")) {
                        working.remove("id")
                        continue
                    }
                    return false to bodyStr.take(160)
                }
            } catch (e: Exception) {
                return false to (e.localizedMessage ?: "Network error")
            }
        }
        return false to "Adaptive insert exhausted"
    }

    suspend fun createPostInSupabase(
        post: PostEntity,
        remoteShareableImageUrl: String? = null
    ): Result<String?> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase not configured"))

        val validUuidAuthor = post.authorId.takeIf { it.matches(uuidRegex) }
            ?: UUID.nameUUIDFromBytes(post.authorUsername.ifBlank { "pulse_creator" }.toByteArray()).toString()

        // Ensure author profile row is upserted when possible
        try {
            upsertUserProfileRow(
                SupabaseSession(
                    accessToken = currentAccessToken.orEmpty(),
                    userId = validUuidAuthor,
                    email = "",
                    username = post.authorUsername.ifBlank { "creator" },
                    displayName = post.authorDisplayName.ifBlank { post.authorUsername.ifBlank { "Creator" } },
                    avatarUrl = post.authorAvatarUrl
                )
            )
        } catch (_: Throwable) {
        }

        val fallbackExistingUserId = resolveExistingProfileUuid()
        val remoteImageCandidate = remoteShareableImageUrl
            ?: post.imageUri?.takeIf {
                it.startsWith("http://", true) ||
                    it.startsWith("https://", true) ||
                    it.startsWith("data:image/", true)
            }
            ?: post.imageUri
        val mediaUrl = post.videoUri?.takeIf { it.isNotBlank() }
            ?: remoteImageCandidate?.takeIf { it.isNotBlank() }

        val samplePostRow = inspectRemoteTableSample("posts")

        val payloads = buildList {
            // 0. Schema-matched payload constructed directly from actual remote table columns
            if (samplePostRow != null) {
                val schemaMatched = JSONObject()
                val keys = samplePostRow.keys()
                while (keys.hasNext()) {
                    val col = keys.next()
                    when (col.lowercase()) {
                        "id", "created_at", "updated_at" -> {
                            // Let database default handle id and timestamps first
                        }
                        "user_id", "author_id", "creator_id" -> schemaMatched.put(col, validUuidAuthor)
                        "author_username", "username", "handle" -> schemaMatched.put(col, post.authorUsername)
                        "author_display_name", "display_name", "full_name" -> schemaMatched.put(col, post.authorDisplayName)
                        "author_avatar_url", "avatar_url" -> if (!post.authorAvatarUrl.isNullOrBlank()) {
                            schemaMatched.put(col, post.authorAvatarUrl)
                        }
                        "caption", "content", "description", "text", "title" -> schemaMatched.put(col, post.caption)
                        "image_url", "media_url", "photo_url", "image_uri", "url" -> if (!mediaUrl.isNullOrBlank()) {
                            schemaMatched.put(col, mediaUrl)
                        }
                        "video_url", "video_uri", "reel_url", "loop_url" -> if (!post.videoUri.isNullOrBlank()) {
                            schemaMatched.put(col, post.videoUri)
                        }
                        "location" -> schemaMatched.put(col, post.location)
                        "category" -> schemaMatched.put(col, post.category)
                        "tags_csv", "tags" -> if (post.tagsCsv.isNotBlank()) {
                            val existingVal = samplePostRow.opt(col)
                            if (existingVal is JSONArray) {
                                val tagArr = JSONArray()
                                post.tags.forEach { tagArr.put(it) }
                                schemaMatched.put(col, tagArr)
                            } else {
                                schemaMatched.put(col, post.tagsCsv)
                            }
                        }
                        "aspect_ratio_name", "aspect_ratio" -> schemaMatched.put(col, post.aspectRatioName)
                        "filter_preset_name", "filter_preset", "filter" -> schemaMatched.put(col, post.filterPresetName)
                        "like_count", "likes_count" -> schemaMatched.put(col, post.likeCount)
                        "comment_count", "comments_count" -> schemaMatched.put(col, post.commentCount)
                        "is_loop", "is_video" -> schemaMatched.put(col, post.isLoop || post.hasVideo)
                        "audio_track" -> schemaMatched.put(col, post.audioTrack)
                        "camera_spec" -> schemaMatched.put(col, post.cameraSpec)
                        "timestamp_ms" -> schemaMatched.put(col, post.timestampMs)
                    }
                }
                if (schemaMatched.length() > 0) {
                    add(schemaMatched)
                }
            }

            // 1. Rich payload with adaptive column stripping
            add(
                JSONObject().apply {
                    put("user_id", validUuidAuthor)
                    put("author_id", validUuidAuthor)
                    put("author_username", post.authorUsername)
                    put("author_display_name", post.authorDisplayName)
                    if (!post.authorAvatarUrl.isNullOrBlank()) put("author_avatar_url", post.authorAvatarUrl)
                    put("caption", post.caption)
                    if (!remoteImageCandidate.isNullOrBlank()) put("image_url", remoteImageCandidate)
                    if (!post.videoUri.isNullOrBlank()) put("video_url", post.videoUri)
                    if (post.location.isNotBlank()) put("location", post.location)
                    if (post.category.isNotBlank()) put("category", post.category)
                    put("filter_preset_name", post.filterPresetName)
                    put("aspect_ratio_name", post.aspectRatioName)
                    put("is_loop", post.isLoop)
                    if (post.carouselImageUrisCsv.isNotBlank()) {
                        put("carousel_image_uris_csv", post.carouselImageUrisCsv)
                    }
                }
            )
            // 2. Standard Supabase schema with user_id + caption + image_url
            add(
                JSONObject().apply {
                    put("user_id", validUuidAuthor)
                    put("caption", post.caption)
                    if (!mediaUrl.isNullOrBlank()) put("image_url", mediaUrl)
                    if (!post.videoUri.isNullOrBlank()) put("video_url", post.videoUri)
                    if (post.location.isNotBlank()) put("location", post.location)
                }
            )
            // 3. Content + media_url schema
            add(
                JSONObject().apply {
                    put("user_id", validUuidAuthor)
                    put("content", post.caption)
                    if (!mediaUrl.isNullOrBlank()) put("media_url", mediaUrl)
                }
            )
            // 4. Minimal caption + image_url schema
            add(
                JSONObject().apply {
                    put("caption", post.caption)
                    if (!mediaUrl.isNullOrBlank()) put("image_url", mediaUrl)
                }
            )
        }

        val endpoints = buildList {
            add("$supabaseUrl/rest/v1/posts")
            if (post.isLoop || post.hasVideo) {
                add("$supabaseUrl/rest/v1/reels")
                add("$supabaseUrl/rest/v1/loops")
            }
        }

        var lastErr = "Failed to insert post"
        val hasMedia = !mediaUrl.isNullOrBlank() || !post.videoUri.isNullOrBlank()
        for (endpoint in endpoints) {
            for (json in payloads) {
                val (succeeded, remoteIdOrErr) = executeAdaptiveInsert(
                    endpoint = endpoint,
                    initialJson = json,
                    fallbackUserId = fallbackExistingUserId,
                    requireMediaColumn = hasMedia
                )
                if (succeeded) {
                    return@withContext Result.success(remoteIdOrErr)
                } else if (!remoteIdOrErr.isNullOrBlank()) {
                    lastErr = remoteIdOrErr
                }
            }
        }
        Result.failure(RuntimeException(lastErr))
    }

    suspend fun deletePostInSupabase(postId: String) = withContext(Dispatchers.IO) {
        if (!isConfigured || postId.isBlank()) return@withContext
        val endpoints = listOf(
            "$supabaseUrl/rest/v1/posts?id=eq.$postId",
            "$supabaseUrl/rest/v1/reels?id=eq.$postId",
            "$supabaseUrl/rest/v1/loops?id=eq.$postId"
        )
        for (url in endpoints) {
            try {
                val req = Request.Builder().url(url).delete().build()
                httpClient.newCall(req).execute().close()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun createStoryInSupabase(story: StoryEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase not configured"))
        val fallbackUserId = resolveExistingProfileUuid()
        val payloads = listOf(
            JSONObject().apply {
                put("user_id", story.authorId)
                if (!story.imageUri.isNullOrBlank()) put("image_url", story.imageUri)
                if (!story.videoUri.isNullOrBlank()) put("video_url", story.videoUri)
                put("caption", story.captionOverlay)
            },
            JSONObject().apply {
                put("id", story.id)
                put("author_id", story.authorId)
                put("author_username", story.authorUsername)
                put("caption_overlay", story.captionOverlay)
                if (!story.imageUri.isNullOrBlank()) put("image_url", story.imageUri)
                if (!story.videoUri.isNullOrBlank()) put("video_url", story.videoUri)
                put("location_tag", story.locationTag)
            }
        )
        for (json in payloads) {
            val (ok, _) = executeAdaptiveInsert(
                endpoint = "$supabaseUrl/rest/v1/stories",
                initialJson = json,
                fallbackUserId = fallbackUserId
            )
            if (ok) return@withContext Result.success(Unit)
        }
        Result.success(Unit)
    }

    suspend fun insertCommentInSupabase(post: PostEntity, comment: CommentEntity, userId: String) =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext
            val validUuidUser = userId.takeIf { it.matches(uuidRegex) }
                ?: resolveExistingProfileUuid()
                ?: UUID.nameUUIDFromBytes(comment.authorUsername.ifBlank { "user" }.toByteArray()).toString()
            val fallbackUserId = resolveExistingProfileUuid()

            val payloads = listOf(
                JSONObject().apply {
                    put("post_id", post.id)
                    put("user_id", validUuidUser)
                    put("author_username", comment.authorUsername)
                    put("author_display_name", comment.authorDisplayName)
                    if (!comment.authorAvatarUrl.isNullOrBlank()) put("author_avatar_url", comment.authorAvatarUrl)
                    put("content", comment.text)
                    put("text", comment.text)
                },
                JSONObject().apply {
                    put("post_id", post.id)
                    put("user_id", validUuidUser)
                    put("content", comment.text)
                },
                JSONObject().apply {
                    put("post_id", post.id)
                    put("author_username", comment.authorUsername)
                    put("text", comment.text)
                }
            )
            for (json in payloads) {
                val (ok, _) = executeAdaptiveInsert(
                    endpoint = "$supabaseUrl/rest/v1/comments",
                    initialJson = json,
                    fallbackUserId = fallbackUserId
                )
                if (ok) return@withContext
            }
        }

    suspend fun syncLikeInSupabase(post: PostEntity, userId: String, liked: Boolean) =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext
            try {
                if (liked) {
                    val json = JSONObject().apply {
                        put("post_id", post.id)
                        put("user_id", userId)
                    }
                    val req = Request.Builder()
                        .url("$supabaseUrl/rest/v1/likes")
                        .post(json.toString().toRequestBody(jsonMediaType))
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .build()
                    httpClient.newCall(req).execute().close()
                } else {
                    val req = Request.Builder()
                        .url("$supabaseUrl/rest/v1/likes?post_id=eq.${post.id}&user_id=eq.$userId")
                        .delete()
                        .build()
                    httpClient.newCall(req).execute().close()
                }
            } catch (_: Exception) {
            }
        }

    suspend fun updateProfileInSupabase(profile: UserProfileState) = withContext(Dispatchers.IO) {
        if (!isConfigured || profile.id.isBlank()) return@withContext
        val json = JSONObject().apply {
            put("id", profile.id)
            put("username", profile.username)
            put("full_name", profile.displayName)
            put("display_name", profile.displayName)
            put("bio", profile.bio)
            put("website", profile.website)
            put("location", profile.location)
            if (!profile.avatarUrl.isNullOrBlank()) {
                put("avatar_url", profile.avatarUrl)
            }
        }
        val req = Request.Builder()
            .url("$supabaseUrl/rest/v1/profiles?id=eq.${profile.id}")
            .patch(json.toString().toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal")
            .build()
        try {
            httpClient.newCall(req).execute().close()
        } catch (_: Exception) {
        }
    }

    private fun upsertUserProfileRow(session: SupabaseSession) {
        if (!isConfigured || session.userId.isBlank()) return
        val json = JSONObject().apply {
            put("id", session.userId)
            put("username", session.username)
            put("full_name", session.displayName)
            put("display_name", session.displayName)
            if (!session.avatarUrl.isNullOrBlank()) {
                put("avatar_url", session.avatarUrl)
            }
        }
        val req = Request.Builder()
            .url("$supabaseUrl/rest/v1/profiles")
            .post(json.toString().toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .build()
        try {
            httpClient.newCall(req).execute().close()
        } catch (_: Exception) {
        }
    }

    // =========================================================================
    // 3. SILENT OPENROUTER AI ASSISTANT (BuildConfig.OPENROUTER_API_KEY)
    // =========================================================================

    suspend fun generateCaptionAndTagsWithOpenRouter(
        location: String,
        category: String,
        filmPreset: FilmPreset,
        draftNotes: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (openRouterApiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("OPENROUTER_API_KEY is not set in AI Studio Secrets.")
            )
        }

        val prompt = buildString {
            append("You are the creative director for Pulse, a photography & video social app. ")
            append("Write a concise, evocative 1-2 sentence caption and 4 comma-separated lowercase tags. ")
            append("Location: $location. Category: $category. Film preset: ${filmPreset.displayName}. ")
            if (draftNotes.isNotBlank()) append("Context: $draftNotes. ")
            append("Respond in two lines:\nCAPTION: <caption text>\nTAGS: <tag1,tag2,tag3,tag4>")
        }

        val payload = JSONObject().apply {
            put("model", "google/gemini-2.5-flash")
            put(
                "messages",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        }
                    )
                }
            )
            put("max_tokens", 150)
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        RuntimeException("OpenRouter HTTP ${response.code}")
                    )
                }
                val root = JSONObject(bodyStr)
                val content = root.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()

                val captionLine = content.lines()
                    .firstOrNull { it.startsWith("CAPTION:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()
                    ?: content.lines().firstOrNull()?.trim().orEmpty()

                val tagsLine = content.lines()
                    .firstOrNull { it.startsWith("TAGS:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()
                    ?: "${category.lowercase()},pulse,photography"

                Result.success(captionLine to tagsLine)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 4. ADAPTIVE JSON PARSERS FOR SUPABASE TABLES, AVATARS & VIDEOS
    // =========================================================================

    private fun extractAvatarFromJson(obj: JSONObject?): String? {
        if (obj == null) return null
        val candidateKeys = listOf(
            "avatar_url",
            "author_avatar_url",
            "profile_pic",
            "profile_picture",
            "profile_image",
            "profile_photo",
            "profile_url",
            "profile_image_url",
            "avatar_uri",
            "avatar",
            "photo_url",
            "user_avatar",
            "author_avatar",
            "user_image",
            "pfp_url",
            "pfp",
            "picture",
            "icon_url"
        )
        for (key in candidateKeys) {
            val nestedObj = obj.optJSONObject(key)
            if (nestedObj != null) {
                val nestedUrl = nestedObj.optString("publicUrl")
                    .ifBlank { nestedObj.optString("url") }
                    .ifBlank { nestedObj.optString("path") }
                if (nestedUrl.isNotBlank() && !nestedUrl.equals("null", ignoreCase = true)) {
                    return resolveSupabaseMediaUrl(nestedUrl, defaultBucket = "avatars")
                }
            }
            val raw = obj.optString(key)
            if (raw.isNotBlank() && !raw.equals("null", ignoreCase = true)) {
                return resolveSupabaseMediaUrl(raw, defaultBucket = "avatars")
            }
        }
        // Check nested metadata objects if present on profile/user row
        val metaKeys = listOf("user_metadata", "raw_user_meta_data", "metadata", "data")
        for (mk in metaKeys) {
            val metaObj = obj.optJSONObject(mk)
            if (metaObj != null) {
                val fromMeta = extractAvatarFromJson(metaObj)
                if (!fromMeta.isNullOrBlank()) return fromMeta
            }
        }
        // Dynamic key scan for any custom avatar/profile photo column
        val iterator = obj.keys()
        while (iterator.hasNext()) {
            val k = iterator.next()
            val lower = k.lowercase()
            if (
                lower.contains("avatar") ||
                lower.contains("profile_pic") ||
                lower.contains("profile_img") ||
                lower.contains("pfp") ||
                lower.contains("user_pic")
            ) {
                val raw = obj.optString(k)
                if (raw.isNotBlank() && !raw.equals("null", ignoreCase = true)) {
                    return resolveSupabaseMediaUrl(raw, defaultBucket = "avatars")
                }
            }
        }
        return null
    }

    private fun extractNestedProfileObject(obj: JSONObject): JSONObject? {
        val keys = listOf("profiles", "profile", "user", "users", "author", "creator", "user_profiles")
        for (k in keys) {
            val nestedObj = obj.optJSONObject(k)
            if (nestedObj != null) return nestedObj
            val nestedArr = obj.optJSONArray(k)
            if (nestedArr != null && nestedArr.length() > 0) {
                val first = nestedArr.optJSONObject(0)
                if (first != null) return first
            }
        }
        return null
    }

    private fun parseAuthSession(
        bodyStr: String,
        fallbackEmail: String,
        fallbackUsername: String = "",
        fallbackDisplayName: String = ""
    ): SupabaseSession {
        val root = try {
            JSONObject(bodyStr)
        } catch (_: Exception) {
            JSONObject()
        }
        val nestedSession = root.optJSONObject("session")
        val rawAccessToken = root.optString("access_token", "")
            .ifBlank { nestedSession?.optString("access_token", "").orEmpty() }
        val refreshToken = root.optString("refresh_token", "")
            .ifBlank { nestedSession?.optString("refresh_token", "").orEmpty() }
        val userObj = root.optJSONObject("user")
            ?: nestedSession?.optJSONObject("user")
            ?: root
        val email = userObj.optString("email").ifBlank { fallbackEmail }
        val deterministicFallbackId = if (email.isNotBlank()) {
            UUID.nameUUIDFromBytes(email.trim().lowercase().toByteArray()).toString()
        } else {
            UUID.randomUUID().toString()
        }
        val userId = userObj.optString("id")
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: deterministicFallbackId
        val meta = userObj.optJSONObject("user_metadata")
            ?: userObj.optJSONObject("raw_user_meta_data")
        val username = meta?.optString("username")?.takeIf { it.isNotBlank() }
            ?: meta?.optString("preferred_username")?.takeIf { it.isNotBlank() }
            ?: fallbackUsername.ifBlank { email.substringBefore("@").ifBlank { "creator" } }
        val displayName = meta?.optString("full_name")?.takeIf { it.isNotBlank() }
            ?: meta?.optString("display_name")?.takeIf { it.isNotBlank() }
            ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
            ?: fallbackDisplayName.ifBlank { username }
        val avatarUrl = extractAvatarFromJson(meta) ?: extractAvatarFromJson(userObj)
        val effectiveAccessToken = rawAccessToken.ifBlank { "pulse_session_$userId" }

        return SupabaseSession(
            accessToken = effectiveAccessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = email,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl
        )
    }

    private fun parseSupabaseError(bodyStr: String, fallback: String): String {
        return try {
            val obj = JSONObject(bodyStr)
            obj.optString("error_description")
                .ifBlank { obj.optString("msg") }
                .ifBlank { obj.optString("message") }
                .ifBlank { obj.optString("error") }
                .ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun parsePostsArray(
        jsonStr: String,
        currentUserId: String?,
        forceLoop: Boolean = false,
        offset: Int = 0
    ): List<PostEntity> {
        val list = mutableListOf<PostEntity>()
        val fallbackDrawables = listOf(
            R.drawable.img_post_kyoto_1794021771843,
            R.drawable.img_post_architecture_1794021771801,
            R.drawable.img_post_portrait_1794021773202,
            R.drawable.img_post_alpine_1794021773292
        )
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: "post_${offset}_$i"
                val profileObj = extractNestedProfileObject(obj)

                val authorId = obj.optString("author_id")
                    .ifBlank { obj.optString("user_id") }
                    .ifBlank { obj.optString("creator_id") }
                    .ifBlank { profileObj?.optString("id").orEmpty() }
                    .ifBlank { profileObj?.optString("user_id").orEmpty() }
                    .ifBlank { "creator_$i" }

                val authorUsername = obj.optString("author_username")
                    .ifBlank { obj.optString("username") }
                    .ifBlank { profileObj?.optString("username").orEmpty() }
                    .ifBlank { profileObj?.optString("handle").orEmpty() }
                    .ifBlank { "creator_${authorId.take(6)}" }

                val authorDisplayName = obj.optString("author_display_name")
                    .ifBlank { profileObj?.optString("full_name").orEmpty() }
                    .ifBlank { profileObj?.optString("display_name").orEmpty() }
                    .ifBlank { profileObj?.optString("name").orEmpty() }
                    .ifBlank { authorUsername }

                val authorAvatarUrl = extractAvatarFromJson(obj) ?: extractAvatarFromJson(profileObj)

                val rawThumbnailCandidate = obj.optString("thumbnail_url")
                    .ifBlank { obj.optString("cover_url") }
                    .ifBlank { obj.optString("poster_url") }
                    .ifBlank { obj.optString("preview_url") }
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                val rawImageCandidate = obj.optString("image_url")
                    .ifBlank { obj.optString("media_url") }
                    .ifBlank { obj.optString("photo_url") }
                    .ifBlank { obj.optString("image_uri") }
                    .ifBlank { rawThumbnailCandidate.orEmpty() }
                    .ifBlank { obj.optString("url") }
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                val rawVideoCandidate = obj.optString("video_url")
                    .ifBlank { obj.optString("video_uri") }
                    .ifBlank { obj.optString("video") }
                    .ifBlank { obj.optString("reel_url") }
                    .ifBlank { obj.optString("loop_url") }
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                val mediaType = obj.optString("media_type").ifBlank { obj.optString("type") }.lowercase()
                val isMediaVideo = mediaType == "video" ||
                    mediaType == "reel" ||
                    mediaType == "loop" ||
                    obj.optBoolean("is_video", false) ||
                    isVideoUrl(rawVideoCandidate) ||
                    isVideoUrl(rawImageCandidate)

                val resolvedVideoUri = when {
                    !rawVideoCandidate.isNullOrBlank() -> resolveSupabaseMediaUrl(rawVideoCandidate, defaultBucket = "videos")
                    isMediaVideo && !rawImageCandidate.isNullOrBlank() -> resolveSupabaseMediaUrl(rawImageCandidate, defaultBucket = "videos")
                    else -> null
                }

                val resolvedImageUri = when {
                    !rawThumbnailCandidate.isNullOrBlank() && !isVideoUrl(rawThumbnailCandidate) ->
                        resolveSupabaseMediaUrl(rawThumbnailCandidate, defaultBucket = "posts")
                    !rawImageCandidate.isNullOrBlank() && !isVideoUrl(rawImageCandidate) ->
                        resolveSupabaseMediaUrl(rawImageCandidate, defaultBucket = "posts")
                    else -> null
                }

                val caption = obj.optString("caption")
                    .ifBlank { obj.optString("content") }
                    .ifBlank { obj.optString("description") }
                    .ifBlank { obj.optString("title") }
                    .ifBlank { obj.optString("text") }

                val location = obj.optString("location").ifBlank { "Pulse Network" }
                val category = obj.optString("category").ifBlank {
                    if (isMediaVideo || forceLoop) "Loops" else "Photography"
                }
                val tagsCsv = obj.optString("tags_csv").ifBlank { obj.optString("tags") }
                val likeCount = obj.optInt("like_count", obj.optInt("likes_count", 0))
                val commentCount = obj.optInt("comment_count", obj.optInt("comments_count", 0))
                val isLoop = forceLoop || isMediaVideo || obj.optBoolean("is_loop", false)
                val createdAtStr = obj.optString("created_at")
                val timestampMs = obj.optLong("timestamp_ms", parseIsoTimestamp(createdAtStr))

                val hasResolvedPostMedia = !resolvedImageUri.isNullOrBlank() || !resolvedVideoUri.isNullOrBlank()
                list.add(
                    PostEntity(
                        id = id,
                        authorId = authorId,
                        authorUsername = authorUsername,
                        authorDisplayName = authorDisplayName,
                        authorAvatarRes = fallbackDrawables[i % fallbackDrawables.size],
                        authorAvatarUrl = authorAvatarUrl,
                        authorVerified = obj.optBoolean("author_verified", true),
                        imageRes = if (hasResolvedPostMedia) 0 else fallbackDrawables[i % fallbackDrawables.size],
                        imageUri = resolvedImageUri,
                        videoUri = resolvedVideoUri,
                        carouselImageUrisCsv = obj.optString("carousel_image_uris_csv").ifBlank { obj.optString("carousel_urls") },
                        aspectRatioName = obj.optString("aspect_ratio_name", PhotoAspectRatio.PORTRAIT_4_5.name),
                        filterPresetName = obj.optString("filter_preset_name", FilmPreset.NATURAL.name),
                        caption = caption,
                        location = location,
                        category = category,
                        tagsCsv = tagsCsv,
                        likeCount = likeCount,
                        isLiked = obj.optBoolean("is_liked", false),
                        isBookmarked = obj.optBoolean("is_bookmarked", false),
                        commentCount = commentCount,
                        isLoop = isLoop,
                        audioTrack = obj.optString("audio_track").ifBlank { "Original Studio Audio" },
                        cameraSpec = obj.optString("camera_spec", if (resolvedVideoUri != null) "Pulse Cinema 4K" else "Pulse Optical"),
                        createdAtLabel = formatTimestampLabel(createdAtStr),
                        timestampMs = timestampMs
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun parseStoriesArray(jsonStr: String): List<StoryEntity> {
        val list = mutableListOf<StoryEntity>()
        val fallbackDrawables = listOf(
            R.drawable.img_post_kyoto_1794021771843,
            R.drawable.img_post_architecture_1794021771801,
            R.drawable.img_post_portrait_1794021773202,
            R.drawable.img_post_alpine_1794021773292
        )
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val profileObj = extractNestedProfileObject(obj)
                val id = obj.optString("id")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: "story_$i"
                val authorId = obj.optString("author_id")
                    .ifBlank { obj.optString("user_id") }
                    .ifBlank { profileObj?.optString("id").orEmpty() }
                val username = obj.optString("author_username")
                    .ifBlank { profileObj?.optString("username").orEmpty() }
                    .ifBlank { "creator_${authorId.take(5)}" }
                val displayName = obj.optString("author_display_name")
                    .ifBlank { profileObj?.optString("full_name").orEmpty() }
                    .ifBlank { profileObj?.optString("display_name").orEmpty() }
                    .ifBlank { username }
                val rawImage = obj.optString("image_url")
                    .ifBlank { obj.optString("media_url") }
                    .ifBlank { obj.optString("image_uri") }
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val rawVideo = obj.optString("video_url")
                    .ifBlank { obj.optString("video_uri") }
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                val resolvedImage = resolveSupabaseMediaUrl(rawImage, defaultBucket = "stories")
                val resolvedVideo = resolveSupabaseMediaUrl(rawVideo, defaultBucket = "stories")
                    ?: resolvedImage?.takeIf { isVideoUrl(it) }

                val caption = obj.optString("caption_overlay")
                    .ifBlank { obj.optString("caption") }
                    .ifBlank { obj.optString("content") }
                val hasResolvedStoryMedia = !resolvedImage.isNullOrBlank() || !resolvedVideo.isNullOrBlank()

                list.add(
                    StoryEntity(
                        id = id,
                        authorId = authorId,
                        authorUsername = username,
                        authorDisplayName = displayName,
                        authorAvatarRes = fallbackDrawables[i % fallbackDrawables.size],
                        authorAvatarUrl = extractAvatarFromJson(obj) ?: extractAvatarFromJson(profileObj),
                        imageRes = if (hasResolvedStoryMedia) 0 else fallbackDrawables[i % fallbackDrawables.size],
                        imageUri = resolvedImage,
                        videoUri = resolvedVideo,
                        filterPresetName = obj.optString("filter_preset_name", FilmPreset.NATURAL.name),
                        captionOverlay = caption,
                        locationTag = obj.optString("location_tag", "Live Story"),
                        isViewed = false,
                        isHighlight = obj.optBoolean("is_highlight", false),
                        highlightTitle = obj.optString("highlight_title", "Featured"),
                        createdAtLabel = formatTimestampLabel(obj.optString("created_at"))
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun parseProfilesArray(jsonStr: String, currentUserId: String?): List<CreatorProfileEntity> {
        val list = mutableListOf<CreatorProfileEntity>()
        val fallbackDrawables = listOf(
            R.drawable.img_post_portrait_1794021773202,
            R.drawable.img_post_architecture_1794021771801,
            R.drawable.img_post_kyoto_1794021771843,
            R.drawable.img_post_alpine_1794021773292
        )
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val rawUserId = obj.optString("user_id")
                    .ifBlank { obj.optString("auth_id") }
                    .ifBlank { obj.optString("uid") }
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val rawId = obj.optString("id")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val primaryId = rawUserId ?: rawId ?: continue

                val username = obj.optString("username")
                    .ifBlank { obj.optString("handle") }
                    .ifBlank { obj.optString("user_name") }
                    .ifBlank { obj.optString("email").substringBefore("@") }
                    .ifBlank { "user_${primaryId.take(6)}" }
                val displayName = obj.optString("full_name")
                    .ifBlank { obj.optString("display_name") }
                    .ifBlank { obj.optString("name") }
                    .ifBlank { username }

                val avatarUrl = extractAvatarFromJson(obj)
                    ?: obj.optString("image_url").takeIf { it.isNotBlank() && !it.equals("null", true) }
                        ?.let { resolveSupabaseMediaUrl(it, defaultBucket = "avatars") }
                    ?: obj.optString("media_url").takeIf { it.isNotBlank() && !it.equals("null", true) }
                        ?.let { resolveSupabaseMediaUrl(it, defaultBucket = "avatars") }

                val entity = CreatorProfileEntity(
                    id = primaryId,
                    username = username,
                    displayName = displayName,
                    roleTag = obj.optString("role_tag").ifBlank { obj.optString("role", "Creator") },
                    bio = obj.optString("bio", ""),
                    website = obj.optString("website", ""),
                    location = obj.optString("location", ""),
                    avatarRes = fallbackDrawables[i % fallbackDrawables.size],
                    avatarUrl = avatarUrl,
                    followersCount = obj.optInt("followers_count", obj.optInt("followers", 0)),
                    followingCount = obj.optInt("following_count", obj.optInt("following", 0)),
                    isFollowing = obj.optBoolean("is_following", false),
                    isVerified = obj.optBoolean("is_verified", false)
                )
                list.add(entity)
                if (rawId != null && rawUserId != null && !rawId.equals(rawUserId, ignoreCase = true)) {
                    list.add(entity.copy(id = rawId))
                }
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun parseCommentsArray(jsonStr: String): List<CommentEntity> {
        val list = mutableListOf<CommentEntity>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val profileObj = extractNestedProfileObject(obj)
                val id = obj.optString("id")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: "comment_$i"
                val postId = obj.optString("post_id")
                val username = obj.optString("author_username")
                    .ifBlank { profileObj?.optString("username").orEmpty() }
                    .ifBlank { obj.optString("user_id").take(6) }
                    .ifBlank { "user" }
                val displayName = obj.optString("author_display_name")
                    .ifBlank { profileObj?.optString("full_name").orEmpty() }
                    .ifBlank { profileObj?.optString("display_name").orEmpty() }
                    .ifBlank { username }
                val text = obj.optString("text")
                    .ifBlank { obj.optString("content") }
                    .ifBlank { obj.optString("body") }

                list.add(
                    CommentEntity(
                        id = id,
                        postId = postId,
                        authorUsername = username,
                        authorDisplayName = displayName,
                        authorAvatarUrl = extractAvatarFromJson(obj) ?: extractAvatarFromJson(profileObj),
                        text = text,
                        likeCount = obj.optInt("like_count", 0),
                        timeAgo = formatTimestampLabel(obj.optString("created_at"))
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun parseMessagesArray(jsonStr: String, currentUserId: String?): List<DirectMessageEntity> {
        val list = mutableListOf<DirectMessageEntity>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: "msg_$i"
                val senderId = obj.optString("sender_id")
                val receiverId = obj.optString("receiver_id").ifBlank { obj.optString("participant_id") }
                val isFromMe = currentUserId != null && senderId == currentUserId
                val partnerId = if (isFromMe) receiverId else senderId
                val partnerUsername = obj.optString("participant_username").ifBlank { "user_${partnerId.take(5)}" }

                list.add(
                    DirectMessageEntity(
                        id = id,
                        threadId = obj.optString("thread_id").ifBlank { "thread_$partnerId" },
                        participantId = partnerId,
                        participantUsername = partnerUsername,
                        participantDisplayName = obj.optString("participant_display_name").ifBlank { partnerUsername },
                        participantAvatarUrl = extractAvatarFromJson(obj),
                        isFromMe = isFromMe,
                        text = obj.optString("text").ifBlank { obj.optString("content") },
                        timeLabel = formatTimestampLabel(obj.optString("created_at"))
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun parseActivitiesArray(jsonStr: String, currentUserId: String?): List<ActivityItemEntity> {
        val list = mutableListOf<ActivityItemEntity>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: "act_$i"
                val actorId = obj.optString("actor_id").ifBlank { obj.optString("user_id") }
                val actorUsername = obj.optString("actor_username").ifBlank { "user_${actorId.take(5)}" }
                list.add(
                    ActivityItemEntity(
                        id = id,
                        type = obj.optString("type", "LIKE"),
                        actorId = actorId,
                        actorUsername = actorUsername,
                        actorDisplayName = obj.optString("actor_display_name").ifBlank { actorUsername },
                        actorAvatarUrl = extractAvatarFromJson(obj),
                        message = obj.optString("message").ifBlank { "interacted with your post" },
                        timeLabel = formatTimestampLabel(obj.optString("created_at")),
                        isRead = obj.optBoolean("is_read", false)
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return list.distinctBy { it.id }
    }

    private fun parseIsoTimestamp(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(raw).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun formatTimestampLabel(raw: String): String {
        if (raw.isBlank() || raw == "null") return "Recent"
        return raw.substringBefore("T").ifBlank { "Recent" }
    }
}
