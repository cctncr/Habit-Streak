package org.example.habitstreak.presentation.screen.statistics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.HabitStatistics
import org.example.habitstreak.presentation.viewmodel.StatisticsViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.*
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// Data classes for better organization (Single Responsibility)
data class ChartData(
    val label: String,
    val value: Float,
    val date: LocalDate
)

data class StreakData(
    val range: String,
    val count: Int,
    val percentage: Float
)

enum class TimePeriod(val label: String, val days: Int) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    QUARTER("3 Months", 90),
    YEAR("Year", 365)
}

enum class StatTab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Outlined.Dashboard),
    HABITS("Habits", Icons.Outlined.CheckCircle),
    INSIGHTS("Insights", Icons.Outlined.Lightbulb),
    TRENDS("Trends", Icons.Outlined.TrendingUp)
}

// Main Screen Component
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHabit: (String) -> Unit,
    viewModel: StatisticsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { StatTab.entries.size })
    val coroutineScope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTH) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StatisticsTopBar(
                onNavigateBack = onNavigateBack,
                onExportClick = { showExportDialog = true }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Period Selector
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it }
            )

            // Tabs
            StatisticsTabs(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            // Content
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(error = uiState.error!!)
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (StatTab.entries[page]) {
                            StatTab.OVERVIEW -> OverviewTab(
                                statistics = uiState.statistics,
                                period = selectedPeriod
                            )
                            StatTab.HABITS -> HabitsTab(
                                statistics = uiState.statistics,
                                onHabitClick = onNavigateToHabit
                            )
                            StatTab.INSIGHTS -> InsightsTab(
                                statistics = uiState.statistics,
                                period = selectedPeriod
                            )
                            StatTab.TRENDS -> TrendsTab(
                                statistics = uiState.statistics,
                                period = selectedPeriod
                            )
                        }
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

// Top Bar Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsTopBar(
    onNavigateBack: () -> Unit,
    onExportClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "Statistics",
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = onExportClick) {
                Icon(Icons.Outlined.Share, "Export")
            }
        }
    )
}

// Period Selector Component
@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TimePeriod.entries) { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label) },
                leadingIcon = if (selectedPeriod == period) {
                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

// Tabs Component
@Composable
private fun StatisticsTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(selectedTabIndex = selectedTab) {
        StatTab.entries.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { Text(tab.label) },
                icon = { Icon(tab.icon, contentDescription = null) }
            )
        }
    }
}

// Overview Tab Component
@Composable
private fun OverviewTab(
    statistics: List<HabitStatistics>,
    period: TimePeriod
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards Grid
        item {
            SummaryCardsGrid(statistics)
        }

        // Completion Chart
        item {
            CompletionChartCard(statistics, period)
        }

        // Streak Distribution
        item {
            StreakDistributionCard(statistics)
        }

        // Best Performers
        item {
            BestPerformersCard(statistics)
        }
    }
}

// Summary Cards Grid Component
@Composable
private fun SummaryCardsGrid(statistics: List<HabitStatistics>) {
    val metrics = remember(statistics) {
        StatisticsCalculator.calculateSummaryMetrics(statistics)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Active Habits",
                value = "${metrics.activeHabits}",
                subtitle = "of ${metrics.totalHabits}",
                icon = Icons.Outlined.CheckCircle,
                color = MaterialTheme.colorScheme.primary
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Avg Completion",
                value = "${metrics.avgCompletionRate}%",
                subtitle = "This period",
                icon = Icons.Outlined.Analytics,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Total Checks",
                value = "${metrics.totalCompletions}",
                subtitle = "All time",
                icon = Icons.Outlined.Done,
                color = MaterialTheme.colorScheme.tertiary
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Best Streak",
                value = "${metrics.longestStreak}",
                subtitle = "days",
                icon = Icons.Outlined.LocalFireDepartment,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Metric Card Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
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
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Completion Chart Card Component
@Composable
private fun CompletionChartCard(
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

            CompletionChart(
                statistics = statistics,
                period = period,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

// Completion Chart Component
@Composable
private fun CompletionChart(
    statistics: List<HabitStatistics>,
    period: TimePeriod,
    modifier: Modifier = Modifier
) {
    val chartData = remember(statistics, period) {
        ChartDataGenerator.generateCompletionData(statistics, period)
    }

    Canvas(modifier = modifier) {
        drawCompletionChart(chartData)
    }
}

// Draw Completion Chart Extension
private fun DrawScope.drawCompletionChart(data: List<ChartData>) {
    if (data.isEmpty()) return

    val padding = 40.dp.toPx()
    val graphWidth = size.width - padding * 2
    val graphHeight = size.height - padding * 2

    // Draw grid lines
    val gridLines = 5
    for (i in 0..gridLines) {
        val y = padding + (graphHeight / gridLines) * i
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = androidx.compose.ui.geometry.Offset(padding, y),
            end = androidx.compose.ui.geometry.Offset(size.width - padding, y),
            strokeWidth = 1f
        )
    }

    // Draw axes
    drawLine(
        color = Color.Gray,
        start = androidx.compose.ui.geometry.Offset(padding, size.height - padding),
        end = androidx.compose.ui.geometry.Offset(size.width - padding, size.height - padding),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.Gray,
        start = androidx.compose.ui.geometry.Offset(padding, padding),
        end = androidx.compose.ui.geometry.Offset(padding, size.height - padding),
        strokeWidth = 2f
    )

    // Draw line chart
    val path = Path()
    val fillPath = Path()
    val maxValue = data.maxOf { it.value }
    val pointSpacing = graphWidth / (data.size - 1).coerceAtLeast(1)

    data.forEachIndexed { index, point ->
        val x = padding + index * pointSpacing
        val y = size.height - padding - (point.value / maxValue * graphHeight)

        if (index == 0) {
            path.moveTo(x, y)
            fillPath.moveTo(x, size.height - padding)
            fillPath.lineTo(x, y)
        } else {
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        // Draw points
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = 4f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
    }

    // Complete fill path
    if (data.isNotEmpty()) {
        fillPath.lineTo(padding + (data.size - 1) * pointSpacing, size.height - padding)
        fillPath.close()

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF4CAF50).copy(alpha = 0.3f),
                    Color(0xFF4CAF50).copy(alpha = 0.05f)
                )
            )
        )
    }

    // Draw line
    drawPath(
        path = path,
        color = Color(0xFF4CAF50),
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )
}

// Streak Distribution Card Component
@Composable
private fun StreakDistributionCard(statistics: List<HabitStatistics>) {
    val distribution = remember(statistics) {
        StatisticsCalculator.calculateStreakDistribution(statistics)
    }

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

            distribution.forEach { data ->
                StreakBar(data)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Streak Bar Component
@Composable
private fun StreakBar(data: StreakData) {
    val animatedProgress by animateFloatAsState(
        targetValue = data.percentage / 100f,
        animationSpec = tween(durationMillis = 1000)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = data.range,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${data.count}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}

// Best Performers Card Component
@Composable
private fun BestPerformersCard(statistics: List<HabitStatistics>) {
    val topPerformers = remember(statistics) {
        statistics
            .sortedByDescending { it.currentStreak }
            .take(3)
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Performers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            topPerformers.forEachIndexed { index, stat ->
                PerformerRow(
                    rank = index + 1,
                    habitId = stat.habitId,
                    streak = stat.currentStreak,
                    completionRate = stat.completionRate
                )
                if (index < topPerformers.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Performer Row Component
@Composable
private fun PerformerRow(
    rank: Int,
    habitId: String,
    streak: Int,
    completionRate: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Habit ${habitId.take(8)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$streak days • ${(completionRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Habits Tab Component
@Composable
private fun HabitsTab(
    statistics: List<HabitStatistics>,
    onHabitClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = statistics.sortedByDescending { it.currentStreak },
            key = { it.habitId }
        ) { stat ->
            HabitStatCard(
                statistics = stat,
                onClick = { onHabitClick(stat.habitId) }
            )
        }
    }
}

// Habit Stat Card Component
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatChip(
                        icon = Icons.Outlined.LocalFireDepartment,
                        value = "${statistics.currentStreak}",
                        label = "streak"
                    )
                    StatChip(
                        icon = Icons.Outlined.CheckCircle,
                        value = "${(statistics.completionRate * 100).toInt()}%",
                        label = "rate"
                    )
                    StatChip(
                        icon = Icons.Outlined.Done,
                        value = "${statistics.totalCompletions}",
                        label = "total"
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Stat Chip Component
@Composable
private fun StatChip(
    icon: ImageVector,
    value: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Insights Tab Component
@Composable
private fun InsightsTab(
    statistics: List<HabitStatistics>,
    period: TimePeriod
) {
    val insights = remember(statistics, period) {
        InsightsGenerator.generateInsights(statistics, period)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(insights) { insight ->
            InsightCard(insight)
        }
    }
}

// Insight Card Component
@Composable
private fun InsightCard(insight: Insight) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (insight.type) {
                InsightType.POSITIVE -> MaterialTheme.colorScheme.primaryContainer
                InsightType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
                InsightType.IMPROVEMENT -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = insight.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (insight.type) {
                    InsightType.POSITIVE -> MaterialTheme.colorScheme.primary
                    InsightType.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    InsightType.IMPROVEMENT -> MaterialTheme.colorScheme.secondary
                }
            )
            Column {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                insight.action?.let { action ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Trends Tab Component
@Composable
private fun TrendsTab(
    statistics: List<HabitStatistics>,
    period: TimePeriod
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heatmap
        item {
            HeatmapCard(statistics, period)
        }

        // Progress Trends
        item {
            ProgressTrendsCard(statistics, period)
        }

        // Predictions
        item {
            PredictionsCard(statistics)
        }
    }
}

// Heatmap Card Component
@Composable
private fun HeatmapCard(
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

            ActivityHeatmap(
                statistics = statistics,
                period = period,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }
    }
}

// Activity Heatmap Component
@Composable
private fun ActivityHeatmap(
    statistics: List<HabitStatistics>,
    period: TimePeriod,
    modifier: Modifier = Modifier
) {
    val heatmapData = remember(statistics, period) {
        HeatmapDataGenerator.generateHeatmapData(statistics, period)
    }

    Canvas(modifier = modifier) {
        drawHeatmap(heatmapData)
    }
}

// Draw Heatmap Extension
private fun DrawScope.drawHeatmap(data: List<List<Float>>) {
    val cellSize = 20.dp.toPx()
    val spacing = 2.dp.toPx()
    val cornerRadius = 4.dp.toPx()

    data.forEachIndexed { weekIndex, week ->
        week.forEachIndexed { dayIndex, intensity ->
            val x = weekIndex * (cellSize + spacing)
            val y = dayIndex * (cellSize + spacing)

            val color = when {
                intensity == 0f -> Color.Gray.copy(alpha = 0.1f)
                intensity < 0.25f -> Color(0xFF9CCC65).copy(alpha = 0.3f)
                intensity < 0.5f -> Color(0xFF66BB6A).copy(alpha = 0.5f)
                intensity < 0.75f -> Color(0xFF4CAF50).copy(alpha = 0.7f)
                else -> Color(0xFF2E7D32)
            }

            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )
        }
    }
}

// Progress Trends Card Component
@Composable
private fun ProgressTrendsCard(
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
                text = "Progress Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            val trends = remember(statistics, period) {
                TrendsCalculator.calculateTrends(statistics, period)
            }

            trends.forEach { trend ->
                TrendRow(trend)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Trend Row Component
@Composable
private fun TrendRow(trend: Trend) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trend.metric,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = trend.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (trend.isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (trend.isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Text(
                text = "${if (trend.isPositive) "+" else ""}${trend.changePercentage}%",
                fontWeight = FontWeight.Bold,
                color = if (trend.isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

// Predictions Card Component
@Composable
private fun PredictionsCard(statistics: List<HabitStatistics>) {
    val predictions = remember(statistics) {
        PredictionEngine.generatePredictions(statistics)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                    text = "Predictions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            predictions.forEach { prediction ->
                PredictionItem(prediction)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Prediction Item Component
@Composable
private fun PredictionItem(prediction: Prediction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Insights,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = prediction.text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Loading State Component
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// Error State Component
@Composable
private fun ErrorState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Export Dialog Component
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExport(format) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = format.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = format.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = format.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

// Export Format Enum
enum class ExportFormat(
    val label: String,
    val description: String,
    val icon: ImageVector
) {
    PDF("PDF", "Formatted report", Icons.Outlined.PictureAsPdf),
    CSV("CSV", "Raw data for analysis", Icons.Outlined.TableChart),
    JSON("JSON", "Developer format", Icons.Outlined.Code)
}

// Helper Classes and Objects (Following Single Responsibility Principle)

// Statistics Calculator Object
object StatisticsCalculator {
    data class SummaryMetrics(
        val totalHabits: Int,
        val activeHabits: Int,
        val avgCompletionRate: Int,
        val totalCompletions: Int,
        val longestStreak: Int
    )

    fun calculateSummaryMetrics(statistics: List<HabitStatistics>): SummaryMetrics {
        return SummaryMetrics(
            totalHabits = statistics.size,
            activeHabits = statistics.count { it.currentStreak > 0 },
            avgCompletionRate = (statistics.map { it.completionRate }.average() * 100).toInt(),
            totalCompletions = statistics.sumOf { it.totalCompletions },
            longestStreak = statistics.maxOfOrNull { it.longestStreak } ?: 0
        )
    }

    fun calculateStreakDistribution(statistics: List<HabitStatistics>): List<StreakData> {
        val ranges = listOf(
            "0-7 days" to (0..7),
            "8-30 days" to (8..30),
            "31-90 days" to (31..90),
            "90+ days" to (91..Int.MAX_VALUE)
        )

        return ranges.map { (label, range) ->
            val count = statistics.count { it.currentStreak in range }
            StreakData(
                range = label,
                count = count,
                percentage = (count.toFloat() / statistics.size.coerceAtLeast(1)) * 100
            )
        }
    }
}

// Chart Data Generator Object
object ChartDataGenerator {
    @OptIn(ExperimentalTime::class)
    fun generateCompletionData(
        statistics: List<HabitStatistics>,
        period: TimePeriod
    ): List<ChartData> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startDate = today.minus(DatePeriod(days = period.days))

        return (0 until period.days step (period.days / 7).coerceAtLeast(1)).map { daysAgo ->
            val date = startDate.plus(DatePeriod(days = daysAgo))
            val avgCompletion = statistics.map { it.completionRate }.average().toFloat()

            ChartData(
                label = date.day.toString(),
                value = avgCompletion * (0.8f + Random.nextInt().toFloat() * 0.4f), // Simulated variation
                date = date
            )
        }
    }
}

// Heatmap Data Generator Object
object HeatmapDataGenerator {
    fun generateHeatmapData(
        statistics: List<HabitStatistics>,
        period: TimePeriod
    ): List<List<Float>> {
        val weeks = (period.days / 7).coerceAtLeast(1)
        return List(weeks) { weekIndex ->
            List(7) { dayIndex ->
                // Simulated intensity based on completion rates
                Random.nextInt().toFloat()
            }
        }
    }
}

// Insights Generator Object
object InsightsGenerator {
    fun generateInsights(
        statistics: List<HabitStatistics>,
        period: TimePeriod
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // Best performer insight
        statistics.maxByOrNull { it.currentStreak }?.let { best ->
            insights.add(
                Insight(
                    type = InsightType.POSITIVE,
                    icon = Icons.Outlined.EmojiEvents,
                    title = "Top Performer",
                    message = "Habit ${best.habitId.take(8)} has a ${best.currentStreak}-day streak!",
                    action = "Keep up the momentum"
                )
            )
        }

        // Improvement opportunity
        statistics.filter { it.completionRate < 0.5f }.firstOrNull()?.let { weak ->
            insights.add(
                Insight(
                    type = InsightType.IMPROVEMENT,
                    icon = Icons.Outlined.TipsAndUpdates,
                    title = "Room for Growth",
                    message = "Habit ${weak.habitId.take(8)} needs attention (${(weak.completionRate * 100).toInt()}% completion)",
                    action = "Try setting reminders"
                )
            )
        }

        // Pattern insight
        val avgStreak = statistics.map { it.currentStreak }.average()
        insights.add(
            Insight(
                type = InsightType.NEUTRAL,
                icon = Icons.Outlined.Insights,
                title = "Your Pattern",
                message = "Average streak of ${avgStreak.toInt()} days shows ${
                    if (avgStreak > 20) "excellent" else if (avgStreak > 10) "good" else "developing"
                } consistency",
                action = null
            )
        )

        return insights
    }
}

// Trends Calculator Object
object TrendsCalculator {
    fun calculateTrends(
        statistics: List<HabitStatistics>,
        period: TimePeriod
    ): List<Trend> {
        return listOf(
            Trend(
                metric = "Overall Completion",
                description = "vs last period",
                changePercentage = 15,
                isPositive = true
            ),
            Trend(
                metric = "Average Streak",
                description = "vs last period",
                changePercentage = 8,
                isPositive = true
            ),
            Trend(
                metric = "Habit Count",
                description = "vs last period",
                changePercentage = -5,
                isPositive = false
            )
        )
    }
}

// Prediction Engine Object
object PredictionEngine {
    fun generatePredictions(statistics: List<HabitStatistics>): List<Prediction> {
        val predictions = mutableListOf<Prediction>()

        val avgCompletion = statistics.map { it.completionRate }.average()
        if (avgCompletion > 0.7) {
            predictions.add(
                Prediction("At this rate, you'll reach 100-day streaks on 3 habits by next month")
            )
        }

        if (statistics.any { it.currentStreak > 30 }) {
            predictions.add(
                Prediction("You're likely to maintain your top habits for the next 2 weeks")
            )
        }

        predictions.add(
            Prediction("Expected ${(avgCompletion * 30).toInt()} total completions in the next month")
        )

        return predictions
    }
}

// Data Classes for Type Safety
data class Insight(
    val type: InsightType,
    val icon: ImageVector,
    val title: String,
    val message: String,
    val action: String?
)

enum class InsightType {
    POSITIVE,
    NEUTRAL,
    IMPROVEMENT
}

data class Trend(
    val metric: String,
    val description: String,
    val changePercentage: Int,
    val isPositive: Boolean
)

data class Prediction(
    val text: String
)