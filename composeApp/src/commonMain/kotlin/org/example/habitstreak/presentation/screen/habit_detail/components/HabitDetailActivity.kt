package org.example.habitstreak.presentation.screen.habit_detail.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import org.example.habitstreak.core.extensions.formatRelative
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.presentation.screen.habit_detail.ActivityTab
import org.example.habitstreak.presentation.screen.habit_detail.getLabel

/**
 * Activity section component for habit detail screen following Single Responsibility Principle.
 * Handles only activity tab display with history and notes.
 */
@Composable
fun HabitDetailActivity(
    records: List<HabitRecord>,
    habit: Habit,
    selectedActivityTab: ActivityTab,
    onTabChanged: (ActivityTab) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Tab Selector
        ActivityTabSelector(
            selectedTab = selectedActivityTab,
            onTabChanged = onTabChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        AnimatedContent(
            targetState = selectedActivityTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200))
            },
            label = "activity_tab"
        ) { tab ->
            when (tab) {
                ActivityTab.HISTORY -> {
                    ActivityHistory(
                        records = records,
                        habit = habit,
                        onDateClick = onDateClick
                    )
                }
                ActivityTab.NOTES -> {
                    NotesList(
                        records = records.filter { !it.note.isNullOrBlank() },
                        onDateClick = onDateClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTabSelector(
    selectedTab: ActivityTab,
    onTabChanged: (ActivityTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            ActivityTab.entries.forEach { tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedTab == tab)
                                MaterialTheme.colorScheme.surface
                            else Color.Transparent
                        )
                        .clickable { onTabChanged(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.getLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == tab)
                            FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityHistory(
    records: List<HabitRecord>,
    habit: Habit,
    onDateClick: (LocalDate) -> Unit
) {
    if (records.isEmpty()) {
        EmptyState(message = "No activity yet")
    } else {
        val sortedRecords = records.sortedByDescending { it.date }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sortedRecords.take(10).forEach { record ->
                ActivityItem(
                    record = record,
                    habit = habit,
                    onClick = { onDateClick(record.date) }
                )
            }
        }
    }
}

@Composable
private fun ActivityItem(
    record: HabitRecord,
    habit: Habit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = record.date.day.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = getMonthAbbreviation(record.date.monthNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatRelative(record.date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (record.note.isNotBlank()) {
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val habitColor = habit.color.composeColor
            val progress = (record.completedCount.toFloat() / habit.targetCount.coerceAtLeast(1))
            val percentage = (progress * 100).toInt()

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 2.dp.toPx()

                    // Background ring
                    drawCircle(
                        color = habitColor.copy(alpha = 0.2f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Progress ring
                    if (progress > 0f) {
                        drawArc(
                            color = habitColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress.coerceAtMost(1f),
                            useCenter = false,
                            topLeft = Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Percentage text
                Text(
                    text = "${percentage.coerceAtMost(999)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = if (percentage >= 100) habitColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun NotesList(
    records: List<HabitRecord>,
    onDateClick: (LocalDate) -> Unit
) {
    if (records.isEmpty()) {
        EmptyState(message = "No notes yet")
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            records.sortedByDescending { it.date }.take(10).forEach { record ->
                NoteItem(
                    record = record,
                    onClick = { onDateClick(record.date) }
                )
            }
        }
    }
}

@Composable
private fun NoteItem(
    record: HabitRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.StickyNote2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatRelative(record.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper function
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