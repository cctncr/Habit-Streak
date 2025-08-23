package org.example.habit_streak.presention.components

import androidx.compose.ui.graphics.Color
import org.example.habit_streak.domain.model.HabitColor

object HabitStreakTheme {
    val backgroundColor = Color(0xFFFAFAFA)
    val surfaceColor = Color.White
    val primaryTextColor = Color(0xFF2C3E50)
    val secondaryTextColor = Color(0xFF7F8C8D)
    val dividerColor = Color(0xFFECF0F1)
    val successColor = Color(0xFF27AE60)
    val errorColor = Color(0xFFE74C3C)

    // Convert HabitColor to Compose Color
    fun habitColorToComposeColor(habitColor: HabitColor): Color {
        val intColor = habitColor.hex.removePrefix("#").toInt(16)
        return if (habitColor.hex.length == 7) { // #RRGGBB
            Color(intColor or (0xFF shl 24)) // alpha ekle
        } else {
            Color(intColor)
        }
    }
}