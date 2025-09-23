package org.example.habitstreak.presentation.screen.habit_detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.HabitType
import org.example.habitstreak.domain.model.getType
import org.example.habitstreak.presentation.model.YearMonth
import kotlin.time.ExperimentalTime

/**
 * Calendar component for habit detail screen following Single Responsibility Principle.
 * Handles only calendar display and date selection logic.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun HabitDetailCalendar(
    currentMonth: YearMonth,
    records: List<HabitRecord>,
    habit: Habit,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val createdDate = habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val createdMonth = YearMonth(createdDate.year, createdDate.monthNumber)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Navigation
            MonthNavigation(
                currentMonth = currentMonth,
                onMonthChange = onMonthChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day labels - Starting from Monday
            WeekdayHeaders()

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            CalendarGrid(
                currentMonth = currentMonth,
                records = records,
                habit = habit,
                today = today,
                onDateSelected = onDateSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation chips
            NavigationChips(
                createdMonth = createdMonth,
                today = today,
                onMonthChange = onMonthChange
            )
        }
    }
}

@Composable
private fun MonthNavigation(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onMonthChange(currentMonth.previous()) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "Previous month",
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "${getMonthName(currentMonth.month)} ${currentMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        IconButton(
            onClick = { onMonthChange(currentMonth.next()) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Next month",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WeekdayHeaders(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    records: List<HabitRecord>,
    habit: Habit,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysInMonth = getDaysInMonth(currentMonth.year, currentMonth.month)
    val firstDayOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
    // Convert to Monday-based (1=Monday, 7=Sunday)
    val firstDayOfWeek = when (firstDayOfMonth.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }

    val totalCells = ((daysInMonth + firstDayOfWeek - 1) / 7 + 1) * 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.height((totalCells / 7 * 42).dp),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(totalCells) { index ->
            val dayNumber = index - firstDayOfWeek + 1

            if (dayNumber in 1..daysInMonth) {
                val date = LocalDate(currentMonth.year, currentMonth.month, dayNumber)
                val record = records.find { it.date == date }
                val targetCount = habit.targetCount.coerceAtLeast(1)
                val completionRate = when {
                    record == null -> 0f
                    habit.getType() == HabitType.COUNTABLE ->
                        (record.completedCount.toFloat() / targetCount).coerceIn(0f, 1f)
                    else -> if (record.completedCount > 0) 1f else 0f
                }

                CalendarDayCell(
                    dayNumber = dayNumber,
                    date = date,
                    completionRate = completionRate,
                    isToday = date == today,
                    isFuture = date > today,
                    hasNote = records.any { it.date == date && it.note.isNotBlank() },
                    onClick = { onDateSelected(date) }
                )
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    date: LocalDate,
    completionRate: Float,
    isToday: Boolean,
    isFuture: Boolean,
    hasNote: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        completionRate >= 1f -> MaterialTheme.colorScheme.primary
        completionRate > 0.5f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        completionRate > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val textColor = when {
        completionRate >= 0.5f -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isToday) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (hasNote) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
            }
        }
    }
}

@Composable
private fun NavigationChips(
    createdMonth: YearMonth,
    today: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        AssistChip(
            onClick = { onMonthChange(createdMonth) },
            label = { Text("Created") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        AssistChip(
            onClick = { onMonthChange(YearMonth(today.year, today.monthNumber)) },
            label = { Text("Today") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Today,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}

// Helper functions
private fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }
}

private fun getMonthName(month: Int): String {
    return when (month) {
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
}