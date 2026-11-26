package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.FirebaseSafe
import com.example.core.configureAppCheck
import com.example.notifications.HavelNotificationHelper
import com.example.ui.auth.SignInScreen
import com.example.ui.screens.HavelAppShell
import com.example.ui.theme.HavelTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

internal fun FirebaseAuth?.authStateFlow(): Flow<FirebaseUser?> {
    if (this == null) return flowOf(null)
    return callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        addAuthStateListener(listener)
        awaitClose { removeAuthStateListener(listener) }
    }
}

class MainActivity : ComponentActivity() {

    private var openCaptureFromIntent by mutableStateOf(false)
    private var openVoiceFromIntent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Safely configure Firebase App Check if Firebase is available
        configureAppCheck(this, intent)

        handleLaunchIntent(intent)
        enableEdgeToEdge()

        setContent {
            HavelTheme {
                HavelRootContent(
                    openCaptureImmediately = openCaptureFromIntent,
                    openVoiceImmediately = openVoiceFromIntent,
                    onConsumedLaunchTrigger = {
                        openCaptureFromIntent = false
                        openVoiceFromIntent = false
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(HavelNotificationHelper.EXTRA_OPEN_VOICE_CAPTURE, false)) {
            openVoiceFromIntent = true
            intent.removeExtra(HavelNotificationHelper.EXTRA_OPEN_VOICE_CAPTURE)
        }
        if (intent.getBooleanExtra(HavelNotificationHelper.EXTRA_OPEN_CAPTURE_SHEET, false)) {
            openCaptureFromIntent = true
            intent.removeExtra(HavelNotificationHelper.EXTRA_OPEN_CAPTURE_SHEET)
        }
    }
}

@Composable
fun HavelRootContent(
    openCaptureImmediately: Boolean,
    openVoiceImmediately: Boolean,
    onConsumedLaunchTrigger: () -> Unit,
    auth: FirebaseAuth? = FirebaseSafe.authOrNull(LocalContext.current)
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth?.currentUser)
    val user = currentUser
    // Havel is engineered for 0ms zero-friction entry: open directly into the Local-First Workspace
    // without blocking behind a sign-in wall, while supporting optional Google Cloud Sign-In.
    var useLocalFirstMode by remember { mutableStateOf(true) }

    if (user == null && !useLocalFirstMode) {
        SignInScreen(
            onAuthSuccess = { useLocalFirstMode = true },
            onContinueLocal = { useLocalFirstMode = true }
        )
    } else {
        val uid = user?.uid ?: FirebaseSafe.LOCAL_USER_ID
        val name = user?.displayName.orEmpty().ifBlank { FirebaseSafe.LOCAL_USER_NAME }
        val email = user?.email.orEmpty().ifBlank { FirebaseSafe.LOCAL_USER_EMAIL }
        val photoUrl = user?.photoUrl?.toString()

        key(uid) {
            HavelAppShell(
                currentUserId = uid,
                currentUserName = name,
                currentUserEmail = email,
                currentUserPhotoUrl = photoUrl,
                openCaptureImmediately = openCaptureImmediately,
                openVoiceImmediately = openVoiceImmediately,
                onConsumedLaunchTrigger = onConsumedLaunchTrigger
            )
        }
    }
}
