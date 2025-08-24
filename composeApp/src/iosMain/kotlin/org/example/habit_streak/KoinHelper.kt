package org.example.habit_streak

import org.example.habit_streak.di.appModule
import org.example.habit_streak.di.iosModule
import org.koin.core.context.startKoin

fun initKoinIOS() {
    startKoin {
        modules(appModule, iosModule)
    }
}