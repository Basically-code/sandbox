package com.example

import com.example.data.ai.HavelAiService
import com.example.data.local.LocalNoteEntity
import com.example.data.model.BrainstormEvent
import com.example.data.model.CaptureSource
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority
import com.example.data.model.PanelIdea
import com.example.data.model.ReminderCadence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val aiService = HavelAiService()

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun analyzeCaptureLocally_autoClassifiesTagsAndPolishesSpeech() {
        val enrichment = aiService.analyzeCaptureLocally(
            "um like record a YouTube video hook about zero friction lock screen notes"
        )
        assertTrue(enrichment.title.isNotBlank())
        assertTrue(enrichment.cleanContent.endsWith("."))
        assertFalse(enrichment.cleanContent.lowercase().startsWith("um"))
        assertTrue("#ScriptIdea" in enrichment.tags)
    }

    @Test
    fun analyzeCaptureLocally_detectsExplicitDeadlineAndHighPriority() {
        val enrichment = aiService.analyzeCaptureLocally(
            "Send Havel startup pitch deck by Friday"
        )
        assertTrue(enrichment.isHighPriority)
        assertTrue(enrichment.deadlineEpochMs > System.currentTimeMillis())
        assertTrue(enrichment.deadlineLabel.isNotBlank())
        assertTrue("#Startup" in enrichment.tags)
    }

    @Test
    fun synthesizeStitchedScriptLocally_createsNonDestructiveScriptFromRelatedNotes() {
        val notes = listOf(
            NoteItem(
                id = "n1",
                title = "Lock Screen Hook",
                rawContent = "Unlocking your phone destroys fleeting creative ideas.",
                cleanContent = "Unlocking your phone destroys fleeting creative ideas.",
                tags = listOf("#ScriptIdea")
            ),
            NoteItem(
                id = "n2",
                title = "Floating Bubble Flow",
                rawContent = "Tap the sage bubble over any screen and jot in zero milliseconds.",
                cleanContent = "Tap the sage bubble over any screen and jot in zero milliseconds.",
                tags = listOf("#ScriptIdea")
            ),
            NoteItem(
                id = "n3",
                title = "Stitched Script Ending",
                rawContent = "Let Havel's AI stitch your fragmented thoughts into one cohesive script.",
                cleanContent = "Let Havel's AI stitch your fragmented thoughts into one cohesive script.",
                tags = listOf("#ScriptIdea")
            )
        )

        val stitched = aiService.synthesizeStitchedScriptLocally(notes, "#ScriptIdea")
        assertEquals("#ScriptIdea", stitched.themeTag)
        assertTrue(stitched.title.contains("Stitched Script"))
        assertTrue(stitched.scriptBody.contains("1. OPENING HOOK"))
        assertTrue(stitched.scriptBody.contains("2. CORE NARRATIVE"))
        assertTrue(stitched.scriptBody.contains("3. CLOSING CALL TO ACTION"))
    }

    @Test
    fun summarizeBrainstormEventLocally_ranksTopUpvotedSparks() {
        val event = BrainstormEvent(
            id = "e1",
            title = "Zero-Friction Sprint",
            topicPrompt = "How do we make lock screen capture instant?"
        )
        val ideas = listOf(
            PanelIdea(
                id = "i1",
                authorName = "Alex",
                content = "Direct Reply notification bar",
                tag = "#Startup",
                upvoterUids = listOf("u1")
            ),
            PanelIdea(
                id = "i2",
                authorName = "Maya",
                content = "Full Lock Screen Pad with showWhenLocked",
                tag = "#ScriptIdea",
                upvoterUids = listOf("u1", "u2", "u3")
            )
        )

        val summary = aiService.summarizeBrainstormEventLocally(event, ideas)
        assertTrue(summary.contains("Zero-Friction Sprint"))
        assertTrue(summary.contains("Maya"))
    }

    @Test
    fun localNoteEntity_roundTripsToNoteItemPreservingFields() {
        val entity = LocalNoteEntity(
            id = "note_ls_1",
            userId = "local_havel_creator",
            rawContent = "Quick lock screen thought",
            cleanContent = "Quick lock screen thought.",
            title = "Quick Lock Screen Thought",
            tagsPipeSeparated = "#ScriptIdea|#VoiceDraft",
            captureSource = CaptureSource.LOCK_SCREEN,
            priority = NotePriority.HIGH,
            deadlineEpochMs = 1800000000000L,
            deadlineLabel = "Tomorrow • 5:00 PM",
            reminderCadence = ReminderCadence.ESCALATING_1W_1D_1H,
            aiProcessed = true
        )

        val item = entity.toNoteItem()
        assertEquals("note_ls_1", item.id)
        assertEquals(listOf("#ScriptIdea", "#VoiceDraft"), item.tags)
        assertEquals(CaptureSource.LOCK_SCREEN, item.captureSource)
        assertEquals(NotePriority.HIGH, item.priority)
        assertEquals(ReminderCadence.ESCALATING_1W_1D_1H, item.reminderCadence)
    }
}
