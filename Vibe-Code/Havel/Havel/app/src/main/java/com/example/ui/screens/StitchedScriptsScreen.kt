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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.model.NoteItem
import com.example.data.model.StitchedScript
import com.example.ui.viewmodel.StitchSuggestion
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchedScriptsScreen(
    scriptsState: UiState<List<StitchedScript>>,
    notes: List<NoteItem>,
    suggestions: List<StitchSuggestion>,
    isAiBusy: Boolean,
    onStitchNotes: (List<NoteItem>, String?, () -> Unit) -> Unit,
    onDeleteScript: (String) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showBuilderSheet by remember { mutableStateOf(false) }
    var expandedScript by remember { mutableStateOf<StitchedScript?>(null) }

    if (showBuilderSheet) {
        val builderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val activeNotes = remember(notes) { notes.filter { !it.isArchived } }
        val selectedIds = remember { mutableStateListOf<String>() }

        ModalBottomSheet(
            onDismissRequest = { showBuilderSheet = false },
            sheetState = builderSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Synthesize Idea Script", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Pick 2 or more messy notes. Havel preserves every original note verbatim and writes a cohesive script.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeNotes.forEach { note ->
                        val checked = note.id in selectedIds
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(note.id) else selectedIds.add(note.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title.ifBlank { note.rawContent.take(36) },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = note.rawContent,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val chosen = activeNotes.filter { it.id in selectedIds }
                        onStitchNotes(chosen, chosen.firstOrNull()?.tags?.firstOrNull()) {
                            showBuilderSheet = false
                        }
                    },
                    enabled = selectedIds.size >= 2 && !isAiBusy,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_stitch_button")
                ) {
                    Text(if (isAiBusy) "Synthesizing with Gemini…" else "Stitch ${selectedIds.size} Notes")
                }
            }
        }
    }

    expandedScript?.let { script ->
        val detailState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val linkedNotes = remember(script, notes) {
            notes.filter { it.id in script.linkedNoteIds }
        }
        ModalBottomSheet(
            onDismissRequest = { expandedScript = null },
            sheetState = detailState,
            containerColor = MaterialTheme.colorScheme.surface
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = script.themeTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Row {
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString("${script.title}\n\n${script.scriptBody}"))
                            onShowMessage("Stitched script copied")
                        }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy script")
                        }
                        IconButton(onClick = {
                            onDeleteScript(script.id)
                            expandedScript = null
                        }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete script",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text(text = script.title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = script.insightSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = script.scriptBody,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(18.dp)
                    )
                }

                if (linkedNotes.isNotEmpty()) {
                    Text(
                        text = "Linked Raw Notes (Preserved Verbatim)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    linkedNotes.forEach { rawNote ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedScript = null
                                    onNoteClick(rawNote)
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = rawNote.title.ifBlank { "Raw Capture" },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = rawNote.rawContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
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
            when (scriptsState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(scriptsState.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is UiState.Success -> {
                    val scripts = scriptsState.data
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
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
                                            text = "Idea Linking & Creativity AI",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                    Text(
                                        text = "Synthesizes messy thoughts across days into one cohesive script while preserving every original capture untouched.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = { showBuilderSheet = true },
                                        enabled = notes.count { !it.isArchived } >= 2 && !isAiBusy,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.testTag("open_stitch_builder")
                                    ) {
                                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (notes.count { !it.isArchived } < 2) {
                                                "Capture 2+ notes to stitch"
                                            } else {
                                                "Stitch Selected Notes"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (suggestions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Detected Clusters (3+ Notes)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            items(suggestions, key = { it.themeTag }) { suggestion ->
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${suggestion.notes.size} notes in ${suggestion.themeTag}",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = suggestion.notes.take(2).joinToString(" • ") {
                                                    it.title.ifBlank { it.rawContent.take(28) }
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Button(
                                            onClick = { onStitchNotes(suggestion.notes, suggestion.themeTag) {} },
                                            enabled = !isAiBusy,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                contentColor = MaterialTheme.colorScheme.onSecondary
                                            )
                                        ) {
                                            Text(if (isAiBusy) "Working…" else "Stitch")
                                        }
                                    }
                                }
                            }
                        }

                        if (scripts.isEmpty()) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("No Stitched Scripts yet", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = "Record 3+ ideas around a topic like #ScriptIdea or #Startup and Havel will combine them into a cohesive script.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(scripts, key = { it.id }) { script ->
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedScript = script }
                                        .testTag("script_card_${script.id}")
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
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "${script.themeTag} • ${script.linkedNoteCount} notes",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = "Tap to read",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(text = script.title, style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = script.insightSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = script.scriptBody,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 4
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
}
