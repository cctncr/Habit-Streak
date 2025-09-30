package org.example.habitstreak.domain.usecase.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.service.NotificationPermissionService
import org.example.habitstreak.domain.service.PermissionResult

/**
 * Use case for managing habit notifications.
 */
class ManageHabitNotificationUseCase(
    private val notificationService: NotificationService,
    private val permissionService: NotificationPermissionService
) {

    sealed class NotificationResult {
        data object Success : NotificationResult()
        data class Error(val error: NotificationError) : NotificationResult()
        data object PermissionRequired : NotificationResult()
    }

    suspend fun enableNotification(habitId: String, time: LocalTime): NotificationResult {
        println("🔔 MANAGE_HABIT_NOTIFICATION_USECASE: enableNotification called for habitId=$habitId, time=$time")
        println("🔔 MANAGE_HABIT_NOTIFICATION_USECASE: Checking permission status...")
        return when (val permissionStatus = permissionService.checkPermissionStatus()) {
            is PermissionResult.Granted -> {
                println("🔔 MANAGE_HABIT_NOTIFICATION_USECASE: Permission granted, enabling notification")
                notificationService.enableNotification(habitId, time).fold(
                    onSuccess = {
                        println("🔔 MANAGE_HABIT_NOTIFICATION_USECASE: Notification service returned success")
                        NotificationResult.Success
                    },
                    onFailure = { error ->
                        when (error) {
                            is NotificationError -> NotificationResult.Error(error)
                            else -> NotificationResult.Error(NotificationError.GeneralError(error))
                        }
                    }
                )
            }
            is PermissionResult.DeniedCanAskAgain -> {
                println("🔔 MANAGE_HABIT_NOTIFICATION_USECASE: Permission denied but can ask again")
                NotificationResult.Error(NotificationError.PermissionDenied(canRequestAgain = true))
            }
            is PermissionResult.DeniedPermanently -> {
                println("🔔 MANAGE_HABIT_NOTIFICATION_USECASE: Permission permanently denied")
                NotificationResult.Error(NotificationError.PermissionDenied(canRequestAgain = false))
            }
            is PermissionResult.GloballyDisabled -> {
                println("🔔 MANAGE_HABIT_NOTIFICATION_USECASE: Notifications globally disabled")
                NotificationResult.Error(NotificationError.GloballyDisabled("Notifications are disabled in device settings"))
            }
            is PermissionResult.Error -> {
                println("🔔 MANAGE_HABIT_NOTIFICATION_USECASE: Permission check error: ${permissionStatus.error}")
                NotificationResult.Error(permissionStatus.error)
            }
        }
    }

    suspend fun disableNotification(habitId: String): NotificationResult {
        return notificationService.disableNotification(habitId).fold(
            onSuccess = { NotificationResult.Success },
            onFailure = { error ->
                when (error) {
                    is NotificationError -> NotificationResult.Error(error)
                    else -> NotificationResult.Error(NotificationError.GeneralError(error))
                }
            }
        )
    }

    /**
     * Request notification permission
     */
    suspend fun requestPermission(): PermissionResult {
        return permissionService.requestPermission()
    }

    /**
     * Check if permission is granted
     */
    suspend fun hasPermission(): Boolean {
        return permissionService.hasPermission()
    }

    /**
     * Open app settings for permission management
     */
    suspend fun openAppSettings(): Boolean {
        return permissionService.openAppSettings()
    }

    fun observeNotificationConfig(habitId: String): Flow<NotificationConfig?> {
        return notificationService.observeNotificationConfig(habitId)
    }
}