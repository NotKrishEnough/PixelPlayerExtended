package com.theveloper.pixelplay.online

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.theveloper.pixelplay.ui.theme.PixelPlayTheme
import kotlinx.coroutines.launch

class GoogleMusicAccountActivity : ComponentActivity() {
    private val playlistsState = mutableStateOf<List<YouTubePlaylist>>(emptyList())
    private val messageState = mutableStateOf<String?>(null)
    private val busyState = mutableStateOf(false)
    private val playlistClient by lazy { YouTubePlaylistClient(this) }

    private val authorizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) lifecycleScope.launch { completeAuthorization(data) }
                else messageState.value = "Google authorization returned no result."
            } else {
                messageState.value = "YouTube access was not granted."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelPlayTheme {
                var account by remember { mutableStateOf<GoogleSignInResult?>(null) }
                val busy by busyState
                val message by messageState
                val playlists by playlistsState
                val scope = rememberCoroutineScope()
                val signIn = remember { GoogleAppSignIn(this) }

                Scaffold(topBar = { TopAppBar(title = { Text("Google account") }) }) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Connect Google", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            account?.let { it.displayName ?: it.email ?: "Signed in" }
                                ?: "Sign in to connect your Google account to this app.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        account?.email?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(24.dp))
                        if (busy) {
                            CircularProgressIndicator()
                        } else if (account == null) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        busyState.value = true
                                        messageState.value = null
                                        try {
                                            account = signIn.signIn(this@GoogleMusicAccountActivity)
                                        } catch (error: Exception) {
                                            messageState.value = error.localizedMessage ?: "Google sign-in failed."
                                        } finally {
                                            busyState.value = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Continue with Google") }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        busyState.value = true
                                        messageState.value = null
                                        try {
                                            val authorization = playlistClient.requestAuthorization()
                                            val pendingIntent = authorization.pendingIntent
                                            if (pendingIntent != null) {
                                                authorizationLauncher.launch(
                                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                                )
                                            } else {
                                                val token = authorization.accessToken
                                                if (token.isNullOrBlank()) {
                                                    messageState.value = "Google did not return a YouTube access token."
                                                } else {
                                                    loadPlaylists(token)
                                                }
                                            }
                                        } catch (error: Exception) {
                                            messageState.value = error.localizedMessage ?: "YouTube authorization failed."
                                        } finally {
                                            busyState.value = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Load YouTube playlists") }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        busyState.value = true
                                        try {
                                            signIn.signOut()
                                            account = null
                                            playlistsState.value = emptyList()
                                            messageState.value = "Signed out."
                                        } catch (error: Exception) {
                                            messageState.value = error.localizedMessage ?: "Sign-out failed."
                                        } finally {
                                            busyState.value = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Sign out") }
                        }
                        message?.let {
                            Spacer(Modifier.height(16.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        if (playlists.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            Text("Your YouTube playlists", style = MaterialTheme.typography.titleMedium)
                            playlists.forEach { playlist ->
                                Spacer(Modifier.height(8.dp))
                                Text(playlist.title, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Playlist access uses Google's separate YouTube authorization. This loads playlist details only; it does not stream YouTube Music audio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    private suspend fun completeAuthorization(data: Intent) {
        busyState.value = true
        try {
            val token = playlistClient.authorizationResult(data).accessToken
            if (token.isNullOrBlank()) {
                messageState.value = "Google did not return a YouTube access token."
            } else {
                loadPlaylists(token)
            }
        } catch (error: Exception) {
            messageState.value = error.localizedMessage ?: "Could not finish YouTube authorization."
        } finally {
            busyState.value = false
        }
    }

    private suspend fun loadPlaylists(accessToken: String) {
        busyState.value = true
        try {
            playlistsState.value = playlistClient.fetchMyPlaylists(accessToken)
            messageState.value = if (playlistsState.value.isEmpty()) {
                "No playlists were found on this YouTube account."
            } else {
                "Loaded ${playlistsState.value.size} playlists."
            }
        } catch (error: Exception) {
            messageState.value = error.localizedMessage ?: "Could not load YouTube playlists."
        } finally {
            busyState.value = false
        }
    }
}
