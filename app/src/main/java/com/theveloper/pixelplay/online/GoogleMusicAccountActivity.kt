package com.theveloper.pixelplay.online

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.theveloper.pixelplay.ui.theme.PixelPlayTheme
import kotlinx.coroutines.launch

class GoogleMusicAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelPlayTheme {
                var account by remember { mutableStateOf<GoogleSignInResult?>(null) }
                var busy by remember { mutableStateOf(false) }
                var message by remember { mutableStateOf<String?>(null) }
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
                                        busy = true
                                        message = null
                                        try {
                                            account = signIn.signIn(this@GoogleMusicAccountActivity)
                                        } catch (error: Exception) {
                                            message = error.localizedMessage ?: "Google sign-in failed."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Continue with Google") }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        try {
                                            signIn.signOut()
                                            account = null
                                            message = "Signed out."
                                        } catch (error: Exception) {
                                            message = error.localizedMessage ?: "Sign-out failed."
                                        } finally {
                                            busy = false
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
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Google sign-in verifies your app account. YouTube playlist access needs a separate Google authorization grant; this screen does not request it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
