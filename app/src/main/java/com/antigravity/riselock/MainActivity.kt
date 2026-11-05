package com.antigravity.riselock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.antigravity.riselock.data.AlarmRepository
import com.antigravity.riselock.mission.ImageSimilarityMatcher
import com.antigravity.riselock.model.Alarm
import com.antigravity.riselock.model.MissionType
import com.antigravity.riselock.service.AlarmAudioService
import com.antigravity.riselock.service.AlarmScheduler
import com.antigravity.riselock.ui.screens.AlarmEditSheet
import com.antigravity.riselock.ui.screens.AlarmListScreen
import com.antigravity.riselock.ui.screens.MissionsScreen
import com.antigravity.riselock.ui.screens.PhotoCaptureScreen
import com.antigravity.riselock.ui.screens.TimerScreen
import com.antigravity.riselock.ui.theme.RiseLockTheme
import com.antigravity.riselock.ui.theme.TerracottaAccent
import com.antigravity.riselock.ui.theme.TerracottaSubtle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: AlarmRepository
    private lateinit var scheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = AlarmRepository(this)
        scheduler = AlarmScheduler(this)

        setContent {
            RiseLockTheme {
                MainAppContainer(
                    repository = repository,
                    scheduler = scheduler,
                    onOpenExactAlarmSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppContainer(
    repository: AlarmRepository,
    scheduler: AlarmScheduler,
    onOpenExactAlarmSettings: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val alarms by repository.alarmsFlow.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Alarms, 1: Timer, 2: Missions
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var isCreatingAlarm by remember { mutableStateOf(false) }

    // Camera capture state for registering reference spots
    var spotToCapture by remember { mutableStateOf<String?>(null) }

    // Permissions Request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        permissions.add(Manifest.permission.CAMERA)
        permissionLauncher.launch(permissions.toTypedArray())
    }

    // Camera Screen overlay for Spot Registration
    if (spotToCapture != null) {
        val spotName = spotToCapture!!
        val context = androidx.compose.ui.platform.LocalContext.current
        PhotoCaptureScreen(
            title = "Register Spot: $spotName",
            instruction = "Point directly at your $spotName and capture",
            onPhotoCaptured = { bitmap ->
                coroutineScope.launch {
                    val (path, hash) = ImageSimilarityMatcher.saveReferencePhoto(context, spotName, bitmap)
                    // If user is currently editing an alarm with photo match, link it
                    if (editingAlarm != null && editingAlarm?.missionType == MissionType.PHOTO_MATCH) {
                        editingAlarm = editingAlarm?.copy(
                            photoSpotName = spotName,
                            photoReferenceUri = path,
                            photoReferenceHash = hash
                        )
                    }
                    Toast.makeText(context, "Spot '$spotName' registered successfully!", Toast.LENGTH_SHORT).show()
                    spotToCapture = null
                }
            },
            onCancel = { spotToCapture = null }
        )
        return
    }

    Scaffold(
        bottomBar = {
            ClaudeBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "tabTransition") { tab ->
                when (tab) {
                    0 -> AlarmListScreen(
                        alarms = alarms,
                        onToggleAlarm = { alarm, enabled ->
                            repository.toggleAlarm(alarm.id, enabled)
                            val updated = alarm.copy(isEnabled = enabled)
                            if (enabled) {
                                scheduler.schedule(updated)
                            } else {
                                scheduler.cancel(alarm)
                            }
                        },
                        onEditAlarm = { alarm -> editingAlarm = alarm },
                        onDeleteAlarm = { alarm ->
                            scheduler.cancel(alarm)
                            repository.deleteAlarm(alarm.id)
                        },
                        onAddAlarm = { isCreatingAlarm = true }
                    )

                    1 -> TimerScreen(
                        onTimerFinished = {
                            // Ring loud alarm for timer
                            val context = repository.hashCode() // Dummy trigger or send notification
                        }
                    )

                    2 -> MissionsScreen(
                        onLaunchCameraForSpot = { spotName ->
                            spotToCapture = spotName
                        }
                    )
                }
            }

            // Edit / Create Alarm Sheet
            if (isCreatingAlarm || editingAlarm != null) {
                AlarmEditSheet(
                    initialAlarm = editingAlarm,
                    onDismiss = {
                        isCreatingAlarm = false
                        editingAlarm = null
                    },
                    onSave = { alarm ->
                        repository.addOrUpdateAlarm(alarm)
                        scheduler.schedule(alarm)
                        isCreatingAlarm = false
                        editingAlarm = null
                    }
                )
            }
        }
    }
}

@Composable
fun ClaudeBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(0, "⏰", "Alarms"),
                Triple(1, "⏳", "Timer"),
                Triple(2, "🎯", "Missions")
            )

            tabs.forEach { (index, icon, label) ->
                val isSelected = selectedTab == index
                val textColor = if (isSelected) TerracottaAccent else MaterialTheme.colorScheme.onSurfaceVariant
                val pillBg = if (isSelected) TerracottaSubtle else Color.Transparent

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBg)
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}

