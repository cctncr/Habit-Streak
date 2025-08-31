package org.example.habitstreak.presentation.screen.create_edit_habit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon
import org.example.habitstreak.domain.model.RepeatUnit
import org.example.habitstreak.presentation.ui.components.common.ReminderTimeDialog
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.example.habitstreak.presentation.ui.components.selection.ColorSelectionGrid
import org.example.habitstreak.presentation.ui.components.selection.IconSelectionGrid
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.presentation.ui.utils.navigationBarsPadding
import org.example.habitstreak.presentation.viewmodel.CreateEditHabitViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun CreateEditHabitScreen(
    habitId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CreateEditHabitViewModel = koinViewModel { parametersOf(habitId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4
    val stepProgress by animateFloatAsState(
        targetValue = (currentStep + 1) / totalSteps.toFloat(),
        animationSpec = tween(500),
        label = "progress"
    )

    var showIconSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showFrequencySheet by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val isFormValid = uiState.title.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.isEditMode) "Edit Habit" else "Create Habit",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!uiState.isEditMode) {
                            Text(
                                text = "Step ${currentStep + 1} of $totalSteps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveHabit(onSuccess = onNavigateBack)
                        },
                        enabled = isFormValid && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.isEditMode) "Save" else "Create",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Progress Indicator
                if (!uiState.isEditMode) {
                    LinearProgressIndicator(
                        progress = { stepProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Step Navigation (for create mode)
                    if (!uiState.isEditMode) {
                        StepIndicator(
                            currentStep = currentStep,
                            totalSteps = totalSteps,
                            onStepClick = { step ->
                                if (step <= currentStep || isFormValid) {
                                    currentStep = step
                                }
                            }
                        )
                    }

                    // Step Content
                    AnimatedContent(
                        targetState = if (uiState.isEditMode) 0 else currentStep,
                        transitionSpec = {
                            slideInHorizontally { width -> width } + fadeIn() with
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        },
                        label = "step"
                    ) { step ->
                        when (step) {
                            0 -> BasicInfoStep(
                                uiState = uiState,
                                viewModel = viewModel,
                                onShowIconSheet = { showIconSheet = true },
                                onShowColorSheet = { showColorSheet = true }
                            )

                            1 -> FrequencyStep(
                                uiState = uiState,
                                viewModel = viewModel,
                                onShowFrequencySheet = { showFrequencySheet = true }
                            )

                            2 -> GoalStep(
                                uiState = uiState,
                                viewModel = viewModel
                            )

                            3 -> ReminderStep(
                                uiState = uiState,
                                viewModel = viewModel,
                                onShowReminderDialog = { showReminderDialog = true }
                            )
                        }
                    }

                    // Advanced Settings
                    if (uiState.isEditMode) {
                        Card(
                            onClick = { showAdvancedSettings = !showAdvancedSettings },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Advanced Settings",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(
                                    imageVector = if (showAdvancedSettings)
                                        Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAdvancedSettings) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Archive option, reset streak, etc.
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Archive Habit",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "Hide this habit without deleting data",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        Switch(
                                            checked = uiState.isArchived,
                                            onCheckedChange = { viewModel.updateArchived(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Navigation Buttons (for create mode)
                    if (!uiState.isEditMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { if (currentStep > 0) currentStep-- },
                                enabled = currentStep > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Previous")
                            }

                            Button(
                                onClick = {
                                    if (currentStep < totalSteps - 1) {
                                        currentStep++
                                    } else {
                                        viewModel.saveHabit(onSuccess = onNavigateBack)
                                    }
                                },
                                enabled = isFormValid
                            ) {
                                Text(if (currentStep < totalSteps - 1) "Next" else "Create Habit")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    if (currentStep < totalSteps - 1) Icons.AutoMirrored.Filled.ArrowForward
                                    else Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    // Error Message
                    AnimatedVisibility(visible = uiState.error != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (showIconSheet) {
        ModalBottomSheet(
            onDismissRequest = { showIconSheet = false }
        ) {
            IconSelectionContent(
                selectedIcon = uiState.selectedIcon,
                selectedColor = uiState.selectedColor,
                onIconSelected = { icon ->
                    viewModel.selectIcon(icon)
                    showIconSheet = false
                }
            )
        }
    }

    if (showColorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorSheet = false }
        ) {
            ColorSelectionContent(
                selectedColor = uiState.selectedColor,
                onColorSelected = { color ->
                    viewModel.selectColor(color)
                    showColorSheet = false
                }
            )
        }
    }

    if (showFrequencySheet) {
        ModalBottomSheet(
            onDismissRequest = { showFrequencySheet = false }
        ) {
            FrequencySelectionContent(
                currentFrequency = uiState.frequency,
                onFrequencySelected = { frequency ->
                    viewModel.updateFrequency(frequency)
                    showFrequencySheet = false
                }
            )
        }
    }

    if (showReminderDialog) {
        ReminderTimeDialog(
            currentTime = uiState.reminderTime,
            onTimeSelected = { time ->
                viewModel.updateReminderTime(time)
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false }
        )
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    onStepClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(totalSteps) { step ->
            StepCircle(
                stepNumber = step + 1,
                isActive = step == currentStep,
                isCompleted = step < currentStep,
                onClick = { onStepClick(step) }
            )
            if (step < totalSteps - 1) {
                HorizontalDivider(
                    Modifier
                        .weight(1f)
                        .padding(top = 20.dp),
                    2.dp,
                    if (step < currentStep)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepCircle(
    stepNumber: Int,
    isActive: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = stepNumber.toString(),
                color = if (isActive)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun BasicInfoStep(
    uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel,
    onShowIconSheet: () -> Unit,
    onShowColorSheet: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Let's start with the basics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Habit Name") },
            placeholder = { Text("e.g., Drink Water, Read, Exercise") },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.error != null && uiState.title.isBlank(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::updateDescription,
            label = { Text("Description (Optional)") },
            placeholder = { Text("Why is this habit important to you?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SelectionCard(
                modifier = Modifier.weight(1f),
                title = "Icon",
                onClick = onShowIconSheet
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = HabitStreakTheme.habitColorToComposeColor(uiState.selectedColor)
                                .copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.selectedIcon.emoji,
                        fontSize = 32.sp
                    )
                }
            }

            SelectionCard(
                modifier = Modifier.weight(1f),
                title = "Color",
                onClick = onShowColorSheet
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = HabitStreakTheme.habitColorToComposeColor(uiState.selectedColor),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencyStep(
    uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel,
    onShowFrequencySheet: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "How often?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Choose when you want to complete this habit",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Quick frequency options
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrequencyChip(
                label = "Every Day",
                isSelected = uiState.frequency is HabitFrequency.Daily,
                onClick = { viewModel.updateFrequency(HabitFrequency.Daily) }
            )
            FrequencyChip(
                label = "Weekdays",
                isSelected = uiState.frequency is HabitFrequency.Weekly &&
                        uiState.frequency.daysOfWeek.size == 5,
                onClick = {
                    viewModel.updateFrequency(
                        HabitFrequency.Weekly(
                            setOf(
                                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                            )
                        )
                    )
                }
            )
            FrequencyChip(
                label = "Weekends",
                isSelected = uiState.frequency is HabitFrequency.Weekly &&
                        uiState.frequency.daysOfWeek.size == 2,
                onClick = {
                    viewModel.updateFrequency(
                        HabitFrequency.Weekly(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
                    )
                }
            )
            FrequencyChip(
                label = "Custom",
                isSelected = uiState.frequency is HabitFrequency.Custom,
                onClick = onShowFrequencySheet
            )
        }

        // Show selected frequency details
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = getFrequencyDescription(uiState.frequency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun GoalStep(
    uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Set your goal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "How much do you want to achieve each time?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Goal type selection
        var isCountable by remember { mutableStateOf(uiState.targetCount > 1) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = !isCountable,
                onClick = {
                    isCountable = false
                    viewModel.updateTargetCount(1)
                    viewModel.updateUnit("")
                },
                label = { Text("Simple Check") },
                leadingIcon = if (!isCountable) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = isCountable,
                onClick = { isCountable = true },
                label = { Text("Countable") },
                leadingIcon = if (isCountable) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(visible = isCountable) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = if (uiState.targetCount > 1) uiState.targetCount.toString() else "",
                        onValueChange = { value ->
                            value.toIntOrNull()?.let {
                                if (it > 0) viewModel.updateTargetCount(it)
                            }
                        },
                        label = { Text("Target Amount") },
                        placeholder = { Text("e.g., 8") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.unit,
                        onValueChange = viewModel::updateUnit,
                        label = { Text("Unit") },
                        placeholder = { Text("e.g., glasses, pages, minutes") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Preset suggestions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(getPresetGoals()) { preset ->
                        SuggestionChip(
                            onClick = {
                                viewModel.updateTargetCount(preset.count)
                                viewModel.updateUnit(preset.unit)
                            },
                            label = { Text("${preset.count} ${preset.unit}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderStep(
    uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel,
    onShowReminderDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Stay on track",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.reminderTime != null)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Reminder",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (uiState.reminderTime != null)
                                "At ${uiState.reminderTime}"
                            else
                                "Get notified to complete your habit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.reminderTime != null,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            onShowReminderDialog()
                        } else {
                            viewModel.updateReminderTime(null)
                        }
                    }
                )
            }
        }

        // Motivational messages
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pro Tip",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Setting a reminder increases habit success rate by 40%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}

@Composable
private fun IconSelectionContent(
    selectedIcon: HabitIcon,
    selectedColor: HabitColor,
    onIconSelected: (HabitIcon) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Choose Icon",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        IconSelectionGrid(
            selectedIcon = selectedIcon,
            selectedColor = selectedColor,
            onIconSelected = onIconSelected,
            modifier = Modifier.height(400.dp)
        )
    }
}

@Composable
private fun ColorSelectionContent(
    selectedColor: HabitColor,
    onColorSelected: (HabitColor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Choose Color",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ColorSelectionGrid(
            selectedColor = selectedColor,
            onColorSelected = onColorSelected,
            modifier = Modifier.height(300.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelectionContent(
    currentFrequency: HabitFrequency,
    onFrequencySelected: (HabitFrequency) -> Unit
) {
    var selectedDays by remember {
        mutableStateOf(
            when (currentFrequency) {
                is HabitFrequency.Weekly -> currentFrequency.daysOfWeek
                else -> emptySet()
            }
        )
    }

    var customInterval by remember { mutableStateOf("2") }
    var customUnit by remember { mutableStateOf(RepeatUnit.DAYS) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Custom Frequency",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly selection
        Text(
            text = "Select specific days",
            style = MaterialTheme.typography.titleMedium
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = {
                        selectedDays = if (day in selectedDays) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                    },
                    label = { Text(day.displayName) }
                )
            }
        }

        Button(
            onClick = {
                if (selectedDays.isNotEmpty()) {
                    onFrequencySelected(HabitFrequency.Weekly(selectedDays))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDays.isNotEmpty()
        ) {
            Text("Set Weekly Schedule")
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        // Custom interval
        Text(
            text = "Or set custom interval",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customInterval,
                onValueChange = { if (it.all { char -> char.isDigit() }) customInterval = it },
                label = { Text("Every") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = false,
                onExpandedChange = { }
            ) {
                OutlinedTextField(
                    value = customUnit.name.lowercase(),
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) }
                )
            }
        }

        Button(
            onClick = {
                customInterval.toIntOrNull()?.let { interval ->
                    if (interval > 0) {
                        onFrequencySelected(
                            HabitFrequency.Custom(
                                repeatInterval = interval,
                                repeatUnit = customUnit
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = customInterval.toIntOrNull()?.let { it > 0 } ?: false
        ) {
            Text("Set Custom Interval")
        }
    }
}

private fun getFrequencyDescription(frequency: HabitFrequency): String {
    return when (frequency) {
        is HabitFrequency.Daily -> "Every day"
        is HabitFrequency.Weekly -> {
            val days = frequency.daysOfWeek.sortedBy { it.ordinal }
            when {
                days.size == 7 -> "Every day"
                days.size == 5 && !days.contains(DayOfWeek.SATURDAY) && !days.contains(DayOfWeek.SUNDAY) ->
                    "Weekdays only"

                days.size == 2 && days.contains(DayOfWeek.SATURDAY) && days.contains(DayOfWeek.SUNDAY) ->
                    "Weekends only"

                else -> "Every ${days.joinToString(", ") { it.displayName }}"
            }
        }

        is HabitFrequency.Monthly -> "On days ${frequency.daysOfMonth.sorted().joinToString(", ")}"
        is HabitFrequency.Custom -> "Every ${frequency.repeatInterval} ${frequency.repeatUnit.name.lowercase()}"
    }
}

private data class PresetGoal(val count: Int, val unit: String)

private fun getPresetGoals() = listOf(
    PresetGoal(8, "glasses"),
    PresetGoal(10000, "steps"),
    PresetGoal(30, "minutes"),
    PresetGoal(20, "pages"),
    PresetGoal(5, "reps"),
    PresetGoal(1, "hour")
)