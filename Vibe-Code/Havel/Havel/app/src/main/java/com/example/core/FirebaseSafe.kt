package com.example.core

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Safe wrapper around Firebase initialization so Havel runs in 0ms Local-First mode
 * even when google-services.json is absent or the device is offline.
 */
object FirebaseSafe {
    private const val TAG = "FirebaseSafe"

    const val LOCAL_USER_ID = "local_havel_creator"
    const val LOCAL_USER_NAME = "Havel Creator"
    const val LOCAL_USER_EMAIL = "local@havel.notes"

    fun isInitialized(context: Context? = null): Boolean {
        return try {
            if (FirebaseApp.getApps(context ?: return FirebaseApp.getInstance() != null).isNotEmpty()) {
                true
            } else if (context != null) {
                // Only attempt initializeApp if google_app_id resource is present
                val appIdRes = context.resources.getIdentifier("google_app_id", "string", context.packageName)
                if (appIdRes != 0 && context.getString(appIdRes).isNotBlank()) {
                    FirebaseApp.initializeApp(context) != null
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not configured, using Local-First mode: ${e.message}")
            false
        }
    }

    fun authOrNull(context: Context? = null): FirebaseAuth? {
        return try {
            if (context != null && !isInitialized(context)) return null
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    fun currentUserOrNull(context: Context? = null): FirebaseUser? {
        return authOrNull(context)?.currentUser
    }

    fun firestoreOrNull(context: Context): FirebaseFirestore? {
        return try {
            if (!isInitialized(context)) return null
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(databaseId)
        } catch (_: Exception) {
            null
        }
    }

    fun webClientIdOrNull(context: Context): String? {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId).takeIf { it.isNotBlank() } else null
        } catch (_: Exception) {
            null
        }
    }
}
