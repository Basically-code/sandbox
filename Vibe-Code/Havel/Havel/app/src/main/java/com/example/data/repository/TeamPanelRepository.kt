package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BrainstormEvent
import com.example.data.model.EventStatus
import com.example.data.model.PanelIdea
import com.example.data.model.PanelInvite
import com.example.data.model.TeamPanel
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

class TeamPanelRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Team Panels.")
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { chars.random() }.joinToString("")
        return "HVL-$suffix"
    }

    fun observePanels(userId: String): Flow<List<TeamPanel>> {
        val query = db.collection("panels")
            .whereArrayContains("memberUids", userId)
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(TeamPanel::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, "panels")
                }
                throw error
            }
    }

    suspend fun createPanel(
        name: String,
        description: String,
        ownerName: String
    ): Result<TeamPanel> = runCatching {
        val uid = requireUserId()
        val panelId = "panel_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val inviteCode = generateInviteCode()
        val panelRef = db.collection("panels").document(panelId)
        val inviteRef = db.collection("panel_invites").document(inviteCode)

        val panelPayload = mapOf(
            "ownerId" to uid,
            "ownerName" to ownerName.ifBlank { "Havel Creator" }.take(100),
            "name" to name.trim().take(80),
            "description" to description.trim().take(300),
            "inviteCode" to inviteCode,
            "memberUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val invitePayload = mapOf(
            "inviteCode" to inviteCode,
            "panelId" to panelId,
            "panelName" to name.trim().take(80),
            "ownerId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            batch.set(panelRef, panelPayload)
            batch.set(inviteRef, invitePayload)
            batch.commit().await()
            TeamPanel(
                id = panelId,
                ownerId = uid,
                ownerName = ownerName.ifBlank { "Havel Creator" },
                name = name.trim(),
                description = description.trim(),
                inviteCode = inviteCode,
                memberUids = listOf(uid)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, panelRef.path)
            throw e
        }
    }

    suspend fun joinPanelByCode(rawCode: String): Result<String> = runCatching {
        val uid = requireUserId()
        val code = rawCode.trim().uppercase()
        require(code.length >= 4) { "Enter a valid invite code (e.g., HVL-8421)." }

        val inviteRef = db.collection("panel_invites").document(code)
        val inviteSnap = try {
            inviteRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, inviteRef.path)
            throw e
        }

        val invite = inviteSnap.toObject(PanelInvite::class.java, ServerTimestampBehavior.ESTIMATE)
            ?: throw IllegalArgumentException("No Team Panel found for code $code.")

        val panelRef = db.collection("panels").document(invite.panelId)
        try {
            panelRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            invite.panelId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, panelRef.path)
            throw e
        }
    }

    fun observePanelIdeas(panelId: String, userId: String): Flow<List<PanelIdea>> {
        val path = "panels/$panelId/ideas"
        val query = db.collection("panels").document(panelId).collection("ideas")
            .whereArrayContains("memberUids", userId)
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(PanelIdea::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addPanelIdea(
        panel: TeamPanel,
        authorName: String,
        content: String,
        tag: String = "",
        sourceNoteId: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val ideaId = "idea_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("panels").document(panel.id).collection("ideas").document(ideaId)
        val payload = mapOf(
            "panelId" to panel.id,
            "authorId" to uid,
            "authorName" to authorName.ifBlank { "Creator" }.take(100),
            "content" to content.trim().take(4000),
            "tag" to tag.trim().take(40),
            "sourceNoteId" to sourceNoteId.take(128),
            "memberUids" to panel.memberUids.distinct().take(50),
            "upvoterUids" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            ideaId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleIdeaUpvote(panelId: String, idea: PanelIdea): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("panels").document(panelId).collection("ideas").document(idea.id)
        val op = if (uid in idea.upvoterUids) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        try {
            docRef.update(
                mapOf(
                    "upvoterUids" to op,
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

    fun observeBrainstormEvents(panelId: String, userId: String): Flow<List<BrainstormEvent>> {
        val path = "panels/$panelId/events"
        val query = db.collection("panels").document(panelId).collection("events")
            .whereArrayContains("memberUids", userId)
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(BrainstormEvent::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createBrainstormEvent(
        panel: TeamPanel,
        creatorName: String,
        title: String,
        topicPrompt: String,
        scheduledEpochMs: Long,
        scheduledLabel: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val eventId = "event_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("panels").document(panel.id).collection("events").document(eventId)
        val payload = mapOf(
            "panelId" to panel.id,
            "creatorId" to uid,
            "creatorName" to creatorName.ifBlank { "Host" }.take(100),
            "title" to title.trim().take(120),
            "topicPrompt" to topicPrompt.trim().take(500),
            "scheduledEpochMs" to scheduledEpochMs.coerceAtLeast(0L),
            "scheduledLabel" to scheduledLabel.trim().take(100),
            "status" to EventStatus.LIVE,
            "aiSummary" to "",
            "participantUids" to listOf(uid),
            "memberUids" to panel.memberUids.distinct().take(50),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            eventId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleEventRsvp(panelId: String, event: BrainstormEvent): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("panels").document(panelId).collection("events").document(event.id)
        val op = if (uid in event.participantUids) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        try {
            docRef.update(
                mapOf(
                    "participantUids" to op,
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

    suspend fun wrapBrainstormEvent(
        panelId: String,
        eventId: String,
        aiSummary: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("panels").document(panelId).collection("events").document(eventId)
        try {
            docRef.update(
                mapOf(
                    "status" to EventStatus.WRAPPED,
                    "aiSummary" to aiSummary.trim().take(4000),
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

    /**
     * Keeps denormalized memberUids synced on subcollection items when new collaborators join.
     */
    suspend fun syncMembershipForPanel(
        panel: TeamPanel,
        ideas: List<PanelIdea>,
        events: List<BrainstormEvent>
    ) {
        val targetMembers = panel.memberUids.distinct().take(50)
        val batch = db.batch()
        var ops = 0
        ideas.filter { it.memberUids.toSet() != targetMembers.toSet() }.forEach { idea ->
            val ref = db.collection("panels").document(panel.id).collection("ideas").document(idea.id)
            batch.update(ref, mapOf("memberUids" to targetMembers, "updatedAt" to FieldValue.serverTimestamp()))
            ops++
        }
        events.filter { it.memberUids.toSet() != targetMembers.toSet() }.forEach { event ->
            val ref = db.collection("panels").document(panel.id).collection("events").document(event.id)
            batch.update(ref, mapOf("memberUids" to targetMembers, "updatedAt" to FieldValue.serverTimestamp()))
            ops++
        }
        if (ops > 0) {
            try {
                batch.commit().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "panels/${panel.id}")
            }
        }
    }
}
