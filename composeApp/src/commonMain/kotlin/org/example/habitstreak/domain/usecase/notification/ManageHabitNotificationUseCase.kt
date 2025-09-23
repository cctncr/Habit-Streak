package org.example.habitstreak.domain.usecase.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.service.PermissionResult

/**
 * Use case for managing habit notifications.
 * Follows Single Responsibility Principle by handling only notification-related operations.
 */
class ManageHabitNotificationUseCase(
    private val notificationService: NotificationService?
) {

    sealed class NotificationResult {
        object Success : NotificationResult()
        data class Error(val error: NotificationError) : NotificationResult()
        object PermissionRequired : NotificationResult()
        object ServiceUnavailable : NotificationResult()
    }

    suspend fun enableNotification(habitId: String, time: LocalTime): NotificationResult {
        if (notificationService == null) {
            return NotificationResult.ServiceUnavailable
        }

        return when (val permissionStatus = notificationService.checkPermissionStatus()) {
            is PermissionResult.Granted -> {
                notificationService.enableNotification(habitId, time).fold(
                    onSuccess = { NotificationResult.Success },
                    onFailure = { error ->
                        when (error) {
                            is NotificationError -> NotificationResult.Error(error)
                            else -> NotificationResult.Error(NotificationError.GeneralError(error))
                        }
                    }
                )
            }
            is PermissionResult.DeniedCanAskAgain -> {
                NotificationResult.Error(NotificationError.PermissionDenied(canRequestAgain = true))
            }
            is PermissionResult.DeniedPermanently -> {
                NotificationResult.Error(NotificationError.PermissionDenied(canRequestAgain = false))
            }
            is PermissionResult.Error -> {
                NotificationResult.Error(permissionStatus.error)
            }
        }
    }

    suspend fun disableNotification(habitId: String): NotificationResult {
        if (notificationService == null) {
            return NotificationResult.ServiceUnavailable
        }

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

    fun observeNotificationConfig(habitId: String): Flow<NotificationConfig?> = flow {
        if (notificationService == null) {
            emit(null)
            return@flow
        }

        notificationService.observeNotificationConfig(habitId).collect { config ->
            emit(config)
        }
    }
}