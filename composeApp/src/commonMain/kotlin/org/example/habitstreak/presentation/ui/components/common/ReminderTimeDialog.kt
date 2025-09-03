package org.example.habitstreak.presentation.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeDialog(
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(selectedTime?.hour ?: 9) }
    var minute by remember { mutableStateOf(selectedTime?.minute ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text("Set Reminder Time")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose when you want to be reminded daily:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Time picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour selector
                        TimeSelector(
                            value = hour,
                            onValueChange = { hour = it },
                            range = 0..23,
                            label = "Hour",
                            formatter = { it.toString().padStart(2, '0') }
                        )

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Minute selector
                        TimeSelector(
                            value = minute,
                            onValueChange = { minute = it },
                            range = 0..59 step 5,
                            label = "Minute",
                            formatter = { it.toString().padStart(2, '0') }
                        )
                    }
                }

                // Quick presets
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetTimeChip(
                        label = "Morning",
                        time = LocalTime(7, 0),
                        onSelect = {
                            hour = 7
                            minute = 0
                        }
                    )
                    PresetTimeChip(
                        label = "Noon",
                        time = LocalTime(12, 0),
                        onSelect = {
                            hour = 12
                            minute = 0
                        }
                    )
                    PresetTimeChip(
                        label = "Evening",
                        time = LocalTime(19, 0),
                        onSelect = {
                            hour = 19
                            minute = 0
                        }
                    )
                    PresetTimeChip(
                        label = "Night",
                        time = LocalTime(21, 0),
                        onSelect = {
                            hour = 21
                            minute = 0
                        }
                    )
                }

                // Display selected time
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected: ${formatTime(LocalTime(hour, minute))}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime(hour, minute))
                }
            ) {
                Text("Set Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimeSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntProgression,
    label: String,
    formatter: (Int) -> String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.size(80.dp, 120.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                items(range.toList()) { item ->
                    val isSelected = item == value
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        onClick = { onValueChange(item) },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = formatter(item),
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetTimeChip(
    label: String,
    time: LocalTime,
    onSelect: () -> Unit
) {
    AssistChip(
        onClick = onSelect,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatTime(time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.height(48.dp)
    )
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val minute = time.minute.toString().padStart(2, '0')
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "$hour:$minute $amPm"
}