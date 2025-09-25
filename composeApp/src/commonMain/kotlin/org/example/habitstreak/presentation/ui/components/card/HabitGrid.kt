package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.util.HabitFrequencyUtils
import org.example.habitstreak.presentation.ui.utils.drawStripedPattern
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun HabitGrid(
    completedDates: Map<LocalDate, Float>,
    startDate: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
    rows: Int = 5,
    boxSize: Dp = 22.dp,
    spacing: Dp = 2.dp,
    cornerRadius: Dp = 3.dp,
    maxHistoryDays: Long = 365L,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    habitRecords: List<HabitRecord> = emptyList(),
    onDateClick: ((LocalDate) -> Unit)? = null,
    habit: Habit? = null
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val gridStartDate = maxOf(
        startDate,
        LocalDate.fromEpochDays(today.toEpochDays() - maxHistoryDays + 1)
    )

    val totalDays = (today.toEpochDays() - gridStartDate.toEpochDays() + 1).toInt()
    val totalColumns = if (totalDays > 0) {
        ((totalDays + rows - 1) / rows)
    } else {
        1
    }

    LaunchedEffect(totalColumns) {
        if (totalColumns > 8 && !listState.isScrollInProgress) {
            coroutineScope.launch {
                delay(200)
                val targetIndex = (totalColumns - 8).coerceAtLeast(0)
                listState.scrollToItem(targetIndex)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        reverseLayout = false
    ) {
        items(count = totalColumns) { columnIndex ->
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                val firstDateInColumn = LocalDate.fromEpochDays(
                    gridStartDate.toEpochDays() + (columnIndex * rows)
                )

                for (rowIndex in 0 until rows) {
                    val date = LocalDate.fromEpochDays(
                        firstDateInColumn.toEpochDays() + rowIndex
                    )

                    if (date <= today && date >= gridStartDate) {
                        val hasNote = habitRecords.any { it.date == date && it.note.isNotBlank() }
                        val isDateActive = habit?.let {
                            val createdDate = it.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                            HabitFrequencyUtils.isActiveOnDate(it.frequency, date, createdDate)
                        } ?: true // Default to true when no habit provided (for backward compatibility)

                        key(date) {
                            DateCell(
                                date = date,
                                progress = completedDates[date] ?: 0f,
                                hasNote = hasNote,
                                isToday = date == today,
                                isFirstOfMonth = date.day == 1,
                                accentColor = accentColor,
                                boxSize = boxSize,
                                cornerRadius = cornerRadius,
                                onClick = onDateClick?.let { { it(date) } },
                                isActive = isDateActive
                            )
                        }
                    } else if (date > today) {
                        FutureDateCell(
                            boxSize = boxSize,
                            cornerRadius = cornerRadius
                        )
                    } else {
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
    hasNote: Boolean = false,
    isToday: Boolean,
    isFirstOfMonth: Boolean,
    accentColor: Color,
    boxSize: Dp,
    cornerRadius: Dp,
    onClick: (() -> Unit)? = null,
    isActive: Boolean = true
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isActive -> accentColor.copy(alpha = 0.2f) // Lighter background for inactive days
            progress >= 1f -> accentColor
            progress >= 0.75f -> accentColor.copy(alpha = 0.8f)
            progress >= 0.5f -> accentColor.copy(alpha = 0.6f)
            progress > 0f -> accentColor.copy(alpha = 0.4f)
            hasNote -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        label = "backgroundColor"
    )

    val borderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isFirstOfMonth -> MaterialTheme.colorScheme.outline
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .then(
                if (isToday || isFirstOfMonth) {
                    Modifier.border(
                        width = if (isToday) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else Modifier
            )
            .then(
                if (onClick != null && isActive) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Striped pattern for inactive days
        if (!isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawStripedPattern(
                    color = accentColor, // Full color like completed cells
                    boxSize = boxSize,
                    cornerRadius = cornerRadius
                )
            }
        }

        when {
            progress >= 1f -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(boxSize * 0.5f),
                    tint = Color.White
                )
            }
            isFirstOfMonth && progress == 0f && !hasNote -> {
                Text(
                    text = getMonthAbbreviation(date.month.number).take(1),
                    fontSize = (boxSize.value * 0.3).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 0.5f) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            hasNote -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                    contentDescription = null,
                    modifier = Modifier.size(boxSize * 0.35f),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            else -> {
                Text(
                    text = date.day.toString(),
                    fontSize = (boxSize.value * 0.22).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FutureDateCell(
    boxSize: Dp,
    cornerRadius: Dp
) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    )
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

