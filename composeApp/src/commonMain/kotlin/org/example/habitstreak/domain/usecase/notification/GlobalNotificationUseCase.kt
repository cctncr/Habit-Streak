package org.example.habitstreak.domain.usecase.notification

import kotlinx.coroutines.flow.first
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.service.PermissionManager
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.error_occurred
import org.example.habitstreak.domain.model.NotificationError
import org.jetbrains.compose.resources.getString
import kotlin.time.ExperimentalTime

/**
 * Unified use case for all global notification operations
 * Consolidates: Enable/Disable/CheckGlobalNotificationsUseCase
 */
class GlobalNotificationUseCase(
    private val permissionManager: PermissionManager,
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService,
    private val habitRepository: HabitRepository,
    private val notificationRepository: NotificationRepository,
    private val scheduler: NotificationScheduler
) {
    /**
     * Check global notification status
     */
    suspend fun checkStatus(): GlobalStatus {
        val hasPermission = permissionManager.hasNotificationPermission()
        if (!hasPermission) {
            return GlobalStatus.NeedsSystemPermission
        }

        val globalEnabled = preferencesRepository.isNotificationsEnabled().first()
        return if (globalEnabled) {
            GlobalStatus.AlreadyEnabled
        } else {
            GlobalStatus.NeedsGlobalEnable
        }
    }

    /**
     * Enable global notifications and re-enable all habit notifications
     */
    @OptIn(ExperimentalTime::class)
    suspend fun enable(): NotificationOperationResult {
        return try {
            preferencesRepository.setNotificationsEnabled(true)

            val configs = notificationRepository.getAllNotificationConfigs()
                .filter { it.isEnabled }

            if (configs.isEmpty()) {
                return NotificationOperationResult.Success
            }

            val successes = mutableListOf<String>()
            val failures = mutableListOf<HabitOperationFailure>()

            configs.forEach { config ->
                try {
                    val habit = habitRepository.getHabitById(config.habitId).getOrNull()
                    if (habit != null && !habit.isArchived) {
                        scheduler.scheduleNotification(
                            config,
                            habit.frequency,
                            habit.createdAt
                        ).fold(
                            onSuccess = { successes.add(config.habitId) },
                            onFailure = { error ->
                                failures.add(
                                    HabitOperationFailure(
                                        habitId = config.habitId,
                                        habitTitle = habit.title,
                                        errorType = FailureType.UNKNOWN,
                                        errorMessage = error.message ?: getString(Res.string.error_occurred),
                                        canRetry = true
                                    )
                                )
                            }
                        )
                    }
                } catch (e: Exception) {
                    // Get habit title for error message
                    val habitTitle = try {
                        habitRepository.getHabitById(config.habitId).getOrNull()?.title ?: "Unknown"
                    } catch (_: Exception) {
                        "Unknown"
                    }

                    failures.add(
                        HabitOperationFailure(
                            habitId = config.habitId,
                            habitTitle = habitTitle,
                            errorType = FailureType.UNKNOWN,
                            errorMessage = e.message ?: getString(Res.string.error_occurred),
                            canRetry = false
                        )
                    )
                }
            }

            when {
                failures.isEmpty() -> NotificationOperationResult.Success
                successes.isEmpty() -> NotificationOperationResult.Error(
                    NotificationError.GeneralError(
                        cause = Exception("All habits failed"),
                        message = getString(Res.string.error_occurred)
                    )
                )
                else -> NotificationOperationResult.PartialSuccess(
                    successCount = successes.size,
                    failureCount = failures.size,
                    failures = failures
                )
            }
        } catch (e: Exception) {
            val errorMsg = getString(Res.string.error_occurred)
            NotificationOperationResult.Error(
                NotificationError.GeneralError(e, errorMsg)
            )
        }
    }

    /**
     * Disable global notifications and cancel all
     */
    suspend fun disable(): NotificationOperationResult {
        return try {
            preferencesRepository.setNotificationsEnabled(false)
            notificationService.cancelAllNotifications()
            NotificationOperationResult.Success
        } catch (e: Exception) {
            val errorMsg = getString(Res.string.error_occurred)
            NotificationOperationResult.Error(
                NotificationError.GeneralError(e, errorMsg)
            )
        }
    }

    sealed class GlobalStatus {
        data object AlreadyEnabled : GlobalStatus()
        data object CanEnable : GlobalStatus()
        data object NeedsSystemPermission : GlobalStatus()
        data object NeedsGlobalEnable : GlobalStatus()
    }
}
