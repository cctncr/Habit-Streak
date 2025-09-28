package org.example.habitstreak.presentation.permission

/**
 * Message types for different permission flow states
 * Following the principle of explicit state representation
 */
enum class PermissionMessageType {
    RATIONALE,      // Why permission is needed
    BENEFIT,        // What user gains
    DENIED_SOFT,    // Permission denied but can retry
    DENIED_HARD,    // Must go to settings
    SUCCESS         // Permission granted confirmation
}

/**
 * Sealed class for permission messages with context awareness
 * Enables type-safe message handling and internationalization support
 */
sealed class PermissionMessage {
    abstract val key: String
    abstract val defaultText: String

    // Settings context messages
    data class SettingsRationale(
        override val key: String = "permission_settings_rationale",
        override val defaultText: String = "Enable notifications to receive daily reminders for all your habits"
    ) : PermissionMessage()

    data class SettingsBenefit(
        override val key: String = "permission_settings_benefit",
        override val defaultText: String = "Stay consistent with timely reminders • Track your progress automatically • Never miss a habit again"
    ) : PermissionMessage()

    data class SettingsDeniedSoft(
        override val key: String = "permission_settings_denied_soft",
        override val defaultText: String = "Notifications help you stay on track with all your habits. You can try enabling them again."
    ) : PermissionMessage()

    data class SettingsDeniedHard(
        override val key: String = "permission_settings_denied_hard",
        override val defaultText: String = "To enable notifications for your habits, please go to Settings > Notifications and allow notifications for Habit Streak."
    ) : PermissionMessage()

    data class SettingsSuccess(
        override val key: String = "permission_settings_success",
        override val defaultText: String = "Great! Notifications are now enabled for all your habits."
    ) : PermissionMessage()

    // Habit detail context messages
    data class HabitDetailRationale(
        override val key: String = "permission_habit_detail_rationale",
        override val defaultText: String = "Get timely reminders for '%s' to maintain your streak"
    ) : PermissionMessage()

    data class HabitDetailBenefit(
        override val key: String = "permission_habit_detail_benefit",
        override val defaultText: String = "Never break your streak • Build consistency • Achieve your goals faster"
    ) : PermissionMessage()

    data class HabitDetailDeniedSoft(
        override val key: String = "permission_habit_detail_denied_soft",
        override val defaultText: String = "Reminders help you maintain your '%s' streak. You can enable them later in settings."
    ) : PermissionMessage()

    data class HabitDetailDeniedHard(
        override val key: String = "permission_habit_detail_denied_hard",
        override val defaultText: String = "To get reminders for '%s', please enable notifications in your device settings."
    ) : PermissionMessage()

    data class HabitDetailSuccess(
        override val key: String = "permission_habit_detail_success",
        override val defaultText: String = "Perfect! You'll now get reminders for '%s'."
    ) : PermissionMessage()

    // Create/Edit habit context messages
    data class CreateEditRationale(
        override val key: String = "permission_create_edit_rationale",
        override val defaultText: String = "Set up smart reminders to never miss your new habit"
    ) : PermissionMessage()

    data class CreateEditBenefit(
        override val key: String = "permission_create_edit_benefit",
        override val defaultText: String = "Start strong • Build the habit from day one • Increase success rate by 300%"
    ) : PermissionMessage()

    data class CreateEditDeniedSoft(
        override val key: String = "permission_create_edit_denied_soft",
        override val defaultText: String = "Notifications significantly improve habit formation success. You can set them up later."
    ) : PermissionMessage()

    data class CreateEditDeniedHard(
        override val key: String = "permission_create_edit_denied_hard",
        override val defaultText: String = "To set up reminders for your new habit, please enable notifications in device settings."
    ) : PermissionMessage()

    data class CreateEditSuccess(
        override val key: String = "permission_create_edit_success",
        override val defaultText: String = "Excellent! Your new habit is set up with smart reminders."
    ) : PermissionMessage()
}

/**
 * Service for generating context-aware permission messages
 * Following Single Responsibility Principle - only handles message generation
 */
class PermissionMessagingService {

    /**
     * Get appropriate message for given context and message type
     * @param context The screen/feature context
     * @param messageType The type of message needed
     * @param habitName Optional habit name for personalization
     * @return Formatted message string
     */
    fun getMessage(
        context: PermissionContext,
        messageType: PermissionMessageType,
        habitName: String? = null
    ): String {
        val message = getMessageForContext(context, messageType)

        return if (habitName != null && message.defaultText.contains("%s")) {
            message.defaultText.replace("%s", habitName)
        } else {
            message.defaultText
        }
    }

    /**
     * Get message object for potential localization
     * @param context The screen/feature context
     * @param messageType The type of message needed
     * @return PermissionMessage object with key and default text
     */
    fun getMessageObject(
        context: PermissionContext,
        messageType: PermissionMessageType
    ): PermissionMessage {
        return getMessageForContext(context, messageType)
    }

    /**
     * Get all benefit points as a list for UI display
     * @param context The screen/feature context
     * @return List of benefit strings
     */
    fun getBenefitPoints(context: PermissionContext): List<String> {
        val benefitMessage = getMessage(context, PermissionMessageType.BENEFIT)
        return benefitMessage.split(" • ")
    }

    private fun getMessageForContext(
        context: PermissionContext,
        messageType: PermissionMessageType
    ): PermissionMessage {
        return when (context) {
            PermissionContext.SETTINGS -> when (messageType) {
                PermissionMessageType.RATIONALE -> PermissionMessage.SettingsRationale()
                PermissionMessageType.BENEFIT -> PermissionMessage.SettingsBenefit()
                PermissionMessageType.DENIED_SOFT -> PermissionMessage.SettingsDeniedSoft()
                PermissionMessageType.DENIED_HARD -> PermissionMessage.SettingsDeniedHard()
                PermissionMessageType.SUCCESS -> PermissionMessage.SettingsSuccess()
            }

            PermissionContext.HABIT_DETAIL -> when (messageType) {
                PermissionMessageType.RATIONALE -> PermissionMessage.HabitDetailRationale()
                PermissionMessageType.BENEFIT -> PermissionMessage.HabitDetailBenefit()
                PermissionMessageType.DENIED_SOFT -> PermissionMessage.HabitDetailDeniedSoft()
                PermissionMessageType.DENIED_HARD -> PermissionMessage.HabitDetailDeniedHard()
                PermissionMessageType.SUCCESS -> PermissionMessage.HabitDetailSuccess()
            }

            PermissionContext.CREATE_EDIT -> when (messageType) {
                PermissionMessageType.RATIONALE -> PermissionMessage.CreateEditRationale()
                PermissionMessageType.BENEFIT -> PermissionMessage.CreateEditBenefit()
                PermissionMessageType.DENIED_SOFT -> PermissionMessage.CreateEditDeniedSoft()
                PermissionMessageType.DENIED_HARD -> PermissionMessage.CreateEditDeniedHard()
                PermissionMessageType.SUCCESS -> PermissionMessage.CreateEditSuccess()
            }
        }
    }
}