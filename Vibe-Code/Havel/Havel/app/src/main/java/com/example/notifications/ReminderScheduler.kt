package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority

object ReminderScheduler {
    private const val ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000L
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    private const val ONE_HOUR_MS = 60 * 60 * 1000L

    fun scheduleEscalatingCascade(context: Context, note: NoteItem) {
        cancelCascade(context, note.id)
        if (note.priority != NotePriority.HIGH || note.deadlineEpochMs <= 0L || note.isArchived) {
            return
        }

        val now = System.currentTimeMillis()
        val stages = listOf(
            Triple(0, note.deadlineEpochMs - ONE_WEEK_MS, "1 week left"),
            Triple(1, note.deadlineEpochMs - ONE_DAY_MS, "1 day left"),
            Triple(2, note.deadlineEpochMs - ONE_HOUR_MS, "1 hour left")
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        stages.forEach { (stageIdx, triggerAt, stageLabel) ->
            if (triggerAt > now + 5_000L) {
                val pendingIntent = buildPendingIntent(
                    context = context,
                    noteId = note.id,
                    stageIndex = stageIdx,
                    title = note.title.ifBlank { "High-Priority Note" },
                    body = note.cleanContent.ifBlank { note.rawContent },
                    stageLabel = stageLabel,
                    deadlineLabel = note.deadlineLabel
                )
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    fun cancelCascade(context: Context, noteId: String) {
        if (noteId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (stageIdx in 0..2) {
            val pendingIntent = buildPendingIntent(
                context = context,
                noteId = noteId,
                stageIndex = stageIdx,
                title = "",
                body = "",
                stageLabel = "",
                deadlineLabel = ""
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun buildPendingIntent(
        context: Context,
        noteId: String,
        stageIndex: Int,
        title: String,
        body: String,
        stageLabel: String,
        deadlineLabel: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_STAGE_LABEL, stageLabel)
            putExtra(ReminderReceiver.EXTRA_DEADLINE_LABEL, deadlineLabel)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, (noteId.hashCode() * 31) + stageIndex)
        }
        val requestCode = (noteId.hashCode() and 0x0FFFFFFF) + stageIndex
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
