package org.example.habitstreak.presentation.ui.components.notification

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

/**
 * Reusable component for notification sound and vibration preferences
 * Following Single Responsibility Principle - only renders preference UI
 */
@Composable
fun NotificationPreferencesSection(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.notification_preferences),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PreferenceSwitch(
            icon = Icons.Outlined.MusicNote,
            title = stringResource(Res.string.setting_sound),
            subtitle = stringResource(Res.string.setting_sound_desc),
            checked = soundEnabled,
            onCheckedChange = onSoundChanged
        )

        PreferenceSwitch(
            icon = Icons.Outlined.Vibration,
            title = stringResource(Res.string.setting_vibration),
            subtitle = stringResource(Res.string.setting_vibration_desc),
            checked = vibrationEnabled,
            onCheckedChange = onVibrationChanged
        )
    }
}

@Composable
private fun PreferenceSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
