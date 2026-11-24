package com.example.ui.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface FirebaseAuthSignInState {
    data object SignedOut : FirebaseAuthSignInState
    data object SigningIn : FirebaseAuthSignInState
    data class SignedIn(val user: FirebaseUser) : FirebaseAuthSignInState
    data class Error(val message: String) : FirebaseAuthSignInState
}

/**
 * Helper class that safely initializes [FirebaseAuth], observes live Firebase sign-in state,
 * and integrates with Jetpack [CredentialManager] and Google Identity (`GetSignInWithGoogleOption`
 * / `GetGoogleIdOption`) using `R.string.default_web_client_id`.
 */
class FirebaseAuthHelper(
    context: Context,
    providedAuth: FirebaseAuth? = null,
    providedCredentialManager: CredentialManager? = null
) {
    private val appContext: Context = context.applicationContext

    val firebaseAuth: FirebaseAuth? = providedAuth ?: runCatching {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        Firebase.auth
    }.getOrNull()

    private val credentialManager: CredentialManager by lazy {
        providedCredentialManager ?: CredentialManager.create(appContext)
    }

    private val _signInState = MutableStateFlow<FirebaseAuthSignInState>(
        firebaseAuth?.currentUser?.let { FirebaseAuthSignInState.SignedIn(it) }
            ?: FirebaseAuthSignInState.SignedOut
    )
    val signInState: StateFlow<FirebaseAuthSignInState> = _signInState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    val isSignedIn: Boolean
        get() = firebaseAuth?.currentUser != null

    fun currentUserIdOrNull(): String? = firebaseAuth?.currentUser?.uid?.takeIf { it.isNotBlank() }

    fun requireUserId(): String = currentUserIdOrNull()
        ?: throw IllegalStateException("User must be signed in with Google before accessing Firebase.")

    /**
     * Emits the current [FirebaseUser] whenever Firebase Authentication state changes.
     */
    fun authStateFlow(): Flow<FirebaseUser?> {
        val auth = firebaseAuth ?: return flowOf(null)
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { state ->
                val user = state.currentUser
                _signInState.value = if (user != null) {
                    FirebaseAuthSignInState.SignedIn(user)
                } else if (_signInState.value !is FirebaseAuthSignInState.SigningIn &&
                    _signInState.value !is FirebaseAuthSignInState.Error
                ) {
                    FirebaseAuthSignInState.SignedOut
                } else {
                    _signInState.value
                }
                trySend(user)
            }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    /**
     * Attempts silent auto sign-in for returning users using [GetGoogleIdOption] ONLY
     * (single credential option per [GetCredentialRequest]).
     */
    suspend fun attemptSilentSignIn(): Result<FirebaseUser> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase Authentication is not initialized."))
        auth.currentUser?.let { existing ->
            _signInState.value = FirebaseAuthSignInState.SignedIn(existing)
            return Result.success(existing)
        }

        val clientId = resolveWebClientId()
            ?: return Result.failure(
                IllegalStateException("Google Sign-In configuration missing: default_web_client_id not found")
            )

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credential = credentialManager.getCredential(appContext, request).credential
            exchangeGoogleCredential(auth, credential)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun attemptSilentSignIn(
        scope: CoroutineScope,
        onAuthSuccess: (FirebaseUser) -> Unit = {},
        onUnauthenticated: () -> Unit = {}
    ) {
        scope.launch {
            attemptSilentSignIn()
                .onSuccess(onAuthSuccess)
                .onFailure { onUnauthenticated() }
        }
    }

    /**
     * Launches interactive Google Sign-In using [GetSignInWithGoogleOption] ONLY
     * (single credential option per [GetCredentialRequest]).
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        val auth = firebaseAuth
            ?: return Result.failure<FirebaseUser>(
                IllegalStateException("Firebase Authentication is not initialized.")
            ).also {
                _signInState.value = FirebaseAuthSignInState.Error("Firebase Authentication is unavailable.")
            }

        val clientId = resolveWebClientId()
        if (clientId == null) {
            val msg = "Google Sign-In configuration missing: default_web_client_id not found"
            _signInState.value = FirebaseAuthSignInState.Error(msg)
            return Result.failure(IllegalStateException(msg))
        }

        _signInState.value = FirebaseAuthSignInState.SigningIn
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val activity = activityContext.findActivity()
                ?: throw IllegalStateException("Activity context required for Google Sign-In.")
            val credential = credentialManager.getCredential(activity, request).credential
            exchangeGoogleCredential(auth, credential).onFailure { err ->
                _signInState.value = FirebaseAuthSignInState.Error(
                    err.localizedMessage ?: "Google Sign-In failed. Please try again."
                )
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            _signInState.value = auth.currentUser?.let { FirebaseAuthSignInState.SignedIn(it) }
                ?: FirebaseAuthSignInState.SignedOut
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            _signInState.value = FirebaseAuthSignInState.Error(
                e.localizedMessage ?: "Google Sign-In failed. Please try again."
            )
            Result.failure(e)
        }
    }

    fun signInWithGoogle(
        activityContext: Context,
        scope: CoroutineScope,
        onAuthSuccess: (FirebaseUser) -> Unit = {},
        onAuthError: (String) -> Unit = {},
        onAuthCancelled: () -> Unit = {}
    ) {
        scope.launch {
            signInWithGoogle(activityContext)
                .onSuccess(onAuthSuccess)
                .onFailure { err ->
                    if (err is GetCredentialCancellationException) {
                        onAuthCancelled()
                    } else {
                        onAuthError(err.localizedMessage ?: "Google Sign-In failed. Please try again.")
                    }
                }
        }
    }

    /**
     * Clears Credential Manager state and signs out of Firebase Authentication inside
     * [NonCancellable] so UI unmount never interrupts session teardown.
     */
    suspend fun signOut() {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                runCatching { firebaseAuth?.signOut() }
                _signInState.value = FirebaseAuthSignInState.SignedOut
            }
        }
    }

    fun signOut(
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            signOut()
            onSignOutComplete()
        }
    }

    fun clearError() {
        if (_signInState.value is FirebaseAuthSignInState.Error) {
            _signInState.value = firebaseAuth?.currentUser?.let { FirebaseAuthSignInState.SignedIn(it) }
                ?: FirebaseAuthSignInState.SignedOut
        }
    }

    private suspend fun exchangeGoogleCredential(
        auth: FirebaseAuth,
        credential: androidx.credentials.Credential
    ): Result<FirebaseUser> {
        if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return Result.failure(IllegalStateException("Unexpected credential type returned from Google Sign-In."))
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authResult = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        val signedInUser = authResult.user ?: auth.currentUser
            ?: return Result.failure(IllegalStateException("Firebase sign-in returned no authenticated user."))
        _signInState.value = FirebaseAuthSignInState.SignedIn(signedInUser)
        return Result.success(signedInUser)
    }

    private fun resolveWebClientId(): String? = try {
        appContext.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private companion object {
        const val TAG = "Auth"
    }
}
