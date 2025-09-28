package org.example.habitstreak.presentation.permission

import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.domain.service.PermissionResult
import org.example.habitstreak.data.cache.PermissionStateCache

/**
 * Context for permission requests - determines which screen triggered the request
 * This enables context-aware messaging and analytics tracking
 */
enum class PermissionContext {
    SETTINGS,
    HABIT_DETAIL,
    CREATE_EDIT
}

/**
 * Unified permission flow orchestrator following Single Responsibility Principle
 * Handles permission flow logic across all screens while maintaining DIP compliance
 */
class PermissionFlowHandler(
    private val permissionManager: PermissionManager,
    private val messagingService: PermissionMessagingService,
    private val stateCache: PermissionStateCache
) {

    /**
     * Main entry point for permission requests with context-aware flow
     * @param context The screen/feature context requesting permission
     * @param habitName Optional habit name for personalized messages
     * @param onResult Callback with permission granted status
     */
    suspend fun requestPermissionWithFlow(
        context: PermissionContext,
        habitName: String? = null,
        onResult: (PermissionFlowResult) -> Unit
    ) {
        // Check cache first to avoid repeated permission checks
        val cachedResult = stateCache.getCachedPermissionState()
        if (cachedResult != null) {
            onResult(PermissionFlowResult.Granted)
            return
        }

        try {
            // Step 1: Check if permission already granted
            if (permissionManager.hasNotificationPermission()) {
                stateCache.cachePermissionState(true)
                onResult(PermissionFlowResult.Granted)
                return
            }

            // Step 2: Check if we can request permission
            if (!permissionManager.canRequestPermission()) {
                onResult(
                    PermissionFlowResult.ShowSettingsDialog(
                        message = messagingService.getMessage(
                            context = context,
                            messageType = PermissionMessageType.DENIED_HARD,
                            habitName = habitName
                        )
                    )
                )
                return
            }

            // Step 3: Show rationale dialog with context-aware message
            onResult(
                PermissionFlowResult.ShowRationaleDialog(
                    context = context,
                    rationaleMessage = messagingService.getMessage(
                        context = context,
                        messageType = PermissionMessageType.RATIONALE,
                        habitName = habitName
                    ),
                    benefitMessage = messagingService.getMessage(
                        context = context,
                        messageType = PermissionMessageType.BENEFIT,
                        habitName = habitName
                    )
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
        context: PermissionContext,
        habitName: String? = null,
        onResult: (PermissionFlowResult) -> Unit
    ) {
        try {
            when (val result = permissionManager.requestNotificationPermission()) {
                is PermissionResult.Granted -> {
                    stateCache.cachePermissionState(true)
                    onResult(
                        PermissionFlowResult.PermissionGranted(
                            message = messagingService.getMessage(
                                context = context,
                                messageType = PermissionMessageType.SUCCESS,
                                habitName = habitName
                            )
                        )
                    )
                }

                is PermissionResult.DeniedCanAskAgain -> {
                    onResult(
                        PermissionFlowResult.ShowSoftDenialDialog(
                            message = messagingService.getMessage(
                                context = context,
                                messageType = PermissionMessageType.DENIED_SOFT,
                                habitName = habitName
                            )
                        )
                    )
                }

                is PermissionResult.DeniedPermanently -> {
                    onResult(
                        PermissionFlowResult.ShowSettingsDialog(
                            message = messagingService.getMessage(
                                context = context,
                                messageType = PermissionMessageType.DENIED_HARD,
                                habitName = habitName
                            )
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
    suspend fun handleOpenSettings(context: PermissionContext): Boolean {
        return try {
            val result = permissionManager.openAppSettings()
            if (result) {
                // Cache that user went to settings (for analytics purposes if needed later)
                stateCache.cachePermissionState(false) // Reset cache since user is going to change settings
            }
            result
        } catch (e: Exception) {
            // Log the exception for debugging
            println("Error opening app settings: ${e.message}")
            false
        }
    }

    /**
     * Handle user's choice to never ask again
     */
    fun handleNeverAskAgain(context: PermissionContext) {
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
    object Granted : PermissionFlowResult()

    data class PermissionGranted(val message: String) : PermissionFlowResult()

    data class ShowRationaleDialog(
        val context: PermissionContext,
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