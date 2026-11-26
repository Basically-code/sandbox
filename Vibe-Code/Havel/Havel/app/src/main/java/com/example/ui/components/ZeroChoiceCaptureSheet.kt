package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.CaptureSource
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.NotePriority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroChoiceCaptureSheet(
    startWithVoice: Boolean,
    availableTags: List<String> = DEFAULT_VARIETY_TAGS,
    onDismiss: () -> Unit,
    onSave: (
        rawText: String,
        source: String,
        manualTags: List<String>,
        priorityOverride: String?,
        deadlineEpochMs: Long,
        deadlineLabel: String
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    var text by remember { mutableStateOf("") }
    var partialVoiceText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var usedVoice by remember { mutableStateOf(startWithVoice) }
    var rmsLevel by remember { mutableFloatStateOf(0.15f) }
    var voiceStatusMessage by remember { mutableStateOf<String?>(null) }

    var showOptionalControls by remember { mutableStateOf(false) }
    val selectedTags = remember { mutableStateListOf<String>() }
    var isHighPriority by remember { mutableStateOf(false) }
    var selectedDeadlineOffsetDays by remember { mutableStateOf<Int?>(null) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceStatusMessage = "Speech recognition service is unavailable on this device. You can type below."
            isListening = false
            return
        }
        voiceStatusMessage = null
        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            speechRecognizer = it
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                usedVoice = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1f)
                rmsLevel = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                rmsLevel = 0.15f
            }

            override fun onError(error: Int) {
                isListening = false
                rmsLevel = 0.15f
                voiceStatusMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch speech — tap mic to try again or type."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for No-Touch Voice Notes."
                    else -> "Voice capture paused (code $error). Type or tap mic to retry."
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()?.trim().orEmpty()
                if (spoken.isNotBlank()) {
                    text = if (text.isBlank()) spoken else "${text.trim()} $spoken"
                }
                partialVoiceText = ""
                isListening = false
                rmsLevel = 0.15f
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partialVoiceText = matches?.firstOrNull()?.trim().orEmpty()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
        isListening = true
        usedVoice = true
    }

    fun stopSpeechRecognition() {
        speechRecognizer?.stopListening()
        isListening = false
        rmsLevel = 0.15f
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechRecognition()
        } else {
            voiceStatusMessage = "Microphone access denied. You can still type effortlessly below."
        }
    }

    fun requestMicAndStart() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startSpeechRecognition()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    LaunchedEffect(startWithVoice) {
        if (startWithVoice) {
            requestMicAndStart()
        } else {
            runCatching { focusRequester.requestFocus() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopSpeechRecognition()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isListening) "Listening… speak naturally" else "Just write along",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        stopSpeechRecognition()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("capture_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close quick capture"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Zero-choice bare cursor input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val combinedPreview = if (partialVoiceText.isNotBlank()) {
                    if (text.isBlank()) partialVoiceText else "${text.trim()} $partialVoiceText"
                } else {
                    text
                }
                if (combinedPreview.isEmpty()) {
                    Text(
                        text = stringResource(R.string.capture_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
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
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("capture_text_input")
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

            // Calm Desaturated Sage voice waveform
            AnimatedVisibility(visible = isListening) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SageVoiceWaveform(
                        amplitude = rmsLevel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(vertical = 4.dp)
                    )
                }
            }

            if (voiceStatusMessage != null) {
                Text(
                    text = voiceStatusMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Optional zero-friction override drawer (collapsed by default per Replan #2)
            AnimatedVisibility(visible = showOptionalControls) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Optional tag or priority (AI auto-tags if left blank)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTags.forEach { tag ->
                            val selected = tag in selectedTags
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) selectedTags.remove(tag) else selectedTags.add(tag)
                                },
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
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("High Priority (1w→1d→1h)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                        listOf(1 to "Tomorrow", 7 to "Next Week").forEach { (days, label) ->
                            val active = selectedDeadlineOffsetDays == days
                            FilterChip(
                                selected = active,
                                onClick = {
                                    selectedDeadlineOffsetDays = if (active) null else days
                                    if (!active) isHighPriority = true
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isListening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                if (isListening) stopSpeechRecognition() else requestMicAndStart()
                            }
                            .testTag("capture_mic_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = if (isListening) "Stop voice capture" else "Start voice capture",
                                tint = if (isListening) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TextButton(
                        onClick = { showOptionalControls = !showOptionalControls },
                        modifier = Modifier.testTag("capture_options_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showOptionalControls) "Hide tags" else "Tags")
                    }
                }

                val canSave = text.isNotBlank() || partialVoiceText.isNotBlank()
                Button(
                    onClick = {
                        stopSpeechRecognition()
                        val finalContent = if (text.isNotBlank()) text else partialVoiceText
                        val deadlineMs = selectedDeadlineOffsetDays?.let { days ->
                            System.currentTimeMillis() + days * 86_400_000L
                        } ?: 0L
                        val deadlineLabel = if (deadlineMs > 0L) {
                            SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(deadlineMs))
                        } else {
                            ""
                        }
                        onSave(
                            finalContent,
                            if (usedVoice) CaptureSource.VOICE else CaptureSource.IN_APP,
                            selectedTags.toList(),
                            if (isHighPriority) NotePriority.HIGH else null,
                            deadlineMs,
                            deadlineLabel
                        )
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("capture_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.capture_save))
                }
            }
        }
    }
}

@Composable
fun SageVoiceWaveform(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    val sage = MaterialTheme.colorScheme.primary
    val terracotta = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        val barCount = 26
        val spacing = size.width / (barCount * 1.6f)
        val barWidth = spacing * 0.8f
        val step = size.width / barCount
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val wave = (sin(phase + i * 0.45f) + 1f) * 0.5f
            val h = (size.height * 0.18f) + (size.height * 0.72f * amplitude * wave)
            val x = i * step + (step - barWidth) / 2f
            drawRoundRect(
                color = if (i % 5 == 0) terracotta.copy(alpha = 0.85f) else sage.copy(alpha = 0.85f),
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
