package com.example.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.HavelPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores Havel's Lock Screen Availability after device reboot or app update.
 */
class HavelBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = HavelPreferences(context).settingsFlow.first()
                    if (settings.lockScreenCaptureEnabled) {
                        LockScreenCaptureService.start(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
