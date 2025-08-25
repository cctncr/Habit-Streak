package org.example.habitstreak.presentation.screen.create_edit_habit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.RepeatUnit

@Composable
fun FrequencySelectionDialog(
    currentFrequency: HabitFrequency,
    onFrequencySelected: (HabitFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFrequency by remember { mutableStateOf(currentFrequency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Frequency") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrequencyOption(
                    text = "Daily",
                    isSelected = selectedFrequency is HabitFrequency.Daily,
                    onClick = { selectedFrequency = HabitFrequency.Daily }
                )

                FrequencyOption(
                    text = "Weekly",
                    isSelected = selectedFrequency is HabitFrequency.Weekly,
                    onClick = {
                        selectedFrequency = HabitFrequency.Weekly(
                            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                        )
                    }
                )

                FrequencyOption(
                    text = "Custom",
                    isSelected = selectedFrequency is HabitFrequency.Custom,
                    onClick = {
                        selectedFrequency = HabitFrequency.Custom(2, RepeatUnit.DAYS)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onFrequencySelected(selectedFrequency) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}