package org.example.habitstreak.presentation.screen.statistics.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

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

enum class TimePeriod(val days: Int) {
    WEEK(7),
    MONTH(30),
    QUARTER(90),
    YEAR(365);

    @Composable
    fun getLabel(): String = when (this) {
        WEEK -> stringResource(Res.string.period_week)
        MONTH -> stringResource(Res.string.period_month)
        QUARTER -> stringResource(Res.string.period_quarter)
        YEAR -> stringResource(Res.string.period_year)
    }
}

enum class StatTab(val icon: ImageVector) {
    OVERVIEW(Icons.Outlined.Dashboard),
    HABITS(Icons.Outlined.CheckCircle),
    INSIGHTS(Icons.Outlined.Lightbulb),
    TRENDS(Icons.Outlined.TrendingUp);

    @Composable
    fun getLabel(): String = when (this) {
        OVERVIEW -> stringResource(Res.string.stat_tab_overview)
        HABITS -> stringResource(Res.string.stat_tab_habits)
        INSIGHTS -> stringResource(Res.string.stat_tab_insights)
        TRENDS -> stringResource(Res.string.stat_tab_trends)
    }
}

enum class ExportFormat(
    val extension: String,
    val mimeType: String
) {
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
    PDF("pdf", "application/pdf");

    @Composable
    fun getDisplayName(): String = when (this) {
        CSV -> stringResource(Res.string.export_csv)
        JSON -> stringResource(Res.string.export_json)
        PDF -> stringResource(Res.string.export_pdf)
    }
}

data class Insight(
    val type: InsightType,
    val title: String,
    val description: String,
    val value: String
)

enum class InsightType {
    STREAK, CONSISTENCY, IMPROVEMENT, WARNING
}

data class Trend(
    val name: String,
    val direction: String,
    val percentage: Float
)

data class Prediction(
    val habitName: String,
    val prediction: String,
    val confidence: Float
)