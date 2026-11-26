package com.example.lockscreen

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.core.FirebaseSafe
import com.example.data.ai.HavelAiService
import com.example.data.local.HavelLocalDatabase
import com.example.data.local.LocalNoteEntity
import com.example.data.model.CaptureSource
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.NoteItem
import com.example.data.model.NotePriority
import com.example.data.model.ReminderCadence
import com.example.data.repository.HavelNotesRepository
import com.example.notifications.HavelNotificationHelper
import com.example.notifications.ReminderScheduler
import com.example.ui.components.HavelAppIconEmblem
import com.example.ui.components.SageVoiceWaveform
import com.example.ui.theme.HavelStationeryBackground
import com.example.ui.theme.HavelTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Dedicated Zero-Friction Lock Screen Note-Taking Activity.
 * Renders directly over the Android lock screen (showWhenLocked + turnScreenOn) so users can
 * "just switch on and write along" or record No-Touch Voice Notes without unlocking their phone.
 */
class LockScreenNoteActivity : ComponentActivity() {

    private var startWithVoice by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenWindow()
        startWithVoice = intent?.getBooleanExtra(EXTRA_START_VOICE, false) == true
        enableEdgeToEdge()

        setContent {
            // Dusk stationery tone on Lock Screen prevents glare in day or night
            HavelTheme(darkTheme = true) {
                LockScreenCaptureSurface(
                    startWithVoice = startWithVoice,
                    onOpenFullWorkspace = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguard?.isKeyguardLocked == true) {
                            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                                override fun onDismissSucceeded() {
                                    startActivity(mainIntent)
                                    finish()
                                }
                                override fun onDismissCancelled() {
                                    startActivity(mainIntent)
                                    finish()
                                }
                            })
                        } else {
                            startActivity(mainIntent)
                            finish()
                        }
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startWithVoice = intent.getBooleanExtra(EXTRA_START_VOICE, false)
    }

    private fun configureLockScreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_START_VOICE = "extra_lockscreen_start_voice"

        fun createIntent(context: Context, startVoice: Boolean = false): Intent {
            return Intent(context, LockScreenNoteActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_START_VOICE, startVoice)
            }
        }
    }
}

@Composable
private fun LockScreenCaptureSurface(
    startWithVoice: Boolean,
    onOpenFullWorkspace: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var currentTimeLabel by remember {
        mutableStateOf(SimpleDateFormat("h:mm", Locale.US).format(Date()))
    }
    var currentDateLabel by remember {
        mutableStateOf(SimpleDateFormat("EEEE, MMMM d", Locale.US).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTimeLabel = SimpleDateFormat("h:mm", Locale.US).format(now)
            currentDateLabel = SimpleDateFormat("EEEE, MMMM d", Locale.US).format(now)
            delay(15_000L)
        }
    }

    var text by remember { mutableStateOf("") }
    var partialVoiceText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableFloatStateOf(0.2f) }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val selectedTags = remember { mutableStateListOf<String>() }
    var isHighPriority by remember { mutableStateOf(false) }
    val sessionCaptures = remember { mutableStateListOf<NoteItem>() }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun startSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            statusBanner = "Voice recognition unavailable on this device — type below."
            isListening = false
            return
        }
        statusBanner = null
        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            speechRecognizer = it
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0.12f, 1f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                rmsLevel = 0.2f
            }
            override fun onError(error: Int) {
                isListening = false
                rmsLevel = 0.2f
                statusBanner = "Voice paused — tap mic to speak again or write directly."
            }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                if (spoken.isNotBlank()) {
                    text = if (text.isBlank()) spoken else "${text.trim()} $spoken"
                }
                partialVoiceText = ""
                isListening = false
                rmsLevel = 0.2f
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialVoiceText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(recognizerIntent)
        isListening = true
    }

    fun stopSpeech() {
        speechRecognizer?.stopListening()
        isListening = false
        rmsLevel = 0.2f
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startSpeech() else statusBanner = "Microphone permission needed for voice capture."
    }

    fun triggerVoiceCapture() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) startSpeech() else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    LaunchedEffect(startWithVoice) {
        if (startWithVoice) {
            triggerVoiceCapture()
        } else {
            runCatching { focusRequester.requestFocus() }
        }
    }

    fun saveLockScreenThought(closeAfterSave: Boolean) {
        stopSpeech()
        val raw = (if (text.isNotBlank()) text else partialVoiceText).trim()
        if (raw.isBlank() || isSaving) return
        isSaving = true
        val chosenTags = selectedTags.toList()
        val chosenHighPriority = isHighPriority

        scope.launch {
            val savedNote = withContext(Dispatchers.IO) {
                val aiService = HavelAiService()
                val enrichment = aiService.analyzeCapture(raw, DEFAULT_VARIETY_TAGS).getOrElse {
                    aiService.analyzeCaptureLocally(raw, DEFAULT_VARIETY_TAGS)
                }
                val finalTags = (chosenTags + enrichment.tags).distinct().take(5)
                val finalPriority = if (chosenHighPriority || enrichment.isHighPriority) {
                    NotePriority.HIGH
                } else {
                    NotePriority.NORMAL
                }
                val finalCadence = if (finalPriority == NotePriority.HIGH || enrichment.deadlineEpochMs > 0L) {
                    ReminderCadence.ESCALATING_1W_1D_1H
                } else {
                    ReminderCadence.DAILY_DIGEST
                }
                val uid = FirebaseSafe.currentUserOrNull(context)?.uid ?: FirebaseSafe.LOCAL_USER_ID
                val noteId = "note_ls_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                val now = System.currentTimeMillis()

                val localEntity = LocalNoteEntity(
                    id = noteId,
                    userId = uid,
                    rawContent = raw,
                    cleanContent = enrichment.cleanContent,
                    title = enrichment.title,
                    tagsPipeSeparated = finalTags.joinToString("|"),
                    captureSource = CaptureSource.LOCK_SCREEN,
                    priority = finalPriority,
                    deadlineEpochMs = enrichment.deadlineEpochMs,
                    deadlineLabel = enrichment.deadlineLabel,
                    reminderCadence = finalCadence,
                    aiProcessed = true,
                    isArchived = false,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
                HavelLocalDatabase.getInstance(context).localWorkspaceDao().upsertNote(localEntity)

                // Also sync to Firestore if signed in
                if (FirebaseSafe.currentUserOrNull(context) != null) {
                    runCatching {
                        HavelNotesRepository(context).createNote(
                            rawContent = raw,
                            cleanContent = enrichment.cleanContent,
                            title = enrichment.title,
                            tags = finalTags,
                            captureSource = CaptureSource.LOCK_SCREEN,
                            priority = finalPriority,
                            deadlineEpochMs = enrichment.deadlineEpochMs,
                            deadlineLabel = enrichment.deadlineLabel,
                            reminderCadence = finalCadence,
                            aiProcessed = true
                        )
                    }
                }

                val item = localEntity.toNoteItem()
                if (finalPriority == NotePriority.HIGH && enrichment.deadlineEpochMs > 0L) {
                    ReminderScheduler.scheduleEscalatingCascade(context, item)
                }
                HavelNotificationHelper.showLockScreenQuickCapture(
                    context = context,
                    statusText = "Saved on Lock Screen (${finalTags.joinToString(" ")}): \"${enrichment.title}\""
                )
                item
            }

            sessionCaptures.add(0, savedNote)
            text = ""
            partialVoiceText = ""
            selectedTags.clear()
            isHighPriority = false
            isSaving = false
            statusBanner = "Saved in 0ms • AI tagged ${savedNote.tags.joinToString(" ")}"

            if (closeAfterSave) {
                onClose()
            }
        }
    }

    HavelStationeryBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Lock Screen Header with Clock, Emblem, and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HavelAppIconEmblem(modifier = Modifier.size(38.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "LOCK SCREEN PAD • 0ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onOpenFullWorkspace,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("lockscreen_open_workspace")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Workspace", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("lockscreen_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss Lock Screen Pad"
                            )
                        }
                    }
                }

                // Lock Screen Time & Date Display
                Column {
                    Text(
                        text = currentTimeLabel,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$currentDateLabel • Switch on and write along",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Zero-Choice Instant Stationery Note Card
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isListening) "Listening on Lock Screen…" else "Write a fleeting thought…",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AI Auto-Tags",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            val combined = if (partialVoiceText.isNotBlank()) {
                                if (text.isBlank()) partialVoiceText else "${text.trim()} $partialVoiceText"
                            } else {
                                text
                            }
                            if (combined.isEmpty()) {
                                Text(
                                    text = "Type immediately or tap the sage mic to speak without unlocking…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            BasicTextField(
                                value = text,
                                onValueChange = { text = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .focusRequester(focusRequester)
                                    .testTag("lockscreen_note_input")
                            )
                            if (partialVoiceText.isNotBlank()) {
                                Text(
                                    text = partialVoiceText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.BottomStart)
                                )
                            }
                        }

                        AnimatedVisibility(visible = isListening) {
                            SageVoiceWaveform(
                                amplitude = rmsLevel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            )
                        }

                        // Optional 1-tap Variety Template Tags & High-Priority toggle
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = isHighPriority,
                                onClick = { isHighPriority = !isHighPriority },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = { Text("1w→1d→1h Priority", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                            DEFAULT_VARIETY_TAGS.forEach { tag ->
                                val selected = tag in selectedTags
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) selectedTags.remove(tag) else selectedTags.add(tag)
                                    },
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        // Action Bar: Voice Mic + Save & Continue + Save & Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isListening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        if (isListening) stopSpeech() else triggerVoiceCapture()
                                    }
                                    .testTag("lockscreen_mic_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                        contentDescription = "Voice Note on Lock Screen",
                                        tint = if (isListening) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val canSave = (text.isNotBlank() || partialVoiceText.isNotBlank()) && !isSaving
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { saveLockScreenThought(closeAfterSave = false) },
                                    enabled = canSave,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("lockscreen_save_next_button")
                                ) {
                                    Text("Save + Next")
                                }
                                Button(
                                    onClick = { saveLockScreenThought(closeAfterSave = true) },
                                    enabled = canSave,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("lockscreen_save_done_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Note")
                                }
                            }
                        }
                    }
                }

                if (statusBanner != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = statusBanner.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (sessionCaptures.isNotEmpty()) {
                    Text(
                        text = "Captured on Lock Screen (${sessionCaptures.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    sessionCaptures.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = note.tags.joinToString(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = note.cleanContent,
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
}
