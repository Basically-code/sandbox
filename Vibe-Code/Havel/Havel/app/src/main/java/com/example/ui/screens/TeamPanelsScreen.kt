package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.model.BrainstormEvent
import com.example.data.model.EventStatus
import com.example.data.model.PanelIdea
import com.example.data.model.TeamPanel
import com.example.ui.viewmodel.UiState

@Composable
fun TeamPanelsScreen(
    currentUserId: String,
    panelsState: UiState<List<TeamPanel>>,
    selectedPanelId: String?,
    ideasState: UiState<List<PanelIdea>>,
    eventsState: UiState<List<BrainstormEvent>>,
    isAiBusy: Boolean,
    onSelectPanel: (String) -> Unit,
    onCreatePanel: (String, String) -> Unit,
    onJoinPanel: (String) -> Unit,
    onPostIdea: (TeamPanel, String, String) -> Unit,
    onToggleUpvote: (String, PanelIdea) -> Unit,
    onCreateEvent: (TeamPanel, String, String, String) -> Unit,
    onToggleRsvp: (String, BrainstormEvent) -> Unit,
    onWrapEventWithAi: (TeamPanel, BrainstormEvent, List<PanelIdea>) -> Unit,
    onSyncMembers: (TeamPanel, List<PanelIdea>, List<BrainstormEvent>) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var ideaInput by remember { mutableStateOf("") }
    var ideaTag by remember { mutableStateOf("#Startup") }

    if (showCreateDialog) {
        var panelName by remember { mutableStateOf("") }
        var panelDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Team Panel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = panelName,
                        onValueChange = { panelName = it },
                        label = { Text("Panel Name") },
                        placeholder = { Text("e.g., Q4 Creator Launch") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = panelDesc,
                        onValueChange = { panelDesc = it },
                        label = { Text("Description") },
                        placeholder = { Text("Shared raw thoughts & sprint brainstorms") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePanel(panelName, panelDesc)
                        showCreateDialog = false
                    },
                    enabled = panelName.trim().length >= 2
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showJoinDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Team Panel") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Invite Code") },
                    placeholder = { Text("e.g., HVL-8421") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onJoinPanel(code)
                        showJoinDialog = false
                    },
                    enabled = code.trim().length >= 4
                ) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            when (panelsState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(panelsState.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is UiState.Success -> {
                    val panels = panelsState.data
                    val activePanel = panels.firstOrNull { it.id == selectedPanelId } ?: panels.firstOrNull()

                    LaunchedEffect(activePanel?.id) {
                        if (activePanel != null && selectedPanelId != activePanel.id) {
                            onSelectPanel(activePanel.id)
                        }
                    }

                    val ideas = (ideasState as? UiState.Success)?.data.orEmpty()
                    val events = (eventsState as? UiState.Success)?.data.orEmpty()

                    LaunchedEffect(activePanel?.memberUids, ideas.size, events.size) {
                        if (activePanel != null && (ideas.isNotEmpty() || events.isNotEmpty())) {
                            onSyncMembers(activePanel, ideas, events)
                        }
                    }

                    if (showEventDialog && activePanel != null) {
                        var title by remember { mutableStateOf("") }
                        var prompt by remember { mutableStateOf("") }
                        var whenLabel by remember { mutableStateOf("Live Now") }
                        AlertDialog(
                            onDismissRequest = { showEventDialog = false },
                            title = { Text("Start Brainstorming Event") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Session Title") },
                                        placeholder = { Text("e.g., 20-Min Hook Sprint") },
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = prompt,
                                        onValueChange = { prompt = it },
                                        label = { Text("Topic Prompt") },
                                        placeholder = { Text("What angle hasn't been tried yet?") }
                                    )
                                    OutlinedTextField(
                                        value = whenLabel,
                                        onValueChange = { whenLabel = it },
                                        label = { Text("Schedule Label") },
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onCreateEvent(activePanel, title, prompt, whenLabel)
                                        showEventDialog = false
                                    },
                                    enabled = title.trim().length >= 2
                                ) { Text("Launch") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEventDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Top actions: Create / Join Panel
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showCreateDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("create_panel_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New Panel")
                                }
                                OutlinedButton(
                                    onClick = { showJoinDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("join_panel_button")
                                ) {
                                    Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Join Code")
                                }
                            }
                        }

                        if (panels.isEmpty()) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text("Team Panel Workspace", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = "Create a shared Panel to exchange raw notes with teammates and run AI-summarized brainstorming events.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (activePanel != null) {
                            // Panel switcher chips
                            if (panels.size > 1) {
                                item {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        panels.forEach { panel ->
                                            FilterChip(
                                                selected = panel.id == activePanel.id,
                                                onClick = { onSelectPanel(panel.id) },
                                                label = { Text(panel.name) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Active Panel Header Card with Invite Code
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(activePanel.name, style = MaterialTheme.typography.headlineSmall)
                                                if (activePanel.description.isNotBlank()) {
                                                    Text(
                                                        text = activePanel.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.clickable {
                                                    clipboard.setText(AnnotatedString(activePanel.inviteCode))
                                                    onShowMessage("Invite code ${activePanel.inviteCode} copied")
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = activePanel.inviteCode,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Rounded.ContentCopy,
                                                        contentDescription = "Copy invite code",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "${activePanel.memberUids.size} collaborator(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Brainstorming Events Section
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Text("Brainstorming Events", style = MaterialTheme.typography.titleMedium)
                                    }
                                    TextButton(onClick = { showEventDialog = true }) {
                                        Text("+ New Session")
                                    }
                                }
                            }

                            if (events.isEmpty()) {
                                item {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No brainstorming sessions yet — tap + New Session to kick off a live prompt.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(events, key = { it.id }) { event ->
                                    BrainstormEventCard(
                                        event = event,
                                        currentUserId = currentUserId,
                                        isAiBusy = isAiBusy,
                                        onToggleRsvp = { onToggleRsvp(activePanel.id, event) },
                                        onWrapWithAi = { onWrapEventWithAi(activePanel, event, ideas) }
                                    )
                                }
                            }

                            // Post raw idea into panel
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Drop Raw Idea into Panel", style = MaterialTheme.typography.titleSmall)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = ideaInput,
                                                onValueChange = { ideaInput = it },
                                                placeholder = { Text("Share an unpolished spark…") },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("panel_idea_input"),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    onPostIdea(activePanel, ideaInput, ideaTag)
                                                    ideaInput = ""
                                                },
                                                enabled = ideaInput.isNotBlank(),
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("panel_idea_send")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Send,
                                                    contentDescription = "Post idea to panel",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("#Startup", "#ScriptIdea", "#Meeting", "#Design").forEach { tag ->
                                                FilterChip(
                                                    selected = ideaTag == tag,
                                                    onClick = { ideaTag = tag },
                                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Shared Panel Ideas Stream
                            items(ideas, key = { it.id }) { idea ->
                                val upvoted = currentUserId in idea.upvoterUids
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = idea.authorName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (idea.tag.isNotBlank()) {
                                                Text(
                                                    text = idea.tag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(text = idea.content, style = MaterialTheme.typography.bodyMedium)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TextButton(onClick = { onToggleUpvote(activePanel.id, idea) }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ThumbUp,
                                                    contentDescription = "Upvote idea",
                                                    tint = if (upvoted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("${idea.upvoterUids.size}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrainstormEventCard(
    event: BrainstormEvent,
    currentUserId: String,
    isAiBusy: Boolean,
    onToggleRsvp: () -> Unit,
    onWrapWithAi: () -> Unit
) {
    val isWrapped = event.status == EventStatus.WRAPPED
    val isJoined = currentUserId in event.participantUids

    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (isWrapped) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${event.status.uppercase()} • ${event.scheduledLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (event.topicPrompt.isNotBlank()) {
                Text(
                    text = event.topicPrompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isWrapped && event.aiSummary.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "AI Session Synthesis",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = event.aiSummary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onToggleRsvp) {
                        Text(if (isJoined) "Joined (${event.participantUids.size})" else "RSVP (${event.participantUids.size})")
                    }
                    Button(
                        onClick = onWrapWithAi,
                        enabled = !isAiBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isAiBusy) "Summarizing…" else "Wrap & Summarize")
                    }
                }
            }
        }
    }
}
