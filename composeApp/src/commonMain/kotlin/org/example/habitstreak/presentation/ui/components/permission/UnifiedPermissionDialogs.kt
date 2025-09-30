package org.example.habitstreak.presentation.ui.components.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.example.habitstreak.presentation.permission.BasePermissionHandler
import org.example.habitstreak.presentation.permission.PermissionFlowResult

/**
 * Unified permission dialog management component
 * Eliminates duplicate dialog handling code across screens
 */
@Composable
fun UnifiedPermissionDialogs(
    dialogState: PermissionFlowResult?,
    permissionHandler: BasePermissionHandler,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    when (dialogState) {
        is PermissionFlowResult.ShowRationaleDialog -> {
            PermissionRationaleDialog(
                rationaleMessage = dialogState.rationaleMessage,
                benefitMessage = dialogState.benefitMessage,
                habitName = null, // Handler has habitName internally
                onRequestPermission = {
                    permissionHandler.launchPlatformPermissionRequest()
                },
                onDismiss = onDismiss,
                onNeverAskAgain = {
                    permissionHandler.handleNeverAskAgain()
                    onDismiss()
                }
            )
        }

        is PermissionFlowResult.ShowSettingsDialog -> {
            SettingsNavigationDialog(
                message = dialogState.message,
                onOpenSettings = {
                    coroutineScope.launch {
                        permissionHandler.openSettings()
                        onDismiss()
                    }
                },
                onDismiss = onDismiss,
                onDisableFeature = {
                    // User chooses to disable notifications feature
                    onDismiss()
                }
            )
        }

        is PermissionFlowResult.ShowSoftDenialDialog -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text("Permission Needed")
                },
                text = {
                    Text(dialogState.message)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDismiss()
                            permissionHandler.launchPlatformPermissionRequest()
                        }
                    ) {
                        Text("Try Again")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Not Now")
                    }
                }
            )
        }

        else -> {
            // Handle success states
            if (dialogState is PermissionFlowResult.PermissionGranted ||
                dialogState is PermissionFlowResult.Granted) {
                // Auto dismiss on success
                onDismiss()
            }
        }
    }
}

/**
 * Extension function to check if permission flow result represents success
 */
fun PermissionFlowResult?.isGranted(): Boolean {
    return this is PermissionFlowResult.PermissionGranted ||
           this is PermissionFlowResult.Granted
}

/**
 * Extension function to check if permission flow result requires dialog
 */
fun PermissionFlowResult?.requiresDialog(): Boolean {
    return this is PermissionFlowResult.ShowRationaleDialog ||
           this is PermissionFlowResult.ShowSettingsDialog ||
           this is PermissionFlowResult.ShowSoftDenialDialog
}