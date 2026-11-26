package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object CaptureSource {
    const val IN_APP = "in_app"
    const val VOICE = "voice"
    const val LOCK_SCREEN = "lock_screen"
    const val FLOATING_BUBBLE = "floating_bubble"
}

object NotePriority {
    const val NORMAL = "normal"
    const val HIGH = "high"
}

object ReminderCadence {
    const val NONE = "none"
    const val DAILY_DIGEST = "daily_digest"
    const val ESCALATING_1W_1D_1H = "escalating_1w_1d_1h"
}

object EventStatus {
    const val UPCOMING = "upcoming"
    const val LIVE = "live"
    const val WRAPPED = "wrapped"
}

val DEFAULT_VARIETY_TAGS = listOf(
    "#ScriptIdea",
    "#Startup",
    "#Meeting",
    "#Grocery",
    "#VoiceDraft",
    "#Personal"
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val defaultTags: List<String> = DEFAULT_VARIETY_TAGS,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "displayName" to displayName.ifBlank { "Havel Creator" }.take(100),
            "email" to email.take(200),
            "defaultTags" to defaultTags.take(15),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        photoUrl?.takeIf { it.isNotBlank() }?.let { map["photoUrl"] = it.take(500) }
        return map
    }
}

@IgnoreExtraProperties
data class NoteItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val rawContent: String = "",
    val cleanContent: String = "",
    val title: String = "",
    val tags: List<String> = emptyList(),
    val captureSource: String = CaptureSource.IN_APP,
    val priority: String = NotePriority.NORMAL,
    val deadlineEpochMs: Long = 0L,
    val deadlineLabel: String = "",
    val reminderCadence: String = ReminderCadence.DAILY_DIGEST,
    val aiProcessed: Boolean = false,
    val isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "rawContent" to rawContent.take(5000),
        "cleanContent" to cleanContent.take(5000),
        "title" to title.take(120),
        "tags" to tags.take(10),
        "captureSource" to captureSource,
        "priority" to priority,
        "deadlineEpochMs" to deadlineEpochMs,
        "deadlineLabel" to deadlineLabel.take(100),
        "reminderCadence" to reminderCadence,
        "aiProcessed" to aiProcessed,
        "isArchived" to isArchived,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class StitchedScript(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val themeTag: String = "#ScriptIdea",
    val insightSummary: String = "",
    val scriptBody: String = "",
    val linkedNoteIds: List<String> = emptyList(),
    val linkedNoteCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(160),
        "themeTag" to themeTag.ifBlank { "#ScriptIdea" }.take(40),
        "insightSummary" to insightSummary.take(600),
        "scriptBody" to scriptBody.take(12000),
        "linkedNoteIds" to linkedNoteIds.take(30),
        "linkedNoteCount" to linkedNoteCount.coerceIn(1L, 30L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class TeamPanel(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val description: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PanelInvite(
    @DocumentId val id: String = "",
    val inviteCode: String = "",
    val panelId: String = "",
    val panelName: String = "",
    val ownerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PanelIdea(
    @DocumentId val id: String = "",
    val panelId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val tag: String = "",
    val sourceNoteId: String = "",
    val memberUids: List<String> = emptyList(),
    val upvoterUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BrainstormEvent(
    @DocumentId val id: String = "",
    val panelId: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val topicPrompt: String = "",
    val scheduledEpochMs: Long = 0L,
    val scheduledLabel: String = "",
    val status: String = EventStatus.UPCOMING,
    val aiSummary: String = "",
    val participantUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
