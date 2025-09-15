package org.example.habitstreak.presentation.screen.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import org.example.habitstreak.domain.model.HabitStatistics
import org.example.habitstreak.presentation.viewmodel.StatisticsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHabit: (String) -> Unit = {},
    viewModel: StatisticsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.nav_statistics),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.nav_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Export functionality */ }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(Res.string.action_export)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
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
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Cards
                    item {
                        SummaryCards(uiState.statistics)
                    }

                    // Completion Chart
                    item {
                        CompletionChart(uiState.statistics)
                    }

                    // Habit List
                    items(uiState.statistics) { stat ->
                        HabitStatCard(
                            statistics = stat,
                            onClick = { onNavigateToHabit(stat.habitId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCards(statistics: List<org.example.habitstreak.domain.model.HabitStatistics>) {
    val totalHabits = statistics.size
    val activeHabits = statistics.count { it.currentStreak > 0 }
    val avgCompletion = if (statistics.isNotEmpty()) {
        (statistics.map { it.completionRate }.average() * 100).toInt()
    } else 0
    val totalChecks = statistics.sumOf { it.totalCompletions }
    val bestStreak = statistics.maxOfOrNull { it.longestStreak } ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(Res.string.stat_tab_overview),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.stat_active_habits),
                value = "$activeHabits",
                subtitle = "of $totalHabits",
                icon = Icons.Outlined.CheckCircle,
                color = MaterialTheme.colorScheme.primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.stat_avg_completion),
                value = "$avgCompletion%",
                subtitle = stringResource(Res.string.stat_this_period),
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
                title = stringResource(Res.string.stat_total_checks),
                value = "$totalChecks",
                subtitle = stringResource(Res.string.stat_all_time),
                icon = Icons.Outlined.Done,
                color = MaterialTheme.colorScheme.tertiary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.stat_best_streak),
                value = "$bestStreak",
                subtitle = stringResource(Res.string.unit_days),
                icon = Icons.Outlined.LocalFireDepartment,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
private fun CompletionChart(statistics: List<org.example.habitstreak.domain.model.HabitStatistics>) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.stat_completion_trend),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Simple line chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                drawSimpleChart(statistics)
            }
        }
    }
}

private fun DrawScope.drawSimpleChart(statistics: List<org.example.habitstreak.domain.model.HabitStatistics>) {
    if (statistics.isEmpty()) return

    val padding = 40.dp.toPx()
    val chartWidth = size.width - padding * 2
    val chartHeight = size.height - padding * 2

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

    // Draw simple completion rate line
    val path = Path()
    val maxValue = 1f
    val pointSpacing = chartWidth / (statistics.size - 1).coerceAtLeast(1)

    statistics.forEachIndexed { index, stat ->
        val x = padding + index * pointSpacing
        val y = size.height - padding - (stat.completionRate / maxValue * chartHeight)

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }

        // Draw points
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = 4f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
    }

    // Draw line
    drawPath(
        path = path,
        color = Color(0xFF4CAF50),
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatInfo(
                        icon = Icons.Outlined.LocalFireDepartment,
                        value = "${statistics.currentStreak}",
                        label = stringResource(Res.string.label_streak)
                    )
                    StatInfo(
                        icon = Icons.Outlined.CheckCircle,
                        value = "${(statistics.completionRate * 100).toInt()}%",
                        label = stringResource(Res.string.label_completion_rate)
                    )
                    StatInfo(
                        icon = Icons.Outlined.Done,
                        value = "${statistics.totalCompletions}",
                        label = stringResource(Res.string.label_total_completions)
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

@Composable
private fun StatInfo(
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