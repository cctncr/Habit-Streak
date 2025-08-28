package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.HabitRecord

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
    habitRecords: List<HabitRecord> = emptyList(), // Note bilgisi için eklendi
    onDateClick: (LocalDate) -> Unit = {}
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
        if (totalColumns > 8) {
            coroutineScope.launch {
                delay(100)
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

                        DateCell(
                            date = date,
                            progress = completedDates[date] ?: 0f,
                            hasNote = hasNote, // Eklendi
                            isToday = date == today,
                            isFirstOfMonth = date.day == 1,
                            accentColor = accentColor,
                            boxSize = boxSize,
                            cornerRadius = cornerRadius,
                            onClick = { onDateClick(date) }
                        )
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
    hasNote: Boolean = false, // Eklendi
    isToday: Boolean,
    isFirstOfMonth: Boolean,
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
            hasNote -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) // Note indicator
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "bg_color"
    )

    val borderColor = if (isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = if (isToday) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable { onClick() }
    ) {
        when {
            isFirstOfMonth -> {
                Text(
                    text = getMonthAbbreviation(date.month.number),
                    fontSize = (boxSize.value * 0.20).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 0.5f) Color.White
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            progress >= 1f -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(boxSize * 0.45f),
                    tint = Color.White
                )
            }
            progress > 0f -> {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = (boxSize.value * 0.22).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 0.5f) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            hasNote -> {
                // Note indicator
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