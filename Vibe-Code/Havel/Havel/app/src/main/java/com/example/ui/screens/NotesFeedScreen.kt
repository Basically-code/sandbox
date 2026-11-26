package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.HavelSettings
import com.example.data.model.CaptureSource
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority
import com.example.notifications.HavelNotificationHelper
import com.example.overlay.FloatingBubbleService
import com.example.ui.viewmodel.StitchSuggestion
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotesFeedScreen(
    notesState: UiState<List<NoteItem>>,
    stitchSuggestions: List<StitchSuggestion>,
    settings: HavelSettings = HavelSettings(),
    isAiBusy: Boolean,
    onOpenQuickCapture: () -> Unit,
    onOpenVoiceCapture: () -> Unit,
    onOpenLockScreenPad: () -> Unit = {},
    onToggleLockScreen: (Boolean) -> Unit = {},
    onToggleFloatingBubble: (Boolean) -> Unit = {},
    onNoteClick: (NoteItem) -> Unit,
    onAutoStitchSuggestion: (StitchSuggestion) -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("All") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleLockScreen(true)
            onShowMessage("Lock Screen Direct Reply & Lock Pad active")
        } else {
            onShowMessage("Notification permission needed for Lock Screen Write bar.")
        }
    }

    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FloatingBubbleService::class.java)
            )
            onToggleFloatingBubble(true)
            onShowMessage("Floating Sage Bubble active over apps")
        } else {
            onShowMessage("Overlay permission not granted yet.")
        }
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
            when (notesState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = notesState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val activeNotes = remember(notesState.data) {
                        notesState.data.filter { !it.isArchived }
                    }
                    val allTags = remember(activeNotes) {
                        (listOf("All") + DEFAULT_VARIETY_TAGS + activeNotes.flatMap { it.tags }).distinct()
                    }
                    val filteredNotes = remember(activeNotes, searchQuery, selectedTag) {
                        activeNotes.filter { note ->
                            val matchesTag = selectedTag == "All" || selectedTag in note.tags
                            val q = searchQuery.trim()
                            val matchesSearch = q.isEmpty() ||
                                note.title.contains(q, ignoreCase = true) ||
                                note.cleanContent.contains(q, ignoreCase = true) ||
                                note.rawContent.contains(q, ignoreCase = true) ||
                                note.tags.any { it.contains(q, ignoreCase = true) }
                            matchesTag && matchesSearch
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 112.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Zero-Friction Surface Strip: Lock Screen Pad & Floating Bubble right on Home
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Lock Screen Note-Taking Surface Card
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (settings.lockScreenCaptureEnabled) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onOpenLockScreenPad() }
                                        .testTag("home_lockscreen_pad_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Lock Screen Pad",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                            Text(
                                                text = if (settings.lockScreenCaptureEnabled) "ON" else "OFF",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "Switch on & write along without unlocking",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = onOpenLockScreenPad,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Open Pad", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    val enable = !settings.lockScreenCaptureEnabled
                                                    if (enable &&
                                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                        !HavelNotificationHelper.hasNotificationPermission(context)
                                                    ) {
                                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    } else {
                                                        onToggleLockScreen(enable)
                                                        onShowMessage(
                                                            if (enable) "Lock Screen Write bar enabled" else "Lock Screen Write bar paused"
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(
                                                    if (settings.lockScreenCaptureEnabled) "Active" else "Enable",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. On-Spot Floating Bubble Surface Card
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (settings.floatingBubbleEnabled) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val enable = !settings.floatingBubbleEnabled
                                            if (!enable) {
                                                context.stopService(Intent(context, FloatingBubbleService::class.java))
                                                onToggleFloatingBubble(false)
                                                onShowMessage("Floating Sage Bubble closed")
                                            } else if (!Settings.canDrawOverlays(context)) {
                                                val intent = Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                                overlaySettingsLauncher.launch(intent)
                                            } else {
                                                ContextCompat.startForegroundService(
                                                    context,
                                                    Intent(context, FloatingBubbleService::class.java)
                                                )
                                                onToggleFloatingBubble(true)
                                                onShowMessage("Floating Sage Bubble active over apps")
                                            }
                                        }
                                        .testTag("home_floating_bubble_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.BubbleChart,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "On-Spot Bubble",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                            Text(
                                                text = if (settings.floatingBubbleEnabled) "ON" else "OFF",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "Floating sage bubble over any active app",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                val enable = !settings.floatingBubbleEnabled
                                                if (!enable) {
                                                    context.stopService(Intent(context, FloatingBubbleService::class.java))
                                                    onToggleFloatingBubble(false)
                                                    onShowMessage("Floating Sage Bubble closed")
                                                } else if (!Settings.canDrawOverlays(context)) {
                                                    val intent = Intent(
                                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        Uri.parse("package:${context.packageName}")
                                                    )
                                                    overlaySettingsLauncher.launch(intent)
                                                } else {
                                                    ContextCompat.startForegroundService(
                                                        context,
                                                        Intent(context, FloatingBubbleService::class.java)
                                                    )
                                                    onToggleFloatingBubble(true)
                                                    onShowMessage("Floating Sage Bubble active over apps")
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                if (settings.floatingBubbleEnabled) "Dismiss Bubble" else "Launch Bubble",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Zero-friction inline capture prompt card
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenQuickCapture() }
                                    .testTag("inline_capture_prompt")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Just write along…",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Zero menus at capture. AI auto-tags & detects deadlines in 0ms.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clickable { onOpenVoiceCapture() }
                                            .testTag("inline_voice_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Mic,
                                                contentDescription = "Capture voice note",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Search bar & Variety Template filter chips
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search thoughts or #tags…") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("notes_search_input")
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allTags.forEach { tag ->
                                    FilterChip(
                                        selected = selectedTag == tag,
                                        onClick = { selectedTag = tag },
                                        label = {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        // AI Idea Linking Suggestion Banner (when 3+ notes share a theme)
                        if (stitchSuggestions.isNotEmpty()) {
                            val topSuggestion = stitchSuggestions.first()
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "Idea Linking Ready • ${topSuggestion.notes.size} notes in ${topSuggestion.themeTag}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Text(
                                            text = "Havel detected ${topSuggestion.notes.size} related raw thoughts. Synthesize them into one cohesive Stitched Script without altering your originals.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Button(
                                            onClick = { onAutoStitchSuggestion(topSuggestion) },
                                            enabled = !isAiBusy,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                contentColor = MaterialTheme.colorScheme.onSecondary
                                            ),
                                            modifier = Modifier.testTag("stitch_suggestion_button")
                                        ) {
                                            Text(if (isAiBusy) "Stitching Script…" else "Stitch Idea Script")
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredNotes.isEmpty()) {
                            item {
                                EmptyNotesCard(
                                    hasNotes = activeNotes.isNotEmpty(),
                                    onQuickCapture = onOpenQuickCapture
                                )
                            }
                        } else {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteStationeryCard(
                                    note = note,
                                    onClick = { onNoteClick(note) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteStationeryCard(
    note: NoteItem,
    onClick: () -> Unit
) {
    val isHigh = note.priority == NotePriority.HIGH
    val dateLabel = remember(note.createdAt) {
        val date = note.createdAt?.toDate()
        if (date != null) SimpleDateFormat("MMM d • h:mm a", Locale.US).format(date) else "Just now"
    }
    val sourceBadgeLabel = remember(note.captureSource) {
        when (note.captureSource) {
            CaptureSource.LOCK_SCREEN -> "Lock Screen"
            CaptureSource.FLOATING_BUBBLE -> "On-Spot Bubble"
            CaptureSource.VOICE -> "No-Touch Voice"
            else -> "Workspace"
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isHigh) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
        ),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sourceIcon = when (note.captureSource) {
                        CaptureSource.VOICE -> Icons.Rounded.Mic
                        CaptureSource.LOCK_SCREEN -> Icons.Rounded.Lock
                        CaptureSource.FLOATING_BUBBLE -> Icons.Rounded.BubbleChart
                        else -> Icons.Rounded.EditNote
                    }
                    Icon(
                        imageVector = sourceIcon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$sourceBadgeLabel • $dateLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isHigh) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = note.deadlineLabel.ifBlank { "1w→1d→1h" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = note.cleanContent.ifBlank { note.rawContent },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (note.tags.isNotEmpty() || !note.aiProcessed) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    note.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (!note.aiProcessed) {
                        Text(
                            text = "Organizing…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotesCard(
    hasNotes: Boolean,
    onQuickCapture: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = if (hasNotes) "No matching thoughts" else "Your stationery canvas is ready",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (hasNotes) {
                    "Try clearing your search or switching template tags."
                } else {
                    "Tap Capture below, speak a No-Touch Voice Note, or open the Lock Screen Pad."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasNotes) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onQuickCapture) {
                    Text("Capture First Thought")
                }
            }
        }
    }
}
