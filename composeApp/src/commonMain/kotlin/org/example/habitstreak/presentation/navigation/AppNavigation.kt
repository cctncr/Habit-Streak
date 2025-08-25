package org.example.habitstreak.presentation.navigation

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
import androidx.compose.ui.ExperimentalComposeUiApi
import org.example.habitstreak.presentation.screen.create_edit_habit.CreateEditHabitScreen
import org.example.habitstreak.presentation.screen.habits.HabitsScreen

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppNavigation() {
    val navigationState = rememberNavigationState()
    var currentScreen by remember { mutableStateOf(navigationState.currentScreen) }

    // Handle back press
    BackHandler(enabled = currentScreen != Screen.Habits) {
        if (navigationState.navigateBack()) {
            currentScreen = navigationState.currentScreen
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            when {
                targetState is Screen.CreateEdit && initialState is Screen.Habits -> {
                    slideInHorizontally(
                        initialOffsetX = { width -> width },
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { width -> -width / 3 },
                        animationSpec = tween(300)
                    )
                }
                targetState is Screen.Habits && initialState is Screen.CreateEdit -> {
                    slideInHorizontally(
                        initialOffsetX = { width -> -width / 3 },
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { width -> width },
                        animationSpec = tween(300)
                    )
                }
                else -> {
                    slideInHorizontally(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(animationSpec = tween(300))
                }
            }
        }
    ) { screen ->
        when (screen) {
            is Screen.Habits -> {
                HabitsScreen(
                    onNavigateToCreateHabit = {
                        navigationState.navigateTo(Screen.CreateEdit())
                        currentScreen = navigationState.currentScreen
                    },
                    onNavigateToHabitDetail = { habitId ->
                        navigationState.navigateTo(Screen.CreateEdit(habitId))
                        currentScreen = navigationState.currentScreen
                    }
                )
            }
            is Screen.CreateEdit -> {
                CreateEditHabitScreen(
                    habitId = screen.habitId,
                    onNavigateBack = {
                        if (navigationState.navigateBack()) {
                            currentScreen = navigationState.currentScreen
                        }
                    }
                )
            }
            is Screen.HabitDetail -> {
                // Future implementation
            }
            is Screen.Statistics -> {
                // Future implementation
            }
        }
    }
}

// BackHandler for multiplatform
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)