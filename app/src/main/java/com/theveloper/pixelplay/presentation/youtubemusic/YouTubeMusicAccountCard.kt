package com.theveloper.pixelplay.presentation.youtubemusic

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun YouTubeMusicAccountCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var playlists by remember { mutableStateOf<List<YouTubeMusicPlaylist>>(emptyList()) }

    suspend fun refresh() {
        val cookie = YouTubeMusicSessionStore.read(context)
        if (cookie.isNullOrBlank()) {
            connected = false
            playlists = emptyList()
            return
        }
        loading = true
        error = null
        try {
            playlists = YouTubeMusicRepository.fetchPlaylists(cookie)
            connected = true
        } catch (t: Throwable) {
            error = t.message ?: "Unable to sync YouTube Music library."
            connected = true
        } finally {
            loading = false
        }
    }

    val loginLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) scope.launch { refresh() }
    }

    LaunchedEffect(Unit) {
        connected = !YouTubeMusicSessionStore.read(context).isNullOrBlank()
        if (connected) refresh()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Rounded.MusicNote, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("YouTube Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (connected) "Account session saved on this device" else "Connect your Google account to sync playlists",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { loginLauncher.launch(Intent(context, YouTubeMusicLoginActivity::class.java)) },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) { Text(if (connected) "Reconnect" else "Connect") }
                if (connected) {
                    OutlinedButton(onClick = { scope.launch { refresh() } }, enabled = !loading) { Text("Refresh") }
                    OutlinedButton(onClick = {
                        YouTubeMusicSessionStore.clear(context)
                        connected = false
                        playlists = emptyList()
                        error = null
                    }, enabled = !loading) { Text("Disconnect") }
                }
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (playlists.isNotEmpty()) {
                Text("Your playlists", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                playlists.take(20).forEach { playlist ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (playlist.thumbnailUrl.isNotBlank()) {
                            AsyncImage(model = playlist.thumbnailUrl, contentDescription = null,
                                modifier = Modifier.size(48.dp))
                        } else {
                            androidx.compose.material3.Icon(Icons.Rounded.MusicNote, contentDescription = null,
                                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            if (playlist.subtitle.isNotBlank()) Text(playlist.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
            if (connected && playlists.isEmpty() && !loading && error == null) {
                Text("No playlists found in this account.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
