/*
 * Independent online-music provider contract for PixelPlayerExtended.
 *
 * This file is newly authored and does not derive from DA Tunes or other
 * third-party provider implementations. Provider implementations must use
 * authorized APIs and comply with their terms.
 */
package com.theveloper.pixelplay.online

/**
 * A provider-neutral boundary between online catalog/account features and the
 * app's existing playback/UI layers. This contract intentionally contains no
 * authentication, scraping, stream extraction, or network implementation.
 */
interface OnlineMusicProvider {
    val id: String
    val displayName: String

    suspend fun accountState(): OnlineAccountState

    /** Starts the provider's documented sign-in flow, if supported. */
    suspend fun signIn(): OnlineAccountState

    suspend fun signOut()

    suspend fun search(query: String, pageToken: String? = null): OnlinePage<OnlineTrack>

    /** Returns a playable item only when the provider is authorized to supply it. */
    suspend fun resolvePlayback(track: OnlineTrack): OnlinePlayback?

    suspend fun playlists(pageToken: String? = null): OnlinePage<OnlinePlaylist>
}

sealed interface OnlineAccountState {
    data object SignedOut : OnlineAccountState
    data class SignedIn(val displayName: String?, val accountId: String?) : OnlineAccountState
    data class Unavailable(val reason: String) : OnlineAccountState
}

data class OnlineTrack(
    val id: String,
    val title: String,
    val artists: List<String> = emptyList(),
    val album: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val providerId: String,
)

data class OnlinePlaylist(
    val id: String,
    val title: String,
    val artworkUrl: String? = null,
    val trackCount: Int? = null,
    val providerId: String,
)

data class OnlinePage<T>(
    val items: List<T>,
    val nextPageToken: String? = null,
)

data class OnlinePlayback(
    /** URI must be supplied by an authorized provider implementation. */
    val uri: String,
    val mimeType: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val expiresAtEpochMs: Long? = null,
)
