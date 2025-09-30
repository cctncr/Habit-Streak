package org.example.habitstreak.domain.model

import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDateTime

/**
 * Enhanced notification configuration following ISP and SRP
 * More specific interfaces can be extracted if needed
 */
data class NotificationConfig(
    val id: String? = null,
    val habitId: String,
    val time: LocalTime,
    val isEnabled: Boolean = true,
    val message: String = "Time to complete your habit!",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val priority: NotificationPriority = NotificationPriority.DEFAULT,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

/**
 * Notification priority levels
 */
enum class NotificationPriority {
    LOW,
    DEFAULT,
    HIGH,
    URGENT
}

/**
 * Basic notification interface (ISP compliance)
 */
interface BasicNotification {
    val habitId: String
    val time: LocalTime
    val isEnabled: Boolean
}

/**
 * Extended notification interface for advanced features
 */
interface AdvancedNotification : BasicNotification {
    val message: String
    val soundEnabled: Boolean
    val vibrationEnabled: Boolean
    val priority: NotificationPriority
}

/**
 * Extension function to convert config to basic interface
 */
fun NotificationConfig.toBasic(): BasicNotification = object : BasicNotification {
    override val habitId: String = this@toBasic.habitId
    override val time: LocalTime = this@toBasic.time
    override val isEnabled: Boolean = this@toBasic.isEnabled
}