package org.example.habitstreak.presentation.screen.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.model.HabitStatistics
import org.example.habitstreak.presentation.viewmodel.StatisticsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHabit: (String) -> Unit,
    viewModel: StatisticsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var selectedPeriod by remember { mutableStateOf(TimePeriod.WEEK) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    listOf("Overview", "Habits", "Insights").forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }

                // Period Selector
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it },
                    modifier = Modifier.padding(16.dp)
                )

                // Content Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> OverviewTab(
                            statistics = uiState.statistics,
                            period = selectedPeriod
                        )
                        1 -> HabitsTab(
                            statistics = uiState.statistics,
                            onHabitClick = { habitId ->
                                viewModel.selectHabit(habitId)
                                onNavigateToHabit(habitId)
                            }
                        )
                        2 -> InsightsTab(
                            statistics = uiState.statistics,
                            period = selectedPeriod
                        )
                    }
                }
            }
        }

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onExport = { format ->
                    // Handle export
                    showExportDialog = false
                }
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TimePeriod.entries.toTypedArray()) { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label) },
                leadingIcon = if (selectedPeriod == period) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun OverviewTab(
    statistics: List<HabitStatistics>,
    period: TimePeriod
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards
        item {
            OverallStatsCards(statistics)
        }

        // Completion Rate Chart
        item {
            CompletionRateChart(
                statistics = statistics,
                period = period
            )
        }

        // Streak Distribution
        item {
            StreakDistributionCard(statistics)
        }

        // Heatmap
        item {
            ActivityHeatmap(
                statistics = statistics,
                period = period
            )
        }

        // Best Performance
        item {
            BestPerformanceCard(statistics)
        }
    }
}

@Composable
private fun HabitsTab(
    statistics: List<HabitStatistics>,
    onHabitClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(statistics.sortedByDescending { it.currentStreak }) { stat ->
            HabitStatCard(
                statistics = stat,
                onClick = { onHabitClick(stat.habitId) }
            )
        }
    }
}

@Composable
private fun InsightsTab(
    statistics: List<HabitStatistics>,
    period: TimePeriod
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Insights
        item {
            InsightCard(
                icon = Icons.Outlined.TipsAndUpdates,
                title = "Key Insight",
                message = generateInsight(statistics),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Patterns
        item {
            PatternsCard(statistics)
        }

        // Recommendations
        item {
            RecommendationsCard(statistics)
        }

        // Achievements
        item {
            AchievementsCard(statistics)
        }
    }
}

@Composable
private fun OverallStatsCards(statistics: List<HabitStatistics>) {
    val totalHabits = statistics.size
    val activeHabits = statistics.count { it.currentStreak > 0 }
    val avgCompletionRate = statistics.map { it.completionRate }.average() * 100
    val totalCompletions = statistics.sumOf { it.totalCompletions }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "$activeHabits/$totalHabits",
                label = "Active Habits",
                icon = Icons.Outlined.CheckCircle,
                color = MaterialTheme.colorScheme.primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${avgCompletionRate.toInt()}%",
                label = "Avg Completion",
                icon = Icons.Outlined.Analytics,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = totalCompletions.toString(),
                label = "Total Checks",
                icon = Icons.Outlined.Done,
                color = MaterialTheme.colorScheme.tertiary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = statistics.maxOfOrNull { it.longestStreak }?.toString() ?: "0",
                label = "Best Streak",
                icon = Icons.Outlined.LocalFireDepartment,
                color = Color(0xFFFF6B35)
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
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
private fun CompletionRateChart(
    statistics: List<HabitStatistics>,
    period: TimePeriod
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Completion Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Simplified line chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                drawCompletionChart(statistics, period)
            }
        }
    }
}

@Composable
private fun StreakDistributionCard(statistics: List<HabitStatistics>) {
    val distribution = mapOf(
        "0 days" to statistics.count { it.currentStreak == 0 },
        "1-7 days" to statistics.count { it.currentStreak in 1..7 },
        "8-30 days" to statistics.count { it.currentStreak in 8..30 },
        "30+ days" to statistics.count { it.currentStreak > 30 }
    )

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Streak Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            distribution.forEach { (range, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = range,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )

                    val maxCount = distribution.values.maxOrNull() ?: 1
                    val progress = count.toFloat() / maxCount

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(
                                    when (range) {
                                        "30+ days" -> Color(0xFFFFD700)
                                        "8-30 days" -> MaterialTheme.colorScheme.primary
                                        "1-7 days" -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        )
                    }

                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityHeatmap(
    statistics: List<HabitStatistics>,
    period: TimePeriod
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Activity Heatmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Simplified heatmap grid
            val weeks = when (period) {
                TimePeriod.WEEK -> 1
                TimePeriod.MONTH -> 4
                TimePeriod.QUARTER -> 12
                TimePeriod.YEAR -> 52
                TimePeriod.ALL -> 52
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(weeks) { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(7) { day ->
                            val intensity = (0..100).random() / 100f
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = intensity
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BestPerformanceCard(statistics: List<HabitStatistics>) {
    val bestStreak = statistics.maxByOrNull { it.longestStreak }
    val mostConsistent = statistics.maxByOrNull { it.completionRate }
    val mostActive = statistics.maxByOrNull { it.totalCompletions }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Top Performers 🏆",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                Triple("Longest Streak", bestStreak, "${bestStreak?.longestStreak ?: 0} days"),
                Triple("Most Consistent", mostConsistent, "${((mostConsistent?.completionRate ?: 0f) * 100).toInt()}%"),
                Triple("Most Active", mostActive, "${mostActive?.totalCompletions ?: 0} checks")
            ).forEach { (label, stat, value) ->
                if (stat != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitStatCard(
    statistics: HabitStatistics,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Habit ${statistics.habitId.take(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Last: ${statistics.lastCompletedDate ?: "Never"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatChip(
                    value = statistics.currentStreak.toString(),
                    label = "Streak",
                    color = Color(0xFFFF6B35)
                )
                StatChip(
                    value = "${(statistics.completionRate * 100).toInt()}%",
                    label = "Rate",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

@Composable
private fun InsightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PatternsCard(statistics: List<HabitStatistics>) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                "Most productive day: Monday",
                "Average streak length: ${statistics.map { it.currentStreak }.average().toInt()} days",
                "Success rate improving by 15% monthly"
            ).forEach { pattern ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Insights,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pattern,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationsCard(statistics: List<HabitStatistics>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                "Focus on habits with streaks below 7 days",
                "Try completing habits in the morning for better consistency",
                "Consider grouping similar habits together"
            ).forEach { recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsCard(statistics: List<HabitStatistics>) {
    val achievements = listOf(
        Achievement("Early Bird", "Complete 5 habits before 9 AM", true),
        Achievement("Week Warrior", "7-day streak on all habits", false),
        Achievement("Centurion", "100 total completions", true),
        Achievement("Perfectionist", "100% completion for a month", false)
    )

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementBadge(achievement)
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    Card(
        modifier = Modifier.size(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                if (achievement.unlocked) Icons.Default.EmojiEvents else Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (achievement.unlocked)
                    Color(0xFFFFD700)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = achievement.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Statistics") },
        text = {
            Column {
                Text("Choose export format:")
                Spacer(modifier = Modifier.height(16.dp))
                ExportFormat.entries.forEach { format ->
                    Card(
                        onClick = { onExport(format) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (format) {
                                    ExportFormat.PDF -> Icons.Outlined.PictureAsPdf
                                    ExportFormat.CSV -> Icons.Outlined.TableChart
                                    ExportFormat.JSON -> Icons.Outlined.Code
                                },
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(format.label)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

private fun DrawScope.drawCompletionChart(
    statistics: List<HabitStatistics>,
    period: TimePeriod
) {
    val padding = 40.dp.toPx()
    val graphWidth = size.width - padding * 2
    val graphHeight = size.height - padding * 2

    // Draw axes
    drawLine(
        color = Color.Gray,
        start = Offset(padding, size.height - padding),
        end = Offset(size.width - padding, size.height - padding),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.Gray,
        start = Offset(padding, padding),
        end = Offset(padding, size.height - padding),
        strokeWidth = 2f
    )

    // Sample data points
    val dataPoints = listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.8f, 0.75f, 0.9f)
    val pointSpacing = graphWidth / (dataPoints.size - 1)

    // Draw line
    val path = Path()
    dataPoints.forEachIndexed { index, value ->
        val x = padding + index * pointSpacing
        val y = size.height - padding - (value * graphHeight)

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = Color(0xFF4CAF50),
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    // Draw points
    dataPoints.forEachIndexed { index, value ->
        val x = padding + index * pointSpacing
        val y = size.height - padding - (value * graphHeight)

        drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = 4f,
            center = Offset(x, y)
        )
    }
}

private fun generateInsight(statistics: List<HabitStatistics>): String {
    val avgStreak = statistics.map { it.currentStreak }.average()
    val completionRate = statistics.map { it.completionRate }.average() * 100

    return when {
        avgStreak > 20 -> "Amazing consistency! Your average streak of ${avgStreak.toInt()} days shows exceptional dedication."
        completionRate > 80 -> "Great job! You're completing ${completionRate.toInt()}% of your habits regularly."
        statistics.any { it.currentStreak > 30 } -> "You have habits with 30+ day streaks! Keep up the momentum."
        else -> "Focus on building consistency. Try starting with just one habit at a time."
    }
}

enum class TimePeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    QUARTER("Quarter"),
    YEAR("Year"),
    ALL("All Time")
}

enum class ExportFormat(val label: String) {
    PDF("PDF Report"),
    CSV("CSV Data"),
    JSON("JSON Export")
}

data class Achievement(
    val name: String,
    val description: String,
    val unlocked: Boolean
)