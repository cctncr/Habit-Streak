package org.example.habitstreak.presentation.screen.create_edit_habit.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import habitstreak.composeapp.generated.resources.*
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.RepeatUnit
import org.jetbrains.compose.resources.stringResource

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

@Composable
fun getFrequencyDisplayText(frequency: HabitFrequency): String {
    return when (frequency) {
        is HabitFrequency.Daily -> stringResource(Res.string.frequency_daily_desc)
        is HabitFrequency.Weekly -> stringResource(Res.string.frequency_weekly_desc, frequency.daysOfWeek.size)
        is HabitFrequency.Monthly -> stringResource(Res.string.frequency_monthly_desc, frequency.daysOfMonth.size)
        is HabitFrequency.Custom -> {
            val unitString = when (frequency.repeatUnit) {
                RepeatUnit.DAYS -> stringResource(Res.string.custom_frequency_days)
                RepeatUnit.WEEKS -> stringResource(Res.string.custom_frequency_weeks)
                RepeatUnit.MONTHS -> stringResource(Res.string.custom_frequency_months)
            }
            stringResource(Res.string.frequency_custom_desc, frequency.repeatInterval, unitString)
        }
    }
}