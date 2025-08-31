package org.example.habitstreak.presentation.screen.habit_detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.presentation.ui.components.NotificationSettingsCard
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.presentation.ui.utils.DateFormatter
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime

// Enum definitions
enum class StatsTimeFilter(val label: String) {
    ALL_TIME("All Time"),
    THIS_YEAR("This Year"),
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    LAST_30_DAYS("Last 30 Days")
}

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

    var selectedDate by remember(habitId) { mutableStateOf<LocalDate?>(null) }
    var showDeleteDialog by remember(habitId) { mutableStateOf(false) }
    var selectedTab by remember(habitId) { mutableStateOf(ActivityTab.HISTORY) }
    var statsFilter by remember(habitId) { mutableStateOf(StatsTimeFilter.ALL_TIME) }

    LaunchedEffect(habitId) {
        selectedDate = null
        selectedTab = ActivityTab.HISTORY
        statsFilter = StatsTimeFilter.ALL_TIME
        viewModel.loadData()
    }

    uiState.habit?.let { habit ->
        Scaffold(
            topBar = {
                HabitDetailTopBar(
                    habit = habit,
                    statistics = uiState.statistics,
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
                if (habit.description.isNotBlank()) {
                    HabitDescriptionStrip(
                        description = habit.description,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                CalendarView(
                    currentMonth = uiState.selectedMonth,
                    records = uiState.records,
                    habit = habit,
                    selectedDate = selectedDate,
                    today = dateProvider.today(),
                    onDateSelected = { date ->
                        selectedDate = date
                        coroutineScope.launch {
                            bottomSheetState.show()
                        }
                    },
                    onMonthChange = viewModel::changeMonth,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                NotificationSettingsCard(
                    isEnabled = uiState.isNotificationEnabled,
                    notificationTime = uiState.notificationTime,
                    onToggleEnabled = { enabled ->
                        viewModel.toggleNotification(enabled)
                    },
                    onTimeChanged = { time ->
                        viewModel.updateNotificationTime(time)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats with Filter
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Time Filter
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(StatsTimeFilter.entries.toTypedArray()) { filter ->
                            FilterChip(
                                selected = statsFilter == filter,
                                onClick = {
                                    statsFilter = filter
                                    viewModel.updateStatsFilter(filter)
                                },
                                label = { Text(filter.label) },
                                leadingIcon = if (statsFilter == filter) {
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
                    }

                    // Stats Cards
                    StatsCards(
                        statistics = uiState.filteredStatistics ?: uiState.statistics,
                        habit = habit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Activity/Notes Toggle and Content
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Toggle Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActivityTab.entries.forEach { tab ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (selectedTab == tab)
                                            MaterialTheme.colorScheme.surface
                                        else
                                            Color.Transparent
                                    )
                                    .clickable { selectedTab = tab }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content based on selected tab
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_content"
                    ) { tab ->
                        when (tab) {
                            ActivityTab.HISTORY -> {
                                ActivityHistory(
                                    records = uiState.records,
                                    habit = habit,
                                    today = dateProvider.today(),
                                    onRecordClick = { record ->
                                        selectedDate = record.date
                                        coroutineScope.launch {
                                            bottomSheetState.show()
                                        }
                                    }
                                )
                            }
                            ActivityTab.NOTES -> {
                                NotesHistory(
                                    records = uiState.records,
                                    today = dateProvider.today(),
                                    onNoteClick = { record ->
                                        selectedDate = record.date
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

        // Selected Date Bottom Sheet
        if (selectedDate != null) {
            SelectedDateBottomSheet(
                date = selectedDate!!,
                habit = habit,
                record = uiState.records.find { it.date == selectedDate },
                sheetState = bottomSheetState,
                today = dateProvider.today(),
                onDismiss = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        selectedDate = null
                    }
                },
                onUpdateProgress = { value ->
                    viewModel.updateProgress(selectedDate!!, value)
                },
                onUpdateNote = { note ->
                    val record = uiState.records.find { it.date == selectedDate }
                    viewModel.updateProgress(selectedDate!!, record?.completedCount ?: 0, note)
                },
                onDeleteRecord = {
                    viewModel.deleteRecord(selectedDate!!)
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        selectedDate = null
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Habit") },
                text = { Text("Are you sure you want to delete this habit? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHabit {
                                onNavigateBack()
                            }
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete")
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

    // Show loading or error states
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.habit == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Habit not found")
            }
        }
    }

    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitDetailTopBar(
    habit: Habit,
    statistics: HabitDetailViewModel.HabitStats?,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = habit.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                statistics?.let { stats ->
                    Text(
                        text = "${stats.currentStreak} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit habit"
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = HabitStreakTheme.habitColorToComposeColor(habit.color).copy(alpha = 0.1f)
        )
    )
}

@Composable
private fun HabitDescriptionStrip(
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun CalendarView(
    currentMonth: org.example.habitstreak.presentation.model.YearMonth?,
    records: List<HabitRecord>,
    habit: Habit,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (org.example.habitstreak.presentation.model.YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)

    if (currentMonth == null) return

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newMonth = if (currentMonth.monthNumber == 1) {
                        org.example.habitstreak.presentation.model.YearMonth(currentMonth.year - 1, 12)
                    } else {
                        org.example.habitstreak.presentation.model.YearMonth(currentMonth.year, currentMonth.monthNumber - 1)
                    }
                    onMonthChange(newMonth)
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }

                Text(
                    text = "${monthNames[currentMonth.monthNumber - 1]} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    val newMonth = if (currentMonth.monthNumber == 12) {
                        org.example.habitstreak.presentation.model.YearMonth(currentMonth.year + 1, 1)
                    } else {
                        org.example.habitstreak.presentation.model.YearMonth(currentMonth.year, currentMonth.monthNumber + 1)
                    }
                    onMonthChange(newMonth)
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day of Week Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                dayNames.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            CalendarGrid(
                yearMonth = currentMonth,
                records = records,
                habit = habit,
                selectedDate = selectedDate,
                onDateClick = onDateSelected,
                habitColor = habitColor,
                today = today
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AssistChip(
                            onClick = {
                                onMonthChange(
                                    org.example.habitstreak.presentation.model.YearMonth(
                                        habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.year,
                                        habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.month.number
                                    )
                                )
                            },
                            label = { Text("Created") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    item {
                        AssistChip(
                            onClick = {
                                onMonthChange(
                                    org.example.habitstreak.presentation.model.YearMonth(
                                        today.year,
                                        today.monthNumber
                                    )
                                )
                            },
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
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: org.example.habitstreak.presentation.model.YearMonth,
    records: List<HabitRecord>,
    habit: Habit,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    habitColor: Color,
    today: LocalDate
) {
    val firstDayOfMonth = LocalDate(yearMonth.year, yearMonth.monthNumber, 1)
    val lastDayOfMonth = firstDayOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysInMonth = lastDayOfMonth.dayOfMonth

    // ISO-8601 week offset calculation (Monday = 1)
    val offset = (firstDayOfWeek.ordinal + 1) % 7
    val totalCells = offset + daysInMonth
    val rows = (totalCells + 6) / 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height((rows * 48).dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Empty cells before month starts
        items(offset) {
            Spacer(modifier = Modifier.size(40.dp))
        }

        // Days of the month
        items(daysInMonth) { dayIndex ->
            val date = LocalDate(yearMonth.year, yearMonth.monthNumber, dayIndex + 1)
            val record = records.find { it.date == date }
            val progress = record?.let {
                it.completedCount.toFloat() / habit.targetCount.coerceAtLeast(1)
            } ?: 0f

            val isToday = date == today
            val isSelected = date == selectedDate
            val isFuture = date > today
            val isPast = date < today
            val hasNote = record?.note?.isNotBlank() == true

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isSelected -> habitColor
                            isToday -> habitColor.copy(alpha = 0.3f)
                            progress > 0f -> habitColor.copy(alpha = progress * 0.8f + 0.2f)
                            else -> Color.Transparent
                        }
                    )
                    .clickable(enabled = !isFuture) {
                        onDateClick(date)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (dayIndex + 1).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isSelected -> Color.White
                        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        progress > 0.5f -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )

                if (hasNote) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isSelected || progress > 0.5f) Color.White else habitColor,
                                shape = RoundedCornerShape(3.dp)
                            )
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCards(
    statistics: HabitDetailViewModel.HabitStats?,
    habit: Habit,
    modifier: Modifier = Modifier
) {
    val stats = statistics ?: HabitDetailViewModel.HabitStats()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = stats.thisMonthCount.toString(),
            label = "Total",
            icon = Icons.Outlined.CheckCircle,
            color = MaterialTheme.colorScheme.primary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = stats.longestStreak.toString(),
            label = "Best Streak",
            icon = Icons.Outlined.LocalFireDepartment,
            color = MaterialTheme.colorScheme.secondary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "${(stats.completionRate * 100).toInt()}%",
            label = "Rate",
            icon = Icons.Outlined.Percent,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityHistory(
    records: List<HabitRecord>,
    habit: Habit,
    today: LocalDate,
    onRecordClick: (HabitRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        EmptyStateMessage("No activity yet")
        return
    }

    val sortedRecords = records.sortedByDescending { it.date }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            sortedRecords.take(10).forEach { record ->
                ActivityHistoryItem(
                    record = record,
                    habit = habit,
                    today = today,
                    onClick = { onRecordClick(record) }
                )
            }
        }
    }
}

@Composable
private fun ActivityHistoryItem(
    record: HabitRecord,
    habit: Habit,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = record.completedCount.toFloat() / habit.targetCount.coerceAtLeast(1)
    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)

    ListItem(
        modifier = modifier.clickable { onClick() },
        headlineContent = {
            Text(DateFormatter.formatRelativeDate(record.date, today))
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${record.completedCount}/${habit.targetCount}")
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f),
                    color = habitColor,
                )
                if (record.note.isNotBlank()) {
                    Icon(
                        Icons.Outlined.Note,
                        contentDescription = "Has note",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = habitColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = record.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = habitColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun NotesHistory(
    records: List<HabitRecord>,
    today: LocalDate,
    onNoteClick: (HabitRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val recordsWithNotes = records.filter { it.note.isNotBlank() }

    if (recordsWithNotes.isEmpty()) {
        EmptyStateMessage("No notes yet")
        return
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            recordsWithNotes.sortedByDescending { it.date }.forEach { record ->
                NotesHistoryItem(
                    record = record,
                    today = today,
                    onClick = { onNoteClick(record) }
                )
            }
        }
    }
}

@Composable
private fun NotesHistoryItem(
    record: HabitRecord,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable { onClick() },
        headlineContent = {
            Text(DateFormatter.formatRelativeDate(record.date, today))
        },
        supportingContent = {
            Text(
                text = record.note,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                Icons.Outlined.Note,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
private fun EmptyStateMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Bottom sheet component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedDateBottomSheet(
    date: LocalDate,
    habit: Habit,
    record: HabitRecord?,
    sheetState: SheetState,
    today: LocalDate,
    onDismiss: () -> Unit,
    onUpdateProgress: (Int) -> Unit,
    onUpdateNote: (String?) -> Unit,
    onDeleteRecord: () -> Unit
) {
    var progress by remember(record) {
        mutableStateOf(record?.completedCount ?: 0)
    }
    var note by remember(record) {
        mutableStateOf(record?.note ?: "")
    }

    val isFuture = date > today

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when {
                            isFuture -> "Future Date"
                            else -> DateFormatter.formatRelativeDate(date, today)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateFormatter.formatFullDate(date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Progress section
            if (!isFuture) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = HabitStreakTheme.habitColorToComposeColor(habit.color).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Progress: $progress/${habit.targetCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress controls here
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    progress = 0
                                    onUpdateProgress(0)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear")
                            }
                            Button(
                                onClick = {
                                    progress = habit.targetCount
                                    onUpdateProgress(habit.targetCount)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complete")
                            }
                        }
                    }
                }
            }

            // Note section
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                            onUpdateNote(it.takeIf { it.isNotBlank() })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add a note...") },
                        maxLines = 3
                    )
                }
            }

            // Delete option for existing records
            if (record != null && !isFuture) {
                TextButton(
                    onClick = onDeleteRecord,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete record")
                }
            }
        }
    }
}