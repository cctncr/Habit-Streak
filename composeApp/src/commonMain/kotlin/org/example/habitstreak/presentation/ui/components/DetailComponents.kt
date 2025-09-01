package org.example.habitstreak.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.HabitType
import org.example.habitstreak.domain.model.getType
import org.example.habitstreak.presentation.model.YearMonth
import org.example.habitstreak.presentation.screen.habit_detail.StatsTimeFilter
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel

@Composable
fun ProgressCard(
    stats: HabitDetailViewModel.HabitStats,
    habit: Habit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current Streak Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = (stats.completionRate / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stats.currentStreak.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat(
                    icon = Icons.Outlined.LocalFireDepartment,
                    value = stats.longestStreak.toString(),
                    label = "Best Streak"
                )
                QuickStat(
                    icon = Icons.Outlined.CalendarToday,
                    value = stats.totalDays.toString(),
                    label = "Total Days"
                )
                QuickStat(
                    icon = Icons.Outlined.Percent,
                    value = "${stats.completionRate.toInt()}%",
                    label = "Success Rate"
                )
            }
        }
    }
}

@Composable
private fun QuickStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatsCard(
    stats: HabitDetailViewModel.HabitStats,
    filter: StatsTimeFilter,
    onFilterChange: (StatsTimeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsTimeFilter.values().forEach { timeFilter ->
                    FilterChip(
                        selected = filter == timeFilter,
                        onClick = { onFilterChange(timeFilter) },
                        label = { Text(timeFilter.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "This Week",
                    value = stats.thisWeekCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "This Month",
                    value = stats.thisMonthCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Average",
                    value = String.format("%.1f", stats.averagePerDay.toFloat()),
                    modifier = Modifier.weight(1f)
                )
            }

            stats.lastCompleted?.let { date ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Last completed: ${DateFormatter.formatRelative(date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    records: List<HabitRecord>,
    habit: Habit,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val prevMonth = if (currentMonth.month == 1) {
                            YearMonth(currentMonth.year - 1, 12)
                        } else {
                            YearMonth(currentMonth.year, currentMonth.month - 1)
                        }
                        onMonthChange(prevMonth)
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }

                Text(
                    text = "${getMonthName(currentMonth.month)} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val nextMonth = if (currentMonth.month == 12) {
                            YearMonth(currentMonth.year + 1, 1)
                        } else {
                            YearMonth(currentMonth.year, currentMonth.month + 1)
                        }
                        onMonthChange(nextMonth)
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val daysInMonth = getDaysInMonth(currentMonth.year, currentMonth.month)
            val firstDayOfWeek = getFirstDayOfWeek(currentMonth.year, currentMonth.month)
            val totalCells = ((daysInMonth + firstDayOfWeek - 1) / 7 + 1) * 7

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height((totalCells / 7 * 48).dp),
                userScrollEnabled = false
            ) {
                items(totalCells) { index ->
                    val dayNumber = index - firstDayOfWeek + 2

                    if (dayNumber in 1..daysInMonth) {
                        val date = LocalDate(currentMonth.year, currentMonth.month, dayNumber)
                        val record = records.find { it.date == date }
                        val isCompleted = record != null &&
                                record.completedCount >= habit.targetCount.coerceAtLeast(1)

                        DayCell(
                            dayNumber = dayNumber,
                            isCompleted = isCompleted,
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            isFuture = date > today,
                            completionRate = if (habit.getType() == HabitType.COUNTABLE && record != null) {
                                record.completedCount.toFloat() / habit.targetCount.coerceAtLeast(1)
                            } else if (isCompleted) 1f else 0f,
                            onClick = { onDateSelected(date) }
                        )
                    } else {
                        Box(modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isCompleted: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    completionRate: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = !isFuture) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (completionRate > 0 && completionRate < 1) {
            CircularProgressIndicator(
                progress = completionRate,
                modifier = Modifier.size(32.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }

        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Helper functions
private fun getMonthName(month: Int): String = when (month) {
    1 -> "January"
    2 -> "February"
    3 -> "March"
    4 -> "April"
    5 -> "May"
    6 -> "June"
    7 -> "July"
    8 -> "August"
    9 -> "September"
    10 -> "October"
    11 -> "November"
    12 -> "December"
    else -> ""
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    val date = LocalDate(year, month, 1)
    return date.dayOfWeek.ordinal
}