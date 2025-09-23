package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
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
 * Medium size habit card with 3 rows grid (non-clickable)
 * Default view mode
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun HabitCardMedium(
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
    var currentValue by remember { mutableIntStateOf(0) }
    var currentNote by remember { mutableStateOf("") }

    val isCompleted = todayProgress >= 1f
    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)
    val hasNote = todayRecord?.note?.isNotBlank() == true
    val isTodayActive = HabitFrequencyUtils.isActiveOnDate(habit.frequency, today, habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date)

    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HabitIconDisplay(
                    icon = habit.icon,
                    color = habitColor,
                    size = 32.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (habit.targetCount > 1) {
                            Text(
                                text = "${(todayProgress * habit.targetCount).toInt()}/${habit.targetCount} ${habit.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (hasNote) {
                            Icon(
                                Icons.AutoMirrored.Outlined.StickyNote2,
                                contentDescription = "Has note",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (currentStreak > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF4ED))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 12.sp
                        )
                        Text(
                            text = currentStreak.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B35)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (isTodayActive) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 22.dp)
                            ) {
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
                            buttonSize = 44.dp,
                            strokeWidth = 3.dp,
                            onClick = {
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
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }

            // Grid - 3 rows, not clickable
            val gridStartDate = today.minus(DatePeriod(days = 89))

            HabitGrid(
                completedDates = completionHistory,
                startDate = gridStartDate,
                today = today,
                accentColor = habitColor,
                rows = 3,
                boxSize = 28.dp,
                spacing = 2.dp,
                cornerRadius = 4.dp,
                maxHistoryDays = 90L,
                habitRecords = habitRecords.filter {
                    it.date >= gridStartDate && it.date <= today
                },
                onDateClick = null, // Not clickable in medium view
                habit = habit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }

    // Progress Bottom Sheet
    if (showProgressSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showProgressSheet = false
            },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Title
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

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
                        onUpdateProgress(today, currentValue, currentNote)
                        showProgressSheet = false
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