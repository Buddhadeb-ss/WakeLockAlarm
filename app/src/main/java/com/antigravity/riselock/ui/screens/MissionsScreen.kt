package com.antigravity.riselock.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.riselock.mission.ImageSimilarityMatcher
import com.antigravity.riselock.mission.StepDetectorHelper
import com.antigravity.riselock.ui.components.TerracottaButton
import com.antigravity.riselock.ui.components.TerracottaOutlinedButton
import com.antigravity.riselock.ui.components.WarmCard
import com.antigravity.riselock.ui.theme.SuccessSage
import com.antigravity.riselock.ui.theme.TerracottaAccent
import com.antigravity.riselock.ui.theme.TerracottaSubtle
import kotlinx.coroutines.launch

@Composable
fun MissionsScreen(
    onLaunchCameraForSpot: (spotName: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isTestingSteps by remember { mutableStateOf(false) }
    var testStepsCount by remember { mutableIntStateOf(0) }

    val stepHelper = remember {
        StepDetectorHelper(context) { steps ->
            testStepsCount = steps
        }
    }

    DisposableEffect(isTestingSteps) {
        if (isTestingSteps) {
            stepHelper.startListening(0)
        } else {
            stepHelper.stopListening()
        }
        onDispose {
            stepHelper.stopListening()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Missions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TerracottaAccent,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Wake-Up Challenges",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "To dismiss your alarm in the morning, complete one of these guaranteed wake-up missions.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Mission 1: Step Mission Card & Interactive Test
        item {
            WarmCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👣", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Step Challenge",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Walk 20 to 50 steps to stop ringing",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    if (isTestingSteps) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = TerracottaSubtle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$testStepsCount",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        color = TerracottaAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Steps detected in real-time. Walk around your room!",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    TerracottaOutlinedButton(
                        text = if (isTestingSteps) "Stop Testing Sensor" else "Test Step Sensor Now",
                        onClick = {
                            if (isTestingSteps) {
                                isTestingSteps = false
                            } else {
                                testStepsCount = 0
                                isTestingSteps = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Mission 2: Photo Match Mission Card & Spot Manager
        item {
            WarmCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📷", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Photo Match Challenge",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Requires live camera match. No gallery photos.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Text(
                        text = "Register reference spots in advance. When the alarm sounds, you must walk to this location and take a live matching photo to shut it off.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    val recommendedSpots = listOf(
                        "Bathroom Sink" to "Forces you to wash face",
                        "Kitchen Counter" to "Forces you to drink water",
                        "Front Door" to "Forces you out of bed",
                        "Coffee Maker" to "Starts your morning brew"
                    )

                    recommendedSpots.forEach { (spot, reason) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLaunchCameraForSpot(spot) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spot,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TerracottaSubtle
                                ) {
                                    Text(
                                        text = "Capture Spot",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TerracottaAccent,
                                            fontWeight = FontWeight.Bold
                                        )
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

