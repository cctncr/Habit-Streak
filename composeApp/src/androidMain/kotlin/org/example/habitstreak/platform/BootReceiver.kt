package org.example.habitstreak.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.habitstreak.di.appModule
import org.example.habitstreak.di.androidModule
import org.example.habitstreak.domain.service.NotificationService
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Reschedules notifications after device reboot
 * Following Single Responsibility Principle
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Ensure Koin is initialized
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(appModule, androidModule)
            }
        }

        // Reschedule all notifications
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationService = GlobalContext.get().get<NotificationService>()
                notificationService.syncAllNotifications()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}