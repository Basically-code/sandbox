package com.example.data.ai

import android.util.Log
import com.example.core.FirebaseSafe
import com.example.data.model.BrainstormEvent
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.NoteItem
import com.example.data.model.PanelIdea
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NoteAiEnrichment(
    val title: String,
    val cleanContent: String,
    val tags: List<String>,
    val isHighPriority: Boolean,
    val deadlineIsoOrEmpty: String,
    val deadlineLabel: String,
    val deadlineEpochMs: Long
)

data class StitchedScriptDraft(
    val title: String,
    val themeTag: String,
    val insightSummary: String,
    val scriptBody: String
)

class HavelAiService {

    private val noteSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "A concise 2-6 word title capturing the core thought"),
                "cleanContent" to Schema.string(description = "Lightly stitched, punctuated readable version of the raw note without changing the user's voice"),
                "tags" to Schema.array(
                    items = Schema.string(),
                    description = "1 to 3 hashtags matching the note's topic (preferring available template tags like #ScriptIdea, #Startup, #Meeting, #Grocery, #VoiceDraft, #Personal)"
                ),
                "isHighPriority" to Schema.boolean(description = "True only if urgent, explicitly important, or contains a deadline/time commitment"),
                "deadlineOffsetDays" to Schema.integer(description = "Integer days from today if a deadline is mentioned (0 = today, 1 = tomorrow, 7 = next week, -1 if no deadline)"),
                "deadlineLabel" to Schema.string(description = "Human-readable deadline label such as 'Fri, Nov 15 • 5:00 PM' or empty string if none")
            )
        )
    }

    private val stitchedScriptSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Compelling title for the cohesive synthesized idea script"),
                "themeTag" to Schema.string(description = "Single primary hashtag e.g. #ScriptIdea or #Startup"),
                "insightSummary" to Schema.string(description = "1-2 sentence summary explaining how these raw notes connect"),
                "scriptBody" to Schema.string(description = "Cohesive, structured script or narrative synthesizing all provided raw notes with clear sections (Hook, Core Idea, Actionable Beats)")
            )
        )
    }

    suspend fun analyzeCapture(
        rawText: String,
        availableTags: List<String> = DEFAULT_VARIETY_TAGS
    ): Result<NoteAiEnrichment> {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Note text is empty."))
        }

        if (FirebaseSafe.isInitialized()) {
            try {
                val model = Firebase.ai.generativeModel(
                    modelName = "gemini-2.5-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = noteSchema
                    }
                )
                val todayLabel = SimpleDateFormat("yyyy-MM-dd (EEE)", Locale.US).format(Date())
                val prompt = """
                    You are Havel's background organization engine.
                    Current date: $todayLabel
                    Available template tags: ${availableTags.joinToString(", ")}
                    
                    Raw captured thought:
                    "$trimmed"
                    
                    Instructions:
                    1. Keep the user's authentic voice in cleanContent — fix speech-to-text stumbles or punctuation gently without losing meaning.
                    2. Classify into 1-3 hashtags (use matching tags from available template tags when appropriate).
                    3. Set isHighPriority = true ONLY if the note sounds urgent or includes a deadline/commitment (e.g., "by Friday", "tomorrow", "pitch deck", "urgent").
                    4. If a deadline is detected, estimate deadlineOffsetDays (0 for today, 1 for tomorrow, etc.) and provide a concise deadlineLabel; otherwise set deadlineOffsetDays = -1 and deadlineLabel = "".
                """.trimIndent()

                val response = withTimeout(AI_TIMEOUT_MS) {
                    model.generateContent(prompt)
                }
                val jsonStr = response.text
                if (!jsonStr.isNullOrBlank()) {
                    val json = JSONObject(jsonStr)
                    val title = json.optString("title", "").trim()
                    val cleanContent = json.optString("cleanContent", trimmed).trim().ifBlank { trimmed }
                    val tagsArray = json.optJSONArray("tags")
                    val tags = buildList {
                        if (tagsArray != null) {
                            for (i in 0 until tagsArray.length()) {
                                val t = tagsArray.optString(i).trim()
                                if (t.isNotBlank()) add(if (t.startsWith("#")) t else "#$t")
                            }
                        }
                    }.distinct().take(4)

                    val isHigh = json.optBoolean("isHighPriority", false)
                    val offsetDays = json.optInt("deadlineOffsetDays", -1)
                    val deadlineLabel = json.optString("deadlineLabel", "").trim()
                    val deadlineEpochMs = if (offsetDays >= 0) {
                        System.currentTimeMillis() + offsetDays.coerceAtMost(365) * 86_400_000L
                    } else {
                        0L
                    }

                    return Result.success(
                        NoteAiEnrichment(
                            title = title.ifBlank { deriveSmartTitle(trimmed) },
                            cleanContent = cleanContent,
                            tags = tags.ifEmpty { classifyLocalTags(trimmed, availableTags) },
                            isHighPriority = isHigh || deadlineEpochMs > 0L,
                            deadlineIsoOrEmpty = "",
                            deadlineLabel = if (deadlineEpochMs > 0L && deadlineLabel.isBlank()) {
                                formatDeadlineDate(deadlineEpochMs)
                            } else {
                                deadlineLabel
                            },
                            deadlineEpochMs = deadlineEpochMs
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Cloud AI unavailable (${e.message}), using Havel On-Device AI Engine")
            }
        }

        return Result.success(analyzeCaptureLocally(trimmed, availableTags))
    }

    suspend fun synthesizeStitchedScript(
        notes: List<NoteItem>,
        themeHint: String? = null
    ): Result<StitchedScriptDraft> {
        require(notes.size >= 2) { "Select at least 2 related notes to stitch into a script." }

        if (FirebaseSafe.isInitialized()) {
            try {
                val stitchModel = Firebase.ai.generativeModel(
                    modelName = "gemini-2.5-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = stitchedScriptSchema
                    }
                )
                val notesBlock = notes.mapIndexed { idx, note ->
                    val tagsStr = note.tags.joinToString(" ")
                    "Note ${idx + 1} (${note.title.ifBlank { "Untitled" }} | $tagsStr):\n${note.rawContent}"
                }.joinToString("\n\n")

                val prompt = """
                    You are Havel's Idea Linking & Creativity AI.
                    A creator captured these separate raw thoughts across time${themeHint?.let { " around '$it'" } ?: ""}:
                    
                    $notesBlock
                    
                    Synthesize these raw notes into ONE cohesive, compelling "Stitched Script" without losing the creator's original ideas or voice.
                """.trimIndent()

                val response = withTimeout(AI_TIMEOUT_MS) {
                    stitchModel.generateContent(prompt)
                }
                val jsonStr = response.text
                if (!jsonStr.isNullOrBlank()) {
                    val json = JSONObject(jsonStr)
                    val body = json.optString("scriptBody", "").trim()
                    if (body.isNotBlank()) {
                        return Result.success(
                            StitchedScriptDraft(
                                title = json.optString("title", "Stitched Idea Script").trim(),
                                themeTag = json.optString("themeTag", themeHint ?: "#ScriptIdea").trim().let {
                                    if (it.startsWith("#")) it else "#$it"
                                },
                                insightSummary = json.optString(
                                    "insightSummary",
                                    "Synthesized from ${notes.size} raw notes."
                                ).trim(),
                                scriptBody = body
                            )
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Cloud AI stitching unavailable (${e.message}), using On-Device Script Synthesizer")
            }
        }

        return Result.success(synthesizeStitchedScriptLocally(notes, themeHint))
    }

    suspend fun summarizeBrainstormEvent(
        event: BrainstormEvent,
        ideas: List<PanelIdea>
    ): Result<String> {
        if (FirebaseSafe.isInitialized()) {
            try {
                val summaryModel = Firebase.ai.generativeModel(modelName = "gemini-2.5-flash")
                val ideasText = if (ideas.isEmpty()) {
                    "No individual idea cards logged yet."
                } else {
                    ideas.joinToString("\n") {
                        "- [${it.authorName}] (${it.tag.ifBlank { "#Idea" }}): ${it.content} [${it.upvoterUids.size} votes]"
                    }
                }
                val prompt = """
                    Summarize this Team Panel Brainstorming Event cleanly and concisely in 3-5 bullet points with next steps.
                    Event: ${event.title}
                    Topic Prompt: ${event.topicPrompt}
                    Team Ideas:
                    $ideasText
                """.trimIndent()

                val response = withTimeout(AI_TIMEOUT_MS) {
                    summaryModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    return Result.success(text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Cloud AI summary unavailable (${e.message}), using On-Device Brainstorm Synthesizer")
            }
        }

        return Result.success(summarizeBrainstormEventLocally(event, ideas))
    }

    /**
     * Deterministic On-Device AI Engine for 0ms Offline / Local-First Note Enrichment.
     */
    fun analyzeCaptureLocally(
        rawText: String,
        availableTags: List<String> = DEFAULT_VARIETY_TAGS
    ): NoteAiEnrichment {
        val cleanContent = polishTextLocally(rawText)
        val title = deriveSmartTitle(cleanContent)
        val tags = classifyLocalTags(rawText, availableTags)
        val (deadlineMs, deadlineLabel, isUrgent) = detectDeadlineAndPriority(rawText)

        return NoteAiEnrichment(
            title = title,
            cleanContent = cleanContent,
            tags = tags,
            isHighPriority = isUrgent || deadlineMs > 0L,
            deadlineIsoOrEmpty = "",
            deadlineLabel = deadlineLabel,
            deadlineEpochMs = deadlineMs
        )
    }

    fun synthesizeStitchedScriptLocally(
        notes: List<NoteItem>,
        themeHint: String? = null
    ): StitchedScriptDraft {
        val sortedNotes = notes.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        val primaryTag = themeHint?.takeIf { it.isNotBlank() }
            ?: sortedNotes.flatMap { it.tags }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?: "#ScriptIdea"

        val leadTitle = sortedNotes.firstOrNull()?.title?.takeIf { it.isNotBlank() }
            ?: deriveSmartTitle(sortedNotes.first().rawContent)
        val scriptTitle = when (primaryTag.lowercase(Locale.US)) {
            "#scriptidea" -> "Stitched Script: $leadTitle"
            "#startup" -> "Product Blueprint: $leadTitle"
            "#meeting" -> "Action Brief: $leadTitle"
            else -> "Synthesized Idea Script: $leadTitle"
        }

        val hookNote = sortedNotes.first()
        val bodyNotes = if (sortedNotes.size > 2) sortedNotes.subList(1, sortedNotes.size - 1) else sortedNotes.drop(1)
        val closingNote = if (sortedNotes.size >= 2) sortedNotes.last() else hookNote

        val bodyBuilder = StringBuilder()
        bodyBuilder.appendLine("1. OPENING HOOK")
        bodyBuilder.appendLine(hookNote.cleanContent.ifBlank { hookNote.rawContent })
        bodyBuilder.appendLine()
        bodyBuilder.appendLine("2. CORE NARRATIVE & STITCHED BEATS")
        if (bodyNotes.isEmpty()) {
            bodyBuilder.appendLine("• ${closingNote.cleanContent.ifBlank { closingNote.rawContent }}")
        } else {
            bodyNotes.forEachIndexed { idx, note ->
                val heading = note.title.ifBlank { "Beat ${idx + 1}" }
                bodyBuilder.appendLine("• [$heading]: ${note.cleanContent.ifBlank { note.rawContent }}")
            }
        }
        bodyBuilder.appendLine()
        bodyBuilder.appendLine("3. CLOSING CALL TO ACTION & NEXT STEPS")
        bodyBuilder.appendLine(closingNote.cleanContent.ifBlank { closingNote.rawContent })

        return StitchedScriptDraft(
            title = scriptTitle.take(80),
            themeTag = if (primaryTag.startsWith("#")) primaryTag else "#$primaryTag",
            insightSummary = "Non-destructively stitched ${notes.size} related raw thoughts around $primaryTag while preserving all original captures verbatim.",
            scriptBody = bodyBuilder.toString().trim()
        )
    }

    fun summarizeBrainstormEventLocally(
        event: BrainstormEvent,
        ideas: List<PanelIdea>
    ): String {
        val rankedIdeas = ideas.sortedByDescending { it.upvoterUids.size }
        val sb = StringBuilder()
        sb.appendLine("• Session Focus: ${event.title}" + (event.topicPrompt.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""))
        if (rankedIdeas.isEmpty()) {
            sb.appendLine("• Key Outcome: Session wrapped. Ready to capture raw team thoughts via Lock Screen or Panel stream.")
        } else {
            val top = rankedIdeas.take(3)
            top.forEachIndexed { index, idea ->
                sb.appendLine("• Top Spark #${index + 1} (${idea.authorName}, ${idea.upvoterUids.size} vote(s)): ${idea.content}")
            }
            val tags = rankedIdeas.mapNotNull { it.tag.takeIf { t -> t.isNotBlank() } }.distinct()
            if (tags.isNotEmpty()) {
                sb.appendLine("• Next Step: Synthesize ${rankedIdeas.size} panel sparks (${tags.joinToString(", ")}) into a Stitched Script.")
            }
        }
        return sb.toString().trim()
    }

    private fun polishTextLocally(rawText: String): String {
        val cleaned = rawText.trim()
            .replace(Regex("^(?:um+|uh+|er+|like,\\s*)+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) return rawText.trim()
        val capitalized = cleaned.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
        return if (capitalized.last() in listOf('.', '!', '?', '…')) {
            capitalized
        } else {
            "$capitalized."
        }
    }

    private fun deriveSmartTitle(text: String): String {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val stripped = firstLine
            .replace(Regex("^(?:remind me to|note to self:?|todo:?|remember to|i need to|we should)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("#\\w+"), "")
            .replace(Regex("[.!?…]+$"), "")
            .trim()
        val words = stripped.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return "Quick Thought"
        return words.take(6).joinToString(" ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }.take(52)
    }

    private fun classifyLocalTags(rawText: String, availableTags: List<String>): List<String> {
        val lower = rawText.lowercase(Locale.US)
        val explicitTags = Regex("#[A-Za-z0-9_]+").findAll(rawText).map { it.value }.toList()
        val matched = mutableListOf<String>()
        matched.addAll(explicitTags)

        if (listOf("script", "youtube", "video", "hook", "episode", "podcast", "story", "intro", "cta", "reel", "creator").any { it in lower }) {
            matched.add("#ScriptIdea")
        }
        if (listOf("startup", "pitch", "product", "launch", "mvp", "investor", "user", "feature", "growth", "roadmap", "app").any { it in lower }) {
            matched.add("#Startup")
        }
        if (listOf("meeting", "sync", "call", "agenda", "team", "client", "standup", "discuss", "1:1", "presentation").any { it in lower }) {
            matched.add("#Meeting")
        }
        if (listOf("buy", "grocery", "milk", "coffee", "eggs", "store", "shop", "market", "bread", "dinner", "list").any { it in lower }) {
            matched.add("#Grocery")
        }
        if (listOf("voice", "spoken", "record", "audio", "mic", "walk", "on the go").any { it in lower }) {
            matched.add("#VoiceDraft")
        }
        if (listOf("journal", "personal", "family", "gym", "workout", "read", "book", "weekend", "life").any { it in lower }) {
            matched.add("#Personal")
        }

        if (matched.isEmpty()) {
            matched.add(availableTags.firstOrNull() ?: "#ScriptIdea")
        }
        return matched.distinct().take(3)
    }

    private fun detectDeadlineAndPriority(rawText: String): Triple<Long, String, Boolean> {
        val lower = rawText.lowercase(Locale.US)
        val now = System.currentTimeMillis()
        val hourMs = 3_600_000L
        val dayMs = 86_400_000L

        val isUrgentKeyword = listOf(
            "urgent", "asap", "high priority", "important", "deadline", "critical",
            "must ", "don't forget", "remind me", "pitch deck", "before ", "by "
        ).any { it in lower }

        // Check "in X hours"
        Regex("in\\s+(\\d+)\\s+hours?").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { hrs ->
            val target = now + hrs.coerceIn(1, 720) * hourMs
            return Triple(target, "In $hrs hour(s) • ${formatTimeOnly(target)}", true)
        }

        // Check "in X days"
        Regex("in\\s+(\\d+)\\s+days?").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { days ->
            val target = now + days.coerceIn(1, 365) * dayMs
            return Triple(target, formatDeadlineDate(target), true)
        }

        // Check "tomorrow"
        if ("tomorrow" in lower) {
            val target = now + dayMs
            return Triple(target, "Tomorrow • ${formatDeadlineDate(target)}", true)
        }

        // Check "today" or "tonight"
        if ("tonight" in lower || "today" in lower || "this evening" in lower) {
            val target = now + 4 * hourMs
            return Triple(target, "Today • ${formatTimeOnly(target)}", true)
        }

        // Check "next week"
        if ("next week" in lower) {
            val target = now + 7 * dayMs
            return Triple(target, "Next Week • ${formatDeadlineDate(target)}", true)
        }

        // Check day of week (Monday..Sunday)
        val weekdays = mapOf(
            "sunday" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY
        )
        for ((name, calDay) in weekdays) {
            if (Regex("\\b$name\\b").containsMatchIn(lower)) {
                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                var daysAhead = (calDay - currentDay + 7) % 7
                if (daysAhead == 0) daysAhead = 7
                val target = now + daysAhead * dayMs
                return Triple(target, formatDeadlineDate(target), true)
            }
        }

        return Triple(0L, "", isUrgentKeyword)
    }

    private fun formatDeadlineDate(epochMs: Long): String {
        return SimpleDateFormat("EEE, MMM d • 5:00 PM", Locale.US).format(Date(epochMs))
    }

    private fun formatTimeOnly(epochMs: Long): String {
        return SimpleDateFormat("h:mm a", Locale.US).format(Date(epochMs))
    }

    private companion object {
        const val TAG = "HavelAiService"
        const val AI_TIMEOUT_MS = 25_000L
    }
}
