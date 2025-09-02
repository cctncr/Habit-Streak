package org.example.habitstreak.presentation.screen.habit_detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.HabitType
import org.example.habitstreak.domain.model.getType
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.presentation.model.YearMonth
import org.example.habitstreak.presentation.ui.components.NotificationSettingsCard
import org.example.habitstreak.presentation.ui.theme.AppTheme
import org.example.habitstreak.presentation.ui.utils.formatShort
import org.example.habitstreak.presentation.ui.utils.formatLong
import org.example.habitstreak.presentation.ui.utils.formatRelative
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime

enum class ActivityTab(val label: String) {
    HISTORY("History"),
    NOTES("Notes")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: HabitDetailViewModel = koinViewModel(key = habitId) { parametersOf(habitId) },
    dateProvider: DateProvider = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedActivityTab by remember { mutableStateOf(ActivityTab.HISTORY) }
    val today = dateProvider.today()

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HabitDetailViewModel.UiEvent.RequestNotificationPermission -> {
                    println("Permission request needed - integrate with platform-specific handler")
                }
                is HabitDetailViewModel.UiEvent.OpenAppSettings -> {
                    println("Opening app settings - integrate with platform-specific handler")
                }
            }
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                MinimalTopBar(
                    habit = uiState.habit,
                    stats = uiState.stats,
                    onNavigateBack = onNavigateBack,
                    onEdit = onNavigateToEdit,
                    onDelete = { showDeleteDialog = true }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                uiState.habit?.let { habit ->
                    // Habit Header
                    HabitHeader(
                        habit = habit,
                        stats = uiState.stats,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )

                    // Calendar with navigation chips
                    CalendarSection(
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
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        // Tab Selector
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                ActivityTab.values().forEach { tab ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selectedActivityTab == tab)
                                                    MaterialTheme.colorScheme.surface
                                                else Color.Transparent
                                            )
                                            .clickable { selectedActivityTab = tab }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selectedActivityTab == tab)
                                                FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

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
                                        records = uiState.records,
                                        habit = habit,
                                        onDateClick = { date ->
                                            selectedDate = date
                                            coroutineScope.launch {
                                                bottomSheetState.show()
                                            }
                                        }
                                    )
                                }
                                ActivityTab.NOTES -> {
                                    NotesList(
                                        records = uiState.records.filter { !it.note.isNullOrBlank() },
                                        onDateClick = { date ->
                                            selectedDate = date
                                            coroutineScope.launch {
                                                bottomSheetState.show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

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
                title = { Text("Delete Habit?") },
                text = { Text("This will permanently delete the habit and all its records.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHabit()
                            onNavigateBack()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalTopBar(
    habit: Habit?,
    stats: HabitDetailViewModel.HabitStats,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            habit?.let {
                Column {
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (stats.currentStreak > 0) {
                        Text(
                            text = "${stats.currentStreak} day streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun HabitHeader(
    habit: Habit,
    stats: HabitDetailViewModel.HabitStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(habit.color.composeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = habit.icon.emoji,
                fontSize = 24.sp
            )
        }

        // Title and Description
        Column(modifier = Modifier.weight(1f)) {
            if (!habit.description.isNullOrBlank()) {
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Current Streak Badge
        if (stats.currentStreak > 0) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${stats.currentStreak}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun CalendarSection(
    currentMonth: YearMonth,
    records: List<HabitRecord>,
    habit: Habit,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val createdDate = habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val createdMonth = YearMonth(createdDate.year, createdDate.monthNumber)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onMonthChange(currentMonth.previous()) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous month",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${getMonthName(currentMonth.month)} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = { onMonthChange(currentMonth.next()) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next month",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day labels - Starting from Monday
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val daysInMonth = getDaysInMonth(currentMonth.year, currentMonth.month)
            val firstDayOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
            // Convert to Monday-based (1=Monday, 7=Sunday)
            val firstDayOfWeek = when (firstDayOfMonth.dayOfWeek) {
                DayOfWeek.MONDAY -> 0
                DayOfWeek.TUESDAY -> 1
                DayOfWeek.WEDNESDAY -> 2
                DayOfWeek.THURSDAY -> 3
                DayOfWeek.FRIDAY -> 4
                DayOfWeek.SATURDAY -> 5
                DayOfWeek.SUNDAY -> 6
            }

            val totalCells = ((daysInMonth + firstDayOfWeek - 1) / 7 + 1) * 7

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height((totalCells / 7 * 42).dp),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(totalCells) { index ->
                    val dayNumber = index - firstDayOfWeek + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = LocalDate(currentMonth.year, currentMonth.month, dayNumber)
                        val record = records.find { it.date == date }
                        val targetCount = habit.targetCount.coerceAtLeast(1)
                        val completionRate = when {
                            record == null -> 0f
                            habit.getType() == HabitType.COUNTABLE ->
                                (record.completedCount.toFloat() / targetCount).coerceIn(0f, 1f)
                            else -> if (record.completedCount > 0) 1f else 0f
                        }

                        DayCell(
                            dayNumber = dayNumber,
                            date = date,
                            completionRate = completionRate,
                            isToday = date == today,
                            isFuture = date > today,
                            hasNote = !record?.note.isNullOrBlank(),
                            onClick = { onDateSelected(date) }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = { onMonthChange(createdMonth) },
                    label = { Text("Created") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )

                AssistChip(
                    onClick = { onMonthChange(YearMonth(today.year, today.monthNumber)) },
                    label = { Text("Today") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    date: LocalDate,
    completionRate: Float,
    isToday: Boolean,
    isFuture: Boolean,
    hasNote: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        completionRate >= 1f -> MaterialTheme.colorScheme.primary
        completionRate > 0.5f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        completionRate > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val textColor = when {
        completionRate >= 0.5f -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isToday) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (hasNote) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
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
                        text = record.date.dayOfMonth.toString(),
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
                if (!record.note.isNullOrBlank()) {
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Progress Percentage
            val percentage = ((record.completedCount.toFloat() / habit.targetCount.coerceAtLeast(1)) * 100).toInt()
            Surface(
                shape = CircleShape,
                color = if (percentage >= 100)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${percentage.coerceAtMost(999)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (percentage >= 100)
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
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
                    text = record.note ?: "",
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentValue > 0)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentValue = if (currentValue > 0) 0 else 1
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentValue > 0) "Completed" else "Not Completed",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = currentValue > 0,
                                onCheckedChange = { checked ->
                                    currentValue = if (checked) 1 else 0
                                }
                            )
                        }
                    }
                }
                HabitType.COUNTABLE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Improved counter UX
                        if (!showDirectInput) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Decrease buttons
                                FilledTonalIconButton(
                                    onClick = { if (currentValue >= 10) currentValue -= 10 },
                                    enabled = currentValue >= 10,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-10", style = MaterialTheme.typography.labelSmall)
                                }

                                FilledIconButton(
                                    onClick = { if (currentValue > 0) currentValue-- },
                                    enabled = currentValue > 0,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }

                                // Current value (clickable for direct input)
                                Surface(
                                    onClick = { showDirectInput = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.widthIn(min = 80.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = currentValue.toString(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "/ ${habit.targetCount} ${habit.unit ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Increase buttons
                                FilledIconButton(
                                    onClick = { currentValue++ },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }

                                FilledTonalIconButton(
                                    onClick = { currentValue += 10 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+10", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            // Direct input mode
                            OutlinedTextField(
                                value = currentValue.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let {
                                        if (it >= 0) currentValue = it
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        showDirectInput = false
                                        keyboardController?.hide()
                                    }
                                ),
                                modifier = Modifier
                                    .width(120.dp)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            LaunchedEffect(showDirectInput) {
                                if (showDirectInput) {
                                    focusRequester.requestFocus()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick preset buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0, habit.targetCount / 2, habit.targetCount, habit.targetCount * 2)
                                .distinct()
                                .filter { it >= 0 }
                                .forEach { value ->
                                    AssistChip(
                                        onClick = { currentValue = value },
                                        label = { Text(value.toString()) },
                                        modifier = Modifier.height(32.dp)
                                    )
                                }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = (currentValue.toFloat() / habit.targetCount).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
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
private fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }
}

private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    val date = LocalDate(year, month, 1)
    return date.dayOfWeek.ordinal
}

private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
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