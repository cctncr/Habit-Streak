package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.presentation.ui.components.input.CountableHabitInputPanel
import org.example.habitstreak.presentation.ui.components.input.SimpleCheckHabitInputPanel
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitProgressBottomSheet(
    visible: Boolean,
    habit: Habit,
    selectedDate: LocalDate?,
    today: LocalDate,
    habitRecords: List<HabitRecord>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible || selectedDate == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val record = remember(selectedDate, habitRecords) {
        habitRecords.find { it.date == selectedDate }
    }

    var currentValue by remember(selectedDate, record) {
        mutableIntStateOf(record?.completedCount ?: 0)
    }
    var currentNote by remember(selectedDate, record) {
        mutableStateOf(record?.note ?: "")
    }

    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selectedDate == today) "Today" else selectedDate.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (habit.targetCount == 1) {
                SimpleCheckHabitInputPanel(
                    isCompleted = currentValue >= 1,
                    onToggle = { isCompleted ->
                        currentValue = if (isCompleted) 1 else 0
                    },
                    accentColor = habitColor
                )
            } else {
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

            OutlinedTextField(
                value = currentNote,
                onValueChange = { currentNote = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("Add a note...") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(selectedDate, currentValue, currentNote)
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
