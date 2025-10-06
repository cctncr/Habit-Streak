package org.example.habitstreak.domain.usecase.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.model.NotificationPeriod
import org.example.habitstreak.domain.service.NotificationService

/**
 * Unified use case for all habit-level notification operations
 * Consolidates: ManageHabitNotificationUseCase + UpdateNotificationPeriodUseCase
 */
class HabitNotificationUseCase(
    private val notificationService: NotificationService
) {
    /**
     * Enable notification for a habit
     */
    suspend fun enable(habitId: String, time: LocalTime): NotificationOperationResult {
        return notificationService.enableNotification(habitId, time).toOperationResult()
    }

    /**
     * Disable notification for a habit
     */
    suspend fun disable(habitId: String): NotificationOperationResult {
        return notificationService.disableNotification(habitId).toOperationResult()
    }

    /**
     * Update notification period
     */
    suspend fun updatePeriod(habitId: String, period: NotificationPeriod): NotificationOperationResult {
        return notificationService.updateNotificationPeriod(habitId, period).toOperationResult()
    }

    /**
     * Update time and period atomically
     */
    suspend fun updateTimeAndPeriod(
        habitId: String,
        time: LocalTime,
        period: NotificationPeriod
    ): NotificationOperationResult {
        // First update period
        val periodResult = updatePeriod(habitId, period)
        if (periodResult is NotificationOperationResult.Error) {
            return periodResult
        }

        // Then update time (which triggers reschedule)
        return enable(habitId, time)
    }

    /**
     * Observe notification config for a habit
     */
    fun observe(habitId: String): Flow<NotificationConfig?> {
        return notificationService.observeNotificationConfig(habitId)
    }

    private fun Result<Unit>.toOperationResult(): NotificationOperationResult {
        return fold(
            onSuccess = { NotificationOperationResult.Success },
            onFailure = { error ->
                when (error) {
                    is org.example.habitstreak.domain.model.NotificationError ->
                        NotificationOperationResult.Error(error)
                    else ->
                        NotificationOperationResult.Error(
                            org.example.habitstreak.domain.model.NotificationError.GeneralError(error)
                        )
                }
            }
        )
    }
}
