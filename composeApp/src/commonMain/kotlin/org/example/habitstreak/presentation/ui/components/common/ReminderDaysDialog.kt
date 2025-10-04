package org.example.habitstreak.presentation.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.NotificationPeriod
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

/**
 * Reminder Days Dialog Component
 * Follows Single Responsibility Principle - only handles day selection UI for notifications
 */
@Composable
fun ReminderDaysDialog(
    selectedPeriod: NotificationPeriod,
    habitFrequency: HabitFrequency?,
    onPeriodSelected: (NotificationPeriod) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for temporary selection before confirmation
    var tempPeriod by remember(selectedPeriod) { mutableStateOf(selectedPeriod) }
    var showValidationError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.select_reminder_days))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Every Day Option
                RadioOptionRow(
                    selected = tempPeriod is NotificationPeriod.EveryDay,
                    onClick = {
                        tempPeriod = NotificationPeriod.EveryDay
                        showValidationError = false
                    },
                    title = stringResource(Res.string.every_day),
                    description = stringResource(Res.string.every_day_description)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Active Days Only Option
                RadioOptionRow(
                    selected = tempPeriod is NotificationPeriod.ActiveDaysOnly,
                    onClick = {
                        tempPeriod = NotificationPeriod.ActiveDaysOnly
                        showValidationError = false
                    },
                    title = stringResource(Res.string.active_days_only),
                    description = stringResource(Res.string.active_days_description),
                    showInfoIcon = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Select Days Option
                RadioOptionRow(
                    selected = tempPeriod is NotificationPeriod.SelectedDays,
                    onClick = {
                        tempPeriod = NotificationPeriod.SelectedDays(emptySet())
                        showValidationError = false
                    },
                    title = stringResource(Res.string.select_specific_days),
                    description = stringResource(Res.string.select_days_description)
                )

                // Day Selection Chips (only visible when Select Days is chosen)
                AnimatedVisibility(visible = tempPeriod is NotificationPeriod.SelectedDays) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        DaySelectionChips(
                            selectedDays = (tempPeriod as? NotificationPeriod.SelectedDays)?.daysOfWeek
                                ?: emptySet(),
                            onDaysChanged = { days ->
                                tempPeriod = NotificationPeriod.SelectedDays(days)
                                showValidationError = days.isEmpty()
                            }
                        )

                        // Validation Error
                        if (showValidationError) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.at_least_one_day),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate if Select Days is chosen
                    if (tempPeriod is NotificationPeriod.SelectedDays) {
                        val days = (tempPeriod as NotificationPeriod.SelectedDays).daysOfWeek
                        if (days.isEmpty()) {
                            showValidationError = true
                            return@TextButton
                        }
                    }
                    onPeriodSelected(tempPeriod)
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun RadioOptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String,
    showInfoIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // Click handled by Row
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (showInfoIcon) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DaySelectionChips(
    selectedDays: Set<DayOfWeek>,
    onDaysChanged: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = remember {
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val isSelected = selectedDays.contains(day)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newDays = if (isSelected) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                    onDaysChanged(newDays)
                },
                label = {
                    Text(getDayName(day))
                }
            )
        }
    }
}

@Composable
private fun getDayName(day: DayOfWeek): String {
    return when (day) {
        DayOfWeek.MONDAY -> stringResource(Res.string.monday)
        DayOfWeek.TUESDAY -> stringResource(Res.string.tuesday)
        DayOfWeek.WEDNESDAY -> stringResource(Res.string.wednesday)
        DayOfWeek.THURSDAY -> stringResource(Res.string.thursday)
        DayOfWeek.FRIDAY -> stringResource(Res.string.friday)
        DayOfWeek.SATURDAY -> stringResource(Res.string.saturday)
        DayOfWeek.SUNDAY -> stringResource(Res.string.sunday)
    }
}
