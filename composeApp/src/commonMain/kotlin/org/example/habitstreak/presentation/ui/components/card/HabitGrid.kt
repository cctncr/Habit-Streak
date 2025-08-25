package org.example.habitstreak.presentation.ui.components.habit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate

@Composable
fun HabitGrid(
    completedDates: Map<LocalDate, Float>, // Date -> progress (0-1 for percentage)
    startDate: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
    rows: Int = 3,
    boxSize: Dp = 28.dp,
    spacing: Dp = 3.dp,
    cornerRadius: Dp = 4.dp,
    maxHistoryDays: Long = 90L,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onDateClick: (LocalDate) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Calculate date range
    val earliestDate = maxOf(
        startDate,
        today.minus(DatePeriod(days = maxHistoryDays.toInt()))
    )

    // Calculate columns needed
    val totalDays = earliestDate.until(today, DateTimeUnit.DAY) + 1
    val columnsNeeded = ((totalDays + rows - 1) / rows).coerceAtLeast(1)

    // Scroll to today (rightmost) on first composition
    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        reverseLayout = true // Today on the right
    ) {
        items(count = 1555) { columnIndex ->
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Calculate dates for this column (top to bottom)
                val topDate = today.minus(DatePeriod(days = columnIndex * rows))

                // Month header (only if first day of month is in column)
                val columnDates = (0 until rows).map { rowIndex ->
                    topDate.minus(DatePeriod(days = rows - 1 - rowIndex))
                }

                val showMonthHeader = columnDates.any {
                    it.day == 1 && it >= earliestDate && it <= today
                }

                if (showMonthHeader) {
                    val monthDate = columnDates.first { it.day == 1 }
                    Text(
                        text = getMonthAbbreviation(monthDate.month.number),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(14.dp)
                            .padding(horizontal = 2.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Date cells (top to bottom)
                columnDates.forEach { date ->
                    if (date in earliestDate..today) {
                        DateCell(
                            date = date,
                            progress = completedDates[date] ?: 0f,
                            isToday = date == today,
                            accentColor = accentColor,
                            boxSize = boxSize,
                            cornerRadius = cornerRadius,
                            onClick = { onDateClick(date) }
                        )
                    } else {
                        // Empty spacer for dates outside range
                        Spacer(modifier = Modifier.size(boxSize))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    progress: Float,
    isToday: Boolean,
    accentColor: Color,
    boxSize: Dp,
    cornerRadius: Dp,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            progress >= 1f -> accentColor
            progress >= 0.75f -> accentColor.copy(alpha = 0.8f)
            progress >= 0.5f -> accentColor.copy(alpha = 0.6f)
            progress > 0f -> accentColor.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "bg"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
    ) {
        when {
            progress >= 1f -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(boxSize * 0.6f),
                    tint = Color.White
                )
            }
            progress > 0f -> {
                Text(
                    text = "${(progress * 100).toInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            else -> {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun getMonthAbbreviation(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
}