package org.example.habitstreak.presentation.screen.habit_detail.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.HabitType
import org.example.habitstreak.domain.model.getType
import org.example.habitstreak.domain.util.HabitFrequencyUtils
import org.example.habitstreak.presentation.model.YearMonth
import org.example.habitstreak.presentation.ui.utils.drawStripedPattern
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import kotlin.time.ExperimentalTime

/**
 * Pagination-based calendar component for habit detail screen following Single Responsibility Principle.
 * Handles calendar display, date selection logic, and swipe navigation between months.
 */
@OptIn(ExperimentalTime::class, ExperimentalFoundationApi::class)
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
    val todayMonth = YearMonth(today.year, today.monthNumber)

    // Calculate initial months to display
    var monthsToShow by remember { mutableStateOf(calculateInitialMonthRange(createdMonth, todayMonth)) }
    val initialPage = monthsToShow.indexOf(currentMonth).takeIf { it >= 0 } ?: (monthsToShow.size / 2)

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { monthsToShow.size }
    )

    val coroutineScope = rememberCoroutineScope()

    // Update current month when page changes and handle dynamic range extension
    LaunchedEffect(pagerState.currentPage, monthsToShow.size) {
        if (pagerState.currentPage < monthsToShow.size) {
            onMonthChange(monthsToShow[pagerState.currentPage])
        }

        val threshold = 3

        // Extend forward when approaching the end
        if (pagerState.currentPage > monthsToShow.size - threshold) {
            val additionalMonths = generateNextMonths(monthsToShow.last(), 6)
            monthsToShow = monthsToShow + additionalMonths
        }

        // Extend backward when approaching the beginning
        if (pagerState.currentPage < threshold && pagerState.currentPage >= 0) {
            val additionalMonths = generatePreviousMonths(monthsToShow.first(), 6)
            val oldSize = monthsToShow.size
            monthsToShow = additionalMonths + monthsToShow
            // Adjust pager position to maintain current view
            val newPosition = pagerState.currentPage + (monthsToShow.size - oldSize)
            pagerState.scrollToPage(newPosition)
        }
    }

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
            // Month Navigation with current month indicator
            MonthNavigationHeader(
                currentMonth = if (pagerState.currentPage < monthsToShow.size) monthsToShow[pagerState.currentPage] else currentMonth,
                onPreviousMonth = {
                    coroutineScope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onNextMonth = {
                    coroutineScope.launch {
                        if (pagerState.currentPage < monthsToShow.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                canNavigatePrevious = pagerState.currentPage > 0,
                canNavigateNext = pagerState.currentPage < monthsToShow.size - 1
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day labels - Starting from Monday
            WeekdayHeaders()

            Spacer(modifier = Modifier.height(8.dp))

            // Paginated Calendar
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                if (pageIndex < monthsToShow.size) {
                    CalendarGrid(
                        currentMonth = monthsToShow[pageIndex],
                        records = records,
                        habit = habit,
                        today = today,
                        onDateSelected = onDateSelected
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation chips
            NavigationChips(
                createdMonth = createdMonth,
                today = today,
                onNavigateToMonth = { targetMonth ->
                    val targetIndex = monthsToShow.indexOf(targetMonth)
                    if (targetIndex >= 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetIndex)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            enabled = canNavigatePrevious,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = stringResource(Res.string.previous_month),
                modifier = Modifier.size(20.dp),
                tint = if (canNavigatePrevious) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
            )
        }

        Text(
            text = "${getLocalizedMonthName(currentMonth.month)} ${currentMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        IconButton(
            onClick = onNextMonth,
            enabled = canNavigateNext,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = stringResource(Res.string.next_month),
                modifier = Modifier.size(20.dp),
                tint = if (canNavigateNext) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
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
        val weekdayLabels = listOf(
            stringResource(Res.string.day_mon_short),
            stringResource(Res.string.day_tue_short),
            stringResource(Res.string.day_wed_short),
            stringResource(Res.string.day_thu_short),
            stringResource(Res.string.day_fri_short),
            stringResource(Res.string.day_sat_short),
            stringResource(Res.string.day_sun_short)
        )

        weekdayLabels.forEach { day ->
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

@OptIn(ExperimentalTime::class)
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
    val createdDate = habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date

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

    // Always show 6 rows for consistent layout, but optimize for content
    val actualRows = ((daysInMonth + firstDayOfWeek - 1) / 7 + 1)
    val totalCells = 6 * 7 // Fixed 42 cells for consistency

    // Optimal height calculation: cells (36dp) + spacing (4dp between rows)
    // 6 rows: (6 * 36dp) + (5 * 4dp spacing) = 216dp + 20dp = 236dp
    val optimizedHeight = 236.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.height(optimizedHeight),
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

                val isDateActive = HabitFrequencyUtils.isActiveOnDate(habit.frequency, date, createdDate)

                CalendarDayCell(
                    dayNumber = dayNumber,
                    date = date,
                    completionRate = completionRate,
                    isToday = date == today,
                    isFuture = date > today,
                    hasNote = records.any { it.date == date && it.note.isNotBlank() },
                    isActive = isDateActive,
                    habit = habit,
                    onClick = { onDateSelected(date) }
                )
            } else {
                // Empty cell for padding - maintains consistent calendar height
                Spacer(modifier = Modifier.size(12.dp))
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
    isActive: Boolean,
    habit: Habit,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isActive -> habit.color.composeColor.copy(alpha = 0.2f)
            completionRate >= 1f -> habit.color.composeColor
            completionRate > 0.5f -> habit.color.composeColor.copy(alpha = 0.6f)
            completionRate > 0f -> habit.color.composeColor.copy(alpha = 0.3f)
            hasNote -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "backgroundColor"
    )

    val textColor = when {
        completionRate >= 0.5f -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
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
                        borderColor,
                        RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable(enabled = isActive) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Striped pattern for inactive days
        if (!isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawStripedPattern(
                    color = habit.color.composeColor, // Full color like completed cells
                    boxSize = 36.dp,
                    cornerRadius = 8.dp
                )
            }
        }

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
    onNavigateToMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        AssistChip(
            onClick = { onNavigateToMonth(createdMonth) },
            label = { Text(stringResource(Res.string.created)) },
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
            onClick = { onNavigateToMonth(YearMonth(today.year, today.monthNumber)) },
            label = { Text(stringResource(Res.string.today)) },
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

@Composable
private fun getLocalizedMonthName(month: Int): String {
    return when (month) {
        1 -> stringResource(Res.string.month_january)
        2 -> stringResource(Res.string.month_february)
        3 -> stringResource(Res.string.month_march)
        4 -> stringResource(Res.string.month_april)
        5 -> stringResource(Res.string.month_may)
        6 -> stringResource(Res.string.month_june)
        7 -> stringResource(Res.string.month_july)
        8 -> stringResource(Res.string.month_august)
        9 -> stringResource(Res.string.month_september)
        10 -> stringResource(Res.string.month_october)
        11 -> stringResource(Res.string.month_november)
        12 -> stringResource(Res.string.month_december)
        else -> ""
    }
}

private fun calculateInitialMonthRange(createdMonth: YearMonth, todayMonth: YearMonth): List<YearMonth> {
    val months = mutableListOf<YearMonth>()

    // Add some months before creation for better UX
    var current = createdMonth.previous().previous().previous()

    // Add months until today plus some future months
    val endMonth = todayMonth.next().next().next().next().next().next() // 6 months ahead

    while (current <= endMonth) {
        months.add(current)
        current = current.next()
    }

    return months
}

private fun generateNextMonths(fromMonth: YearMonth, count: Int): List<YearMonth> {
    val months = mutableListOf<YearMonth>()
    var current = fromMonth.next()

    repeat(count) {
        months.add(current)
        current = current.next()
    }

    return months
}

private fun generatePreviousMonths(fromMonth: YearMonth, count: Int): List<YearMonth> {
    val months = mutableListOf<YearMonth>()
    var current = fromMonth.previous()

    repeat(count) {
        months.add(0, current) // Add to beginning
        current = current.previous()
    }

    return months
}

