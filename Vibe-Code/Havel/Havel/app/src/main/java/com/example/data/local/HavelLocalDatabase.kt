package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BrainstormEvent
import com.example.data.model.CaptureSource
import com.example.data.model.EventStatus
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority
import com.example.data.model.PanelIdea
import com.example.data.model.ReminderCadence
import com.example.data.model.StitchedScript
import com.example.data.model.TeamPanel
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "pending_captures")
data class PendingCaptureEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val rawContent: String,
    val captureSource: String,
    val presetTag: String = "",
    val priorityOverride: String = "",
    val deadlineEpochMs: Long = 0L,
    val deadlineLabel: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_notes")
data class LocalNoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val rawContent: String,
    val cleanContent: String,
    val title: String,
    val tagsPipeSeparated: String,
    val captureSource: String,
    val priority: String,
    val deadlineEpochMs: Long,
    val deadlineLabel: String,
    val reminderCadence: String,
    val aiProcessed: Boolean,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toNoteItem(): NoteItem = NoteItem(
        id = id,
        userId = userId,
        rawContent = rawContent,
        cleanContent = cleanContent,
        title = title,
        tags = tagsPipeSeparated.split("|").map { it.trim() }.filter { it.isNotBlank() },
        captureSource = captureSource,
        priority = priority,
        deadlineEpochMs = deadlineEpochMs,
        deadlineLabel = deadlineLabel,
        reminderCadence = reminderCadence,
        aiProcessed = aiProcessed,
        isArchived = isArchived,
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(updatedAtEpochMs))
    )

    companion object {
        fun fromNoteItem(note: NoteItem): LocalNoteEntity {
            val now = System.currentTimeMillis()
            return LocalNoteEntity(
                id = note.id,
                userId = note.userId,
                rawContent = note.rawContent,
                cleanContent = note.cleanContent,
                title = note.title,
                tagsPipeSeparated = note.tags.joinToString("|"),
                captureSource = note.captureSource,
                priority = note.priority,
                deadlineEpochMs = note.deadlineEpochMs,
                deadlineLabel = note.deadlineLabel,
                reminderCadence = note.reminderCadence,
                aiProcessed = note.aiProcessed,
                isArchived = note.isArchived,
                createdAtEpochMs = note.createdAt?.toDate()?.time ?: now,
                updatedAtEpochMs = note.updatedAt?.toDate()?.time ?: now
            )
        }
    }
}

@Entity(tableName = "local_stitched_scripts")
data class LocalStitchedScriptEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val themeTag: String,
    val insightSummary: String,
    val scriptBody: String,
    val linkedNoteIdsPipeSeparated: String,
    val linkedNoteCount: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toStitchedScript(): StitchedScript {
        val ids = linkedNoteIdsPipeSeparated.split("|").map { it.trim() }.filter { it.isNotBlank() }
        return StitchedScript(
            id = id,
            userId = userId,
            title = title,
            themeTag = themeTag,
            insightSummary = insightSummary,
            scriptBody = scriptBody,
            linkedNoteIds = ids,
            linkedNoteCount = linkedNoteCount.coerceAtLeast(ids.size.toLong()),
            createdAt = Timestamp(Date(createdAtEpochMs)),
            updatedAt = Timestamp(Date(createdAtEpochMs))
        )
    }
}

@Entity(tableName = "local_team_panels")
data class LocalTeamPanelEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val ownerName: String,
    val name: String,
    val description: String,
    val inviteCode: String,
    val memberUidsPipeSeparated: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toTeamPanel(): TeamPanel = TeamPanel(
        id = id,
        ownerId = ownerId,
        ownerName = ownerName,
        name = name,
        description = description,
        inviteCode = inviteCode,
        memberUids = memberUidsPipeSeparated.split("|").map { it.trim() }.filter { it.isNotBlank() },
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(updatedAtEpochMs))
    )
}

@Entity(tableName = "local_panel_ideas")
data class LocalPanelIdeaEntity(
    @PrimaryKey val id: String,
    val panelId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val tag: String,
    val sourceNoteId: String,
    val upvoterUidsPipeSeparated: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toPanelIdea(): PanelIdea = PanelIdea(
        id = id,
        panelId = panelId,
        authorId = authorId,
        authorName = authorName,
        content = content,
        tag = tag,
        sourceNoteId = sourceNoteId,
        memberUids = listOf(authorId),
        upvoterUids = upvoterUidsPipeSeparated.split("|").map { it.trim() }.filter { it.isNotBlank() },
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(createdAtEpochMs))
    )
}

@Entity(tableName = "local_brainstorm_events")
data class LocalBrainstormEventEntity(
    @PrimaryKey val id: String,
    val panelId: String,
    val creatorId: String,
    val creatorName: String,
    val title: String,
    val topicPrompt: String,
    val scheduledEpochMs: Long,
    val scheduledLabel: String,
    val status: String,
    val aiSummary: String,
    val participantUidsPipeSeparated: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toBrainstormEvent(): BrainstormEvent = BrainstormEvent(
        id = id,
        panelId = panelId,
        creatorId = creatorId,
        creatorName = creatorName,
        title = title,
        topicPrompt = topicPrompt,
        scheduledEpochMs = scheduledEpochMs,
        scheduledLabel = scheduledLabel,
        status = status,
        aiSummary = aiSummary,
        participantUids = participantUidsPipeSeparated.split("|").map { it.trim() }.filter { it.isNotBlank() },
        memberUids = listOf(creatorId),
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(createdAtEpochMs))
    )
}

@Dao
interface PendingCaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(capture: PendingCaptureEntity): Long

    @Query("SELECT * FROM pending_captures ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<PendingCaptureEntity>>

    @Query("SELECT * FROM pending_captures ORDER BY createdAtEpochMs ASC")
    suspend fun getAllSync(): List<PendingCaptureEntity>

    @Query("DELETE FROM pending_captures WHERE localId = :localId")
    suspend fun deleteById(localId: Long)
}

@Dao
interface LocalWorkspaceDao {
    @Query("SELECT * FROM local_notes ORDER BY createdAtEpochMs DESC")
    fun observeNotes(): Flow<List<LocalNoteEntity>>

    @Query("SELECT * FROM local_notes ORDER BY createdAtEpochMs DESC")
    suspend fun getNotesSync(): List<LocalNoteEntity>

    @Query("SELECT COUNT(*) FROM local_notes")
    suspend fun getNotesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: LocalNoteEntity)

    @Query("DELETE FROM local_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("UPDATE local_notes SET isArchived = :archived, updatedAtEpochMs = :now WHERE id = :noteId")
    suspend fun setNoteArchived(noteId: String, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM local_stitched_scripts ORDER BY createdAtEpochMs DESC")
    fun observeScripts(): Flow<List<LocalStitchedScriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScript(script: LocalStitchedScriptEntity)

    @Query("DELETE FROM local_stitched_scripts WHERE id = :scriptId")
    suspend fun deleteScript(scriptId: String)

    @Query("SELECT * FROM local_team_panels ORDER BY updatedAtEpochMs DESC")
    fun observePanels(): Flow<List<LocalTeamPanelEntity>>

    @Query("SELECT * FROM local_team_panels WHERE inviteCode = :code LIMIT 1")
    suspend fun findPanelByInviteCode(code: String): LocalTeamPanelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPanel(panel: LocalTeamPanelEntity)

    @Query("SELECT * FROM local_panel_ideas WHERE panelId = :panelId ORDER BY createdAtEpochMs DESC")
    fun observePanelIdeas(panelId: String): Flow<List<LocalPanelIdeaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPanelIdea(idea: LocalPanelIdeaEntity)

    @Query("SELECT * FROM local_brainstorm_events WHERE panelId = :panelId ORDER BY createdAtEpochMs DESC")
    fun observeBrainstormEvents(panelId: String): Flow<List<LocalBrainstormEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBrainstormEvent(event: LocalBrainstormEventEntity)
}

@Database(
    entities = [
        PendingCaptureEntity::class,
        LocalNoteEntity::class,
        LocalStitchedScriptEntity::class,
        LocalTeamPanelEntity::class,
        LocalPanelIdeaEntity::class,
        LocalBrainstormEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HavelLocalDatabase : RoomDatabase() {
    abstract fun pendingCaptureDao(): PendingCaptureDao
    abstract fun localWorkspaceDao(): LocalWorkspaceDao

    companion object {
        @Volatile
        private var INSTANCE: HavelLocalDatabase? = null

        fun getInstance(context: Context): HavelLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HavelLocalDatabase::class.java,
                    "havel_local_captures.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        suspend fun seedStarterWorkspaceIfEmpty(context: Context, userId: String, userName: String) {
            val prefs = context.applicationContext.getSharedPreferences("havel_seed_state", Context.MODE_PRIVATE)
            if (prefs.getBoolean("seeded_v2", false)) return
            val dao = getInstance(context).localWorkspaceDao()
            if (dao.getNotesCount() > 0) {
                prefs.edit().putBoolean("seeded_v2", true).apply()
                return
            }

            val now = System.currentTimeMillis()
            val twoDaysMs = 2 * 86_400_000L

            // 1. Lock Screen captured thought
            dao.upsertNote(
                LocalNoteEntity(
                    id = "note_seed_lockscreen_1",
                    userId = userId,
                    rawContent = "Open video with the lock screen problem: every time I unlock my phone to write one idea, 4 notifications hijack my attention.",
                    cleanContent = "Open video with the lock screen problem: every time I unlock my phone to write one idea, 4 notifications hijack my attention.",
                    title = "Lock Screen Attention Hook",
                    tagsPipeSeparated = "#ScriptIdea|#VoiceDraft",
                    captureSource = CaptureSource.LOCK_SCREEN,
                    priority = NotePriority.NORMAL,
                    deadlineEpochMs = 0L,
                    deadlineLabel = "",
                    reminderCadence = ReminderCadence.DAILY_DIGEST,
                    aiProcessed = true,
                    createdAtEpochMs = now - 3_600_000L,
                    updatedAtEpochMs = now - 3_600_000L
                )
            )

            // 2. Floating Bubble captured thought (#ScriptIdea #2)
            dao.upsertNote(
                LocalNoteEntity(
                    id = "note_seed_bubble_2",
                    userId = userId,
                    rawContent = "Middle beat for the YouTube script: show the floating sage bubble over a browser article so creators never leave their reading flow.",
                    cleanContent = "Middle beat for the YouTube script: show the floating sage bubble over a browser article so creators never leave their reading flow.",
                    title = "On-Spot Bubble Demo Beat",
                    tagsPipeSeparated = "#ScriptIdea|#Startup",
                    captureSource = CaptureSource.FLOATING_BUBBLE,
                    priority = NotePriority.NORMAL,
                    deadlineEpochMs = 0L,
                    deadlineLabel = "",
                    reminderCadence = ReminderCadence.DAILY_DIGEST,
                    aiProcessed = true,
                    createdAtEpochMs = now - 7_200_000L,
                    updatedAtEpochMs = now - 7_200_000L
                )
            )

            // 3. Voice captured thought (#ScriptIdea #3 -> triggers 3+ Idea Linking suggestion!)
            dao.upsertNote(
                LocalNoteEntity(
                    id = "note_seed_voice_3",
                    userId = userId,
                    rawContent = "Closing call to action for the script: don't organize folders at capture time, just switch on and write along and let Havel stitch your raw voice notes later.",
                    cleanContent = "Closing call to action for the script: don't organize folders at capture time — just switch on and write along, and let Havel stitch your raw voice notes later.",
                    title = "Zero-Choice CTA Ending",
                    tagsPipeSeparated = "#ScriptIdea|#VoiceDraft",
                    captureSource = CaptureSource.VOICE,
                    priority = NotePriority.NORMAL,
                    deadlineEpochMs = 0L,
                    deadlineLabel = "",
                    reminderCadence = ReminderCadence.DAILY_DIGEST,
                    aiProcessed = true,
                    createdAtEpochMs = now - 10_800_000L,
                    updatedAtEpochMs = now - 10_800_000L
                )
            )

            // 4. High-Priority Escalating Reminder note (1w -> 1d -> 1h)
            dao.upsertNote(
                LocalNoteEntity(
                    id = "note_seed_priority_4",
                    userId = userId,
                    rawContent = "Send Havel product architecture pitch deck and lock screen prototype before Friday 5 PM",
                    cleanContent = "Send Havel product architecture pitch deck and lock screen prototype before Friday 5:00 PM.",
                    title = "Send Havel Pitch Deck",
                    tagsPipeSeparated = "#Startup|#Meeting",
                    captureSource = CaptureSource.IN_APP,
                    priority = NotePriority.HIGH,
                    deadlineEpochMs = now + twoDaysMs,
                    deadlineLabel = "In 2 days • 5:00 PM",
                    reminderCadence = ReminderCadence.ESCALATING_1W_1D_1H,
                    aiProcessed = true,
                    createdAtEpochMs = now - 1_800_000L,
                    updatedAtEpochMs = now - 1_800_000L
                )
            )

            // 5. Starter Team Panel + Ideas + Live Brainstorm Event
            val panelId = "panel_havel_studio"
            dao.upsertPanel(
                LocalTeamPanelEntity(
                    id = panelId,
                    ownerId = userId,
                    ownerName = userName,
                    name = "Havel Creator Lab",
                    description = "Zero-friction product & content brainstorming room",
                    inviteCode = "HVL-2026",
                    memberUidsPipeSeparated = userId,
                    createdAtEpochMs = now - 86_400_000L,
                    updatedAtEpochMs = now - 1_200_000L
                )
            )
            dao.upsertPanelIdea(
                LocalPanelIdeaEntity(
                    id = "idea_seed_1",
                    panelId = panelId,
                    authorId = userId,
                    authorName = userName,
                    content = "Lock screen pad should let you jot multiple rapid thoughts without unlocking the phone.",
                    tag = "#Startup",
                    sourceNoteId = "note_seed_lockscreen_1",
                    upvoterUidsPipeSeparated = userId,
                    createdAtEpochMs = now - 3_600_000L
                )
            )
            dao.upsertPanelIdea(
                LocalPanelIdeaEntity(
                    id = "idea_seed_2",
                    panelId = panelId,
                    authorId = "teammate_maya",
                    authorName = "Maya (Product)",
                    content = "Keep original voice notes verbatim and generate a separate Stitched Script card when 3+ notes match.",
                    tag = "#ScriptIdea",
                    sourceNoteId = "",
                    upvoterUidsPipeSeparated = "$userId|teammate_maya",
                    createdAtEpochMs = now - 5_400_000L
                )
            )
            dao.upsertBrainstormEvent(
                LocalBrainstormEventEntity(
                    id = "event_seed_1",
                    panelId = panelId,
                    creatorId = userId,
                    creatorName = userName,
                    title = "Zero-Friction Capture Sprint",
                    topicPrompt = "How do we cut idea-to-saved-note time to under 0.5 seconds on Lock Screen?",
                    scheduledEpochMs = now + 3_600_000L,
                    scheduledLabel = "Live Now",
                    status = EventStatus.LIVE,
                    aiSummary = "",
                    participantUidsPipeSeparated = "$userId|teammate_maya",
                    createdAtEpochMs = now - 3_600_000L
                )
            )

            prefs.edit().putBoolean("seeded_v2", true).apply()
        }
    }
}
