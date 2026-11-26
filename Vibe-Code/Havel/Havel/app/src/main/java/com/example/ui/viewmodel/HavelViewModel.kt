package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.FirebaseSafe
import com.example.data.ai.HavelAiService
import com.example.data.local.HavelLocalDatabase
import com.example.data.local.HavelPreferences
import com.example.data.local.HavelSettings
import com.example.data.local.LocalBrainstormEventEntity
import com.example.data.local.LocalNoteEntity
import com.example.data.local.LocalPanelIdeaEntity
import com.example.data.local.LocalStitchedScriptEntity
import com.example.data.local.LocalTeamPanelEntity
import com.example.data.local.LocalWorkspaceDao
import com.example.data.local.PendingCaptureDao
import com.example.data.local.ThemePreference
import com.example.data.model.BrainstormEvent
import com.example.data.model.CaptureSource
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.EventStatus
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority
import com.example.data.model.PanelIdea
import com.example.data.model.ReminderCadence
import com.example.data.model.StitchedScript
import com.example.data.model.TeamPanel
import com.example.data.model.UserProfile
import com.example.data.repository.HavelNotesRepository
import com.example.data.repository.TeamPanelRepository
import com.example.lockscreen.LockScreenCaptureService
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class StitchSuggestion(
    val themeTag: String,
    val notes: List<NoteItem>
)

@OptIn(ExperimentalCoroutinesApi::class)
class HavelViewModel(
    private val appContext: Context,
    private val notesRepository: HavelNotesRepository?,
    private val panelRepository: TeamPanelRepository?,
    private val aiService: HavelAiService,
    private val pendingCaptureDao: PendingCaptureDao,
    private val localWorkspaceDao: LocalWorkspaceDao = HavelLocalDatabase.getInstance(appContext).localWorkspaceDao(),
    private val preferences: HavelPreferences,
    private val currentUserId: String,
    private val currentUserName: String,
    private val currentUserEmail: String,
    private val currentUserPhotoUrl: String?
) : ViewModel() {

    private val isCloudAuthenticated: Boolean
        get() = notesRepository != null && FirebaseSafe.currentUserOrNull(appContext) != null

    val settings: StateFlow<HavelSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), HavelSettings())

    private val localFallbackProfile = UserProfile(
        id = currentUserId,
        userId = currentUserId,
        displayName = currentUserName,
        email = currentUserEmail,
        photoUrl = currentUserPhotoUrl,
        defaultTags = DEFAULT_VARIETY_TAGS
    )

    val userProfile: StateFlow<UserProfile?> = (
        if (notesRepository != null && currentUserId != FirebaseSafe.LOCAL_USER_ID) {
            notesRepository.observeUserProfile(currentUserId)
                .catch { emit(localFallbackProfile) }
        } else {
            flowOf(localFallbackProfile)
        }
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), localFallbackProfile)

    private val localNotesFlow = localWorkspaceDao.observeNotes()
        .map { list -> list.map { it.toNoteItem() } }

    private val cloudNotesFlow = if (notesRepository != null && currentUserId != FirebaseSafe.LOCAL_USER_ID) {
        notesRepository.observeNotes(currentUserId).catch { emit(emptyList()) }
    } else {
        flowOf(emptyList())
    }

    val notesState: StateFlow<UiState<List<NoteItem>>> = combine(localNotesFlow, cloudNotesFlow) { local, cloud ->
        val merged = (local + cloud)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        UiState.Success(merged) as UiState<List<NoteItem>>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val localScriptsFlow = localWorkspaceDao.observeScripts()
        .map { list -> list.map { it.toStitchedScript() } }

    private val cloudScriptsFlow = if (notesRepository != null && currentUserId != FirebaseSafe.LOCAL_USER_ID) {
        notesRepository.observeStitchedScripts(currentUserId).catch { emit(emptyList()) }
    } else {
        flowOf(emptyList())
    }

    val scriptsState: StateFlow<UiState<List<StitchedScript>>> = combine(localScriptsFlow, cloudScriptsFlow) { local, cloud ->
        val merged = (local + cloud)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        UiState.Success(merged) as UiState<List<StitchedScript>>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val localPanelsFlow = localWorkspaceDao.observePanels()
        .map { list -> list.map { it.toTeamPanel() } }

    private val cloudPanelsFlow = if (panelRepository != null && currentUserId != FirebaseSafe.LOCAL_USER_ID) {
        panelRepository.observePanels(currentUserId).catch { emit(emptyList()) }
    } else {
        flowOf(emptyList())
    }

    val panelsState: StateFlow<UiState<List<TeamPanel>>> = combine(localPanelsFlow, cloudPanelsFlow) { local, cloud ->
        val merged = (local + cloud)
            .associateBy { it.id }
            .values
            .sortedByDescending { (it.updatedAt ?: it.createdAt)?.toDate()?.time ?: 0L }
        UiState.Success(merged) as UiState<List<TeamPanel>>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _selectedPanelId = MutableStateFlow<String?>(null)
    val selectedPanelId: StateFlow<String?> = _selectedPanelId.asStateFlow()

    val selectedPanelIdeas: StateFlow<UiState<List<PanelIdea>>> = _selectedPanelId
        .flatMapLatest { panelId ->
            if (panelId == null) {
                flowOf<UiState<List<PanelIdea>>>(UiState.Success(emptyList()))
            } else {
                val localFlow = localWorkspaceDao.observePanelIdeas(panelId).map { list -> list.map { it.toPanelIdea() } }
                val cloudFlow = if (panelRepository != null && currentUserId != FirebaseSafe.LOCAL_USER_ID) {
                    panelRepository.observePanelIdeas(panelId, currentUserId).catch { emit(emptyList()) }
                } else {
                    flowOf(emptyList())
                }
                combine(localFlow, cloudFlow) { local, cloud ->
                    val merged = (local + cloud)
                        .associateBy { it.id }
                        .values
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    UiState.Success(merged) as UiState<List<PanelIdea>>
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val selectedPanelEvents: StateFlow<UiState<List<BrainstormEvent>>> = _selectedPanelId
        .flatMapLatest { panelId ->
            if (panelId == null) {
                flowOf<UiState<List<BrainstormEvent>>>(UiState.Success(emptyList()))
            } else {
                val localFlow = localWorkspaceDao.observeBrainstormEvents(panelId).map { list -> list.map { it.toBrainstormEvent() } }
                val cloudFlow = if (panelRepository != null && currentUserId != FirebaseSafe.LOCAL_USER_ID) {
                    panelRepository.observeBrainstormEvents(panelId, currentUserId).catch { emit(emptyList()) }
                } else {
                    flowOf(emptyList())
                }
                combine(localFlow, cloudFlow) { local, cloud ->
                    val merged = (local + cloud)
                        .associateBy { it.id }
                        .values
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    UiState.Success(merged) as UiState<List<BrainstormEvent>>
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _isAiBusy = MutableStateFlow(false)
    val isAiBusy: StateFlow<Boolean> = _isAiBusy.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            HavelLocalDatabase.seedStarterWorkspaceIfEmpty(appContext, currentUserId, currentUserName)
            if (isCloudAuthenticated) {
                notesRepository?.ensureUserProfile(
                    userId = currentUserId,
                    displayName = currentUserName,
                    email = currentUserEmail,
                    photoUrl = currentUserPhotoUrl
                )
            }
        }

        // Flush any legacy pending captures into localWorkspaceDao + AI organization
        viewModelScope.launch {
            pendingCaptureDao.observeAll().collect { pending ->
                pending.forEach { entry ->
                    pendingCaptureDao.deleteById(entry.localId)
                    captureNote(
                        rawText = entry.rawContent,
                        source = entry.captureSource,
                        manualTags = if (entry.presetTag.isNotBlank()) listOf(entry.presetTag) else emptyList(),
                        manualPriority = entry.priorityOverride.takeIf { it.isNotBlank() },
                        deadlineEpochMs = entry.deadlineEpochMs,
                        deadlineLabel = entry.deadlineLabel
                    )
                }
            }
        }
    }

    fun showMessage(message: String) {
        if (message.isNotBlank()) {
            _statusBanner.value = message
        }
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun selectPanel(panelId: String?) {
        _selectedPanelId.value = panelId
    }

    fun setThemePreference(preference: ThemePreference) {
        viewModelScope.launch {
            preferences.setThemePreference(preference)
        }
    }

    fun setLockScreenCaptureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setLockScreenCaptureEnabled(enabled)
            if (enabled) {
                LockScreenCaptureService.start(appContext)
            } else {
                LockScreenCaptureService.stop(appContext)
            }
        }
    }

    fun setWakeLockPadOnScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setWakeLockPadOnScreenOn(enabled)
        }
    }

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setFloatingBubbleEnabled(enabled)
        }
    }

    fun setAutoAiOrganizeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoAiOrganizeEnabled(enabled)
        }
    }

    fun captureNote(
        rawText: String,
        source: String = CaptureSource.IN_APP,
        manualTags: List<String> = emptyList(),
        manualPriority: String? = null,
        deadlineEpochMs: Long = 0L,
        deadlineLabel: String = ""
    ) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val initialPriority = manualPriority ?: if (deadlineEpochMs > 0L) NotePriority.HIGH else NotePriority.NORMAL
            val initialCadence = if (initialPriority == NotePriority.HIGH || deadlineEpochMs > 0L) {
                ReminderCadence.ESCALATING_1W_1D_1H
            } else {
                ReminderCadence.DAILY_DIGEST
            }
            val firstLineTitle = trimmed.lineSequence().firstOrNull()?.take(48).orEmpty()
            val noteId = "note_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val now = System.currentTimeMillis()

            // Step 1: 0ms Local-First SQLite save
            val initialLocal = LocalNoteEntity(
                id = noteId,
                userId = currentUserId,
                rawContent = trimmed,
                cleanContent = trimmed,
                title = firstLineTitle,
                tagsPipeSeparated = manualTags.joinToString("|"),
                captureSource = source,
                priority = initialPriority,
                deadlineEpochMs = deadlineEpochMs,
                deadlineLabel = deadlineLabel,
                reminderCadence = initialCadence,
                aiProcessed = !settings.value.autoAiOrganizeEnabled,
                isArchived = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            localWorkspaceDao.upsertNote(initialLocal)
            _statusBanner.value = "Captured in 0ms"

            if (initialPriority == NotePriority.HIGH && deadlineEpochMs > 0L) {
                ReminderScheduler.scheduleEscalatingCascade(appContext, initialLocal.toNoteItem())
            }

            // Step 2: Optional Cloud Sync
            var cloudNoteId: String? = null
            if (isCloudAuthenticated) {
                notesRepository?.createNote(
                    rawContent = trimmed,
                    cleanContent = trimmed,
                    title = firstLineTitle,
                    tags = manualTags,
                    captureSource = source,
                    priority = initialPriority,
                    deadlineEpochMs = deadlineEpochMs,
                    deadlineLabel = deadlineLabel,
                    reminderCadence = initialCadence,
                    aiProcessed = false
                )?.onSuccess { cId ->
                    cloudNoteId = cId
                }
            }

            // Step 3: Background AI Auto-Tagging & Deadline Detection
            if (settings.value.autoAiOrganizeEnabled) {
                runBackgroundAiOrganization(
                    localNoteId = noteId,
                    cloudNoteId = cloudNoteId,
                    rawContent = trimmed,
                    captureSource = source,
                    createdAtMs = now,
                    manualTags = manualTags,
                    manualPriority = manualPriority,
                    manualDeadlineMs = deadlineEpochMs,
                    manualDeadlineLabel = deadlineLabel
                )
            }
        }
    }

    fun reOrganizeNoteWithAi(note: NoteItem) {
        viewModelScope.launch {
            _isAiBusy.value = true
            runBackgroundAiOrganization(
                localNoteId = note.id,
                cloudNoteId = note.id.takeIf { isCloudAuthenticated },
                rawContent = note.rawContent,
                captureSource = note.captureSource,
                createdAtMs = note.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                manualTags = emptyList(),
                manualPriority = null,
                manualDeadlineMs = note.deadlineEpochMs,
                manualDeadlineLabel = note.deadlineLabel
            )
            _isAiBusy.value = false
        }
    }

    private fun runBackgroundAiOrganization(
        localNoteId: String,
        cloudNoteId: String?,
        rawContent: String,
        captureSource: String,
        createdAtMs: Long,
        manualTags: List<String>,
        manualPriority: String?,
        manualDeadlineMs: Long,
        manualDeadlineLabel: String
    ) {
        viewModelScope.launch {
            val availableTags = userProfile.value?.defaultTags ?: DEFAULT_VARIETY_TAGS
            val enrichment = aiService.analyzeCapture(rawContent, availableTags).getOrElse {
                aiService.analyzeCaptureLocally(rawContent, availableTags)
            }
            val mergedTags = (manualTags + enrichment.tags).distinct().take(6)
            val finalPriority = manualPriority
                ?: if (enrichment.isHighPriority) NotePriority.HIGH else NotePriority.NORMAL
            val finalDeadlineMs = if (manualDeadlineMs > 0L) manualDeadlineMs else enrichment.deadlineEpochMs
            val finalDeadlineLabel = manualDeadlineLabel.ifBlank { enrichment.deadlineLabel }
            val finalCadence = if (finalPriority == NotePriority.HIGH || finalDeadlineMs > 0L) {
                ReminderCadence.ESCALATING_1W_1D_1H
            } else {
                ReminderCadence.DAILY_DIGEST
            }

            val enrichedLocal = LocalNoteEntity(
                id = localNoteId,
                userId = currentUserId,
                rawContent = rawContent,
                cleanContent = enrichment.cleanContent,
                title = enrichment.title,
                tagsPipeSeparated = mergedTags.joinToString("|"),
                captureSource = captureSource,
                priority = finalPriority,
                deadlineEpochMs = finalDeadlineMs,
                deadlineLabel = finalDeadlineLabel,
                reminderCadence = finalCadence,
                aiProcessed = true,
                isArchived = false,
                createdAtEpochMs = createdAtMs,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            localWorkspaceDao.upsertNote(enrichedLocal)

            if (cloudNoteId != null && isCloudAuthenticated) {
                notesRepository?.updateNoteOrganization(
                    noteId = cloudNoteId,
                    cleanContent = enrichment.cleanContent,
                    title = enrichment.title,
                    tags = mergedTags,
                    priority = finalPriority,
                    deadlineEpochMs = finalDeadlineMs,
                    deadlineLabel = finalDeadlineLabel,
                    reminderCadence = finalCadence,
                    aiProcessed = true
                )
            }

            if (finalPriority == NotePriority.HIGH && finalDeadlineMs > 0L) {
                ReminderScheduler.scheduleEscalatingCascade(appContext, enrichedLocal.toNoteItem())
            }
        }
    }

    fun updateNoteDetails(
        note: NoteItem,
        title: String,
        cleanContent: String,
        tags: List<String>,
        priority: String,
        deadlineEpochMs: Long,
        deadlineLabel: String
    ) {
        viewModelScope.launch {
            val cadence = if (priority == NotePriority.HIGH || deadlineEpochMs > 0L) {
                ReminderCadence.ESCALATING_1W_1D_1H
            } else {
                ReminderCadence.DAILY_DIGEST
            }
            val updated = note.copy(
                title = title,
                cleanContent = cleanContent,
                tags = tags,
                priority = priority,
                deadlineEpochMs = deadlineEpochMs,
                deadlineLabel = deadlineLabel,
                reminderCadence = cadence,
                aiProcessed = true
            )
            localWorkspaceDao.upsertNote(LocalNoteEntity.fromNoteItem(updated))
            ReminderScheduler.scheduleEscalatingCascade(appContext, updated)
            _statusBanner.value = "Note updated"

            if (isCloudAuthenticated) {
                notesRepository?.updateNoteOrganization(
                    noteId = note.id,
                    cleanContent = cleanContent,
                    title = title,
                    tags = tags,
                    priority = priority,
                    deadlineEpochMs = deadlineEpochMs,
                    deadlineLabel = deadlineLabel,
                    reminderCadence = cadence,
                    aiProcessed = true
                )
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            ReminderScheduler.cancelCascade(appContext, noteId)
            localWorkspaceDao.deleteNote(noteId)
            if (isCloudAuthenticated) {
                notesRepository?.deleteNote(noteId)
            }
            _statusBanner.value = "Note deleted"
        }
    }

    fun archiveNote(noteId: String, archived: Boolean) {
        viewModelScope.launch {
            if (archived) ReminderScheduler.cancelCascade(appContext, noteId)
            localWorkspaceDao.setNoteArchived(noteId, archived)
            if (isCloudAuthenticated) {
                notesRepository?.setNoteArchived(noteId, archived)
            }
        }
    }

    fun stitchNotesIntoScript(notes: List<NoteItem>, themeHint: String? = null, onDone: () -> Unit = {}) {
        if (notes.size < 2) {
            _statusBanner.value = "Select at least 2 notes to stitch into a script."
            return
        }
        viewModelScope.launch {
            _isAiBusy.value = true
            val draft = aiService.synthesizeStitchedScript(notes, themeHint).getOrElse {
                aiService.synthesizeStitchedScriptLocally(notes, themeHint)
            }
            val scriptId = "script_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val linkedIds = notes.map { it.id }.distinct()
            val localScript = LocalStitchedScriptEntity(
                id = scriptId,
                userId = currentUserId,
                title = draft.title,
                themeTag = draft.themeTag,
                insightSummary = draft.insightSummary,
                scriptBody = draft.scriptBody,
                linkedNoteIdsPipeSeparated = linkedIds.joinToString("|"),
                linkedNoteCount = linkedIds.size.toLong(),
                createdAtEpochMs = System.currentTimeMillis()
            )
            localWorkspaceDao.upsertScript(localScript)

            if (isCloudAuthenticated) {
                notesRepository?.createStitchedScript(
                    title = draft.title,
                    themeTag = draft.themeTag,
                    insightSummary = draft.insightSummary,
                    scriptBody = draft.scriptBody,
                    linkedNoteIds = linkedIds
                )
            }

            _statusBanner.value = "Stitched ${notes.size} raw notes into \"${draft.title}\""
            _isAiBusy.value = false
            onDone()
        }
    }

    fun deleteStitchedScript(scriptId: String) {
        viewModelScope.launch {
            localWorkspaceDao.deleteScript(scriptId)
            if (isCloudAuthenticated) {
                notesRepository?.deleteStitchedScript(scriptId)
            }
        }
    }

    fun createTeamPanel(name: String, description: String) {
        if (name.trim().length < 2) return
        viewModelScope.launch {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val inviteCode = "HVL-" + (1..4).map { chars.random() }.joinToString("")
            val panelId = "panel_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val now = System.currentTimeMillis()

            val localPanel = LocalTeamPanelEntity(
                id = panelId,
                ownerId = currentUserId,
                ownerName = currentUserName,
                name = name.trim(),
                description = description.trim(),
                inviteCode = inviteCode,
                memberUidsPipeSeparated = currentUserId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            localWorkspaceDao.upsertPanel(localPanel)
            _selectedPanelId.value = panelId
            _statusBanner.value = "Panel created • Invite Code: $inviteCode"

            if (isCloudAuthenticated) {
                panelRepository?.createPanel(
                    name = name,
                    description = description,
                    ownerName = currentUserName
                )?.onSuccess { cloudPanel ->
                    _selectedPanelId.value = cloudPanel.id
                }
            }
        }
    }

    fun joinTeamPanel(inviteCode: String) {
        val code = inviteCode.trim().uppercase()
        if (code.length < 4) return
        viewModelScope.launch {
            val localMatch = localWorkspaceDao.findPanelByInviteCode(code)
            if (localMatch != null) {
                val updatedMembers = (localMatch.memberUidsPipeSeparated.split("|") + currentUserId)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString("|")
                localWorkspaceDao.upsertPanel(
                    localMatch.copy(
                        memberUidsPipeSeparated = updatedMembers,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
                _selectedPanelId.value = localMatch.id
                _statusBanner.value = "Joined ${localMatch.name}!"
                return@launch
            }

            if (isCloudAuthenticated && panelRepository != null) {
                panelRepository.joinPanelByCode(code)
                    .onSuccess { panelId ->
                        _selectedPanelId.value = panelId
                        _statusBanner.value = "Joined Team Panel!"
                    }
                    .onFailure { err ->
                        _statusBanner.value = err.localizedMessage ?: "Could not join with invite code."
                    }
            } else {
                // Create a collaborative local panel room for the entered code so offline testing works seamlessly
                val panelId = "panel_joined_${code.lowercase()}"
                val now = System.currentTimeMillis()
                localWorkspaceDao.upsertPanel(
                    LocalTeamPanelEntity(
                        id = panelId,
                        ownerId = currentUserId,
                        ownerName = currentUserName,
                        name = "Shared Panel ($code)",
                        description = "Joined via invite code $code",
                        inviteCode = code,
                        memberUidsPipeSeparated = currentUserId,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now
                    )
                )
                _selectedPanelId.value = panelId
                _statusBanner.value = "Joined Panel $code!"
            }
        }
    }

    fun postPanelIdea(panel: TeamPanel, content: String, tag: String, sourceNoteId: String = "") {
        if (content.isBlank()) return
        viewModelScope.launch {
            val ideaId = "idea_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            localWorkspaceDao.upsertPanelIdea(
                LocalPanelIdeaEntity(
                    id = ideaId,
                    panelId = panel.id,
                    authorId = currentUserId,
                    authorName = currentUserName,
                    content = content.trim(),
                    tag = tag.trim().ifBlank { "#Startup" },
                    sourceNoteId = sourceNoteId,
                    upvoterUidsPipeSeparated = "",
                    createdAtEpochMs = System.currentTimeMillis()
                )
            )
            _statusBanner.value = "Shared to ${panel.name}"

            if (isCloudAuthenticated) {
                panelRepository?.addPanelIdea(
                    panel = panel,
                    authorName = currentUserName,
                    content = content,
                    tag = tag,
                    sourceNoteId = sourceNoteId
                )
            }
        }
    }

    fun toggleIdeaUpvote(panelId: String, idea: PanelIdea) {
        viewModelScope.launch {
            val currentVotes = idea.upvoterUids.toMutableSet()
            if (currentUserId in currentVotes) {
                currentVotes.remove(currentUserId)
            } else {
                currentVotes.add(currentUserId)
            }
            localWorkspaceDao.upsertPanelIdea(
                LocalPanelIdeaEntity(
                    id = idea.id,
                    panelId = panelId,
                    authorId = idea.authorId,
                    authorName = idea.authorName,
                    content = idea.content,
                    tag = idea.tag,
                    sourceNoteId = idea.sourceNoteId,
                    upvoterUidsPipeSeparated = currentVotes.joinToString("|"),
                    createdAtEpochMs = idea.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            )
            if (isCloudAuthenticated) {
                panelRepository?.toggleIdeaUpvote(panelId, idea)
            }
        }
    }

    fun createBrainstormEvent(
        panel: TeamPanel,
        title: String,
        topicPrompt: String,
        scheduledLabel: String
    ) {
        if (title.trim().length < 2) return
        viewModelScope.launch {
            val eventId = "event_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val now = System.currentTimeMillis()
            localWorkspaceDao.upsertBrainstormEvent(
                LocalBrainstormEventEntity(
                    id = eventId,
                    panelId = panel.id,
                    creatorId = currentUserId,
                    creatorName = currentUserName,
                    title = title.trim(),
                    topicPrompt = topicPrompt.trim(),
                    scheduledEpochMs = now + 3_600_000L,
                    scheduledLabel = scheduledLabel.ifBlank { "Live Now" },
                    status = EventStatus.LIVE,
                    aiSummary = "",
                    participantUidsPipeSeparated = currentUserId,
                    createdAtEpochMs = now
                )
            )
            _statusBanner.value = "Brainstorming event launched!"

            if (isCloudAuthenticated) {
                panelRepository?.createBrainstormEvent(
                    panel = panel,
                    creatorName = currentUserName,
                    title = title,
                    topicPrompt = topicPrompt,
                    scheduledEpochMs = now + 3_600_000L,
                    scheduledLabel = scheduledLabel.ifBlank { "Live Now" }
                )
            }
        }
    }

    fun toggleEventRsvp(panelId: String, event: BrainstormEvent) {
        viewModelScope.launch {
            val participants = event.participantUids.toMutableSet()
            if (currentUserId in participants) {
                participants.remove(currentUserId)
            } else {
                participants.add(currentUserId)
            }
            localWorkspaceDao.upsertBrainstormEvent(
                LocalBrainstormEventEntity(
                    id = event.id,
                    panelId = panelId,
                    creatorId = event.creatorId,
                    creatorName = event.creatorName,
                    title = event.title,
                    topicPrompt = event.topicPrompt,
                    scheduledEpochMs = event.scheduledEpochMs,
                    scheduledLabel = event.scheduledLabel,
                    status = event.status,
                    aiSummary = event.aiSummary,
                    participantUidsPipeSeparated = participants.joinToString("|"),
                    createdAtEpochMs = event.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            )
            if (isCloudAuthenticated) {
                panelRepository?.toggleEventRsvp(panelId, event)
            }
        }
    }

    fun wrapEventWithAi(panel: TeamPanel, event: BrainstormEvent, ideas: List<PanelIdea>) {
        viewModelScope.launch {
            _isAiBusy.value = true
            val summary = aiService.summarizeBrainstormEvent(event, ideas).getOrElse {
                aiService.summarizeBrainstormEventLocally(event, ideas)
            }
            localWorkspaceDao.upsertBrainstormEvent(
                LocalBrainstormEventEntity(
                    id = event.id,
                    panelId = panel.id,
                    creatorId = event.creatorId,
                    creatorName = event.creatorName,
                    title = event.title,
                    topicPrompt = event.topicPrompt,
                    scheduledEpochMs = event.scheduledEpochMs,
                    scheduledLabel = event.scheduledLabel,
                    status = EventStatus.WRAPPED,
                    aiSummary = summary,
                    participantUidsPipeSeparated = event.participantUids.joinToString("|"),
                    createdAtEpochMs = event.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            )
            if (isCloudAuthenticated) {
                panelRepository?.wrapBrainstormEvent(panel.id, event.id, summary)
            }
            _statusBanner.value = "Brainstorming wrapped with AI summary!"
            _isAiBusy.value = false
        }
    }

    fun syncPanelMembershipIfNeeded(
        panel: TeamPanel,
        ideas: List<PanelIdea>,
        events: List<BrainstormEvent>
    ) {
        if (!isCloudAuthenticated) return
        viewModelScope.launch {
            runCatching {
                panelRepository?.syncMembershipForPanel(panel, ideas, events)
            }.onFailure {
                Log.d(TAG, "Skipped cloud member sync: ${it.message}")
            }
        }
    }

    fun detectStitchSuggestions(
        notes: List<NoteItem>,
        existingScripts: List<StitchedScript>
    ): List<StitchSuggestion> {
        val activeNotes = notes.filter { !it.isArchived }
        val existingLinkedSets = existingScripts.map { it.linkedNoteIds.toSet() }
        val byTag = mutableMapOf<String, MutableList<NoteItem>>()
        activeNotes.forEach { note ->
            note.tags.forEach { tag ->
                byTag.getOrPut(tag) { mutableListOf() }.add(note)
            }
        }
        return byTag.entries
            .filter { (_, list) -> list.size >= 3 }
            .map { (tag, list) -> StitchSuggestion(tag, list.take(6)) }
            .filter { suggestion ->
                val ids = suggestion.notes.map { it.id }.toSet()
                existingLinkedSets.none { it.containsAll(ids) }
            }
            .sortedByDescending { it.notes.size }
    }

    private companion object {
        const val TAG = "HavelViewModel"
    }
}
