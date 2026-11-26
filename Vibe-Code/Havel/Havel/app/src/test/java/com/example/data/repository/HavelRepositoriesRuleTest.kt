package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CaptureSource
import com.example.data.model.NotePriority
import com.example.data.model.ReminderCadence
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HavelRepositoriesRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveNote_authenticatedOwner_succeedsAndPreservesRawContent() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = HavelNotesRepository(firestore)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(uid, "Alice", ALICE_EMAIL, null)
        }
        assertTrue(profileRes.isSuccess)

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createNote(
                rawContent = "Record YouTube hook in the elevator before Friday",
                cleanContent = "Record YouTube hook before Friday",
                title = "YouTube Hook",
                tags = listOf("#ScriptIdea"),
                captureSource = CaptureSource.VOICE,
                priority = NotePriority.HIGH,
                deadlineEpochMs = System.currentTimeMillis() + 86_400_000L,
                deadlineLabel = "Friday 5:00 PM",
                reminderCadence = ReminderCadence.ESCALATING_1W_1D_1H,
                aiProcessed = true
            ).getOrThrow()
        }
        assertTrue(noteId.isNotBlank())

        val notes = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeNotes(uid).first { list -> list.any { it.id == noteId } }
        }
        val created = notes.first { it.id == noteId }
        assertEquals("Record YouTube hook in the elevator before Friday", created.rawContent)
        assertEquals(NotePriority.HIGH, created.priority)

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateNoteOrganization(
                noteId = noteId,
                cleanContent = "Refined hook: Stop fighting 3 apps to write 1 thought.",
                title = "Zero-Friction Hook",
                tags = listOf("#ScriptIdea", "#Startup"),
                priority = NotePriority.HIGH,
                deadlineEpochMs = created.deadlineEpochMs,
                deadlineLabel = created.deadlineLabel,
                reminderCadence = ReminderCadence.ESCALATING_1W_1D_1H,
                aiProcessed = true
            )
        }
        assertTrue(updateRes.isSuccess)
    }

    @Test
    fun crossUserNoteAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = HavelNotesRepository(firestore)
        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createNote(rawContent = "Alice private startup pitch note").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = HavelNotesRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getNoteById(aliceUid, noteId)
        }
        assertTrue(result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeNotes_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = HavelNotesRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeNotes("some_random_uid").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED exception", caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun teamPanel_createJoinIdeaAndWrapEvent_enforcesRulesAndSync() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val panelRepo = TeamPanelRepository(firestore)

        val panel = withTimeout(DEFAULT_TIMEOUT_MS) {
            panelRepo.createPanel(
                name = "Havel Launch Sprint",
                description = "Zero-friction brainstorm room",
                ownerName = "Alice"
            ).getOrThrow()
        }

        // Bob joins using invite code
        val bobUid = signInTestUser(BOB_EMAIL)
        val joinedPanelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            panelRepo.joinPanelByCode(panel.inviteCode).getOrThrow()
        }
        assertEquals(panel.id, joinedPanelId)

        val bobPanels = withTimeout(FLOW_TIMEOUT_MS) {
            panelRepo.observePanels(bobUid).first { list -> list.any { it.id == panel.id } }
        }
        val updatedPanel = bobPanels.first { it.id == panel.id }
        assertTrue(bobUid in updatedPanel.memberUids)

        val ideaId = withTimeout(DEFAULT_TIMEOUT_MS) {
            panelRepo.addPanelIdea(
                panel = updatedPanel,
                authorName = "Bob",
                content = "Add calm sage waveform on voice capture sheet",
                tag = "#Startup"
            ).getOrThrow()
        }

        val ideas = withTimeout(FLOW_TIMEOUT_MS) {
            panelRepo.observePanelIdeas(panel.id, bobUid).first { list -> list.any { it.id == ideaId } }
        }
        assertEquals(1, ideas.count { it.id == ideaId })

        val eventId = withTimeout(DEFAULT_TIMEOUT_MS) {
            panelRepo.createBrainstormEvent(
                panel = updatedPanel,
                creatorName = "Bob",
                title = "Onboarding Critique",
                topicPrompt = "How to cut first-note time to < 2s",
                scheduledEpochMs = System.currentTimeMillis() + 3_600_000L,
                scheduledLabel = "Today 4:00 PM"
            ).getOrThrow()
        }

        val wrapRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            panelRepo.wrapBrainstormEvent(
                panelId = panel.id,
                eventId = eventId,
                aiSummary = "Ship lock screen direct reply + floating sage bubble."
            )
        }
        assertTrue(wrapRes.isSuccess)

        // Ensure Charlie (non-member) cannot list Bob/Alice panel ideas
        val charlieUid = signInTestUser(CHARLIE_EMAIL)
        val charlieIdeas = withTimeout(FLOW_TIMEOUT_MS) {
            panelRepo.observePanelIdeas(panel.id, charlieUid).first()
        }
        assertTrue(charlieIdeas.isEmpty())
        assertTrue(aliceUid != bobUid)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_havel@test.com"
        const val BOB_EMAIL = "bob_havel@test.com"
        const val CHARLIE_EMAIL = "charlie_havel@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
