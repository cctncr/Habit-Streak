package org.example.habitstreak.presentation.screen.habit_detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.presentation.ui.components.NotificationSettingsCard
import org.example.habitstreak.presentation.ui.components.common.StreakBadge
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.presentation.ui.utils.DateFormatter
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: HabitDetailViewModel = koinViewModel(key = habitId) { parametersOf(habitId) }, // key eklendi
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
                        statistics = uiState.filteredStatistics,
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
                    viewModel.updateNote(selectedDate!!, note)
                },
                onSetReminder = { time ->
                    viewModel.setFutureReminder(selectedDate!!, time)
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Habit?") },
                text = { Text("This will permanently delete \"${habit.title}\" and all its data.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHabit {
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    } ?: LoadingState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitDetailTopBar(
    habit: Habit,
    statistics: HabitDetailViewModel.HabitStats?,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = habit.icon.emoji,
                    fontSize = 24.sp
                )
                Text(
                    text = habit.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Kompakt streak badge ekle
                statistics?.let { stats ->
                    if (stats.currentStreak > 0) {
                        StreakBadge(
                            streak = stats.currentStreak,
                            modifier = Modifier
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete")
            }
        }
    )
}

@Composable
private fun HabitDescriptionStrip(
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HabitHeader(
    habit: Habit,
    statistics: HabitDetailViewModel.HabitStats?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔥", fontSize = 32.sp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${statistics?.currentStreak ?: 0}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            if (habit.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun CalendarView(
    currentMonth: HabitDetailViewModel.YearMonth,
    records: List<HabitRecord>,
    habit: Habit,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (HabitDetailViewModel.YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)
    val monthDate = LocalDate(currentMonth.year, currentMonth.month, 1)

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
                    onMonthChange(currentMonth.previousMonth())
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${DateFormatter.formatMonthShort(monthDate)} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Hangi yıl olduğunu daha belirgin göster
                    if (currentMonth.year != today.year) {
                        Text(
                            text = if (currentMonth.year > today.year) {
                                "${currentMonth.year - today.year} years from now"
                            } else {
                                "${today.year - currentMonth.year} years ago"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = {
                    onMonthChange(currentMonth.nextMonth())
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Week days header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
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
                                    HabitDetailViewModel.YearMonth(
                                        habit.createdAt.year,
                                        habit.createdAt.month.number
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
                                    HabitDetailViewModel.YearMonth(
                                        today.year,
                                        today.month.number
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
    val firstDayOfMonth = LocalDate(yearMonth.year, yearMonth.month.number, 1)
    val lastDayOfMonth = firstDayOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysInMonth = lastDayOfMonth.dayOfMonth

    // ISO-8601 week için offset hesaplama (Monday = 1)
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
        // Month başlamadan önceki boş hücreler
        items(offset) {
            Spacer(modifier = Modifier.size(40.dp))
        }

        // Ayın günleri
        items(daysInMonth) { dayIndex ->
            val date = LocalDate(yearMonth.year, yearMonth.month.number, dayIndex + 1)
            val record = records.find { it.date == date }
            val progress = record?.let {
                it.completedCount.toFloat() / habit.targetCount.coerceAtLeast(1)
            } ?: 0f

            DayCell(
                date = date,
                progress = progress,
                isSelected = date == selectedDate,
                isToday = date == today,
                habitColor = habitColor,
                onClick = { onDateClick(date) }
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    progress: Float,
    hasNote: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    isPast: Boolean,
    habitColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            progress >= 1f -> habitColor
            progress > 0f -> habitColor.copy(alpha = 0.3f + (0.5f * progress))
            hasNote && isFuture -> MaterialTheme.colorScheme.tertiaryContainer.copy(
                alpha = 0.7f
            ) // Future note
            hasNote && isPast -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) // Past note
            hasNote -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) // Today note
            isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) // Future days
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "bg"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(),
        label = "scale"
    )

    val borderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isSelected -> habitColor
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isToday || isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable { onClick() }, // Tüm günler tıklanabilir
        contentAlignment = Alignment.Center
    ) {
        when {
            progress >= 1f -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            progress > 0f -> {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 0.5f) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            hasNote -> {
                Icon(
                    Icons.AutoMirrored.Outlined.StickyNote2,
                    contentDescription = "Has note",
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        isFuture -> MaterialTheme.colorScheme.onTertiaryContainer
                        isPast -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }

            else -> {
                Text(
                    text = date.day.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isToday -> MaterialTheme.colorScheme.primary
                        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        )

                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun StatsCards(
    statistics: HabitDetailViewModel.HabitStats?,
    habit: Habit
) {
    val stats = statistics ?: HabitDetailViewModel.HabitStats()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Total",
            value = stats.thisMonthCount.toString(), // totalCompletions yerine
            icon = Icons.Outlined.CheckCircle
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Best Streak",
            value = stats.longestStreak.toString(), // bestStreak yerine
            icon = Icons.Outlined.LocalFireDepartment
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Rate",
            value = "${stats.completionRate.toInt()}%", // completionRate zaten var
            icon = Icons.Outlined.Percent
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .width(110.dp)
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
    onRecordClick: (HabitRecord) -> Unit
) {
    if (records.isEmpty()) {
        EmptyStateMessage("No activity yet")
        return
    }

    val sortedRecords = records.sortedByDescending { it.date }

    Card {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 200.dp, max = 600.dp), // Minimum ve maksimum yükseklik
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(sortedRecords) { index, record ->
                ActivityItem(
                    record = record,
                    habit = habit,
                    today = today,
                    onClick = { onRecordClick(record) }
                )
                if (index < sortedRecords.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun NotesHistory(
    records: List<HabitRecord>,
    today: LocalDate,
    onNoteClick: (HabitRecord) -> Unit
) {
    val recordsWithNotes = records.filter { it.note.isNotBlank() }

    if (recordsWithNotes.isEmpty()) {
        EmptyStateMessage("No notes yet")
        return
    }

    val sortedRecordsWithNotes = recordsWithNotes.sortedByDescending { it.date }

    Card {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 200.dp, max = 600.dp), // Minimum ve maksimum yükseklik
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(sortedRecordsWithNotes) { index, record ->
                NoteItem(
                    record = record,
                    today = today,
                    onClick = { onNoteClick(record) }
                )
                if (index < sortedRecordsWithNotes.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(
    record: HabitRecord,
    habit: Habit,
    today: LocalDate,
    onClick: () -> Unit
) {
    val progress = record.completedCount.toFloat() / habit.targetCount.coerceAtLeast(1)
    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)
    val isFuture = record.date > today

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            progress >= 1f -> habitColor
                            progress > 0f -> habitColor.copy(alpha = 0.3f)
                            isFuture && record.note.isNotBlank() -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    progress >= 1f -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    progress > 0f -> {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    isFuture && record.note.isNotBlank() -> {
                        Icon(
                            Icons.AutoMirrored.Outlined.StickyNote2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    else -> {
                        Icon(
                            Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateFormatter.formatRelativeDate(record.date, today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFuture) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                    )

                    // Future indicator
                    if (isFuture) {
                        Text(
                            text = "📅",
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (habit.targetCount > 1 && progress > 0) {
                        Text(
                            text = "${record.completedCount} ${habit.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (record.note.isNotBlank()) {
                        Text(
                            text = record.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, false)
                        )
                    }
                }
            }
        }

        if (record.note.isNotBlank()) {
            Icon(
                Icons.AutoMirrored.Outlined.StickyNote2,
                contentDescription = "Has note",
                modifier = Modifier.size(16.dp),
                tint = if (isFuture) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoteItem(
    record: HabitRecord,
    today: LocalDate,
    onClick: () -> Unit
) {
    val isFuture = record.date > today

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFuture -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateFormatter.formatRelativeDate(record.date, today),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFuture) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    // Future indicator
                    if (isFuture) {
                        Text(
                            text = "📅",
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = DateFormatter.formatDateShort(record.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.note,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
    onUpdateNote: (String) -> Unit,
    onSetReminder: (LocalTime?) -> Unit
) {
    var progress by remember(record) {
        mutableStateOf(record?.completedCount ?: 0)
    }
    var note by remember(record) {
        mutableStateOf(record?.note ?: "")
    }
    var originalNote by remember(record) {
        mutableStateOf(record?.note ?: "")
    }
    var isEditingNote by remember { mutableStateOf(false) }
    var showReminderTime by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var hasChanges by remember { mutableStateOf(false) }

    val habitColor = HabitStreakTheme.habitColorToComposeColor(habit.color)
    val isToday = date == today
    val isFuture = date > today

    // Changes kontrolü
    LaunchedEffect(note, progress) {
        hasChanges = note != originalNote || progress != (record?.completedCount ?: 0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .animateContentSize(),
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
                        text = "${date.day} ${DateFormatter.formatMonthShort(date)} ${date.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Progress Input - İleri tarihler için gösterme
            if (!isFuture) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = habitColor.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$progress / ${habit.targetCount} ${habit.unit}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = habitColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick selection buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0,
                                habit.targetCount / 2,
                                habit.targetCount
                            ).distinct().forEach { value ->
                                FilterChip(
                                    selected = progress == value,
                                    onClick = { progress = value },
                                    label = {
                                        Text(
                                            text = when (value) {
                                                0 -> "None"
                                                habit.targetCount -> "Complete"
                                                else -> "$value"
                                            }
                                        )
                                    },
                                    leadingIcon = if (progress == value) {
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
                        }

                        // Slider for precise control
                        if (habit.targetCount > 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = progress.toFloat(),
                                onValueChange = { progress = it.toInt() },
                                valueRange = 0f..habit.targetCount.toFloat(),
                                steps = habit.targetCount - 1,
                                colors = SliderDefaults.colors(
                                    thumbColor = habitColor,
                                    activeTrackColor = habitColor
                                )
                            )
                        }
                    }
                }
            }

            // Note Section
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.StickyNote2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isFuture) "Future Note" else "Note",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        IconButton(
                            onClick = {
                                isEditingNote = !isEditingNote
                                if (isEditingNote) {
                                    // Editing başladığında original note'u kaydet
                                    originalNote = note
                                }
                            }
                        ) {
                            Icon(
                                if (isEditingNote) Icons.Default.Check else Icons.Outlined.Edit,
                                contentDescription = if (isEditingNote) "Save" else "Edit"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedContent(
                        targetState = isEditingNote,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "note_edit"
                    ) { editing ->
                        if (editing) {
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                placeholder = {
                                    Text(
                                        if (isFuture) "Add a note for this future date..."
                                        else "Add a note about today..."
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                )
                            )
                        } else {
                            Text(
                                text = note.ifBlank { "No note added" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (note.isBlank())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isEditingNote = true }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }

                    // İleri tarih için reminder switch
                    if (isFuture && note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                    alpha = 0.3f
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Column {
                                            Text(
                                                text = "Reminder",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            if (reminderTime != null) {
                                                Text(
                                                    text = "At ${
                                                        DateFormatter.formatTime(
                                                            reminderTime!!
                                                        )
                                                    }",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Switch(
                                        checked = reminderTime != null,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                showReminderTime = true
                                                reminderTime =
                                                    LocalTime(9, 0) // Default time
                                            } else {
                                                reminderTime = null
                                                onSetReminder(null)
                                            }
                                        }
                                    )
                                }

                                // Time picker
                                AnimatedVisibility(visible = showReminderTime && reminderTime != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        TextButton(onClick = {
                                            reminderTime = LocalTime(9, 0)
                                            onSetReminder(reminderTime)
                                            showReminderTime = false
                                        }) {
                                            Text(
                                                "Set Time: ${
                                                    reminderTime?.let {
                                                        DateFormatter.formatTime(
                                                            it
                                                        )
                                                    } ?: "Not set"
                                                }")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save Button - Sadece değişiklik varsa aktif
            Button(
                onClick = {
                    // Progress değiştiğinde kaydet
                    if (progress != (record?.completedCount ?: 0)) {
                        onUpdateProgress(progress)
                    }

                    // Note değiştiğinde kaydet
                    if (note != originalNote) {
                        onUpdateNote(note)
                    }

                    // Reminder varsa kaydet
                    if (reminderTime != null) {
                        onSetReminder(reminderTime)
                    }

                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = habitColor
                ),
                enabled = hasChanges || reminderTime != null
            ) {
                Text(
                    text = if (hasChanges) "Save Changes" else "Close",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// Enums
enum class ActivityTab(val label: String) {
    HISTORY("Activity History"),
    NOTES("Notes")
}

enum class StatsTimeFilter(val label: String) {
    ALL_TIME("Since Created"),
    THIS_MONTH("Selected Month"),
    THIS_WEEK("This Week")
}