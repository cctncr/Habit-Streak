package org.example.habitstreak

import org.example.habitstreak.di.appModule
import org.example.habitstreak.di.iosModule
import org.koin.core.context.startKoin

fun initKoinIOS() {
    startKoin {
        modules(appModule, iosModule)
    }
}