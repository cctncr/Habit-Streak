package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.presentation.ui.model.ViewMode

/**
 * Factory for creating habit cards based on view mode
 * Following Factory Pattern and Open/Closed Principle
 */
object HabitCardFactory {

    @Composable
    fun CreateCard(
        viewMode: ViewMode,
        habit: Habit,
        completionHistory: Map<LocalDate, Float>,
        todayProgress: Float,
        currentStreak: Int,
        today: LocalDate,
        todayRecord: HabitRecord?,
        habitRecords: List<HabitRecord>,
        onUpdateProgress: (LocalDate, Int, String) -> Unit,
        onCardClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        when (viewMode) {
            is ViewMode.Large -> {
                HabitCardLarge(
                    habit = habit,
                    completionHistory = completionHistory,
                    todayProgress = todayProgress,
                    currentStreak = currentStreak,
                    today = today,
                    todayRecord = todayRecord,
                    habitRecords = habitRecords,
                    onUpdateProgress = onUpdateProgress,
                    onCardClick = onCardClick,
                    modifier = modifier
                )
            }
            is ViewMode.Medium -> {
                HabitCardMedium(
                    habit = habit,
                    completionHistory = completionHistory,
                    todayProgress = todayProgress,
                    currentStreak = currentStreak,
                    today = today,
                    todayRecord = todayRecord,
                    habitRecords = habitRecords,
                    onUpdateProgress = onUpdateProgress,
                    onCardClick = onCardClick,
                    modifier = modifier
                )
            }
            is ViewMode.Compact -> {
                HabitCardCompact(
                    habit = habit,
                    completionHistory = completionHistory,
                    todayProgress = todayProgress,
                    currentStreak = currentStreak,
                    today = today,
                    todayRecord = todayRecord,
                    habitRecords = habitRecords,
                    onUpdateProgress = onUpdateProgress,
                    onCardClick = onCardClick,
                    modifier = modifier
                )
            }
        }
    }
}