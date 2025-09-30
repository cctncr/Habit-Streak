package org.example.habitstreak.presentation.ui.components.permission

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

/**
 * Unified dialog for enabling global notifications
 * Used across multiple screens to avoid duplication
 */
@Composable
fun GlobalEnableNotificationDialog(
    habitName: String?,
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
            val message = if (habitName != null) {
                stringResource(Res.string.permission_habit_detail_rationale, habitName)
            } else {
                stringResource(Res.string.permission_settings_rationale)
            }
            Text(message)
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