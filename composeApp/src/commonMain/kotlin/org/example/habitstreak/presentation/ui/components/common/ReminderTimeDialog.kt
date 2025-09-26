package org.example.habitstreak.presentation.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime

@Composable
fun ReminderTimeDialog(
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTime by remember {
        mutableStateOf(selectedTime ?: LocalTime(9, 0))
    }

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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose when you want to be reminded daily:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Infinite Wheel Time Picker
                InfiniteWheelTimePicker(
                    selectedTime = currentTime,
                    onTimeChanged = { newTime ->
                        currentTime = newTime
                    },
                    is24Hour = false,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Selected: ${formatTime(currentTime)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(currentTime)
                    onDismiss()
                }
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val minute = time.minute.toString().padStart(2, '0')
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "$hour:$minute $amPm"
}