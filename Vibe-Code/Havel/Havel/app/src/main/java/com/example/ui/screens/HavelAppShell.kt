package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.FirebaseSafe
import com.example.data.ai.HavelAiService
import com.example.data.local.HavelLocalDatabase
import com.example.data.local.HavelPreferences
import com.example.data.local.ThemePreference
import com.example.data.model.NoteItem
import com.example.data.repository.HavelNotesRepository
import com.example.data.repository.TeamPanelRepository
import com.example.lockscreen.LockScreenCaptureService
import com.example.lockscreen.LockScreenNoteActivity
import com.example.notifications.HavelNotificationHelper
import com.example.ui.auth.signOutHavel
import com.example.ui.components.HavelAppIconEmblem
import com.example.ui.components.NoteDetailSheet
import com.example.ui.components.ZeroChoiceCaptureSheet
import com.example.ui.theme.HavelStationeryBackground
import com.example.ui.theme.HavelTheme
import com.example.ui.viewmodel.HavelViewModel
import com.example.ui.viewmodel.UiState

const val ROUTE_NOTES = "notes_feed"
const val ROUTE_SCRIPTS = "stitched_scripts"
const val ROUTE_REMINDERS = "smart_reminders"
const val ROUTE_PANELS = "team_panels"

private data class HavelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val HAVEL_DESTINATIONS = listOf(
    HavelDestination(ROUTE_NOTES, R.string.nav_capture, Icons.Rounded.Description, Icons.Outlined.Description, "nav_notes"),
    HavelDestination(ROUTE_SCRIPTS, R.string.nav_scripts, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_scripts"),
    HavelDestination(ROUTE_REMINDERS, R.string.nav_reminders, Icons.Rounded.Notifications, Icons.Outlined.Notifications, "nav_reminders"),
    HavelDestination(ROUTE_PANELS, R.string.nav_panels, Icons.Rounded.Groups, Icons.Outlined.Groups, "nav_panels")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavelAppShell(
    currentUserId: String,
    currentUserName: String,
    currentUserEmail: String,
    currentUserPhotoUrl: String?,
    openCaptureImmediately: Boolean,
    openVoiceImmediately: Boolean,
    onConsumedLaunchTrigger: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: HavelViewModel = viewModel(
        key = currentUserId,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val db = FirebaseSafe.firestoreOrNull(appContext)
                val localDb = HavelLocalDatabase.getInstance(appContext)
                return HavelViewModel(
                    appContext = appContext,
                    notesRepository = db?.let { HavelNotesRepository(it) },
                    panelRepository = db?.let { TeamPanelRepository(it) },
                    aiService = HavelAiService(),
                    pendingCaptureDao = localDb.pendingCaptureDao(),
                    localWorkspaceDao = localDb.localWorkspaceDao(),
                    preferences = HavelPreferences(appContext),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserEmail = currentUserEmail,
                    currentUserPhotoUrl = currentUserPhotoUrl
                ) as T
            }
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val notesState by viewModel.notesState.collectAsStateWithLifecycle()
    val scriptsState by viewModel.scriptsState.collectAsStateWithLifecycle()
    val panelsState by viewModel.panelsState.collectAsStateWithLifecycle()
    val selectedPanelId by viewModel.selectedPanelId.collectAsStateWithLifecycle()
    val panelIdeasState by viewModel.selectedPanelIdeas.collectAsStateWithLifecycle()
    val panelEventsState by viewModel.selectedPanelEvents.collectAsStateWithLifecycle()
    val isAiBusy by viewModel.isAiBusy.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val startupNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && settings.lockScreenCaptureEnabled) {
            LockScreenCaptureService.start(appContext)
        }
    }

    // Automatically activate Lock Screen Note-Taking availability on startup
    LaunchedEffect(settings.lockScreenCaptureEnabled) {
        if (settings.lockScreenCaptureEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !HavelNotificationHelper.hasNotificationPermission(context)
            ) {
                startupNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                LockScreenCaptureService.start(appContext)
            }
        }
    }

    val isDark = when (settings.themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DAY_ALABASTER -> false
        ThemePreference.DUSK_SLATE -> true
    }

    var showCaptureSheet by remember { mutableStateOf(false) }
    var captureWithVoice by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteItem?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(openCaptureImmediately, openVoiceImmediately) {
        if (openVoiceImmediately) {
            captureWithVoice = true
            showCaptureSheet = true
            onConsumedLaunchTrigger()
        } else if (openCaptureImmediately) {
            captureWithVoice = false
            showCaptureSheet = true
            onConsumedLaunchTrigger()
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    HavelTheme(darkTheme = isDark) {
        val navController = rememberNavController()
        val navBackStack by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStack?.destination?.route ?: ROUTE_NOTES

        val notesList = (notesState as? UiState.Success)?.data.orEmpty()
        val scriptsList = (scriptsState as? UiState.Success)?.data.orEmpty()
        val panelsList = (panelsState as? UiState.Success)?.data.orEmpty()
        val suggestions = remember(notesList, scriptsList) {
            viewModel.detectStitchSuggestions(notesList, scriptsList)
        }

        if (showAccountDialog) {
            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HavelAppIconEmblem(modifier = Modifier.size(32.dp))
                        Text("Havel Workspace")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "$currentUserName\n${if (currentUserId == FirebaseSafe.LOCAL_USER_ID) "0ms Local-First SQLite Workspace" else currentUserEmail}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto AI Tagging & Deadlines", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = settings.autoAiOrganizeEnabled,
                                onCheckedChange = { viewModel.setAutoAiOrganizeEnabled(it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lock Screen Note Availability", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = settings.lockScreenCaptureEnabled,
                                onCheckedChange = { viewModel.setLockScreenCaptureEnabled(it) }
                            )
                        }
                        Text("Theme Tone", style = MaterialTheme.typography.titleSmall)
                        ThemePreference.entries.forEach { pref ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pref.label, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { viewModel.setThemePreference(pref) }) {
                                    Text(if (settings.themePreference == pref) "Active" else "Use")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (currentUserId != FirebaseSafe.LOCAL_USER_ID) {
                        TextButton(
                            onClick = {
                                showAccountDialog = false
                                signOutHavel(credentialManager, scope)
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Text("Sign Out", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showCaptureSheet) {
            ZeroChoiceCaptureSheet(
                startWithVoice = captureWithVoice,
                availableTags = userProfile?.defaultTags.orEmpty().ifEmpty {
                    com.example.data.model.DEFAULT_VARIETY_TAGS
                },
                onDismiss = { showCaptureSheet = false },
                onSave = { rawText, source, manualTags, priorityOverride, deadlineMs, deadlineLabel ->
                    viewModel.captureNote(
                        rawText = rawText,
                        source = source,
                        manualTags = manualTags,
                        manualPriority = priorityOverride,
                        deadlineEpochMs = deadlineMs,
                        deadlineLabel = deadlineLabel
                    )
                    showCaptureSheet = false
                }
            )
        }

        selectedNote?.let { note ->
            NoteDetailSheet(
                note = note,
                panels = panelsList,
                availableTags = userProfile?.defaultTags.orEmpty().ifEmpty {
                    com.example.data.model.DEFAULT_VARIETY_TAGS
                },
                onDismiss = { selectedNote = null },
                onSaveEdits = { title, cleanContent, tags, priority, deadlineMs, deadlineLabel ->
                    viewModel.updateNoteDetails(
                        note = note,
                        title = title,
                        cleanContent = cleanContent,
                        tags = tags,
                        priority = priority,
                        deadlineEpochMs = deadlineMs,
                        deadlineLabel = deadlineLabel
                    )
                },
                onReRunAi = { viewModel.reOrganizeNoteWithAi(note) },
                onShareToPanel = { panel ->
                    viewModel.postPanelIdea(
                        panel = panel,
                        content = note.cleanContent.ifBlank { note.rawContent },
                        tag = note.tags.firstOrNull().orEmpty(),
                        sourceNoteId = note.id
                    )
                },
                onDelete = { viewModel.deleteNote(note.id) }
            )
        }

        HavelStationeryBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 840.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxHeight()
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            Spacer(modifier = Modifier.size(16.dp))
                            HAVEL_DESTINATIONS.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(ROUTE_NOTES) { saveState = true }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        HavelAppIconEmblem(modifier = Modifier.size(34.dp))
                                        Text(
                                            text = "Havel",
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                        AnimatedVisibility(visible = isAiBusy) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "AI Organizing…",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            context.startActivity(
                                                LockScreenNoteActivity.createIntent(context, startVoice = false)
                                            )
                                        },
                                        modifier = Modifier.testTag("top_lockscreen_pad_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Open Lock Screen Note Pad",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val next = if (isDark) ThemePreference.DAY_ALABASTER else ThemePreference.DUSK_SLATE
                                            viewModel.setThemePreference(next)
                                        },
                                        modifier = Modifier.testTag("theme_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Contrast,
                                            contentDescription = "Switch Day / Dusk stationery tone"
                                        )
                                    }
                                    IconButton(
                                        onClick = { showAccountDialog = true },
                                        modifier = Modifier.testTag("account_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountCircle,
                                            contentDescription = "Workspace & account settings"
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (!useNavigationRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    HAVEL_DESTINATIONS.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(dest.route) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                    popUpTo(ROUTE_NOTES) { saveState = true }
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(dest.labelRes)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SmallFloatingActionButton(
                                    onClick = {
                                        captureWithVoice = true
                                        showCaptureSheet = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.testTag("fab_voice_capture")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = "Quick voice capture"
                                    )
                                }
                                FloatingActionButton(
                                    onClick = {
                                        captureWithVoice = false
                                        showCaptureSheet = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.testTag("fab_quick_capture")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Capture thought"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Capture", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_NOTES
                            ) {
                                composable(ROUTE_NOTES) {
                                    NotesFeedScreen(
                                        notesState = notesState,
                                        stitchSuggestions = suggestions,
                                        settings = settings,
                                        isAiBusy = isAiBusy,
                                        onOpenQuickCapture = {
                                            captureWithVoice = false
                                            showCaptureSheet = true
                                        },
                                        onOpenVoiceCapture = {
                                            captureWithVoice = true
                                            showCaptureSheet = true
                                        },
                                        onOpenLockScreenPad = {
                                            context.startActivity(
                                                LockScreenNoteActivity.createIntent(context, startVoice = false)
                                            )
                                        },
                                        onToggleLockScreen = { viewModel.setLockScreenCaptureEnabled(it) },
                                        onToggleFloatingBubble = { viewModel.setFloatingBubbleEnabled(it) },
                                        onNoteClick = { selectedNote = it },
                                        onAutoStitchSuggestion = { suggestion ->
                                            viewModel.stitchNotesIntoScript(suggestion.notes, suggestion.themeTag)
                                        },
                                        onShowMessage = { msg -> viewModel.showMessage(msg) }
                                    )
                                }
                                composable(ROUTE_SCRIPTS) {
                                    StitchedScriptsScreen(
                                        scriptsState = scriptsState,
                                        notes = notesList,
                                        suggestions = suggestions,
                                        isAiBusy = isAiBusy,
                                        onStitchNotes = { chosen, hint, onDone ->
                                            viewModel.stitchNotesIntoScript(chosen, hint, onDone)
                                        },
                                        onDeleteScript = { viewModel.deleteStitchedScript(it) },
                                        onNoteClick = { selectedNote = it },
                                        onShowMessage = { msg -> viewModel.showMessage(msg) }
                                    )
                                }
                                composable(ROUTE_REMINDERS) {
                                    SmartRemindersScreen(
                                        notes = notesList,
                                        settings = settings,
                                        onToggleLockScreen = { viewModel.setLockScreenCaptureEnabled(it) },
                                        onToggleWakeLockPad = { viewModel.setWakeLockPadOnScreenOn(it) },
                                        onToggleFloatingBubble = { viewModel.setFloatingBubbleEnabled(it) },
                                        onOpenLockScreenPad = {
                                            context.startActivity(
                                                LockScreenNoteActivity.createIntent(context, startVoice = false)
                                            )
                                        },
                                        onNoteClick = { selectedNote = it },
                                        onShowMessage = { msg -> viewModel.showMessage(msg) }
                                    )
                                }
                                composable(ROUTE_PANELS) {
                                    TeamPanelsScreen(
                                        currentUserId = currentUserId,
                                        panelsState = panelsState,
                                        selectedPanelId = selectedPanelId,
                                        ideasState = panelIdeasState,
                                        eventsState = panelEventsState,
                                        isAiBusy = isAiBusy,
                                        onSelectPanel = { viewModel.selectPanel(it) },
                                        onCreatePanel = { name, desc -> viewModel.createTeamPanel(name, desc) },
                                        onJoinPanel = { code -> viewModel.joinTeamPanel(code) },
                                        onPostIdea = { panel, content, tag -> viewModel.postPanelIdea(panel, content, tag) },
                                        onToggleUpvote = { panelId, idea -> viewModel.toggleIdeaUpvote(panelId, idea) },
                                        onCreateEvent = { panel, title, prompt, whenLabel ->
                                            viewModel.createBrainstormEvent(panel, title, prompt, whenLabel)
                                        },
                                        onToggleRsvp = { panelId, event -> viewModel.toggleEventRsvp(panelId, event) },
                                        onWrapEventWithAi = { panel, event, ideas ->
                                            viewModel.wrapEventWithAi(panel, event, ideas)
                                        },
                                        onSyncMembers = { panel, ideas, events ->
                                            viewModel.syncPanelMembershipIfNeeded(panel, ideas, events)
                                        },
                                        onShowMessage = { msg -> viewModel.showMessage(msg) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
