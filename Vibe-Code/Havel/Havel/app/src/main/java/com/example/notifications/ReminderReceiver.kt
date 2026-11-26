package com.example.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!HavelNotificationHelper.hasNotificationPermission(context)) return
        HavelNotificationHelper.ensureChannels(context)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Havel Priority Reminder" }
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val stageLabel = intent.getStringExtra(EXTRA_STAGE_LABEL).orEmpty()
        val deadlineLabel = intent.getStringExtra(EXTRA_DEADLINE_LABEL).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 3001)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val header = listOfNotNull(
            stageLabel.takeIf { it.isNotBlank() },
            deadlineLabel.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        val notification = NotificationCompat.Builder(context, HavelNotificationHelper.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (header.isNotBlank()) "$title ($header)" else title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_STAGE_LABEL = "extra_stage_label"
        const val EXTRA_DEADLINE_LABEL = "extra_deadline_label"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
