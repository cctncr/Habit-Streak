package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.presentation.ui.components.common.HabitIconDisplay
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.domain.util.HabitFrequencyUtils
import kotlin.time.ExperimentalTime

/**
 * Compact size habit card with 1 row grid
 * Minimal view for maximum habits on screen
 */
@OptIn(ExperimentalTime::class)
@Composable
fun HabitCardCompact(
    habit: Habit,
    completionHistory: Map<LocalDate, Float>,
    todayProgress: Float,
    currentStreak: Int,
    today: LocalDate,
    todayRecord: HabitRecord?,
    habitRecords: List<HabitRecord> = emptyList(),
    onUpdateProgress: (LocalDate, Int, String) -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProgressSheet by remember { mutableStateOf(false) }

    val isCompleted = todayProgress >= 1f
    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)
    val hasNote = todayRecord?.note?.isNotBlank() == true
    val isTodayActive = HabitFrequencyUtils.isActiveOnDate(habit.frequency, today, habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date)

    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon and Title
            HabitIconDisplay(
                icon = habit.icon,
                color = habitColor,
                size = 24.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (hasNote) {
                        Icon(
                            Icons.AutoMirrored.Outlined.StickyNote2,
                            contentDescription = "Has note",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }

                    if (currentStreak > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF4ED).copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 10.sp
                            )
                            Text(
                                text = currentStreak.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B35)
                            )
                        }
                    }
                }

                if (habit.targetCount > 1) {
                    Text(
                        text = "${(todayProgress * habit.targetCount).toInt()}/${habit.targetCount} ${habit.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            val gridDateRange = GridDateHelper.calculateDateRange(
                today = today,
                habitRecords = habitRecords,
                minHistoryDays = 29
            )

            HabitGrid(
                completedDates = completionHistory,
                startDate = gridDateRange.effectiveStartDate,
                today = today,
                accentColor = habitColor,
                rows = 1,
                boxSize = 18.dp,
                spacing = 1.dp,
                cornerRadius = 2.dp,
                maxHistoryDays = gridDateRange.actualHistoryDays,
                habitRecords = habitRecords.filter {
                    it.date >= gridDateRange.effectiveStartDate && it.date <= today
                },
                onDateClick = null,
                habit = habit,
                modifier = Modifier.width(180.dp)
            )

            // Progress Button
            if (isTodayActive) {
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 18.dp)
                    ) {
                        showProgressSheet = true
                    }
                ) {
                    HabitProgressButton(
                        progress = todayProgress,
                        isCompleted = isCompleted,
                        targetCount = habit.targetCount,
                        unit = habit.unit,
                        progressColor = habitColor,
                        buttonSize = 36.dp,
                        strokeWidth = 2.5.dp,
                        onClick = {
                            showProgressSheet = true
                        }
                    )
                }
            } else {
                // Inactive day indicator
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }

    HabitProgressBottomSheet(
        visible = showProgressSheet,
        habit = habit,
        selectedDate = today,
        today = today,
        habitRecords = habitRecords,
        onDismiss = {
            showProgressSheet = false
        },
        onSave = { date, value, note ->
            onUpdateProgress(date, value, note)
            showProgressSheet = false
        }
    )
}