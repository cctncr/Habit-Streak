package org.example.habitstreak.presentation.navigation

import androidx.compose.runtime.compositionLocalOf

interface Navigator {
    fun navigateToCreateHabit(habitId: String? = null)
    fun navigateToHabitDetail(habitId: String)
    fun navigateBack()
}

val LocalNavigator = compositionLocalOf<Navigator> {
    error("No Navigator provided")
}