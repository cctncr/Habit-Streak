package org.example.habitstreak.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.domain.model.NotificationPeriod
import org.example.habitstreak.presentation.ui.components.common.NotificationTimeAndPeriodDialog
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

/**
 * Notification Settings Card Component (Compact Design)
 * Follows Single Responsibility Principle - only displays and manages notification settings UI
 * Uses dialog pattern for detailed settings
 */
@Composable
fun NotificationSettingsCard(
    isEnabled: Boolean,
    notificationTime: LocalTime?,
    notificationError: NotificationError?,
    notificationPeriod: NotificationPeriod,
    isGlobalNotificationEnabled: Boolean,
    habitFrequency: HabitFrequency? = null,
    onToggleEnabled: (Boolean) -> Unit,
    onTimeAndPeriodChanged: (LocalTime, NotificationPeriod) -> Unit,
    onEnableGlobalNotifications: () -> Unit,
    onErrorDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.reminder),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            // Global Notification Warning
            if (isEnabled && !isGlobalNotificationEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                GlobalNotificationWarning(
                    onEnableClick = onEnableGlobalNotifications
                )
            }

            // Settings (only shown when enabled) - COMPACT DESIGN
            AnimatedVisibility(visible = isEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Combined Settings Card (Clickable)
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSettingsDialog = true }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = notificationTime?.let { formatTime(it) }
                                            ?: stringResource(Res.string.select_time),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Days Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = formatPeriod(notificationPeriod),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Error handling
            notificationError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                NotificationErrorDisplay(
                    error = error,
                    onDismiss = onErrorDismiss,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }

    // Combined Time and Period Dialog
    if (showSettingsDialog) {
        NotificationTimeAndPeriodDialog(
            selectedTime = notificationTime,
            selectedPeriod = notificationPeriod,
            habitFrequency = habitFrequency,
            onConfirm = { time, period ->
                onTimeAndPeriodChanged(time, period)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun NotificationErrorDisplay(
    error: NotificationError,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = error.message ?: stringResource(Res.string.error_occurred),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            if (error.requiresUserAction()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.dismiss))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(Res.string.settings))
                    }
                }
            }
        }
    }
}

/**
 * Format time to display string (e.g., "09:30 AM")
 */
private fun formatTime(time: LocalTime): String {
    val hour = time.hour
    val minute = time.minute
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val hourStr = if (displayHour < 10) "0$displayHour" else "$displayHour"
    val minuteStr = if (minute < 10) "0$minute" else "$minute"
    return "$hourStr:$minuteStr $amPm"
}

/**
 * Format notification period to display string
 */
@Composable
private fun formatPeriod(period: NotificationPeriod): String {
    return when (period) {
        is NotificationPeriod.EveryDay -> stringResource(Res.string.every_day)
        is NotificationPeriod.ActiveDaysOnly -> stringResource(Res.string.active_days_only)
        is NotificationPeriod.SelectedDays -> {
            if (period.daysOfWeek.isEmpty()) {
                stringResource(Res.string.select_days)
            } else {
                // Use displayName from DayOfWeek enum
                period.daysOfWeek
                    .sortedBy { it.ordinal }
                    .joinToString(", ") { it.displayName }
            }
        }
    }
}
