package org.example.habitstreak

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.material3.Text
import org.example.habitstreak.app.di.appModule
import org.example.habitstreak.di.platformModule
import org.example.habitstreak.platform.IOSNotificationSetup
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}

private fun initKoin() {
    try {
        startKoin {
            modules(appModule, platformModule())
        }
    } catch (e: Exception) {
        // Koin might already be started, ignore
        println("Koin already initialized or failed to initialize: ${e.message}")
    }
}