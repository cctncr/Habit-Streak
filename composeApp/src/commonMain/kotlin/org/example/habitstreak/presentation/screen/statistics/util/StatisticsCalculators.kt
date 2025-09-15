package org.example.habitstreak.presentation.screen.statistics.util

import androidx.compose.runtime.Composable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.example.habitstreak.domain.model.HabitStatistics
import org.example.habitstreak.presentation.screen.statistics.model.*
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object StatisticsCalculator {
    fun calculateCompletionRate(statistics: List<HabitStatistics>): Float {
        if (statistics.isEmpty()) return 0f
        return statistics.map { it.completionRate }.average().toFloat()
    }

    fun calculateAverageStreak(statistics: List<HabitStatistics>): Float {
        if (statistics.isEmpty()) return 0f
        return statistics.map { it.currentStreak }.average().toFloat()
    }

    fun calculateTotalHabits(statistics: List<HabitStatistics>): Int {
        return statistics.size
    }

    fun calculateActiveHabits(statistics: List<HabitStatistics>): Int {
        return statistics.count { it.currentStreak > 0 }
    }

    fun calculateTotalCompletions(statistics: List<HabitStatistics>): Int {
        return statistics.sumOf { it.totalCompletions }
    }

    fun calculateBestStreak(statistics: List<HabitStatistics>): Int {
        return statistics.maxOfOrNull { it.longestStreak } ?: 0
    }
}

object ChartDataGenerator {
    @OptIn(ExperimentalTime::class)
    fun generateWeeklyData(): List<ChartData> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return (0 until 7).map { dayOffset ->
            val date = today.minus(DatePeriod(days = dayOffset))
            ChartData(
                label = date.dayOfWeek.name.take(3),
                value = Random.nextFloat() * 100,
                date = date
            )
        }.reversed()
    }
}

object HeatmapDataGenerator {
    @OptIn(ExperimentalTime::class)
    fun generateYearData(): Map<LocalDate, Float> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val yearAgo = today.minus(DatePeriod(days = 365))
        val data = mutableMapOf<LocalDate, Float>()

        var currentDate = yearAgo
        while (currentDate <= today) {
            data[currentDate] = Random.nextFloat()
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        return data
    }
}

object InsightsGenerator {
    @Composable
    fun generateInsights(statistics: List<HabitStatistics>): List<Insight> {
        val insights = mutableListOf<Insight>()

        // Best streak insight
        val bestStreak = StatisticsCalculator.calculateBestStreak(statistics)
        if (bestStreak > 0) {
            insights.add(
                Insight(
                    type = InsightType.STREAK,
                    title = stringResource(Res.string.insight_best_streak),
                    description = stringResource(Res.string.insight_best_streak_desc),
                    value = "$bestStreak ${stringResource(Res.string.unit_days)}"
                )
            )
        }

        // Consistency insight
        val completionRate = StatisticsCalculator.calculateCompletionRate(statistics)
        insights.add(
            Insight(
                type = InsightType.CONSISTENCY,
                title = stringResource(Res.string.insight_consistency),
                description = stringResource(Res.string.insight_consistency_desc),
                value = "${(completionRate * 100).toInt()}%"
            )
        )

        // Total completions insight
        val totalCompletions = StatisticsCalculator.calculateTotalCompletions(statistics)
        if (totalCompletions > 0) {
            insights.add(
                Insight(
                    type = InsightType.IMPROVEMENT,
                    title = stringResource(Res.string.insight_total_completions),
                    description = stringResource(Res.string.insight_total_completions_desc),
                    value = "$totalCompletions ${stringResource(Res.string.unit_times)}"
                )
            )
        }

        return insights
    }
}

object TrendsCalculator {
    fun calculateTrends(statistics: List<HabitStatistics>): List<Trend> {
        return statistics.map { stat ->
            val direction = when {
                stat.currentStreak >= stat.longestStreak * 0.8 -> "up"
                stat.currentStreak <= stat.longestStreak * 0.3 -> "down"
                else -> "stable"
            }

            // Calculate percentage based on completion rate and current vs longest streak
            val streakRatio = if (stat.longestStreak > 0) {
                stat.currentStreak.toFloat() / stat.longestStreak
            } else 0f

            val percentage = (stat.completionRate * 50f) + (streakRatio * 50f)

            Trend(
                name = "Habit ${stat.habitId.take(8)}",
                direction = direction,
                percentage = percentage
            )
        }
    }
}

object PredictionEngine {
    @Composable
    fun generatePredictions(statistics: List<HabitStatistics>): List<Prediction> {
        return statistics.take(3).map { stat ->
            val prediction = when {
                stat.completionRate >= 0.8f && stat.currentStreak >= stat.longestStreak * 0.7f ->
                    stringResource(Res.string.prediction_excellent)
                stat.completionRate >= 0.6f && stat.currentStreak > 0 ->
                    stringResource(Res.string.prediction_continue)
                stat.completionRate < 0.4f || stat.currentStreak == 0 ->
                    stringResource(Res.string.prediction_attention)
                else ->
                    stringResource(Res.string.prediction_improvement)
            }

            val confidence = when {
                stat.completionRate >= 0.8f -> 0.85f + Random.nextFloat() * 0.15f // 85-100%
                stat.completionRate >= 0.6f -> 0.7f + Random.nextFloat() * 0.2f  // 70-90%
                stat.completionRate >= 0.4f -> 0.6f + Random.nextFloat() * 0.25f // 60-85%
                else -> 0.75f + Random.nextFloat() * 0.2f // 75-95%
            }

            Prediction(
                habitName = "Habit ${stat.habitId.take(8)}",
                prediction = prediction,
                confidence = confidence
            )
        }
    }
}