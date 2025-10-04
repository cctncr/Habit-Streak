package org.example.habitstreak.domain.usecase.notification

import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.domain.model.NotificationPeriod
import org.example.habitstreak.domain.service.NotificationService

/**
 * Use case for updating notification period for a habit
 * Follows Single Responsibility Principle
 */
class UpdateNotificationPeriodUseCase(
    private val notificationService: NotificationService
) {

    suspend fun execute(habitId: String, period: NotificationPeriod): NotificationOperationResult {
        return notificationService.updateNotificationPeriod(habitId, period).fold(
            onSuccess = { NotificationOperationResult.Success },
            onFailure = { error ->
                when (error) {
                    is NotificationError -> NotificationOperationResult.Error(error)
                    else -> NotificationOperationResult.Error(NotificationError.GeneralError(error))
                }
            }
        )
    }
}
