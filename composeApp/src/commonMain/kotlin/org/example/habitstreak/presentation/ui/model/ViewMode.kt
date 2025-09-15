package org.example.habitstreak.presentation.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

sealed class ViewMode(
    val icon: ImageVector,
    val rows: Int,
    val isGridClickable: Boolean,
    val cardHeightMultiplier: Float
) {
    object Large : ViewMode(
        icon = Icons.Outlined.GridView,
        rows = 5,
        isGridClickable = true,
        cardHeightMultiplier = 1.4f
    )

    object Medium : ViewMode(
        icon = Icons.Outlined.ViewAgenda,
        rows = 3,
        isGridClickable = false,
        cardHeightMultiplier = 1.0f
    )

    object Compact : ViewMode(
        icon = Icons.AutoMirrored.Outlined.ViewList,
        rows = 1,
        isGridClickable = false,
        cardHeightMultiplier = 0.6f
    )

    @Composable
    fun getLabel(): String = when (this) {
        Large -> stringResource(Res.string.view_mode_large)
        Medium -> stringResource(Res.string.view_mode_medium)
        Compact -> stringResource(Res.string.view_mode_compact)
    }

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