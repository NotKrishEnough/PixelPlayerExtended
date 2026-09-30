/*
 * App-only Google account sign-in for PixelPlayerExtended.
 *
 * Uses Credential Manager; it does not add the selected account to Android
 * system account settings. The ID token is for app authentication. YouTube
 * playlist access requires a separate, explicit Google authorization flow.
 */
package com.theveloper.pixelplay.online

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.theveloper.pixelplay.BuildConfig

data class GoogleSignInResult(
    val idToken: String,
    val displayName: String?,
    val email: String?,
)

class GoogleAppSignIn(context: Context) {
    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    suspend fun signIn(activity: Activity): GoogleSignInResult {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        require(clientId.isNotBlank()) {
            "GOOGLE_WEB_CLIENT_ID is missing. Add it to the root local.properties file."
        }

        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val response = credentialManager.getCredential(activity, request)
        val credential = response.credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Google sign-in did not return a Google ID token credential." }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return GoogleSignInResult(
            idToken = googleCredential.idToken,
            displayName = googleCredential.displayName,
            email = googleCredential.id,
        )
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
