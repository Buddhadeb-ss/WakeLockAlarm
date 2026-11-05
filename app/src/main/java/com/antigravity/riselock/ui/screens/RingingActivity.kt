package com.antigravity.riselock.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.riselock.mission.ImageSimilarityMatcher
import com.antigravity.riselock.mission.StepDetectorHelper
import com.antigravity.riselock.model.MissionType
import com.antigravity.riselock.service.AlarmAudioService
import com.antigravity.riselock.service.AlarmScheduler
import com.antigravity.riselock.ui.components.TerracottaButton
import com.antigravity.riselock.ui.theme.RiseLockTheme
import com.antigravity.riselock.ui.theme.SuccessSage
import com.antigravity.riselock.ui.theme.TerracottaAccent
import com.antigravity.riselock.ui.theme.TerracottaSubtle
import com.antigravity.riselock.ui.theme.WarmBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure display turns on and displays directly over the lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmLabel = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Wake Up!"
        val missionTypeName = intent.getStringExtra(AlarmScheduler.EXTRA_MISSION_TYPE) ?: "STEPS"
        val missionType = try {
            MissionType.valueOf(missionTypeName)
        } catch (e: Exception) {
            MissionType.STEPS
        }
        val stepTarget = intent.getIntExtra(AlarmScheduler.EXTRA_STEP_TARGET, 30)
        val photoUri = intent.getStringExtra(AlarmScheduler.EXTRA_PHOTO_URI)
        val photoHash = intent.getLongExtra(AlarmScheduler.EXTRA_PHOTO_HASH, 0L)
        val photoSpot = intent.getStringExtra(AlarmScheduler.EXTRA_PHOTO_SPOT) ?: "Bathroom Sink"

        setContent {
            RiseLockTheme {
                RingingScreenContent(
                    alarmLabel = alarmLabel,
                    missionType = missionType,
                    stepTarget = stepTarget,
                    photoUri = photoUri,
                    photoHash = photoHash,
                    photoSpot = photoSpot,
                    onDismissSuccess = {
                        // Stop Foreground Audio Service
                        val stopIntent = Intent(this, AlarmAudioService::class.java).apply {
                            action = AlarmAudioService.ACTION_STOP_ALARM
                        }
                        startService(stopIntent)
                        finish()
                    }
                )
            }
        }
    }

    // Intercept volume buttons to prevent user from silencing the alarm without completing mission
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true // Consume event
        }
        return super.onKeyDown(keyCode, event)
    }

    // Disable Back button so alarm cannot be closed
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Deliberately no-op; user must complete the mission
    }
}

@Composable
fun RingingScreenContent(
    alarmLabel: String,
    missionType: MissionType,
    stepTarget: Int,
    photoUri: String?,
    photoHash: Long,
    photoSpot: String,
    onDismissSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentTimeString by remember { mutableStateOf("") }
    var currentSteps by remember { mutableIntStateOf(0) }
    var isMissionAccomplished by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }
    var photoMatchMessage by remember { mutableStateOf<String?>(null) }
    var matchScore by remember { mutableIntStateOf(0) }

    // Live clock ticker
    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            currentTimeString = format.format(Date())
            delay(1000)
        }
    }

    // Step Detector Listener (Starts automatically for step missions)
    if (missionType == MissionType.STEPS && !isMissionAccomplished) {
        val stepHelper = remember {
            StepDetectorHelper(context) { steps ->
                currentSteps = steps
                if (steps >= stepTarget) {
                    isMissionAccomplished = true
                }
            }
        }

        DisposableEffect(Unit) {
            stepHelper.startListening(0)
            onDispose {
                stepHelper.stopListening()
            }
        }
    }

    // Camera Mode for Photo Match
    if (isCameraActive) {
        PhotoCaptureScreen(
            title = "Aim at: $photoSpot",
            instruction = "Match your registered reference spot to turn off alarm",
            onPhotoCaptured = { capturedBitmap ->
                isCameraActive = false
                coroutineScope.launch {
                    val refBitmap = photoUri?.let { ImageSimilarityMatcher.loadReferencePhoto(it) }
                    if (refBitmap != null) {
                        val score = ImageSimilarityMatcher.calculateSimilarity(capturedBitmap, refBitmap)
                        matchScore = score
                        if (score >= 68) {
                            isMissionAccomplished = true
                        } else {
                            photoMatchMessage = "Matched $score% (need 70%). Check lighting and angle!"
                        }
                    } else {
                        // If no saved reference photo, taking a clear photo of the room unlocks it
                        isMissionAccomplished = true
                    }
                }
            },
            onCancel = {
                isCameraActive = false
            }
        )
        return
    }

    // Main Alarm Ringing UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Time and Alarm Label
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TerracottaSubtle
                ) {
                    Text(
                        text = "ALARM RINGING",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TerracottaAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = currentTimeString,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = alarmLabel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Middle Section: Dynamic Mission Display
            if (isMissionAccomplished) {
                // Success State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(SuccessSage.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✓", fontSize = 54.sp, color = SuccessSage)
                    }
                    Text(
                        text = "Good Morning!",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Mission verified. You are officially awake.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TerracottaButton(
                        text = "Stop Alarm & Dismiss",
                        onClick = onDismissSuccess,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                when (missionType) {
                    MissionType.STEPS -> {
                        // Live Step Counter Dial
                        val progress = (currentSteps.toFloat() / stepTarget.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier.size(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = WarmBorder,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = TerracottaAccent,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$currentSteps",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 62.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TerracottaAccent
                                    )
                                )
                                Text(
                                    text = "/ $stepTarget steps",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    MissionType.PHOTO_MATCH -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(TerracottaSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📷", fontSize = 48.sp)
                            }
                            Text(
                                text = "Take Photo of:",
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = photoSpot,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = TerracottaAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            if (photoMatchMessage != null) {
                                Text(
                                    text = photoMatchMessage!!,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Red),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            TerracottaButton(
                                text = "Open Camera & Match",
                                onClick = { isCameraActive = true },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            )
                        }
                    }

                    MissionType.NONE -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Standard Wake Up",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            TerracottaButton(
                                text = "Turn Off Alarm",
                                onClick = onDismissSuccess,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Notice: Explaining Why There Are No Snooze Buttons
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔒", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (missionType == MissionType.STEPS)
                            "Walk $stepTarget steps with your phone. Ringtone will not stop until completed."
                        else if (missionType == MissionType.PHOTO_MATCH)
                            "Walk to your $photoSpot and take a live matching photo to shut off alarm."
                        else
                            "Wake up and start your day.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

