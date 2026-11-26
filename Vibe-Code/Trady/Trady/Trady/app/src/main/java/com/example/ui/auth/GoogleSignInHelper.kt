package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BorderInput
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val activityContext = context as? Activity ?: context
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type returned.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
    }
  }
}

fun signOut(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

private enum class StartAuthMode {
  SIGN_IN,
  SIGN_UP,
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onEnterWithoutSignUp: () -> Unit = {},
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var activeAuthMode by remember { mutableStateOf<StartAuthMode?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val isLoading = activeAuthMode != null

  val launchGoogleCredentialFlow: (StartAuthMode) -> Unit = { mode ->
    activeAuthMode = mode
    errorMessage = null
    onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        activeAuthMode = null
        onAuthSuccess()
      },
      onAuthError = { msg ->
        activeAuthMode = null
        errorMessage = msg
      },
      scope = coroutineScope,
      onAuthCancelled = { activeAuthMode = null },
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BgPrimaryCanvas)
      .safeDrawingPadding()
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        modifier = Modifier
          .size(112.dp)
          .testTag("start_app_icon"),
        shape = RoundedCornerShape(26.dp),
        color = BgSurfaceCard,
        border = BorderStroke(1.5.dp, BorderHairline),
        shadowElevation = 6.dp,
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_trady_foreground_1794558910635),
          contentDescription = stringResource(R.string.cd_trady_logo),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(18.dp)),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (errorMessage != null) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = StatusDanger.copy(alpha = 0.1f),
          border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.35f)),
        ) {
          Text(
            text = errorMessage ?: "",
            color = StatusDanger,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Button(
        onClick = { launchGoogleCredentialFlow(StartAuthMode.SIGN_IN) },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("start_sign_in_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = BrandPrimary,
          contentColor = Color.White,
        ),
      ) {
        if (activeAuthMode == StartAuthMode.SIGN_IN) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
        } else {
          Text(
            text = stringResource(R.string.start_sign_in),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          )
        }
      }

      OutlinedButton(
        onClick = { launchGoogleCredentialFlow(StartAuthMode.SIGN_UP) },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("start_sign_up_button"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, BorderInput),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = BgSurfaceCard,
          contentColor = BrandPrimary,
        ),
      ) {
        if (activeAuthMode == StartAuthMode.SIGN_UP) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = BrandPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Text(
            text = stringResource(R.string.start_sign_up),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          )
        }
      }

      TextButton(
        onClick = onEnterWithoutSignUp,
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("enter_without_signup_button"),
      ) {
        Text(
          text = stringResource(R.string.enter_without_signup),
          style = MaterialTheme.typography.labelLarge,
          color = TextSecondary,
        )
      }
    }
  }
}
