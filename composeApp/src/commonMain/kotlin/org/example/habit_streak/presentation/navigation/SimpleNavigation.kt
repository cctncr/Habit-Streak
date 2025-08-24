package org.example.habit_streak.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.example.habit_streak.presentation.screen.create_edit_habit.CreateEditHabitScreen
import org.example.habit_streak.presentation.screen.habits.HabitsScreen

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(NavigationScreen.Habits) }
    var selectedHabitId by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                // Forward navigation
                slideInHorizontally(
                    initialOffsetX = { width -> width },
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { width -> -width / 3 },
                    animationSpec = tween(300)
                )
            } else {
                // Backward navigation
                slideInHorizontally(
                    initialOffsetX = { width -> -width / 3 },
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { width -> width },
                    animationSpec = tween(300)
                )
            }
        }
    ) { screen ->
        when (screen) {
            NavigationScreen.Habits -> {
                HabitsScreen(
                    onNavigateToCreateHabit = {
                        selectedHabitId = null
                        currentScreen = NavigationScreen.CreateEdit
                    },
                    onNavigateToHabitDetail = { habitId ->
                        selectedHabitId = habitId
                        currentScreen = NavigationScreen.CreateEdit
                    }
                )
            }
            NavigationScreen.CreateEdit -> {
                CreateEditHabitScreen(
                    habitId = selectedHabitId,
                    onNavigateBack = {
                        currentScreen = NavigationScreen.Habits
                    }
                )
            }
        }
    }
}

enum class NavigationScreen {
    Habits,
    CreateEdit
}
