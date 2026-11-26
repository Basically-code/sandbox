package com.example.lockscreen

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.data.local.HavelPreferences
import com.example.notifications.HavelNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground companion service that keeps Havel's Lock Screen Availability active
 * ("just switch on and write along") and responds to screen-on events while locked.
 */
class LockScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isLocked = keyguard?.isKeyguardLocked == true
                HavelNotificationHelper.showLockScreenQuickCapture(context)
                if (isLocked) {
                    serviceScope.launch {
                        val settings = HavelPreferences(context).settingsFlow.first()
                        if (settings.lockScreenCaptureEnabled && settings.wakeLockPadOnScreenOn) {
                            runCatching {
                                startActivity(LockScreenNoteActivity.createIntent(context, startVoice = false))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        HavelNotificationHelper.ensureChannels(this)
        val notification = HavelNotificationHelper.buildLockScreenNotification(
            context = this,
            statusText = "Switch on and write along — tap Write, Lock Pad, or Voice Note without unlocking."
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    HavelNotificationHelper.NOTIFICATION_ID_LOCKSCREEN,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(HavelNotificationHelper.NOTIFICATION_ID_LOCKSCREEN, notification)
            }
        } catch (_: Exception) {
            // If foreground start is restricted in background, still post notification
            HavelNotificationHelper.showLockScreenQuickCapture(this)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        HavelNotificationHelper.showLockScreenQuickCapture(this)
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            HavelNotificationHelper.showLockScreenQuickCapture(context)
            runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, LockScreenCaptureService::class.java)
                )
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, LockScreenCaptureService::class.java)
                )
            }
            HavelNotificationHelper.dismissLockScreenQuickCapture(context)
        }
    }
}
