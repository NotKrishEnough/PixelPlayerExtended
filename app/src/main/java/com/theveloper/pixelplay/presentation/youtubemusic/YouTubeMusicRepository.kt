package com.theveloper.pixelplay.presentation.youtubemusic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class YouTubeMusicPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String
)

object YouTubeMusicRepository {
    private val client = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
    private const val origin = "https://music.youtube.com"
    private const val userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

    suspend fun fetchPlaylists(cookie: String): List<YouTubeMusicPlaylist> = withContext(Dispatchers.IO) {
        val cookieMap = cookie.split(';').mapNotNull { part ->
            val p = part.trim()
            val i = p.indexOf('=')
            if (i <= 0) null else p.substring(0, i) to p.substring(i + 1)
        }.toMap()
        val sapisid = cookieMap["SAPISID"] ?: cookieMap["__Secure-3PAPISID"]
            ?: error("Session expired. Reconnect your YouTube Music account.")

        val pageRequest = Request.Builder().url(origin).header("Cookie", cookie)
            .header("User-Agent", userAgent).get().build()
        val (apiKey, version, visitorData) = client.newCall(pageRequest).execute().use { response ->
            val html = response.body?.string().orEmpty()
            check(response.isSuccessful) { "YouTube Music config request failed (HTTP ${response.code}). Check your connection and reconnect if needed." }
            fun config(name: String): String? =
                Regex("""["']$name["']\\s*:\\s*["']([^"']+)["']""").find(html)?.groupValues?.get(1)
            val key = config("INNERTUBE_API_KEY")
                ?: error("YouTube Music did not provide API configuration. Reconnect and retry.")
            val clientVersion = config("INNERTUBE_CLIENT_VERSION")
                ?: error("YouTube Music client version was not found. Please update and retry.")
            Triple(key, clientVersion, config("VISITOR_DATA").orEmpty())
        }
        val timestamp = System.currentTimeMillis() / 1000
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$timestamp $sapisid $origin".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val body = JSONObject()
            .put("context", JSONObject().put("client", JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", version)
                 .put("hl", "en").put("gl", "US")
                .apply { if (visitorData.isNotBlank()) put("visitorData", visitorData) }))
            .put("browseId", "FEmusic_library_playlists").toString()
        val url = okhttp3.HttpUrl.Builder().scheme("https").host("music.youtube.com")
            .addPathSegments("youtubei/v1/browse").addQueryParameter("key", apiKey)
            .addQueryParameter("prettyPrint", "false").build()
        val request = Request.Builder().url(url)
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Cookie", cookie)
            .header("Authorization", "SAPISIDHASH ${timestamp}_$digest")
            .header("Origin", origin).header("X-Origin", origin)
            .header("Referer", "$origin/")
            .header("X-Goog-AuthUser", cookieMap["AUTHUSER"] ?: "0")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", version)
            .header("User-Agent", userAgent).build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                val hint = when (response.code) {
                    400 -> "YouTube Music rejected the library request (HTTP 400). Its request parameters may have changed; reconnect and retry."
                    401, 403 -> "YouTube Music rejected this session (HTTP ${response.code}). Reconnect your account and retry."
                    429 -> "YouTube Music is rate-limiting requests. Wait a little and retry."
                    else -> "YouTube Music library request failed (HTTP ${response.code})."
                }
                error(if (detail.isNotBlank()) "$hint $detail" else hint)
            }
            val root = JSONObject(raw)
            val results = mutableListOf<YouTubeMusicPlaylist>()
            fun text(obj: JSONObject?): String {
                if (obj == null) return ""
                obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
                val runs = obj.optJSONArray("runs") ?: return ""
                return (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }
            }
            fun visit(value: Any?) {
                when (value) {
                    is JSONObject -> {
                        val renderer = value.optJSONObject("musicTwoRowItemRenderer")
                            ?: value.optJSONObject("gridPlaylistRenderer")
                            ?: value.optJSONObject("playlistRenderer")
                        if (renderer != null) {
                            val title = text(renderer.optJSONObject("title"))
                            val endpoint = renderer.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                            val id = endpoint?.optString("browseId").orEmpty().ifBlank { renderer.optString("playlistId") }
                            if (id.isNotBlank() && title.isNotBlank()) {
                                val thumb = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                    ?.optJSONObject(0)?.optString("url").orEmpty()
                                results += YouTubeMusicPlaylist(id, title, text(renderer.optJSONObject("subtitle")), thumb)
                            }
                        }
                        val keys = value.keys()
                        while (keys.hasNext()) visit(value.opt(keys.next()))
                    }
                    is org.json.JSONArray -> for (i in 0 until value.length()) visit(value.opt(i))
                }
            }
            visit(root)
            results.distinctBy { it.id }
        }
    }
}
