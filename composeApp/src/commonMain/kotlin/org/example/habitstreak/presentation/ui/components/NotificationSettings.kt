package org.example.habitstreak.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.example.habitstreak.presentation.ui.components.common.InfiniteWheelTimePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.NotificationError
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

@Composable
fun NotificationSettingsCard(
    isEnabled: Boolean,
    notificationTime: LocalTime?,
    onToggleEnabled: (Boolean) -> Unit,
    onTimeChanged: (LocalTime) -> Unit,
    notificationError: NotificationError? = null,
    onErrorDismiss: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var pendingError by remember { mutableStateOf<NotificationError?>(null) }

    LaunchedEffect(notificationError) {
        if (notificationError != null && notificationError != pendingError) {
            pendingError = notificationError
            showErrorDialog = true
        }
    }

    LaunchedEffect(notificationError) {
        if (notificationError == null && pendingError != null) {
            // Error cleared but keep dialog open until user dismisses
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            text = "Daily Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isEnabled && notificationTime != null) {
                                stringResource(Res.string.notification_time_at, formatTime(notificationTime))
                            } else {
                                stringResource(Res.string.get_notified_description)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled || notificationTime != null) {
                            onToggleEnabled(enabled)
                        } else {
                            showTimePicker = true
                        }
                    }
                )
            }

            AnimatedVisibility(visible = isEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = "Time",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Notification Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        AssistChip(
                            onClick = { showTimePicker = true },
                            label = {
                                Text(
                                    text = notificationTime?.let { formatTime(it) } ?: "Set Time"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // ✅ Type-safe error display
                    val displayError = notificationError ?: pendingError
                    if (displayError?.requiresUserAction() == true) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = getErrorDisplayMessage(displayError),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = notificationTime ?: LocalTime(9, 0),
            onTimeSelected = { time ->
                onTimeChanged(time)
                showTimePicker = false

                if (!isEnabled) {
                    onToggleEnabled(true)
                }
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // ✅ Type-safe error dialog with settings option
    if (showErrorDialog && pendingError != null) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                pendingError = null
                onErrorDismiss?.invoke()
            },
            title = { Text("Notification Issue") },
            text = {
                Text(getErrorDialogMessage(pendingError!!))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                        pendingError = null
                        onErrorDismiss?.invoke()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = if (pendingError!!.requiresUserAction() && onOpenSettings != null) {
                {
                    TextButton(
                        onClick = {
                            showErrorDialog = false
                            pendingError = null
                            onOpenSettings() // ✅ Open settings implementation
                            onErrorDismiss?.invoke()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings")
                        }
                    }
                }
            } else null
        )
    }
}

// ✅ Type-safe error message helpers
@Composable
private fun getErrorDisplayMessage(error: NotificationError): String = when (error) {
    is NotificationError.PermissionDenied ->
        if (error.canRequestAgain) stringResource(Res.string.permission_needed_tap_settings)
        else stringResource(Res.string.permission_enable_in_settings)
    is NotificationError.GloballyDisabled -> stringResource(Res.string.notification_error_enable_in_settings)
    else -> stringResource(Res.string.notification_error_check_settings)
}

@Composable
private fun getErrorDialogMessage(error: NotificationError): String = when (error) {
    is NotificationError.PermissionDenied ->
        if (error.canRequestAgain)
            stringResource(Res.string.permission_required_grant_in_settings)
        else
            stringResource(Res.string.notification_permission_permanently_denied)
    is NotificationError.GloballyDisabled ->
        stringResource(Res.string.notifications_disabled_in_app)
    is NotificationError.ServiceUnavailable ->
        stringResource(Res.string.notification_service_unavailable)
    is NotificationError.HabitNotFound ->
        stringResource(Res.string.habit_not_found_refresh)
    is NotificationError.SchedulingFailed ->
        stringResource(Res.string.failed_to_schedule_notification, error.reason)
    is NotificationError.GeneralError ->
        error.message
}

@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTime by remember {
        mutableStateOf(initialTime)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            InfiniteWheelTimePicker(
                selectedTime = currentTime,
                onTimeChanged = { newTime ->
                    currentTime = newTime
                },
                is24Hour = false,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(currentTime)
                }
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

private fun formatTime(time: LocalTime): String = buildString {
    val hour = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    append(hour)
    append(":")
    if (time.minute < 10) append("0")
    append(time.minute)
    append(" ")
    append(if (time.hour < 12) "AM" else "PM")
}