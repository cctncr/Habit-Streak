package org.example.habitstreak

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.habitstreak.di.desktopModule
import org.example.habitstreak.di.initKoin

fun main() = application {
    initKoin(desktopModule)

    Window(
        onCloseRequest = ::exitApplication,
        title = "HabitStreak",
    ) {
        App()
    }
}