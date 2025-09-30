package org.example.habitstreak.presentation.ui.components.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import org.example.habitstreak.presentation.ui.components.notification.NotificationPreferencesSection

/**
 * Unified dialog for enabling global notifications
 * Used across multiple screens to avoid duplication
 * Now includes sound and vibration preferences
 */
@Composable
fun GlobalEnableNotificationDialog(
    habitName: String?,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(Res.string.permission_enable_notifications))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Message
                val message = if (habitName != null) {
                    stringResource(Res.string.permission_habit_detail_rationale, habitName)
                } else {
                    stringResource(Res.string.permission_settings_rationale)
                }
                Text(message)

                Divider()

                // Preferences Section
                NotificationPreferencesSection(
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    onSoundChanged = onSoundChanged,
                    onVibrationChanged = onVibrationChanged
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.permission_enable_notifications))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}