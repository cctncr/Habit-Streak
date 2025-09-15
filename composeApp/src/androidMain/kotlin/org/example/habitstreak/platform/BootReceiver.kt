package org.example.habitstreak.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.service.NotificationService
import org.koin.core.context.GlobalContext

/**
 * Reschedules notifications after device reboot
 * Following Single Responsibility Principle
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val koinContext = GlobalContext.getOrNull()
        if (koinContext == null) {
            return
        }

        // Reschedule all notifications
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationService = koinContext.get<NotificationService>()
                notificationService.syncAllNotifications()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}