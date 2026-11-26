package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority
import com.example.data.model.ReminderCadence
import com.example.data.model.StitchedScript
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class HavelNotesRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<UserProfile> = runCatching {
        val docRef = db.collection("users").document(userId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            val existing = snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            if (existing != null) return@runCatching existing
        }
        val profile = UserProfile(
            id = userId,
            userId = userId,
            displayName = displayName.ifBlank { "Havel Creator" },
            email = email,
            photoUrl = photoUrl,
            defaultTags = DEFAULT_VARIETY_TAGS
        )
        try {
            docRef.set(profile.toCreateMap()).await()
            profile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { it.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun updateDefaultTags(userId: String, tags: List<String>): Result<Unit> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            docRef.update(
                mapOf(
                    "defaultTags" to tags.distinct().take(15),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeNotes(userId: String): Flow<List<NoteItem>> {
        val colRef = db.collection("users").document(userId).collection("notes")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(NoteItem::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun getNoteById(userId: String, noteId: String): Result<NoteItem?> = runCatching {
        val docRef = db.collection("users").document(userId).collection("notes").document(noteId)
        try {
            docRef.get().await().toObject(NoteItem::class.java, ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createNote(
        rawContent: String,
        cleanContent: String = rawContent,
        title: String = "",
        tags: List<String> = emptyList(),
        captureSource: String = "in_app",
        priority: String = NotePriority.NORMAL,
        deadlineEpochMs: Long = 0L,
        deadlineLabel: String = "",
        reminderCadence: String = if (priority == NotePriority.HIGH || deadlineEpochMs > 0L) {
            ReminderCadence.ESCALATING_1W_1D_1H
        } else {
            ReminderCadence.DAILY_DIGEST
        },
        aiProcessed: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val noteId = "note_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
        val note = NoteItem(
            id = noteId,
            userId = uid,
            rawContent = rawContent.trim(),
            cleanContent = cleanContent.trim(),
            title = title.trim(),
            tags = tags.distinct().take(10),
            captureSource = captureSource,
            priority = priority,
            deadlineEpochMs = deadlineEpochMs.coerceAtLeast(0L),
            deadlineLabel = deadlineLabel.trim(),
            reminderCadence = reminderCadence,
            aiProcessed = aiProcessed,
            isArchived = false
        )
        try {
            docRef.set(note.toCreateMap()).await()
            noteId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateNoteOrganization(
        noteId: String,
        cleanContent: String,
        title: String,
        tags: List<String>,
        priority: String,
        deadlineEpochMs: Long,
        deadlineLabel: String,
        reminderCadence: String,
        aiProcessed: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(noteId.isNotBlank()) { "Note ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
        val payload = mapOf(
            "cleanContent" to cleanContent.trim().take(5000),
            "title" to title.trim().take(120),
            "tags" to tags.distinct().take(10),
            "priority" to priority,
            "deadlineEpochMs" to deadlineEpochMs.coerceAtLeast(0L),
            "deadlineLabel" to deadlineLabel.trim().take(100),
            "reminderCadence" to reminderCadence,
            "aiProcessed" to aiProcessed,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun setNoteArchived(noteId: String, archived: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
        try {
            docRef.update(
                mapOf(
                    "isArchived" to archived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeStitchedScripts(userId: String): Flow<List<StitchedScript>> {
        val colRef = db.collection("users").document(userId).collection("stitched_scripts")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(StitchedScript::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun createStitchedScript(
        title: String,
        themeTag: String,
        insightSummary: String,
        scriptBody: String,
        linkedNoteIds: List<String>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val scriptId = "script_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("stitched_scripts").document(scriptId)
        val script = StitchedScript(
            id = scriptId,
            userId = uid,
            title = title.trim(),
            themeTag = themeTag.trim().ifBlank { "#ScriptIdea" },
            insightSummary = insightSummary.trim(),
            scriptBody = scriptBody.trim(),
            linkedNoteIds = linkedNoteIds.distinct().take(30),
            linkedNoteCount = linkedNoteIds.distinct().size.coerceIn(1, 30).toLong()
        )
        try {
            docRef.set(script.toCreateMap()).await()
            scriptId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStitchedScript(scriptId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("stitched_scripts").document(scriptId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
