package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.core.FirebaseSafe
import com.example.data.ai.HavelAiService
import com.example.data.local.HavelLocalDatabase
import com.example.data.local.LocalNoteEntity
import com.example.data.model.CaptureSource
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.NotePriority
import com.example.data.model.ReminderCadence
import com.example.data.repository.HavelNotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class DirectReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = remoteInput.getCharSequence(HavelNotificationHelper.KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (replyText.isBlank()) {
            HavelNotificationHelper.showLockScreenQuickCapture(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val aiService = HavelAiService()
                val enrichment = aiService.analyzeCapture(replyText, DEFAULT_VARIETY_TAGS).getOrElse {
                    aiService.analyzeCaptureLocally(replyText, DEFAULT_VARIETY_TAGS)
                }

                val priority = if (enrichment.isHighPriority) NotePriority.HIGH else NotePriority.NORMAL
                val cadence = if (priority == NotePriority.HIGH || enrichment.deadlineEpochMs > 0L) {
                    ReminderCadence.ESCALATING_1W_1D_1H
                } else {
                    ReminderCadence.DAILY_DIGEST
                }
                val uid = FirebaseSafe.currentUserOrNull(context)?.uid ?: FirebaseSafe.LOCAL_USER_ID
                val noteId = "note_ls_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                val now = System.currentTimeMillis()

                val localNote = LocalNoteEntity(
                    id = noteId,
                    userId = uid,
                    rawContent = replyText,
                    cleanContent = enrichment.cleanContent,
                    title = enrichment.title,
                    tagsPipeSeparated = enrichment.tags.joinToString("|"),
                    captureSource = CaptureSource.LOCK_SCREEN,
                    priority = priority,
                    deadlineEpochMs = enrichment.deadlineEpochMs,
                    deadlineLabel = enrichment.deadlineLabel,
                    reminderCadence = cadence,
                    aiProcessed = true,
                    isArchived = false,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )

                val db = HavelLocalDatabase.getInstance(context)
                db.localWorkspaceDao().upsertNote(localNote)

                if (priority == NotePriority.HIGH && enrichment.deadlineEpochMs > 0L) {
                    ReminderScheduler.scheduleEscalatingCascade(context, localNote.toNoteItem())
                }

                // Also sync to Firestore if signed in
                if (FirebaseSafe.currentUserOrNull(context) != null) {
                    runCatching {
                        val repo = HavelNotesRepository(context)
                        repo.createNote(
                            rawContent = replyText,
                            cleanContent = enrichment.cleanContent,
                            title = enrichment.title,
                            tags = enrichment.tags,
                            captureSource = CaptureSource.LOCK_SCREEN,
                            priority = priority,
                            deadlineEpochMs = enrichment.deadlineEpochMs,
                            deadlineLabel = enrichment.deadlineLabel,
                            reminderCadence = cadence,
                            aiProcessed = true
                        )
                    }
                }

                val tagBadge = enrichment.tags.joinToString(" ")
                HavelNotificationHelper.showLockScreenQuickCapture(
                    context = context,
                    statusText = "Saved ($tagBadge): \"${enrichment.title}\" — tap Write or Lock Pad for next thought."
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
