package org.example.habitstreak.presentation.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ViewMode(
    val icon: ImageVector,
    val label: String,
    val rows: Int,
    val isGridClickable: Boolean,
    val cardHeightMultiplier: Float
) {
    object Large : ViewMode(
        icon = Icons.Outlined.GridView,
        label = "Large Grid",
        rows = 5,
        isGridClickable = true,
        cardHeightMultiplier = 1.4f
    )

    object Medium : ViewMode(
        icon = Icons.Outlined.ViewAgenda,
        label = "Medium Grid",
        rows = 3,
        isGridClickable = false,
        cardHeightMultiplier = 1.0f
    )

    object Compact : ViewMode(
        icon = Icons.AutoMirrored.Outlined.ViewList,
        label = "Compact",
        rows = 1,
        isGridClickable = false,
        cardHeightMultiplier = 0.6f
    )

    companion object {
        fun values() = listOf(Large, Medium, Compact)

        fun fromOrdinal(ordinal: Int): ViewMode = when (ordinal) {
            0 -> Large
            1 -> Medium
            2 -> Compact
            else -> Medium
        }
    }
}