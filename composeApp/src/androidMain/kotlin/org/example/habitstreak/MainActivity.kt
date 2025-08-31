package org.example.habitstreak

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            App()
        }

        // Handle deep links from notifications
        handleNotificationIntent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent()
    }

    private fun handleNotificationIntent() {
        intent?.getStringExtra("habitId")?.let { habitId ->
            // Navigate to habit detail screen
            // You can use a shared navigation state or event bus
        }
    }
}