package org.example.habitstreak

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.habitstreak.app.di.appModule
import org.example.habitstreak.app.di.platformModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(appModule, platformModule())
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "HabitStreak",
    ) {
        App()
    }
}