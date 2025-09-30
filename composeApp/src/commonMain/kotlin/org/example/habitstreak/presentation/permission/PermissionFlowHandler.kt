package org.example.habitstreak.presentation.permission

import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.domain.service.PermissionResult
import org.example.habitstreak.data.cache.PermissionStateCache

/**
 * Unified permission flow orchestrator following Single Responsibility Principle
 * Handles permission flow logic across all screens while maintaining DIP compliance
 */
class PermissionFlowHandler(
    private val permissionManager: PermissionManager,
    private val stateCache: PermissionStateCache
) {

    /**
     * Main entry point for permission requests with unified flow
     * @param habitName Optional habit name for personalized messages
     * @param onResult Callback with permission granted status
     */
    suspend fun requestPermissionWithFlow(
        habitName: String? = null,
        onResult: (PermissionFlowResult) -> Unit
    ) {
        println("🔔 PERMISSION_FLOW_HANDLER: requestPermissionWithFlow called with habitName=$habitName")
        // Check cache first to avoid repeated permission checks
        val cachedResult = stateCache.getCachedPermissionState()
        println("🔔 PERMISSION_FLOW_HANDLER: Cached permission state: $cachedResult")
        if (cachedResult == true) {
            println("🔔 PERMISSION_FLOW_HANDLER: Permission cached as granted, returning Granted")
            onResult(PermissionFlowResult.Granted)
            return
        }
        // If cached as false or null, continue with flow

        try {
            // Step 1: Check if permission already granted
            println("🔔 PERMISSION_FLOW_HANDLER: Checking if permission already granted...")
            if (permissionManager.hasNotificationPermission()) {
                println("🔔 PERMISSION_FLOW_HANDLER: Permission already granted, caching and returning Granted")
                stateCache.cachePermissionState(true)
                onResult(PermissionFlowResult.Granted)
                return
            }

            // Step 2: Check if we can request permission
            if (!permissionManager.canRequestPermission()) {
                println("🔔 PERMISSION_FLOW_HANDLER: Cannot request permission, showing settings dialog")
                onResult(
                    PermissionFlowResult.ShowSettingsDialog(
                        message = "To enable notifications, please allow permissions in Settings"
                    )
                )
                return
            }

            // Step 3: Show rationale dialog
            onResult(
                PermissionFlowResult.ShowRationaleDialog(
                    rationaleMessage = "Enable notifications to receive daily reminders",
                    benefitMessage = "Stay consistent with timely reminders • Track your progress automatically • Never miss a habit again"
                )
            )

        } catch (e: Exception) {
            onResult(
                PermissionFlowResult.Error(
                    message = "Failed to check permission status: ${e.message}"
                )
            )
        }
    }

    /**
     * Handle system permission request result
     */
    suspend fun handleSystemPermissionResult(
        habitName: String? = null,
        onResult: (PermissionFlowResult) -> Unit
    ) {
        try {
            when (val result = permissionManager.requestNotificationPermission()) {
                is PermissionResult.Granted -> {
                    stateCache.cachePermissionState(true)
                    onResult(
                        PermissionFlowResult.PermissionGranted(
                            message = "Notifications enabled successfully"
                        )
                    )
                }

                is PermissionResult.DeniedCanAskAgain -> {
                    onResult(
                        PermissionFlowResult.ShowSoftDenialDialog(
                            message = "Notifications help you stay on track. You can try enabling them again."
                        )
                    )
                }

                is PermissionResult.DeniedPermanently -> {
                    onResult(
                        PermissionFlowResult.ShowSettingsDialog(
                            message = "To enable notifications, please allow permissions in Settings"
                        )
                    )
                }

                is PermissionResult.GloballyDisabled -> {
                    onResult(
                        PermissionFlowResult.ShowSettingsDialog(
                            message = "To enable notifications, please allow permissions in Settings"
                        )
                    )
                }

                is PermissionResult.Error -> {
                    onResult(
                        PermissionFlowResult.Error(
                            message = "Permission request failed: ${result.error}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            onResult(
                PermissionFlowResult.Error(
                    message = "Failed to request permission: ${e.message}"
                )
            )
        }
    }

    /**
     * Handle user's choice to open app settings
     */
    suspend fun handleOpenSettings(): Boolean {
        return try {
            val result = permissionManager.openAppSettings()
            if (result) {
                // Reset cache since user is going to change settings
                stateCache.cachePermissionState(false)
            }
            result
        } catch (e: Exception) {
            println("Error opening app settings: ${e.message}")
            false
        }
    }

    /**
     * Handle user's choice to never ask again
     */
    fun handleNeverAskAgain() {
        // User selected never ask again - no action needed
    }

    /**
     * Check current permission status with caching
     */
    suspend fun hasPermission(): Boolean {
        return stateCache.getCachedPermissionState() ?: run {
            val hasPermission = permissionManager.hasNotificationPermission()
            stateCache.cachePermissionState(hasPermission)
            hasPermission
        }
    }

    /**
     * Clear permission cache (call when app comes to foreground)
     */
    fun invalidateCache() {
        stateCache.invalidateCache()
    }
}

/**
 * Result types for permission flow operations
 * Following the principle of explicit state management
 */
sealed class PermissionFlowResult {
    data object Granted : PermissionFlowResult()

    data class PermissionGranted(val message: String) : PermissionFlowResult()

    data class ShowRationaleDialog(
        val rationaleMessage: String,
        val benefitMessage: String
    ) : PermissionFlowResult()

    data class ShowSoftDenialDialog(
        val message: String
    ) : PermissionFlowResult()

    data class ShowSettingsDialog(
        val message: String
    ) : PermissionFlowResult()

    data class Error(val message: String) : PermissionFlowResult()
}