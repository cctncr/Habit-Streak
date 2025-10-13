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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import habitstreak.composeapp.generated.resources.Res
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
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.month_apr_abbr
import habitstreak.composeapp.generated.resources.month_aug_abbr
import habitstreak.composeapp.generated.resources.month_dec_abbr
import habitstreak.composeapp.generated.resources.month_feb_abbr
import habitstreak.composeapp.generated.resources.month_jan_abbr
import habitstreak.composeapp.generated.resources.month_jul_abbr
import habitstreak.composeapp.generated.resources.month_jun_abbr
import habitstreak.composeapp.generated.resources.month_mar_abbr
import habitstreak.composeapp.generated.resources.month_may_abbr
import habitstreak.composeapp.generated.resources.month_nov_abbr
import habitstreak.composeapp.generated.resources.month_oct_abbr
import habitstreak.composeapp.generated.resources.month_sep_abbr
import habitstreak.composeapp.generated.resources.month_january
import habitstreak.composeapp.generated.resources.month_february
import habitstreak.composeapp.generated.resources.month_march
import habitstreak.composeapp.generated.resources.month_april
import habitstreak.composeapp.generated.resources.month_may
import habitstreak.composeapp.generated.resources.month_june
import habitstreak.composeapp.generated.resources.month_july
import habitstreak.composeapp.generated.resources.month_august
import habitstreak.composeapp.generated.resources.month_september
import habitstreak.composeapp.generated.resources.month_october
import habitstreak.composeapp.generated.resources.month_november
import habitstreak.composeapp.generated.resources.month_december
import habitstreak.composeapp.generated.resources.day_mon_abbr
import habitstreak.composeapp.generated.resources.day_tue_abbr
import habitstreak.composeapp.generated.resources.day_wed_abbr
import habitstreak.composeapp.generated.resources.day_thu_abbr
import habitstreak.composeapp.generated.resources.day_fri_abbr
import habitstreak.composeapp.generated.resources.day_sat_abbr
import habitstreak.composeapp.generated.resources.day_sun_abbr

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
    habit: Habit? = null,
    showDayLabels: Boolean = false,
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

    val dayLabels = listOf(
        stringResource(Res.string.day_mon_abbr),
        stringResource(Res.string.day_tue_abbr),
        stringResource(Res.string.day_wed_abbr),
        stringResource(Res.string.day_thu_abbr),
        stringResource(Res.string.day_fri_abbr),
        stringResource(Res.string.day_sat_abbr),
        stringResource(Res.string.day_sun_abbr)
    )

    if (showDayLabels && rows == 7) {
        Box(modifier = modifier) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    horizontalAlignment = Alignment.End
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    dayLabels.forEach { label ->
                        Box(
                            modifier = Modifier
                                .size(boxSize)
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = label,
                                fontSize = (boxSize.value * 0.28).sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    reverseLayout = false,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 16.dp)
                ) {
                    items(count = totalColumns) { columnIndex ->
                        val firstDateInColumn = LocalDate.fromEpochDays(
                            gridStartDate.toEpochDays() + (columnIndex * rows)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            for (rowIndex in 0 until rows) {
                                val date = LocalDate.fromEpochDays(
                                    firstDateInColumn.toEpochDays() + rowIndex
                                )

                                if (date <= today && date >= gridStartDate) {
                                    val hasNote = habitRecords.any { it.date == date && it.note.isNotBlank() }
                                    val isDateActive = habit?.let {
                                        val createdDate = it.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                                        HabitFrequencyUtils.isActiveOnDate(it.frequency, date, createdDate)
                                    } ?: true

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

                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
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
                            } ?: true

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
                    text = getMonthAbbreviation(date.month.number),
                    fontSize = (boxSize.value * 0.25).sp,
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

@Composable
private fun getMonthAbbreviation(month: Int): String {
    return when (month) {
        1 -> stringResource(Res.string.month_jan_abbr)
        2 -> stringResource(Res.string.month_feb_abbr)
        3 -> stringResource(Res.string.month_mar_abbr)
        4 -> stringResource(Res.string.month_apr_abbr)
        5 -> stringResource(Res.string.month_may_abbr)
        6 -> stringResource(Res.string.month_jun_abbr)
        7 -> stringResource(Res.string.month_jul_abbr)
        8 -> stringResource(Res.string.month_aug_abbr)
        9 -> stringResource(Res.string.month_sep_abbr)
        10 -> stringResource(Res.string.month_oct_abbr)
        11 -> stringResource(Res.string.month_nov_abbr)
        12 -> stringResource(Res.string.month_dec_abbr)
        else -> ""
    }
}