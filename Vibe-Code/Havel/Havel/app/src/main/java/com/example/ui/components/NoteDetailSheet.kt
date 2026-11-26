package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority
import com.example.data.model.TeamPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailSheet(
    note: NoteItem,
    panels: List<TeamPanel>,
    availableTags: List<String> = DEFAULT_VARIETY_TAGS,
    onDismiss: () -> Unit,
    onSaveEdits: (
        title: String,
        cleanContent: String,
        tags: List<String>,
        priority: String,
        deadlineEpochMs: Long,
        deadlineLabel: String
    ) -> Unit,
    onReRunAi: () -> Unit,
    onShareToPanel: (TeamPanel) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(note.id) { mutableStateOf(note.title) }
    var cleanContent by remember(note.id) { mutableStateOf(note.cleanContent.ifBlank { note.rawContent }) }
    val tags = remember(note.id) { mutableStateListOf<String>().apply { addAll(note.tags) } }
    var isHighPriority by remember(note.id) { mutableStateOf(note.priority == NotePriority.HIGH) }
    var deadlineEpochMs by remember(note.id) { mutableLongStateOf(note.deadlineEpochMs) }
    var deadlineLabel by remember(note.id) { mutableStateOf(note.deadlineLabel) }
    var showRawVerbatim by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPanelPicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (deadlineEpochMs > 0L) deadlineEpochMs else System.currentTimeMillis() + 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            deadlineEpochMs = picked + 17 * 3_600_000L // default 5pm
                            deadlineLabel = SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(picked))
                            isHighPriority = true
                        }
                        showDatePicker = false
                    }
                ) { Text("Set Deadline") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Note Details",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row {
                    IconButton(
                        onClick = onReRunAi,
                        modifier = Modifier.testTag("note_detail_rerun_ai")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Re-organize with AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("note_detail_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cleanContent,
                onValueChange = { cleanContent = it },
                label = { Text("Organized Note") },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Non-destructive verbatim preservation reveal
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Original Verbatim Capture",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        TextButton(onClick = { showRawVerbatim = !showRawVerbatim }) {
                            Text(if (showRawVerbatim) "Hide" else "View")
                        }
                    }
                    if (showRawVerbatim) {
                        Text(
                            text = note.rawContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Variety Template Tags
            Text(
                text = "Template Tags",
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (availableTags + tags).distinct().forEach { tag ->
                    val selected = tag in tags
                    FilterChip(
                        selected = selected,
                        onClick = { if (selected) tags.remove(tag) else tags.add(tag) },
                        label = { Text(tag, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Priority-gated Smart Escalating Reminder controls
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isHighPriority) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = if (isHighPriority) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text("High-Priority Escalation", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if (isHighPriority) "1 week → 1 day → 1 hour cascade" else "Sits calmly in Daily Digest",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isHighPriority,
                            onCheckedChange = { isHighPriority = it }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (deadlineLabel.isNotBlank()) "Deadline: $deadlineLabel" else "Set Deadline")
                        }
                        if (deadlineEpochMs > 0L) {
                            TextButton(onClick = {
                                deadlineEpochMs = 0L
                                deadlineLabel = ""
                            }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            if (panels.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showPanelPicker = !showPanelPicker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share to Team Panel")
                }

                if (showPanelPicker) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        panels.forEach { panel ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onShareToPanel(panel)
                                    showPanelPicker = false
                                },
                                label = { Text(panel.name) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    onSaveEdits(
                        title,
                        cleanContent,
                        tags.toList(),
                        if (isHighPriority) NotePriority.HIGH else NotePriority.NORMAL,
                        deadlineEpochMs,
                        deadlineLabel
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("note_detail_save")
            ) {
                Text("Save Changes")
            }
        }
    }
}
