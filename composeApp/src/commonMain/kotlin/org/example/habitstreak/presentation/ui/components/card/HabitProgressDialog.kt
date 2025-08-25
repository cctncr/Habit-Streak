package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HabitProgressDialog(
    show: Boolean,
    habitTitle: String,
    targetCount: Int,
    unit: String,
    currentValue: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    if (!show) return

    var text by remember(currentValue) {
        mutableStateOf(currentValue?.toString() ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(text = habitTitle)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Target: $targetCount ${unit.ifEmpty { "times" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            text = input
                            error = null
                        }
                    },
                    placeholder = {
                        Text("Enter progress (0-$targetCount)")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )

                // Quick select buttons for common values
                if (targetCount > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            targetCount / 4,
                            targetCount / 2,
                            targetCount * 3 / 4,
                            targetCount
                        ).distinct().forEach { value ->
                            FilledTonalButton(
                                onClick = { text = value.toString() },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = value.toString(),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = text.toIntOrNull()
                    when {
                        parsed == null -> error = "Please enter a valid number"
                        parsed < 0 -> error = "Value cannot be negative"
                        parsed > targetCount -> error = "Value exceeds target"
                        else -> {
                            focusManager.clearFocus()
                            onConfirm(parsed)
                            onDismiss()
                        }
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}