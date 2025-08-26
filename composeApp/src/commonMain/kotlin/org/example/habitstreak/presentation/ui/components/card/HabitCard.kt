package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.presentation.ui.components.common.HabitIconDisplay
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme

@Composable
fun HabitCard(
    habit: Habit,
    completionHistory: Map<LocalDate, Float>, // Date -> progress (0-1)
    todayProgress: Float,
    currentStreak: Int,
    today: LocalDate,
    onUpdateProgress: (LocalDate, Int) -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProgressDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val isCompleted = todayProgress >= 1f
    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row with icon, title and progress button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                HabitIconDisplay(
                    icon = habit.icon,
                    color = habitColor,
                    size = 32.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Title and target
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (habit.targetCount > 1) {
                        Text(
                            text = "${(todayProgress * habit.targetCount).toInt()} / ${habit.targetCount} ${habit.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Progress Button
                HabitProgressButton(
                    progress = todayProgress,
                    isCompleted = isCompleted,
                    targetCount = habit.targetCount,
                    unit = habit.unit,
                    progressColor = habitColor,
                    buttonSize = 44.dp,
                    strokeWidth = 3.dp,
                    onClick = {
                        selectedDate = today
                        showProgressDialog = true
                    }
                )
            }

            // Always visible Grid
            HabitGrid(
                completedDates = completionHistory,
                startDate = habit.createdAt,
                today = today,
                accentColor = habitColor,
                rows = 3,
                boxSize = 26.dp,
                spacing = 2.dp,
                cornerRadius = 4.dp,
                maxHistoryDays = 365L, // Show last year for better history view
                onDateClick = { date ->
                    selectedDate = date
                    showProgressDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp) // Fixed height for 3 rows + header
            )
        }
    }

    // Progress Dialog
    HabitProgressDialog(
        show = showProgressDialog,
        habitTitle = habit.title,
        targetCount = habit.targetCount,
        unit = habit.unit,
        currentValue = selectedDate?.let { date ->
            val progress = completionHistory[date] ?: 0f
            (progress * habit.targetCount).toInt()
        },
        onDismiss = {
            showProgressDialog = false
            selectedDate = null
        },
        onConfirm = { value ->
            selectedDate?.let { date ->
                onUpdateProgress(date, value)
            }
        }
    )
}