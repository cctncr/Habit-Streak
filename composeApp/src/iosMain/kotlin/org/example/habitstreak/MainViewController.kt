package org.example.habitstreak

import androidx.compose.ui.window.ComposeUIViewController
import org.example.habitstreak.di.appModule
import org.example.habitstreak.di.platformModule
import org.example.habitstreak.platform.IOSNotificationSetup
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    IOSNotificationSetup.initialize()
    App()
}

private fun initKoin() {
    startKoin {
        modules(appModule, platformModule())
    }
}