package com.example.overlay

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.FirebaseSafe
import com.example.data.ai.HavelAiService
import com.example.data.local.HavelLocalDatabase
import com.example.data.local.LocalNoteEntity
import com.example.data.model.CaptureSource
import com.example.data.model.DEFAULT_VARIETY_TAGS
import com.example.data.model.NotePriority
import com.example.data.model.ReminderCadence
import com.example.data.repository.HavelNotesRepository
import com.example.lockscreen.LockScreenNoteActivity
import com.example.notifications.HavelNotificationHelper
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var expandedCardView: View? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        HavelNotificationHelper.ensureChannels(this)
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            301,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, HavelNotificationHelper.CHANNEL_BUBBLE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Havel On-Spot Bubble Active")
            .setContentText("Tap the floating sage bubble anywhere to capture a thought in 0ms.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                HavelNotificationHelper.NOTIFICATION_ID_BUBBLE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(HavelNotificationHelper.NOTIFICATION_ID_BUBBLE, notification)
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun showBubble() {
        if (bubbleView != null) return
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            dp(58),
            dp(58),
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(16)
            y = dp(180)
        }

        val container = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(
                    Color.parseColor("#262C2E"),
                    Color.parseColor("#6E8F80")
                )
                orientation = GradientDrawable.Orientation.TL_BR
                setStroke(dp(2), Color.parseColor("#F6F5F0"))
            }
            elevation = dp(10).toFloat()
            contentDescription = "Havel quick capture bubble"
        }

        val label = TextView(this).apply {
            text = "✦ H"
            setTextColor(Color.parseColor("#F6F5F0"))
            textSize = 15f
            gravity = Gravity.CENTER
        }
        container.addView(label)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (initialTouchX - event.rawX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) {
                        moved = true
                    }
                    bubbleParams.x = (initialX + dx).coerceAtLeast(0)
                    bubbleParams.y = (initialY + dy).coerceAtLeast(0)
                    windowManager?.updateViewLayout(container, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        toggleExpandedCard()
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = container
        windowManager?.addView(container, bubbleParams)
    }

    private fun toggleExpandedCard() {
        if (expandedCardView != null) {
            removeExpandedCard()
            return
        }
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val cardParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(96)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(Color.parseColor("#F6F5F0"))
                setStroke(dp(1), Color.parseColor("#D6CFC2"))
            }
            elevation = dp(12).toFloat()
        }

        val wrapper = FrameLayout(this).apply {
            setPadding(dp(16), 0, dp(16), 0)
            addView(root)
        }

        val header = TextView(this).apply {
            text = "Havel On-Spot Capture • Zero Menus"
            setTextColor(Color.parseColor("#2C2A29"))
            textSize = 15f
        }
        root.addView(header)

        val input = EditText(this).apply {
            hint = "Just write along… AI auto-tags & detects deadlines"
            setHintTextColor(Color.parseColor("#8A8378"))
            setTextColor(Color.parseColor("#2C2A29"))
            setBackgroundColor(Color.TRANSPARENT)
            minLines = 3
            setPadding(0, dp(10), 0, dp(12))
            requestFocus()
        }
        root.addView(input)

        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val voiceButton = Button(this).apply {
            text = "Voice Note"
            setOnClickListener {
                removeExpandedCard()
                startActivity(LockScreenNoteActivity.createIntent(this@FloatingBubbleService, startVoice = true))
            }
        }

        val dismissButton = Button(this).apply {
            text = "Close"
            setOnClickListener { removeExpandedCard() }
        }

        val saveButton = Button(this).apply {
            text = "Save (0ms)"
            setOnClickListener {
                val content = input.text?.toString()?.trim().orEmpty()
                if (content.isNotBlank()) {
                    saveQuickCapture(content)
                }
                removeExpandedCard()
            }
        }

        actionsRow.addView(voiceButton)
        actionsRow.addView(dismissButton)
        actionsRow.addView(saveButton)
        root.addView(actionsRow)

        expandedCardView = wrapper
        windowManager?.addView(wrapper, cardParams)
    }

    private fun saveQuickCapture(content: String) {
        serviceScope.launch(Dispatchers.IO) {
            val aiService = HavelAiService()
            val enrichment = aiService.analyzeCapture(content, DEFAULT_VARIETY_TAGS).getOrElse {
                aiService.analyzeCaptureLocally(content, DEFAULT_VARIETY_TAGS)
            }
            val priority = if (enrichment.isHighPriority) NotePriority.HIGH else NotePriority.NORMAL
            val cadence = if (priority == NotePriority.HIGH || enrichment.deadlineEpochMs > 0L) {
                ReminderCadence.ESCALATING_1W_1D_1H
            } else {
                ReminderCadence.DAILY_DIGEST
            }
            val uid = FirebaseSafe.currentUserOrNull(this@FloatingBubbleService)?.uid ?: FirebaseSafe.LOCAL_USER_ID
            val noteId = "note_fb_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val now = System.currentTimeMillis()

            val localNote = LocalNoteEntity(
                id = noteId,
                userId = uid,
                rawContent = content,
                cleanContent = enrichment.cleanContent,
                title = enrichment.title,
                tagsPipeSeparated = enrichment.tags.joinToString("|"),
                captureSource = CaptureSource.FLOATING_BUBBLE,
                priority = priority,
                deadlineEpochMs = enrichment.deadlineEpochMs,
                deadlineLabel = enrichment.deadlineLabel,
                reminderCadence = cadence,
                aiProcessed = true,
                isArchived = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )

            val localDb = HavelLocalDatabase.getInstance(this@FloatingBubbleService)
            localDb.localWorkspaceDao().upsertNote(localNote)

            if (priority == NotePriority.HIGH && enrichment.deadlineEpochMs > 0L) {
                ReminderScheduler.scheduleEscalatingCascade(this@FloatingBubbleService, localNote.toNoteItem())
            }

            if (FirebaseSafe.currentUserOrNull(this@FloatingBubbleService) != null) {
                runCatching {
                    val repo = HavelNotesRepository(this@FloatingBubbleService)
                    repo.createNote(
                        rawContent = content,
                        cleanContent = enrichment.cleanContent,
                        title = enrichment.title,
                        tags = enrichment.tags,
                        captureSource = CaptureSource.FLOATING_BUBBLE,
                        priority = priority,
                        deadlineEpochMs = enrichment.deadlineEpochMs,
                        deadlineLabel = enrichment.deadlineLabel,
                        reminderCadence = cadence,
                        aiProcessed = true
                    )
                }
            }

            launch(Dispatchers.Main) {
                Toast.makeText(
                    this@FloatingBubbleService,
                    "Saved to Havel • ${enrichment.tags.joinToString(" ")}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun removeExpandedCard() {
        expandedCardView?.let {
            runCatching { windowManager?.removeView(it) }
            expandedCardView = null
        }
    }

    override fun onDestroy() {
        removeExpandedCard()
        bubbleView?.let {
            runCatching { windowManager?.removeView(it) }
            bubbleView = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
