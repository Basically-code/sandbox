package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.R
import com.example.lockscreen.LockScreenNoteActivity

object HavelNotificationHelper {
    const val CHANNEL_LOCKSCREEN = "havel_lockscreen_capture_v2"
    const val CHANNEL_REMINDERS = "havel_smart_reminders"
    const val CHANNEL_BUBBLE = "havel_floating_bubble"

    const val NOTIFICATION_ID_LOCKSCREEN = 1001
    const val NOTIFICATION_ID_BUBBLE = 1002

    const val KEY_TEXT_REPLY = "key_havel_quick_note"
    const val EXTRA_OPEN_VOICE_CAPTURE = "extra_open_voice_capture"
    const val EXTRA_OPEN_CAPTURE_SHEET = "extra_open_capture_sheet"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // IMPORTANCE_DEFAULT (silent) ensures the notification is prominently visible on the Android Lock Screen
            // rather than minimized/hidden like IMPORTANCE_LOW.
            val lockChannel = NotificationChannel(
                CHANNEL_LOCKSCREEN,
                context.getString(R.string.lock_screen_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.lock_screen_channel_desc)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.reminders_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminders_channel_desc)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val bubbleChannel = NotificationChannel(
                CHANNEL_BUBBLE,
                context.getString(R.string.bubble_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.bubble_channel_desc)
            }

            manager.createNotificationChannel(lockChannel)
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(bubbleChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun buildLockScreenNotification(
        context: Context,
        statusText: String = "Switch on and write along — tap Write, Lock Pad, or Voice Note without unlocking."
    ): Notification {
        ensureChannels(context)

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Write a fleeting thought…")
            .build()

        val replyIntent = Intent(context, DirectReplyReceiver::class.java)
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val writeAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit,
            "Write",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .build()

        val lockPadIntent = LockScreenNoteActivity.createIntent(context, startVoice = false)
        val lockPadPendingIntent = PendingIntent.getActivity(
            context,
            203,
            lockPadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val voiceIntent = LockScreenNoteActivity.createIntent(context, startVoice = true)
        val voicePendingIntent = PendingIntent.getActivity(
            context,
            202,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_LOCKSCREEN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Havel • Lock Screen Instant Capture")
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(lockPadPendingIntent)
            .addAction(writeAction)
            .addAction(android.R.drawable.ic_lock_idle_lock, "Lock Pad", lockPadPendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "Voice Note", voicePendingIntent)
            .build()
    }

    fun showLockScreenQuickCapture(
        context: Context,
        statusText: String = "Switch on and write along — tap Write, Lock Pad, or Voice Note without unlocking."
    ) {
        if (!hasNotificationPermission(context)) return
        val notification = buildLockScreenNotification(context, statusText)
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_LOCKSCREEN, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun dismissLockScreenQuickCapture(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_LOCKSCREEN)
    }
}
