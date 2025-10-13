package org.example.habitstreak.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.example.habitstreak.presentation.screen.archived.ArchivedHabitsScreen
import org.example.habitstreak.presentation.screen.create_edit_habit.CreateEditHabitScreen
import org.example.habitstreak.presentation.screen.habit_detail.HabitDetailScreen
import org.example.habitstreak.presentation.screen.habits.HabitsScreen
import org.example.habitstreak.presentation.screen.settings.SettingsScreen
import org.example.habitstreak.presentation.screen.statistics.StatisticsScreen

@Composable
fun AppNavigation(
    deepLinkHabitId: String? = null,
    shouldNavigateToHabit: Boolean = false,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navigationState = rememberNavigationState()
    var currentScreen by remember { mutableStateOf(navigationState.currentScreen) }

    // Handle deep link navigation
    LaunchedEffect(deepLinkHabitId, shouldNavigateToHabit) {
        if (deepLinkHabitId != null && shouldNavigateToHabit) {
            navigationState.navigateTo(Screen.HabitDetail(deepLinkHabitId))
            currentScreen = navigationState.currentScreen
            onDeepLinkHandled()
        }
    }

    BackHandler(enabled = currentScreen != Screen.Habits) {
        navigationState.navigateBack()
        currentScreen = navigationState.currentScreen
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

                targetState is Screen.Settings && initialState is Screen.Habits -> {
                    slideInHorizontally(
                        initialOffsetX = { width -> -width },
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { width -> width / 3 },
                        animationSpec = tween(300)
                    )
                }

                targetState is Screen.Habits && initialState is Screen.Settings -> {
                    slideInHorizontally(
                        initialOffsetX = { width -> width / 3 },
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { width -> -width },
                        animationSpec = tween(300)
                    )
                }

                else -> {
                    slideInHorizontally(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(animationSpec = tween(300))
                }
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is Screen.Habits -> {
                HabitsScreen(
                    onNavigateToCreateHabit = {
                        navigationState.navigateTo(Screen.CreateEdit())
                        currentScreen = navigationState.currentScreen
                    },
                    onNavigateToStatistics = {
                        navigationState.navigateTo(Screen.Statistics)
                        currentScreen = navigationState.currentScreen
                    },
                    onNavigateToSettings = {
                        navigationState.navigateTo(Screen.Settings)
                        currentScreen = navigationState.currentScreen
                    },
                    onNavigateToHabitDetail = { habitId ->
                        navigationState.navigateTo(Screen.HabitDetail(habitId))
                        currentScreen = navigationState.currentScreen
                    }
                )
            }

            is Screen.CreateEdit -> {
                CreateEditHabitScreen(
                    habitId = screen.habitId,
                    onNavigateBack = {
                        navigationState.navigateBack()
                        currentScreen = navigationState.currentScreen
                    }
                )
            }

            is Screen.Statistics -> {
                StatisticsScreen(
                    onNavigateBack = {
                        navigationState.navigateBack()
                        currentScreen = navigationState.currentScreen
                    },
                    onNavigateToHabit = { habitId ->
                        navigationState.navigateTo(Screen.CreateEdit(habitId))
                        currentScreen = navigationState.currentScreen
                    }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    onNavigateBack = {
                        navigationState.navigateBack()
                        currentScreen = navigationState.currentScreen
                    },
                    onNavigateToAbout = {
                        // TODO: Navigate to about screen
                    },
                    onNavigateToBackup = {
                        // TODO: Navigate to backup screen
                    },
                    onNavigateToArchivedHabits = {
                        // Navigate to archived habits
                        navigationState.navigateTo(Screen.ArchivedHabits)
                        currentScreen = navigationState.currentScreen
                    }
                )
            }

            is Screen.ArchivedHabits -> {
                ArchivedHabitsScreen(
                    onNavigateBack = {
                        navigationState.navigateBack()
                        currentScreen = navigationState.currentScreen
                    },
                    onRestoreHabit = { habitId ->
                        navigationState.navigateBack()
                        currentScreen = navigationState.currentScreen
                    }
                )
            }

            is Screen.HabitDetail -> {
                HabitDetailScreen(
                    habitId = screen.habitId,
                    onNavigateBack = {
                        navigationState.navigateBack()
                        currentScreen = navigationState.currentScreen
                    },
                    onNavigateToEdit = {
                        navigationState.navigateTo(Screen.CreateEdit(screen.habitId))
                        currentScreen = navigationState.currentScreen
                    }
                )
            }
        }
    }
}

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)