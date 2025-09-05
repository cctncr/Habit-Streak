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
    currentNote: String? = null, // Not için mevcut değer
    onDismiss: () -> Unit,
    onConfirm: (value: Int, note: String) -> Unit // Not parametresi eklendi
) {
    if (!show) return

    var text by remember(currentValue) {
        mutableStateOf(currentValue?.toString() ?: "")
    }
    var note by remember(currentNote) {
        mutableStateOf(currentNote ?: "")
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
                    label = { Text("Progress") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = {
                        error?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Not girme alanı
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = {
                        Text("Add a note (optional)")
                    },
                    label = { Text("Note") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick buttons for common values
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { text = "0" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("0", fontSize = 12.sp)
                    }

                    if (targetCount > 1) {
                        FilledTonalButton(
                            onClick = { text = (targetCount / 2).toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${targetCount / 2}", fontSize = 12.sp)
                        }
                    }

                    FilledTonalButton(
                        onClick = { text = targetCount.toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("$targetCount", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = text.toIntOrNull() ?: 0
                    if (value > targetCount) {
                        error = "Value cannot exceed $targetCount"
                    } else {
                        focusManager.clearFocus()
                        onConfirm(value, note)
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
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