package com.theveloper.pixelplay.online

import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uses Google's separate authorization flow for YouTube Data API access.
 * This reads YouTube playlist metadata only; it does not access YouTube Music audio streams.
 */
class YouTubePlaylistClient(private val activity: Activity) {
    companion object {
        const val READ_ONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"
    }

    suspend fun requestAuthorization(): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(READ_ONLY_SCOPE)))
            .build()
        return Identity.getAuthorizationClient(activity).authorize(request).await()
    }

    fun authorizationResult(data: Intent): AuthorizationResult =
        Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)

    suspend fun fetchMyPlaylists(accessToken: String): List<YouTubePlaylist> =
        withContext(Dispatchers.IO) {
            val url = URL("https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&mine=true&maxResults=50")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                if (code !in 200..299) {
                    val reason = runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrNull()
                    throw IllegalStateException(reason ?: "YouTube API request failed (HTTP $code).")
                }
                val items = JSONObject(body).optJSONArray("items") ?: return@withContext emptyList()
                buildList {
                    for (index in 0 until items.length()) {
                        val item = items.getJSONObject(index)
                        val snippet = item.optJSONObject("snippet") ?: continue
                        add(
                            YouTubePlaylist(
                                id = item.optString("id"),
                                title = snippet.optString("title", "Untitled playlist"),
                                description = snippet.optString("description").takeIf { it.isNotBlank() },
                                thumbnailUrl = snippet.optJSONObject("thumbnails")
                                    ?.optJSONObject("medium")?.optString("url")
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
}

data class YouTubePlaylist(
    val id: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?
)
