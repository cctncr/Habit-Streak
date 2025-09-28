package org.example.habitstreak.presentation.screen.habit_detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.example.habitstreak.core.extensions.formatLong
import org.example.habitstreak.presentation.permission.PermissionFlowHandler
import org.example.habitstreak.presentation.permission.PermissionContext
import org.example.habitstreak.presentation.permission.PermissionFlowResult
import org.example.habitstreak.core.extensions.formatRelative
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.HabitType
import org.example.habitstreak.domain.model.getType
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.presentation.model.YearMonth
import org.example.habitstreak.presentation.ui.components.input.SimpleCheckHabitInputPanel
import org.example.habitstreak.presentation.ui.components.input.CountableHabitInputPanel
import org.example.habitstreak.presentation.ui.components.NotificationSettingsCard
import org.example.habitstreak.presentation.ui.components.permission.PermissionRationaleDialog
import org.example.habitstreak.presentation.ui.components.permission.SettingsNavigationDialog
import org.example.habitstreak.presentation.ui.theme.AppTheme
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel
import org.example.habitstreak.presentation.screen.habit_detail.components.HabitDetailCalendar
import org.example.habitstreak.presentation.screen.habit_detail.components.HabitDetailActivity
import org.example.habitstreak.presentation.screen.habit_detail.components.HabitDetailHeader
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

enum class ActivityTab {
    HISTORY,
    NOTES
}

@Composable
fun ActivityTab.getLabel(): String = when (this) {
    ActivityTab.HISTORY -> stringResource(Res.string.history)
    ActivityTab.NOTES -> stringResource(Res.string.notes)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: HabitDetailViewModel = koinViewModel(key = habitId) { parametersOf(habitId) },
    dateProvider: DateProvider = koinInject(),
    permissionFlowHandler: PermissionFlowHandler = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedActivityTab by remember { mutableStateOf(ActivityTab.HISTORY) }

    // Permission dialog state
    var permissionDialogState by remember { mutableStateOf<PermissionFlowResult?>(null) }
    val today = dateProvider.today()

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HabitDetailViewModel.UiEvent.RequestNotificationPermission -> {
                    coroutineScope.launch {
                        permissionFlowHandler.requestPermissionWithFlow(
                            context = PermissionContext.HABIT_DETAIL,
                            habitName = uiState.habit?.title,
                            onResult = { result ->
                                // Handle permission result
                                println("Permission result: $result")
                                permissionDialogState = result
                            }
                        )
                    }
                }

                is HabitDetailViewModel.UiEvent.OpenAppSettings -> {
                    coroutineScope.launch {
                        try {
                            val success = permissionFlowHandler.handleOpenSettings(PermissionContext.HABIT_DETAIL)
                            if (!success) {
                                // Handle failure case if needed
                                println("Failed to open settings")
                            }
                        } catch (e: Exception) {
                            println("Error opening settings: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    AppTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            HabitDetailHeader(
                habit = uiState.habit,
                stats = uiState.stats,
                onNavigateBack = onNavigateBack,
                onEdit = onNavigateToEdit,
                onDelete = { showDeleteDialog = true }
            )

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                uiState.habit?.let { habit ->

                    // Calendar with navigation chips
                    HabitDetailCalendar(
                        currentMonth = uiState.selectedMonth,
                        records = uiState.records,
                        habit = habit,
                        today = today,
                        onDateSelected = { date ->
                            selectedDate = date
                            coroutineScope.launch {
                                bottomSheetState.show()
                            }
                        },
                        onMonthChange = viewModel::changeMonth,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Notification Settings
                    NotificationSettingsCard(
                        isEnabled = uiState.isNotificationEnabled,
                        notificationTime = uiState.notificationTime,
                        notificationError = uiState.notificationError,
                        onToggleEnabled = { enabled ->
                            viewModel.toggleNotification(enabled)
                        },
                        onTimeChanged = { time ->
                            viewModel.updateNotificationTime(time)
                        },
                        onErrorDismiss = {
                            viewModel.clearNotificationError()
                        },
                        onOpenSettings = {
                            viewModel.openAppSettings()
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Activity Tabs
                    HabitDetailActivity(
                        records = uiState.records,
                        habit = habit,
                        selectedActivityTab = selectedActivityTab,
                        onTabChanged = { selectedActivityTab = it },
                        onDateClick = { date ->
                            selectedDate = date
                            coroutineScope.launch {
                                bottomSheetState.show()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Date Detail Bottom Sheet
        selectedDate?.let { date ->
            if (bottomSheetState.isVisible) {
                ModalBottomSheet(
                    onDismissRequest = {
                        selectedDate = null
                        coroutineScope.launch {
                            bottomSheetState.hide()
                        }
                    },
                    sheetState = bottomSheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    uiState.habit?.let { habit ->
                        val record = uiState.records.find { it.date == date }
                        DateDetailSheet(
                            date = date,
                            habit = habit,
                            record = record,
                            today = today,
                            onUpdateProgress = { progress, note ->
                                viewModel.updateProgress(date, progress, note)
                            },
                            onDeleteRecord = {
                                viewModel.deleteRecord(date)
                                selectedDate = null
                                coroutineScope.launch {
                                    bottomSheetState.hide()
                                }
                            },
                            onClose = {
                                selectedDate = null
                                coroutineScope.launch {
                                    bottomSheetState.hide()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(Res.string.delete_habit_title)) },
                text = { Text(stringResource(Res.string.delete_habit_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHabit()
                            onNavigateBack()
                        }
                    ) {
                        Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                }
            )
        }

        // Permission dialogs
        when (val dialogState = permissionDialogState) {
            is PermissionFlowResult.ShowRationaleDialog -> {
                PermissionRationaleDialog(
                    context = dialogState.context,
                    rationaleMessage = dialogState.rationaleMessage,
                    benefitMessage = dialogState.benefitMessage,
                    habitName = uiState.habit?.title,
                    onRequestPermission = {
                        coroutineScope.launch {
                            permissionFlowHandler.handleSystemPermissionResult(
                                context = PermissionContext.HABIT_DETAIL,
                                habitName = uiState.habit?.title,
                                onResult = { result ->
                                    permissionDialogState = result
                                    if (result is PermissionFlowResult.PermissionGranted) {
                                        // Retry enabling notification after permission granted
                                        viewModel.toggleNotification(true)
                                    }
                                }
                            )
                        }
                    },
                    onDismiss = {
                        permissionDialogState = null
                    },
                    onNeverAskAgain = {
                        permissionFlowHandler.handleNeverAskAgain(PermissionContext.HABIT_DETAIL)
                        permissionDialogState = null
                    }
                )
            }

            is PermissionFlowResult.ShowSettingsDialog -> {
                SettingsNavigationDialog(
                    context = PermissionContext.HABIT_DETAIL,
                    message = dialogState.message,
                    onOpenSettings = {
                        coroutineScope.launch {
                            permissionFlowHandler.handleOpenSettings(PermissionContext.HABIT_DETAIL)
                            permissionDialogState = null
                        }
                    },
                    onDismiss = {
                        permissionDialogState = null
                    },
                    onDisableFeature = {
                        // User chooses to disable notifications feature
                        permissionDialogState = null
                    }
                )
            }

            else -> {
                // Handle other dialog states if needed
                if (dialogState is PermissionFlowResult.PermissionGranted ||
                    dialogState is PermissionFlowResult.Granted) {
                    permissionDialogState = null
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun DateDetailSheet(
    date: LocalDate,
    habit: Habit,
    record: HabitRecord?,
    today: LocalDate,
    onUpdateProgress: (Int, String?) -> Unit,
    onDeleteRecord: () -> Unit,
    onClose: () -> Unit
) {
    val isFuture = date > today
    var currentValue by remember(record) {
        mutableStateOf(record?.completedCount ?: 0)
    }
    var note by remember(record) {
        mutableStateOf(record?.note ?: "")
    }
    var showDirectInput by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatLong(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (date == today) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Section (not shown for future dates)
        if (!isFuture) {
            when (habit.getType()) {
                HabitType.YES_NO -> {
                    SimpleCheckHabitInputPanel(
                        isCompleted = currentValue > 0,
                        onToggle = { isCompleted ->
                            currentValue = if (isCompleted) 1 else 0
                        },
                        accentColor = habit.color.composeColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HabitType.COUNTABLE -> {
                    CountableHabitInputPanel(
                        currentValue = currentValue,
                        targetCount = habit.targetCount,
                        unit = habit.unit,
                        onValueChange = { newValue ->
                            currentValue = newValue
                        },
                        onReset = {
                            currentValue = 0
                        },
                        onFillDay = {
                            currentValue = habit.targetCount
                        },
                        accentColor = habit.color.composeColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Note Section (available for all dates)
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 3,
            placeholder = {
                Text(if (isFuture) "Add a note for this future date..." else "Add a note about this day...")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Delete button if record exists
            if (record != null) {
                OutlinedButton(
                    onClick = onDeleteRecord,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }

            // Save Button
            Button(
                onClick = {
                    onUpdateProgress(
                        if (isFuture) 0 else currentValue,
                        note.takeIf { it.isNotBlank() }
                    )
                    onClose()
                },
                modifier = Modifier.weight(if (record != null) 1f else 2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Helper functions

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