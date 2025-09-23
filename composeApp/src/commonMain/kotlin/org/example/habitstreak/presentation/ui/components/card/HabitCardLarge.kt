package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import org.example.habitstreak.presentation.ui.components.input.SimpleCheckHabitInputPanel
import org.example.habitstreak.presentation.ui.components.input.CountableHabitInputPanel
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.domain.util.HabitFrequencyUtils
import kotlin.time.ExperimentalTime

/**
 * Large size habit card with 5 rows grid (clickable)
 * Allows editing past dates
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun HabitCardLarge(
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
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showProgressSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    var currentValue by remember { mutableIntStateOf(0) }
    var currentNote by remember { mutableStateOf("") }

    val isCompleted = todayProgress >= 1f
    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)
    val hasNote = todayRecord?.note?.isNotBlank() == true
    val isTodayActive = HabitFrequencyUtils.isActiveOnDate(habit.frequency, today, habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date)

    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row - Larger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HabitIconDisplay(
                    icon = habit.icon,
                    color = habitColor,
                    size = 42.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (habit.targetCount > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "${(todayProgress * habit.targetCount).toInt()}/${habit.targetCount} ${habit.unit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (hasNote) {
                            Icon(
                                Icons.AutoMirrored.Outlined.StickyNote2,
                                contentDescription = "Has note",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isTodayActive) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 28.dp)
                                ) {
                                    selectedDate = today
                                    val record = habitRecords.find { it.date == today }
                                    currentValue = record?.completedCount ?: 0
                                    currentNote = record?.note ?: ""
                                    showProgressSheet = true
                                }
                        ) {
                            HabitProgressButton(
                                progress = todayProgress,
                                isCompleted = isCompleted,
                                targetCount = habit.targetCount,
                                unit = habit.unit,
                                progressColor = habitColor,
                                buttonSize = 56.dp,
                                strokeWidth = 4.dp,
                                onClick = {
                                    selectedDate = today
                                    val record = habitRecords.find { it.date == today }
                                    currentValue = record?.completedCount ?: 0
                                    currentNote = record?.note ?: ""
                                    showProgressSheet = true
                                }
                            )
                        }
                    } else {
                        // Inactive day indicator
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Light
                            )
                        }
                    }

                    if (currentStreak > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFF4ED)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔥",
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentStreak.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B35)
                                )
                            }
                        }
                    }
                }
            }

            // Description if exists
            if (habit.description.isNotBlank()) {
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Today's note if exists
            if (hasNote && todayRecord.note.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.StickyNote2,
                            contentDescription = "Note",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = todayRecord.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Grid - 5 rows, clickable
            val gridStartDate = today.minus(DatePeriod(days = 149)) // 30 columns * 5 rows

            HabitGrid(
                completedDates = completionHistory,
                startDate = gridStartDate,
                today = today,
                accentColor = habitColor,
                rows = 5,
                boxSize = 32.dp,
                spacing = 3.dp,
                cornerRadius = 6.dp,
                maxHistoryDays = 150L,
                habitRecords = habitRecords.filter {
                    it.date >= gridStartDate && it.date <= today
                },
                onDateClick = { date ->
                    // Only allow clicking on active dates
                    val isDateActive = HabitFrequencyUtils.isActiveOnDate(habit.frequency, date, habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date)
                    if (isDateActive) {
                        selectedDate = date
                        val record = habitRecords.find { it.date == date }
                        currentValue = record?.completedCount ?: 0
                        currentNote = record?.note ?: ""
                        showProgressSheet = true
                    }
                },
                habit = habit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }

    // Progress Bottom Sheet
    if (showProgressSheet && selectedDate != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showProgressSheet = false
                selectedDate = null
            },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Title with Date
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedDate == today) "Today" else
                            selectedDate?.toString() ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Progress Input Panel - different for simple check vs countable habits
                if (habit.targetCount == 1) {
                    // Simple check habit - using common panel same as HabitDetailScreen
                    SimpleCheckHabitInputPanel(
                        isCompleted = currentValue >= 1,
                        onToggle = { isCompleted ->
                            currentValue = if (isCompleted) 1 else 0
                        },
                        accentColor = habitColor
                    )
                } else {
                    // Countable habit - using common panel
                    CountableHabitInputPanel(
                        currentValue = currentValue,
                        targetCount = habit.targetCount,
                        unit = habit.unit,
                        onValueChange = { value ->
                            currentValue = value
                        },
                        onReset = {
                            currentValue = 0
                        },
                        onFillDay = {
                            currentValue = habit.targetCount
                        },
                        accentColor = habitColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note Input
                OutlinedTextField(
                    value = currentNote,
                    onValueChange = { currentNote = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Add a note...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        selectedDate?.let { date ->
                            onUpdateProgress(date, currentValue, currentNote)
                        }
                        showProgressSheet = false
                        selectedDate = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = habitColor
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}