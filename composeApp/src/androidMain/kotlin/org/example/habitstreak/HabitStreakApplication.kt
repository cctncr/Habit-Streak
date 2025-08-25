package org.example.habitstreak

import android.app.Application
import org.example.habitstreak.di.androidModule
import org.example.habitstreak.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class HabitStreakApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin(androidModule) {
            androidLogger(Level.DEBUG)
            androidContext(this@HabitStreakApplication)
        }
    }
}