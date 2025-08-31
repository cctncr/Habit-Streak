package org.example.habitstreak

import android.app.Application
import org.example.habitstreak.di.appModule
import org.example.habitstreak.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HabitStreakApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@HabitStreakApplication)
            modules(appModule, platformModule())
        }
    }
}