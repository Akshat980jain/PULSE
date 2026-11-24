package com.example.ui.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Silent auto sign-in delegating to [FirebaseAuthHelper].
 */
fun attemptSilentGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    FirebaseAuthHelper(
        context = context,
        providedCredentialManager = credentialManager
    ).attemptSilentSignIn(
        scope = scope,
        onAuthSuccess = { onAuthSuccess() },
        onUnauthenticated = onUnauthenticated
    )
}

/**
 * Interactive Google Sign-In delegating to [FirebaseAuthHelper].
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    FirebaseAuthHelper(
        context = context,
        providedCredentialManager = credentialManager
    ).signInWithGoogle(
        activityContext = context,
        scope = scope,
        onAuthSuccess = { onAuthSuccess() },
        onAuthError = onAuthError,
        onAuthCancelled = onAuthCancelled
    )
}

/**
 * Safe sign-out delegating to [FirebaseAuthHelper].
 */
fun signOutFirebaseMessaging(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    FirebaseAuthHelper(
        context = context,
        providedCredentialManager = credentialManager
    ).signOut(
        scope = scope,
        onSignOutComplete = onSignOutComplete
    )
}
