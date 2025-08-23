package org.example.habit_streak

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "HabitStreak",
    ) {
        App()
    }
}