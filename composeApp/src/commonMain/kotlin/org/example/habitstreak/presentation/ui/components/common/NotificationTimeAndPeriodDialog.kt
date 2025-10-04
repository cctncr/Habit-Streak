package org.example.habitstreak.presentation.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.NotificationPeriod
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

/**
 * Combined Dialog for Time and Period Selection
 * Prevents race condition by handling both selections together
 * Follows Single Responsibility Principle - combines time and period selection
 */
@Composable
fun NotificationTimeAndPeriodDialog(
    selectedTime: LocalTime?,
    selectedPeriod: NotificationPeriod,
    habitFrequency: HabitFrequency?,
    onConfirm: (time: LocalTime, period: NotificationPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTime by remember {
        mutableStateOf(selectedTime ?: LocalTime(9, 0))
    }
    var currentPeriod by remember(selectedPeriod) {
        mutableStateOf(selectedPeriod)
    }
    var showValidationError by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

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
            Text("Set Reminder")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // Tab Row for Time and Days
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Time") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Days") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on selected tab
                when (selectedTab) {
                    0 -> {
                        // Time Selection Content
                        TimeSelectionContent(
                            currentTime = currentTime,
                            onTimeChanged = { currentTime = it }
                        )
                    }
                    1 -> {
                        // Period Selection Content
                        PeriodSelectionContent(
                            currentPeriod = currentPeriod,
                            habitFrequency = habitFrequency,
                            showValidationError = showValidationError,
                            onPeriodChanged = { period ->
                                currentPeriod = period
                                showValidationError = false
                            },
                            onValidationError = { showValidationError = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate if Selected Days is chosen
                    if (currentPeriod is NotificationPeriod.SelectedDays) {
                        val days = (currentPeriod as NotificationPeriod.SelectedDays).daysOfWeek
                        if (days.isEmpty()) {
                            showValidationError = true
                            selectedTab = 1 // Switch to days tab to show error
                            return@TextButton
                        }
                    }
                    onConfirm(currentTime, currentPeriod)
                    onDismiss()
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
private fun TimeSelectionContent(
    currentTime: LocalTime,
    onTimeChanged: (LocalTime) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose when you want to be reminded:",
            style = MaterialTheme.typography.bodyMedium
        )

        // Infinite Wheel Time Picker
        InfiniteWheelTimePicker(
            selectedTime = currentTime,
            onTimeChanged = onTimeChanged,
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
}

@Composable
private fun PeriodSelectionContent(
    currentPeriod: NotificationPeriod,
    habitFrequency: HabitFrequency?,
    showValidationError: Boolean,
    onPeriodChanged: (NotificationPeriod) -> Unit,
    onValidationError: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Every Day Option
        RadioOptionRow(
            selected = currentPeriod is NotificationPeriod.EveryDay,
            onClick = {
                onPeriodChanged(NotificationPeriod.EveryDay)
            },
            title = stringResource(Res.string.every_day),
            description = stringResource(Res.string.every_day_description)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Active Days Only Option
        RadioOptionRow(
            selected = currentPeriod is NotificationPeriod.ActiveDaysOnly,
            onClick = {
                onPeriodChanged(NotificationPeriod.ActiveDaysOnly)
            },
            title = stringResource(Res.string.active_days_only),
            description = stringResource(Res.string.active_days_description),
            showInfoIcon = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Select Days Option
        RadioOptionRow(
            selected = currentPeriod is NotificationPeriod.SelectedDays,
            onClick = {
                onPeriodChanged(NotificationPeriod.SelectedDays(emptySet()))
            },
            title = stringResource(Res.string.select_specific_days),
            description = stringResource(Res.string.select_days_description)
        )

        // Day Selection Chips (only visible when Select Days is chosen)
        AnimatedVisibility(visible = currentPeriod is NotificationPeriod.SelectedDays) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))

                DaySelectionChips(
                    selectedDays = (currentPeriod as? NotificationPeriod.SelectedDays)?.daysOfWeek
                        ?: emptySet(),
                    onDaysChanged = { days ->
                        onPeriodChanged(NotificationPeriod.SelectedDays(days))
                        onValidationError(days.isEmpty())
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

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val minute = time.minute.toString().padStart(2, '0')
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "$hour:$minute $amPm"
}
