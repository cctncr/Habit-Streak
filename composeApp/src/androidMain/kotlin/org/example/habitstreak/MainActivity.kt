package org.example.habitstreak

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private var deepLinkHabitId by mutableStateOf<String?>(null)
    private var shouldNavigateToHabit by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            App(
                deepLinkHabitId = deepLinkHabitId,
                shouldNavigateToHabit = shouldNavigateToHabit,
                onDeepLinkHandled = {
                    deepLinkHabitId = null
                    shouldNavigateToHabit = false
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val habitId = intent.getStringExtra("habit_id")
        val navigateToHabit = intent.getBooleanExtra("navigate_to_habit", false)

        if (habitId != null && navigateToHabit) {
            deepLinkHabitId = habitId
            shouldNavigateToHabit = true
        }
    }
}