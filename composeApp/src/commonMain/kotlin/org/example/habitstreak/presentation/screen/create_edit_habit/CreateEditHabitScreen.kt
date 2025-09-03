package org.example.habitstreak.presentation.screen.create_edit_habit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalTime
import org.example.habitstreak.presentation.ui.components.common.ReminderTimeDialog
import org.example.habitstreak.presentation.ui.components.selection.ColorSelectionGrid
import org.example.habitstreak.presentation.ui.components.selection.CustomCategoryDialog
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.presentation.viewmodel.CreateEditHabitViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun CreateEditHabitScreen(
    habitId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CreateEditHabitViewModel = koinViewModel(parameters = { parametersOf(habitId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4
    val focusManager = LocalFocusManager.current

    // Bottom sheets states
    var showIconSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    // Effects
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Show error snackbar if needed
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Habit" else "Create Habit",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isEditMode || currentStep == totalSteps - 1) {
                        TextButton(
                            onClick = {
                                viewModel.saveHabit {
                                    onNavigateBack()
                                }
                            },
                            enabled = !uiState.isLoading && uiState.title.isNotBlank()
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
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.isEditMode) {
                // Step indicator for create mode
                StepIndicator(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    modifier = Modifier.padding(16.dp)
                )

                // Progress bar
                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / totalSteps },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isEditMode) {
                    // Edit mode - show all fields
                    EditModeContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onShowIconSheet = { showIconSheet = true },
                        onShowColorSheet = { showColorSheet = true },
                        onShowReminderDialog = { showReminderDialog = true }
                    )
                } else {
                    // Create mode - step by step
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() with
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() with
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }
                        }
                    ) { step ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (step) {
                                0 -> BasicInfoStep(uiState, viewModel, { showIconSheet = true }, { showColorSheet = true })
                                1 -> CategorySelectionStep(uiState, viewModel)
                                2 -> GoalSettingStep(uiState, viewModel, { showReminderDialog = true })
                                3 -> ReviewStep(uiState)
                            }
                        }
                    }
                }
            }

            // Navigation buttons for create mode
            if (!uiState.isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (currentStep > 0) currentStep-- },
                        enabled = currentStep > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Previous")
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = {
                                if (validateStep(currentStep, uiState)) {
                                    currentStep++
                                }
                            }
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.saveHabit {
                                    onNavigateBack()
                                }
                            },
                            enabled = !uiState.isLoading && uiState.title.isNotBlank()
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Create Habit")
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheets
    if (showColorSheet) {
        ModalBottomSheet(onDismissRequest = { showColorSheet = false }) {
            ColorSelectionGrid(
                uiState.selectedColor,
                onColorSelected = { color ->
                    viewModel.selectColor(color)
                    showColorSheet = false
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showColorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorSheet = false }
        ) {
            ColorSelectionGrid(
                selectedColor = uiState.selectedColor,
                onColorSelected = {
                    viewModel.selectColor(it)
                    showColorSheet = false
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Custom Category Dialog
    if (uiState.showCustomCategoryDialog) {
        CustomCategoryDialog(
            categoryName = uiState.customCategoryName,
            onCategoryNameChange = viewModel::updateCustomCategoryName,
            onConfirm = viewModel::createCustomCategory,
            onDismiss = viewModel::hideCustomCategoryDialog
        )
    }

    // Reminder Time Dialog
    if (showReminderDialog) {
        ReminderTimeDialog(
            selectedTime = uiState.reminderTime,
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            StepCircle(
                stepNumber = index + 1,
                isActive = index <= currentStep,
                isCompleted = index < currentStep
            )
            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (index < currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun StepCircle(
    stepNumber: Int,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape
            ),
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
                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun CategorySelectionStep(
    uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Choose Categories",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Select one or more categories for your habit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Available Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.availableCategories.forEach { category ->
                        val isSelected = uiState.selectedCategories.contains(category)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleCategory(category) },
                            label = {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    // Add custom category chip
                    AssistChip(
                        onClick = viewModel::showCustomCategoryDialog,
                        label = { Text("Add Custom") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.primary,
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            }
        }

        // Selected categories display
        if (uiState.selectedCategories.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Selected Categories (${uiState.selectedCategories.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.selectedCategories.forEach { category ->
                            Chip(
                                onClick = { },
                                label = { Text(category.name) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.toggleCategory(category) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalSettingStep(
    uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel,
    onShowReminderDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Set Your Goal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Target count and unit
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Daily Target",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.targetCount.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { viewModel.updateTargetCount(it) }
                        },
                        label = { Text("Count") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.unit,
                        onValueChange = viewModel::updateUnit,
                        label = { Text("Unit (Optional)") },
                        placeholder = { Text("e.g., glasses, pages, minutes") },
                        modifier = Modifier.weight(2f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true
                    )
                }

                // Quick presets
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 5, 10).forEach { count ->
                        FilterChip(
                            selected = uiState.targetCount == count,
                            onClick = { viewModel.updateTargetCount(count) },
                            label = { Text(count.toString()) }
                        )
                    }
                }
            }
        }

        // Reminder settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowReminderDialog
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Daily Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (uiState.reminderTime != null) {
                                "Set for ${formatTime(uiState.reminderTime)}"
                            } else {
                                "No reminder set"
                            },
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

        // Archive option (only in edit mode)
        if (uiState.isEditMode) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Archive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = "Archive Habit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Hide from active habits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.isArchived,
                        onCheckedChange = viewModel::updateArchived
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(
    uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Review Your Habit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = HabitStreakTheme.habitColorToComposeColor(uiState.selectedColor)
                                    .copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.selectedIcon.emoji,
                            fontSize = 24.sp
                        )
                    }

                    Column {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.description.isNotEmpty()) {
                            Text(
                                text = uiState.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Categories
                if (uiState.selectedCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.selectedCategories.forEach { category ->
                                Chip(
                                    onClick = { },
                                    label = { Text(category.name) }
                                )
                            }
                        }
                    }
                }

                // Goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Daily Goal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (uiState.unit.isNotEmpty()) {
                            "${uiState.targetCount} ${uiState.unit}"
                        } else {
                            "${uiState.targetCount} time${if (uiState.targetCount > 1) "s" else ""}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Reminder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (uiState.reminderTime != null) {
                            formatTime(uiState.reminderTime)
                        } else {
                            "Not set"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.reminderTime != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        // Success message
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Your habit is ready! Tap 'Create Habit' to start your journey.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EditModeContent(
    uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel,
    onShowIconSheet: () -> Unit,
    onShowColorSheet: () -> Unit,
    onShowReminderDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic info
        BasicInfoStep(uiState, viewModel, onShowIconSheet, onShowColorSheet)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Categories
        CategorySelectionStep(uiState, viewModel)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Goal settings
        GoalSettingStep(uiState, viewModel, onShowReminderDialog)
    }
}

@Composable
private fun SelectionCard(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Chip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            label()
            trailingIcon?.invoke()
        }
    }
}

private fun validateStep(step: Int, uiState: org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState): Boolean {
    return when (step) {
        0 -> uiState.title.isNotBlank()
        1 -> true // Categories are optional
        2 -> uiState.targetCount > 0
        else -> true
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val minute = time.minute.toString().padStart(2, '0')
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "$hour:$minute $amPm"
}