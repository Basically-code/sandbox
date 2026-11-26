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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.HavelSettings
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority
import com.example.notifications.HavelNotificationHelper
import com.example.overlay.FloatingBubbleService

@Composable
fun SmartRemindersScreen(
    notes: List<NoteItem>,
    settings: HavelSettings,
    onToggleLockScreen: (Boolean) -> Unit,
    onToggleWakeLockPad: (Boolean) -> Unit = {},
    onToggleFloatingBubble: (Boolean) -> Unit,
    onOpenLockScreenPad: () -> Unit = {},
    onNoteClick: (NoteItem) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleLockScreen(true)
            onShowMessage("Lock Screen Direct Reply bar enabled")
        } else {
            onShowMessage("Notifications permission is needed for Lock Screen Write bar & reminders.")
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

    val activeNotes = remember(notes) { notes.filter { !it.isArchived } }
    val highPriorityNotes = remember(activeNotes) {
        activeNotes.filter { it.priority == NotePriority.HIGH || it.deadlineEpochMs > 0L }
    }
    val dailyDigestNotes = remember(activeNotes) {
        activeNotes.filter { it.priority != NotePriority.HIGH && it.deadlineEpochMs == 0L }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Android Zero-Friction Capture Surface Controls
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Zero-Friction Android Triggers",
                            style = MaterialTheme.typography.titleLarge
                        )

                        // 1. Lock Screen Direct Reply & Lock Pad
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text("Lock Screen Write Bar & Pad", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Persistent Direct Reply notification + Lock Screen Note Pad without unlocking.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.lockScreenCaptureEnabled,
                                onCheckedChange = { enable ->
                                    if (!enable) {
                                        onToggleLockScreen(false)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !HavelNotificationHelper.hasNotificationPermission(context)
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onToggleLockScreen(true)
                                        onShowMessage("Lock Screen Write bar ready")
                                    }
                                },
                                modifier = Modifier.testTag("lock_screen_switch")
                            )
                        }

                        // 1b. Auto-wake Lock Screen Pad on Screen-On while locked
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ScreenLockPortrait,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text("Switch On & Write Along (Auto-Wake)", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Automatically presents Havel's Lock Screen Note Pad when you wake your locked phone.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.wakeLockPadOnScreenOn,
                                onCheckedChange = { enable ->
                                    onToggleWakeLockPad(enable)
                                    onShowMessage(
                                        if (enable) "Lock Screen Auto-Wake enabled" else "Lock Screen Auto-Wake paused"
                                    )
                                },
                                modifier = Modifier.testTag("wake_lock_pad_switch")
                            )
                        }

                        // 2. Floating Overlay Bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BubbleChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text("On-Spot Floating Bubble", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Draggable sage bubble over other apps for instant capture.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.floatingBubbleEnabled,
                                onCheckedChange = { enable ->
                                    if (!enable) {
                                        context.stopService(Intent(context, FloatingBubbleService::class.java))
                                        onToggleFloatingBubble(false)
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
                                        onShowMessage("Floating Sage Bubble active")
                                    }
                                },
                                modifier = Modifier.testTag("floating_bubble_switch")
                            )
                        }

                        Button(
                            onClick = onOpenLockScreenPad,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_lock_screen_pad_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Lock Screen Note Pad Now")
                        }
                    }
                }
            }

            // Priority-Gated Escalating Cadence Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Escalating Cascade (1w → 1d → 1h)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (highPriorityNotes.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Zero reminder fatigue", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Only notes flagged High Priority or containing an explicit deadline (like \"Send pitch deck by Friday\") trigger the 1-week → 1-day → 1-hour nudge cadence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(highPriorityNotes, key = { "hp_${it.id}" }) { note ->
                    EscalatingReminderCard(note = note, onClick = { onNoteClick(note) })
                }
            }

            // Calm Daily Digest Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbTwilight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Calm Daily Digest (${dailyDigestNotes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (dailyDigestNotes.isEmpty()) {
                item {
                    Text(
                        text = "Normal-priority thoughts rest here quietly without interrupting your day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(dailyDigestNotes.take(15), key = { "dd_${it.id}" }) { note ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNoteClick(note) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title.ifBlank { note.rawContent.take(40) },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = note.cleanContent.ifBlank { note.rawContent },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (note.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = note.tags.first(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
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
private fun EscalatingReminderCard(
    note: NoteItem,
    onClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val remainingMs = (note.deadlineEpochMs - now).coerceAtLeast(0L)
    val oneWeek = 7 * 86_400_000L
    val oneDay = 86_400_000L
    val oneHour = 3_600_000L

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = note.title.ifBlank { "High Priority" },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = note.deadlineLabel.ifBlank { "Priority Active" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = note.cleanContent.ifBlank { note.rawContent },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            // 1w -> 1d -> 1h visual cadence pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CadencePill(
                    label = "1 Week",
                    active = note.deadlineEpochMs > 0L && remainingMs <= oneWeek
                )
                CadencePill(
                    label = "1 Day",
                    active = note.deadlineEpochMs > 0L && remainingMs <= oneDay
                )
                CadencePill(
                    label = "1 Hour",
                    active = note.deadlineEpochMs > 0L && remainingMs <= oneHour
                )
            }
        }
    }
}

@Composable
private fun CadencePill(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
