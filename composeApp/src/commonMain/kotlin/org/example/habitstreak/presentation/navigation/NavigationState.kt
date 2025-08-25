package org.example.habitstreak.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

sealed class Screen {
    data object Habits : Screen()
    data class CreateEdit(val habitId: String? = null) : Screen()
    data class HabitDetail(val habitId: String) : Screen()
    data object Statistics : Screen()
    data object Settings : Screen()
}

@Stable
class NavigationState {
    private val _backStack = mutableListOf<Screen>(Screen.Habits)
    val currentScreen: Screen
        get() = _backStack.lastOrNull() ?: Screen.Habits

    fun navigateTo(screen: Screen) {
        _backStack.add(screen)
    }

    fun navigateBack(): Boolean {
        if (_backStack.size > 1) {
            _backStack.removeLast()
            return true
        }
        return false
    }

    fun navigateToRoot() {
        _backStack.clear()
        _backStack.add(Screen.Habits)
    }
}

@Composable
fun rememberNavigationState(): NavigationState {
    return remember { NavigationState() }
}