package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.presentation.ui.model.ViewMode

/**
 * Memoized wrapper for HabitCard to prevent unnecessary recompositions
 * during ViewMode changes. Follows Single Responsibility Principle.
 */
@Composable
fun MemoizedHabitCard(
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
    // Memoize the card content to prevent re-rendering when only ViewMode changes
    val cardContent = remember(
        habit.id,
        habit.title,
        habit.targetCount,
        habit.color,
        habit.icon,
        completionHistory,
        todayProgress,
        currentStreak,
        today,
        todayRecord?.completedCount,
        todayRecord?.note
    ) {
        CardContent(
            habit = habit,
            completionHistory = completionHistory,
            todayProgress = todayProgress,
            currentStreak = currentStreak,
            today = today,
            todayRecord = todayRecord,
            habitRecords = habitRecords,
            onUpdateProgress = onUpdateProgress,
            onCardClick = onCardClick
        )
    }

    // Only recompose when ViewMode changes
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

private data class CardContent(
    val habit: Habit,
    val completionHistory: Map<LocalDate, Float>,
    val todayProgress: Float,
    val currentStreak: Int,
    val today: LocalDate,
    val todayRecord: HabitRecord?,
    val habitRecords: List<HabitRecord>,
    val onUpdateProgress: (LocalDate, Int, String) -> Unit,
    val onCardClick: () -> Unit
)