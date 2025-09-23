package org.example.habitstreak.presentation.screen.create_edit_habit.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import habitstreak.composeapp.generated.resources.*
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.RepeatUnit
import org.jetbrains.compose.resources.stringResource

@Composable
fun AdvancedFrequencyDialog(
    currentFrequency: HabitFrequency,
    onFrequencySelected: (HabitFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFrequency by remember { mutableStateOf(currentFrequency) }
    var showCustomDetails by remember { mutableStateOf(false) }
    var showWeeklyDetails by remember { mutableStateOf(false) }
    var showMonthlyDetails by remember { mutableStateOf(false) }

    if (showCustomDetails) {
        CustomFrequencyDialog(
            currentFrequency = selectedFrequency as? HabitFrequency.Custom
                ?: HabitFrequency.Custom(2, RepeatUnit.DAYS),
            onFrequencySelected = { frequency ->
                selectedFrequency = frequency
                showCustomDetails = false
            },
            onDismiss = { showCustomDetails = false }
        )
    } else if (showWeeklyDetails) {
        WeeklyFrequencyDialog(
            currentFrequency = selectedFrequency as? HabitFrequency.Weekly
                ?: HabitFrequency.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            onFrequencySelected = { frequency ->
                selectedFrequency = frequency
                showWeeklyDetails = false
            },
            onDismiss = { showWeeklyDetails = false }
        )
    } else if (showMonthlyDetails) {
        MonthlyFrequencyDialog(
            currentFrequency = selectedFrequency as? HabitFrequency.Monthly
                ?: HabitFrequency.Monthly(setOf(1, 15)),
            onFrequencySelected = { frequency ->
                selectedFrequency = frequency
                showMonthlyDetails = false
            },
            onDismiss = { showMonthlyDetails = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.frequency_dialog_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrequencyOption(
                        text = stringResource(Res.string.frequency_daily),
                        description = stringResource(Res.string.frequency_daily_desc),
                        isSelected = selectedFrequency is HabitFrequency.Daily,
                        onClick = { selectedFrequency = HabitFrequency.Daily }
                    )

                    FrequencyOption(
                        text = stringResource(Res.string.frequency_weekly),
                        description = getWeeklyDescription(selectedFrequency as? HabitFrequency.Weekly),
                        isSelected = selectedFrequency is HabitFrequency.Weekly,
                        onClick = { showWeeklyDetails = true }
                    )

                    FrequencyOption(
                        text = stringResource(Res.string.frequency_monthly),
                        description = getMonthlyDescription(selectedFrequency as? HabitFrequency.Monthly),
                        isSelected = selectedFrequency is HabitFrequency.Monthly,
                        onClick = { showMonthlyDetails = true }
                    )

                    FrequencyOption(
                        text = stringResource(Res.string.frequency_custom),
                        description = getCustomDescription(selectedFrequency as? HabitFrequency.Custom),
                        isSelected = selectedFrequency is HabitFrequency.Custom,
                        onClick = { showCustomDetails = true }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onFrequencySelected(selectedFrequency) }
                ) {
                    Text(stringResource(Res.string.frequency_dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.frequency_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun FrequencyOption(
    text: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getWeeklyDescription(frequency: HabitFrequency.Weekly?): String {
    return if (frequency != null) {
        stringResource(Res.string.frequency_weekly_desc, frequency.daysOfWeek.size)
    } else {
        stringResource(Res.string.frequency_weekly_desc, 3)
    }
}

@Composable
private fun getMonthlyDescription(frequency: HabitFrequency.Monthly?): String {
    return if (frequency != null) {
        stringResource(Res.string.frequency_monthly_desc, frequency.daysOfMonth.size)
    } else {
        stringResource(Res.string.frequency_monthly_desc, 2)
    }
}

@Composable
private fun getCustomDescription(frequency: HabitFrequency.Custom?): String {
    return if (frequency != null) {
        when (frequency.repeatUnit) {
            RepeatUnit.DAYS -> stringResource(Res.string.frequency_custom_desc, frequency.repeatInterval, stringResource(Res.string.custom_frequency_days))
            RepeatUnit.WEEKS -> stringResource(Res.string.frequency_custom_desc, frequency.repeatInterval, stringResource(Res.string.custom_frequency_weeks))
            RepeatUnit.MONTHS -> stringResource(Res.string.frequency_custom_desc, frequency.repeatInterval, stringResource(Res.string.custom_frequency_months))
        }
    } else {
        stringResource(Res.string.frequency_custom_desc, 2, stringResource(Res.string.custom_frequency_days))
    }
}

@Composable
private fun CustomFrequencyDialog(
    currentFrequency: HabitFrequency.Custom,
    onFrequencySelected: (HabitFrequency.Custom) -> Unit,
    onDismiss: () -> Unit
) {
    var interval by remember { mutableStateOf(currentFrequency.repeatInterval.toString()) }
    var selectedUnit by remember { mutableStateOf(currentFrequency.repeatUnit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.custom_frequency_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.custom_frequency_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(Res.string.custom_frequency_repeat_every))

                    OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Unit Selection
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(60.dp)
                ) {
                    items(RepeatUnit.values()) { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit },
                            label = {
                                Text(
                                    when (unit) {
                                        RepeatUnit.DAYS -> stringResource(Res.string.custom_frequency_days)
                                        RepeatUnit.WEEKS -> stringResource(Res.string.custom_frequency_weeks)
                                        RepeatUnit.MONTHS -> stringResource(Res.string.custom_frequency_months)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intervalInt = interval.toIntOrNull() ?: 1
                    onFrequencySelected(HabitFrequency.Custom(intervalInt.coerceAtLeast(1), selectedUnit))
                }
            ) {
                Text(stringResource(Res.string.frequency_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.frequency_dialog_cancel))
            }
        }
    )
}

@Composable
private fun WeeklyFrequencyDialog(
    currentFrequency: HabitFrequency.Weekly,
    onFrequencySelected: (HabitFrequency.Weekly) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDays by remember { mutableStateOf(currentFrequency.daysOfWeek) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.weekly_frequency_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.weekly_frequency_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(DayOfWeek.values()) { day ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = {
                                Text(
                                    when (day) {
                                        DayOfWeek.MONDAY -> stringResource(Res.string.monday)
                                        DayOfWeek.TUESDAY -> stringResource(Res.string.tuesday)
                                        DayOfWeek.WEDNESDAY -> stringResource(Res.string.wednesday)
                                        DayOfWeek.THURSDAY -> stringResource(Res.string.thursday)
                                        DayOfWeek.FRIDAY -> stringResource(Res.string.friday)
                                        DayOfWeek.SATURDAY -> stringResource(Res.string.saturday)
                                        DayOfWeek.SUNDAY -> stringResource(Res.string.sunday)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onFrequencySelected(HabitFrequency.Weekly(selectedDays)) },
                enabled = selectedDays.isNotEmpty()
            ) {
                Text(stringResource(Res.string.frequency_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.frequency_dialog_cancel))
            }
        }
    )
}

@Composable
private fun MonthlyFrequencyDialog(
    currentFrequency: HabitFrequency.Monthly,
    onFrequencySelected: (HabitFrequency.Monthly) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDays by remember { mutableStateOf(currentFrequency.daysOfMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.monthly_frequency_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.monthly_frequency_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items((1..31).toList()) { day ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(day.toString()) },
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onFrequencySelected(HabitFrequency.Monthly(selectedDays)) },
                enabled = selectedDays.isNotEmpty()
            ) {
                Text(stringResource(Res.string.frequency_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.frequency_dialog_cancel))
            }
        }
    )
}