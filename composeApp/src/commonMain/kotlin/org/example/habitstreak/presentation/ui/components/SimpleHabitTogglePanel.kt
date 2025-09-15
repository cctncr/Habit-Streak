package org.example.habitstreak.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Simplified toggle panel for simple check habits (targetCount = 1)
 * Compact design similar to HabitDetailScreen's approach
 */
@Composable
fun SimpleHabitTogglePanel(
    isCompleted: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status text
        Text(
            text = if (isCompleted) "Completed" else "Not completed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isCompleted) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Large toggle button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) accentColor else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { onToggle(!isCompleted) },
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            } else {
                // Empty circle for uncompleted state
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle instruction
        Text(
            text = "Tap to ${if (isCompleted) "unmark" else "mark"} as done",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mark as incomplete
            OutlinedButton(
                onClick = { onToggle(false) },
                modifier = Modifier.weight(1f),
                enabled = isCompleted,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Mark Incomplete")
            }

            // Mark as complete
            FilledTonalButton(
                onClick = { onToggle(true) },
                modifier = Modifier.weight(1f),
                enabled = !isCompleted,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accentColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Mark Complete")
            }
        }
    }
}