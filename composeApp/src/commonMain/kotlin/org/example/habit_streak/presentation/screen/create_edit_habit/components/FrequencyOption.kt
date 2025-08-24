package org.example.habit_streak.presentation.screen.create_edit_habit.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.habit_streak.domain.model.HabitFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else null
    )
}

fun getFrequencyDisplayText(frequency: HabitFrequency): String {
    return when (frequency) {
        is HabitFrequency.Daily -> "Every day"
        is HabitFrequency.Weekly -> "Weekly (${frequency.daysOfWeek.size} days)"
        is HabitFrequency.Monthly -> "Monthly (${frequency.daysOfMonth.size} days)"
        is HabitFrequency.Custom -> "Every ${frequency.repeatInterval} ${frequency.repeatUnit.name.lowercase()}"
    }
}