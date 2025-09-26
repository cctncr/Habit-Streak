package org.example.habitstreak

import org.example.habitstreak.app.di.appModule
import org.example.habitstreak.app.di.platformModule
import org.koin.core.context.startKoin

fun initKoinIOS() {
    startKoin {
        modules(appModule, platformModule())
    }
}