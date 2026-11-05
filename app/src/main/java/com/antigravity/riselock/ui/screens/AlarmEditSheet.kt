package com.antigravity.riselock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.riselock.model.Alarm
import com.antigravity.riselock.model.MissionType
import com.antigravity.riselock.ui.components.DayChip
import com.antigravity.riselock.ui.components.TerracottaButton
import com.antigravity.riselock.ui.components.TerracottaOutlinedButton
import com.antigravity.riselock.ui.components.TerracottaSwitch
import com.antigravity.riselock.ui.components.WarmCard
import com.antigravity.riselock.ui.theme.TerracottaAccent
import com.antigravity.riselock.ui.theme.TerracottaSubtle
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    initialAlarm: Alarm?,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hour by remember { mutableIntStateOf(initialAlarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Wake Up") }
    var repeatDays by remember { mutableStateOf(initialAlarm?.repeatDays ?: setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) }
    var missionType by remember { mutableStateOf(initialAlarm?.missionType ?: MissionType.STEPS) }
    var stepTarget by remember { mutableFloatStateOf(initialAlarm?.stepTarget?.toFloat() ?: 30f) }
    var photoSpot by remember { mutableStateOf(initialAlarm?.photoSpotName ?: "Bathroom Sink") }
    var isExtraLoud by remember { mutableStateOf(initialAlarm?.isExtraLoud ?: true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialAlarm == null) "New Alarm" else "Edit Alarm",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge.copy(color = TerracottaAccent),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            // Time Selector (Hour / Minute / AM-PM)
            WarmCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isPm = hour >= 12
                    val displayHour = when {
                        hour == 0 -> 12
                        hour > 12 -> hour - 12
                        else -> hour
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Hour Selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "▲",
                                modifier = Modifier.clickable { hour = (hour + 1) % 24 },
                                color = TerracottaAccent
                            )
                            Text(
                                text = String.format("%02d", displayHour),
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light)
                            )
                            Text(
                                text = "▼",
                                modifier = Modifier.clickable { hour = if (hour == 0) 23 else hour - 1 },
                                color = TerracottaAccent
                            )
                        }

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Minute Selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "▲",
                                modifier = Modifier.clickable { minute = (minute + 5) % 60 },
                                color = TerracottaAccent
                            )
                            Text(
                                text = String.format("%02d", minute),
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light)
                            )
                            Text(
                                text = "▼",
                                modifier = Modifier.clickable { minute = if (minute < 5) 55 else minute - 5 },
                                color = TerracottaAccent
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // AM/PM Toggle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TerracottaSubtle,
                            modifier = Modifier.clickable {
                                hour = if (hour >= 12) hour - 12 else hour + 12
                            }
                        ) {
                            Text(
                                text = if (isPm) "PM" else "AM",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaAccent
                                )
                            )
                        }
                    }
                }
            }

            // Repeat Days
            Column {
                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf(
                        "M" to Calendar.MONDAY,
                        "T" to Calendar.TUESDAY,
                        "W" to Calendar.WEDNESDAY,
                        "T" to Calendar.THURSDAY,
                        "F" to Calendar.FRIDAY,
                        "S" to Calendar.SATURDAY,
                        "S" to Calendar.SUNDAY
                    )
                    days.forEach { (initial, calDay) ->
                        val isSelected = repeatDays.contains(calDay)
                        DayChip(
                            dayInitial = initial,
                            isSelected = isSelected,
                            onToggle = {
                                repeatDays = if (isSelected) {
                                    repeatDays - calDay
                                } else {
                                    repeatDays + calDay
                                }
                            }
                        )
                    }
                }
            }

            // Label
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Label") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerracottaAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = TerracottaAccent
                )
            )

            // Mission Selection (The Wake-Up Lock Feature)
            Column {
                Text(
                    text = "Wake-Up Mission (To Turn Off)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Alarm will not stop until this mission is verified. No snooze button.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MissionType.values().forEach { type ->
                        val isSelected = missionType == type
                        val bg = if (isSelected) TerracottaAccent else MaterialTheme.colorScheme.surface
                        val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) TerracottaAccent else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .background(bg)
                                .clickable { missionType = type }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (type) {
                                    MissionType.NONE -> "Standard"
                                    MissionType.STEPS -> "👣 Steps"
                                    MissionType.PHOTO_MATCH -> "📷 Photo"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            )
                        }
                    }
                }

                // Mission details config
                if (missionType == MissionType.STEPS) {
                    Spacer(modifier = Modifier.height(16.dp))
                    WarmCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Target Steps",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "${stepTarget.toInt()} steps",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TerracottaAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Slider(
                                value = stepTarget,
                                onValueChange = { stepTarget = it },
                                valueRange = 10f..100f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = TerracottaAccent,
                                    activeTrackColor = TerracottaAccent
                                )
                            )
                            Text(
                                text = "Take your phone and walk to the bathroom or kitchen. Real-time sensor verification.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                } else if (missionType == MissionType.PHOTO_MATCH) {
                    Spacer(modifier = Modifier.height(16.dp))
                    WarmCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Target Spot to Photograph",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            val spots = listOf("Bathroom Sink", "Kitchen Stove", "Coffee Maker", "Front Door")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                spots.take(2).forEach { s ->
                                    val isSelected = photoSpot == s
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, if (isSelected) TerracottaAccent else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                            .background(if (isSelected) TerracottaSubtle else Color.Transparent)
                                            .clickable { photoSpot = s }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) TerracottaAccent else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                spots.drop(2).forEach { s ->
                                    val isSelected = photoSpot == s
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, if (isSelected) TerracottaAccent else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                            .background(if (isSelected) TerracottaSubtle else Color.Transparent)
                                            .clickable { photoSpot = s }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) TerracottaAccent else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Only direct camera capture is permitted during alarm ringing — no gallery photos allowed.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }

            // Extra Loud Option
            WarmCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Un-Ignorable Volume & Vibration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Forces stream volume to 100% and locks volume buttons during alarm",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    TerracottaSwitch(
                        checked = isExtraLoud,
                        onCheckedChange = { isExtraLoud = it }
                    )
                }
            }

            // Save & Cancel Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TerracottaOutlinedButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TerracottaButton(
                    text = "Save Alarm",
                    onClick = {
                        val alarm = (initialAlarm ?: Alarm(hour = hour, minute = minute)).copy(
                            hour = hour,
                            minute = minute,
                            label = label.ifBlank { "Wake Up" },
                            repeatDays = repeatDays,
                            missionType = missionType,
                            stepTarget = stepTarget.toInt(),
                            photoSpotName = photoSpot,
                            isExtraLoud = isExtraLoud,
                            isEnabled = true
                        )
                        onSave(alarm)
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

