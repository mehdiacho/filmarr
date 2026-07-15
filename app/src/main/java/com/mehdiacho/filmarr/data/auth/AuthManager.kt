package com.mehdiacho.filmarr.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mehdiacho.filmarr.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper over FirebaseAuth. Exposes the current user as a [StateFlow] so the nav shell can
 * bind/unbind the library store on sign-in changes, and offers email/password + Google entry points.
 */
object AuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    init {
        auth.addAuthStateListener { _user.value = it.currentUser }
    }

    val isSignedIn: Boolean get() = auth.currentUser != null

    suspend fun signInEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await(); Unit
    }

    suspend fun signUpEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email.trim(), password).await(); Unit
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = runCatching {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await(); Unit
    }

    fun signOut() = auth.signOut()
}

/**
 * Launches the Credential Manager Google chooser and completes Firebase sign-in with the returned
 * ID token. [context] must be an Activity context so the system UI can attach.
 */
suspend fun signInWithGoogle(context: Context): Result<Unit> = runCatching {
    val credentialManager = CredentialManager.create(context)
    val webClientId = context.getString(R.string.default_web_client_id)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(false)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    val response = credentialManager.getCredential(context, request)
    val credential = response.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        AuthManager.signInWithGoogleIdToken(googleCredential.idToken).getOrThrow()
    } else {
        error("Unexpected credential type: ${credential.type}")
    }
}
