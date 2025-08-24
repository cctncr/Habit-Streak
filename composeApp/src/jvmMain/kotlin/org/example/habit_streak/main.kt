package org.example.habit_streak

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.habit_streak.di.desktopModule
import org.example.habit_streak.di.initKoin

fun main() = application {
    initKoin(desktopModule)

    Window(
        onCloseRequest = ::exitApplication,
        title = "HabitStreak",
    ) {
        App()
    }
}