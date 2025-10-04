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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.example.habitstreak.presentation.ui.components.permission.UnifiedPermissionDialogs
import org.example.habitstreak.presentation.ui.components.permission.GlobalEnableNotificationDialog
import org.example.habitstreak.presentation.permission.PermissionFlowResult
import org.example.habitstreak.presentation.permission.rememberBasePermissionHandler
import org.example.habitstreak.domain.usecase.notification.CheckGlobalNotificationStatusUseCase
import org.example.habitstreak.domain.usecase.notification.EnableGlobalNotificationsUseCase
import org.example.habitstreak.domain.usecase.notification.UpdateNotificationPreferencesUseCase
import org.example.habitstreak.domain.usecase.notification.GetNotificationPreferencesUseCase
import org.example.habitstreak.domain.usecase.notification.NotificationOperationResult
import org.example.habitstreak.presentation.ui.components.selection.ColorSelectionGrid
import org.example.habitstreak.presentation.ui.components.selection.CustomCategoryDialog
import org.example.habitstreak.presentation.ui.components.selection.IconSelectionGrid
import org.example.habitstreak.presentation.ui.components.NotificationSettingsCard
import org.example.habitstreak.presentation.screen.create_edit_habit.components.AdvancedFrequencyDialog
import org.example.habitstreak.presentation.screen.create_edit_habit.components.getFrequencyDisplayText
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.presentation.ui.utils.navigationBarsPadding
import org.example.habitstreak.presentation.viewmodel.CreateEditHabitViewModel
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
@Composable
fun CreateEditHabitScreen(
    habitId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CreateEditHabitViewModel = koinViewModel(key = habitId) { parametersOf(habitId) },
    checkGlobalNotificationStatusUseCase: CheckGlobalNotificationStatusUseCase = koinInject(),
    enableGlobalNotificationsUseCase: EnableGlobalNotificationsUseCase = koinInject(),
    updateNotificationPreferencesUseCase: UpdateNotificationPreferencesUseCase = koinInject(),
    getNotificationPreferencesUseCase: GetNotificationPreferencesUseCase = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember(habitId) { mutableStateOf(0) }
    val totalSteps = 4
    val stepProgress by animateFloatAsState(
        targetValue = (currentStep + 1) / totalSteps.toFloat(),
        animationSpec = tween(500),
        label = "progress"
    )

    var showIconSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var permissionDialogState by remember { mutableStateOf<PermissionFlowResult?>(null) }
    var showGlobalEnableDialog by remember { mutableStateOf<String?>(null) }

    // Notification preferences state
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    // Load preferences on init
    LaunchedEffect(Unit) {
        try {
            val prefs = getNotificationPreferencesUseCase.execute()
            soundEnabled = prefs.soundEnabled
            vibrationEnabled = prefs.vibrationEnabled
        } catch (e: Exception) {
            // Use defaults
        }
    }

    // Unified permission handler
    val permissionHandler = rememberBasePermissionHandler(
        habitName = uiState.title.ifBlank { null }
    ) { granted, canAskAgain ->
        // Platform permission result handling will be done via handler internal logic
        if (granted) {
            viewModel.updateNotificationEnabled(true)
        }
    }

    val isFormValid = uiState.title.isNotBlank()


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.isEditMode) stringResource(Res.string.edit_habit_title) else stringResource(Res.string.create_habit_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!uiState.isEditMode) {
                            Text(
                                text = stringResource(Res.string.step_indicator, currentStep + 1, totalSteps),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        if (!uiState.isEditMode && currentStep > 0) {
                            currentStep--
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (!uiState.isEditMode && currentStep > 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = if (!uiState.isEditMode && currentStep > 0) stringResource(Res.string.previous) else stringResource(Res.string.close)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveHabit(onSuccess = {
                                viewModel.resetForm()
                                onNavigateBack()
                            })
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
                                text = if (uiState.isEditMode) stringResource(Res.string.save) else stringResource(Res.string.action_create),
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
                                onShowFrequencyDialog = { showFrequencyDialog = true }
                            )

                            2 -> CategorySelectionStep(
                                uiState = uiState,
                                viewModel = viewModel,
                            )

                            3 -> GoalStep(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                    }

                    // Advanced Settings (Edit mode only)
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
                                    text = stringResource(Res.string.advanced_settings),
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
                                                text = stringResource(Res.string.archive_habit),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = stringResource(Res.string.archive_habit_description),
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
                                Text(stringResource(Res.string.previous))
                            }

                            Button(
                                onClick = {
                                    if (currentStep < totalSteps - 1) {
                                        currentStep++
                                    } else {
                                        viewModel.saveHabit(onSuccess = {
                                            viewModel.resetForm()
                                            onNavigateBack()
                                        })
                                    }
                                },
                                enabled = isFormValid
                            ) {
                                Text(if (currentStep < totalSteps - 1) stringResource(Res.string.next) else stringResource(Res.string.create_habit_button))
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

    if (showFrequencyDialog) {
        AdvancedFrequencyDialog(
            currentFrequency = uiState.frequency,
            onFrequencySelected = { frequency ->
                viewModel.updateFrequency(frequency)
                showFrequencyDialog = false
            },
            onDismiss = { showFrequencyDialog = false }
        )
    }

    // Unified permission dialogs
    UnifiedPermissionDialogs(
        dialogState = permissionDialogState,
        permissionHandler = permissionHandler,
        onDismiss = { permissionDialogState = null }
    )

    // Global notification enable dialog - using shared component
    if (showGlobalEnableDialog != null) {
        GlobalEnableNotificationDialog(
            habitName = showGlobalEnableDialog,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            onSoundChanged = { soundEnabled = it },
            onVibrationChanged = { vibrationEnabled = it },
            onConfirm = {
                showGlobalEnableDialog = null
                coroutineScope.launch {
                    // Save preferences first
                    try {
                        updateNotificationPreferencesUseCase.updateBoth(soundEnabled, vibrationEnabled)
                    } catch (e: Exception) {
                        // Error saving preferences
                    }

                    // Then enable global notifications
                    when (val result = enableGlobalNotificationsUseCase.execute()) {
                        is NotificationOperationResult.Success,
                        is NotificationOperationResult.PartialSuccess -> {
                            viewModel.updateNotificationEnabled(true)
                        }
                        is NotificationOperationResult.Error -> {
                            // Error enabling global notifications
                        }
                        is NotificationOperationResult.PermissionRequired -> {
                            // Permission required
                        }
                    }
                }
            },
            onDismiss = { showGlobalEnableDialog = null }
        )
    }

    // Custom Category Dialog
    if (uiState.showCustomCategoryDialog) {
        CustomCategoryDialog(
            categoryName = uiState.customCategoryName,
            onCategoryNameChange = viewModel::updateCustomCategoryName,
            onConfirm = {
                viewModel.createCustomCategory()
            },
            onDismiss = { viewModel.hideCustomCategoryDialog() }
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
            text = stringResource(Res.string.lets_start_with_basics),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::updateTitle,
            label = { Text(stringResource(Res.string.habit_name_label)) },
            placeholder = { Text(stringResource(Res.string.habit_name_placeholder)) },
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
            label = { Text(stringResource(Res.string.habit_description_label)) },
            placeholder = { Text(stringResource(Res.string.habit_description_placeholder)) },
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
                title = stringResource(Res.string.icon_selection_title),
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
                title = stringResource(Res.string.color_selection_title),
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

@Composable
private fun FrequencyStep(
    uiState: CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel,
    onShowFrequencyDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.frequency_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.frequency_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Current Frequency Display
        Card(
            onClick = onShowFrequencyDialog,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (uiState.frequency) {
                            is org.example.habitstreak.domain.model.HabitFrequency.Daily ->
                                stringResource(Res.string.frequency_daily)
                            is org.example.habitstreak.domain.model.HabitFrequency.Weekly ->
                                stringResource(Res.string.frequency_weekly)
                            is org.example.habitstreak.domain.model.HabitFrequency.Monthly ->
                                stringResource(Res.string.frequency_monthly)
                            is org.example.habitstreak.domain.model.HabitFrequency.Custom ->
                                stringResource(Res.string.frequency_custom)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = getFrequencyDisplayText(uiState.frequency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Frequency options grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.heightIn(max = 200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FrequencyCard(
                    title = stringResource(Res.string.frequency_daily),
                    description = stringResource(Res.string.frequency_daily_desc),
                    isSelected = uiState.frequency is org.example.habitstreak.domain.model.HabitFrequency.Daily,
                    onClick = {
                        viewModel.updateFrequency(org.example.habitstreak.domain.model.HabitFrequency.Daily)
                    }
                )
            }
            item {
                FrequencyCard(
                    title = stringResource(Res.string.frequency_weekly),
                    description = if (uiState.frequency is org.example.habitstreak.domain.model.HabitFrequency.Weekly) {
                        getFrequencyDisplayText(uiState.frequency)
                    } else {
                        stringResource(Res.string.weekly_frequency_subtitle)
                    },
                    isSelected = uiState.frequency is org.example.habitstreak.domain.model.HabitFrequency.Weekly,
                    onClick = onShowFrequencyDialog
                )
            }
            item {
                FrequencyCard(
                    title = stringResource(Res.string.frequency_monthly),
                    description = if (uiState.frequency is org.example.habitstreak.domain.model.HabitFrequency.Monthly) {
                        getFrequencyDisplayText(uiState.frequency)
                    } else {
                        stringResource(Res.string.monthly_frequency_subtitle)
                    },
                    isSelected = uiState.frequency is org.example.habitstreak.domain.model.HabitFrequency.Monthly,
                    onClick = onShowFrequencyDialog
                )
            }
            item {
                FrequencyCard(
                    title = stringResource(Res.string.frequency_custom),
                    description = if (uiState.frequency is org.example.habitstreak.domain.model.HabitFrequency.Custom) {
                        getFrequencyDisplayText(uiState.frequency)
                    } else {
                        stringResource(Res.string.custom_frequency_subtitle)
                    },
                    isSelected = uiState.frequency is org.example.habitstreak.domain.model.HabitFrequency.Custom,
                    onClick = onShowFrequencyDialog
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelectionStep(
    uiState: CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.choose_categories_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(Res.string.select_one_or_more_categories),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Available Categories
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.available_categories),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Existing categories
                    uiState.availableCategories.forEach { category ->
                        val isSelected = uiState.selectedCategories.contains(category)
                        FilterChip(
                            modifier = Modifier.combinedClickable(
                                onClick = { viewModel.toggleCategory(category) },
                            ),
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
                                containerColor = if (category.isCustom) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                        )
                    }

                    // Add custom category chip - FilterChip olarak değiştirildi
                    FilterChip(
                        selected = false,
                        onClick = viewModel::showCustomCategoryDialog,
                        label = {
                            Text(
                                stringResource(Res.string.add_custom_category),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.primary,
                            iconColor = MaterialTheme.colorScheme.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.primary,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 1.dp,
                            enabled = true,
                            selected = false
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.selected_categories),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.selectedCategories.forEach { category ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.toggleCategory(category) },
                                label = {
                                    Text(
                                        category.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove category",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Text(
                        text = stringResource(Res.string.tap_to_remove_category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalStep(
    uiState: CreateEditHabitUiState,
    viewModel: CreateEditHabitViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(Res.string.set_your_goal),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(Res.string.how_much_to_achieve),
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
                label = { Text(stringResource(Res.string.simple_check)) },
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
                label = { Text(stringResource(Res.string.countable)) },
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
                        label = { Text(stringResource(Res.string.target_amount_label)) },
                        placeholder = { Text(stringResource(Res.string.target_amount_hint)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.unit,
                        onValueChange = viewModel::updateUnit,
                        label = { Text(stringResource(Res.string.unit_label)) },
                        placeholder = { Text(stringResource(Res.string.unit_hint)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Preset suggestions
                val presetGoals = getPresetGoals()
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetGoals) { preset ->
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

        Spacer(modifier = Modifier.height(8.dp))

        // Notification Settings
        NotificationSettingsCard(
            isEnabled = uiState.isNotificationEnabled,
            notificationTime = uiState.reminderTime,
            notificationError = uiState.notificationError,
            notificationPeriod = uiState.notificationPeriod,
            isGlobalNotificationEnabled = uiState.isGlobalNotificationEnabled,
            habitFrequency = uiState.frequency,
            onToggleEnabled = { enabled ->
                if (enabled) {
                    // Just enable the switch - actual notification will be created on save
                    viewModel.updateNotificationEnabled(true)
                } else {
                    viewModel.updateNotificationEnabled(false)
                    viewModel.updateReminderTime(null)
                }
            },
            onTimeAndPeriodChanged = { time, period ->
                // Update both time and period together atomically
                viewModel.updateReminderTimeAndPeriod(time, period)
            },
            onEnableGlobalNotifications = {
                // Not applicable for create/edit screen - global notifications managed in settings
            },
            onErrorDismiss = {
                viewModel.clearNotificationError()
            },
            onOpenSettings = {
                // Not applicable for create/edit screen
            }
        )
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

@Composable
private fun IconSelectionContent(
    selectedIcon: org.example.habitstreak.domain.model.HabitIcon,
    selectedColor: org.example.habitstreak.domain.model.HabitColor,
    onIconSelected: (org.example.habitstreak.domain.model.HabitIcon) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(Res.string.choose_icon_title),
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
    selectedColor: org.example.habitstreak.domain.model.HabitColor,
    onColorSelected: (org.example.habitstreak.domain.model.HabitColor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(Res.string.choose_color_title),
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

private data class PresetGoal(val count: Int, val unit: String)

@Composable
private fun getPresetGoals(): List<PresetGoal> {
    return listOf(
        PresetGoal(8, stringResource(Res.string.preset_glasses)),
        PresetGoal(10000, stringResource(Res.string.preset_steps)),
        PresetGoal(30, stringResource(Res.string.preset_minutes)),
        PresetGoal(20, stringResource(Res.string.preset_pages)),
        PresetGoal(5, stringResource(Res.string.preset_reps)),
        PresetGoal(3, stringResource(Res.string.preset_hour))
    )
}

@Composable
private fun FrequencyCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                }
            )
        }
    }
}

@Composable
private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val minute = time.minute.toString().padStart(2, '0')
    val amPm = if (time.hour < 12) stringResource(Res.string.time_am) else stringResource(Res.string.time_pm)
    return "$hour:$minute $amPm"
}