package org.example.habitstreak

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.platform.initialization.AndroidAppInitializer
import org.example.habitstreak.platform.initialization.SplashScreenController
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private var deepLinkHabitId by mutableStateOf<String?>(null)
    private var shouldNavigateToHabit by mutableStateOf(false)

    // Splash screen state management
    private val isAppReady = MutableStateFlow(false)
    private val isDataReady = MutableStateFlow(false)
    private val appInitializer = AndroidAppInitializer()

    // Inject HabitRepository via Koin
    private val habitRepository: HabitRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreenController = SplashScreenController(
            activity = this,
            isReady = isAppReady
        )
        splashScreenController.install()

        super.onCreate(savedInstanceState)

        handleIntent(intent)

        // Initialize app asynchronously - wait for habits to load from database
        lifecycleScope.launch {
            try {
                appInitializer.initialize(habitRepository)
                // Data loaded successfully - now Compose can start rendering
                isDataReady.value = true
            } catch (e: Exception) {
                // Log error but continue - app will show error state in UI
                e.printStackTrace()
                // Even on error, mark as ready to prevent indefinite splash screen
                isDataReady.value = true
                isAppReady.value = true
            }
        }

        setContent {
            val dataReady by isDataReady.collectAsState()

            // Don't render anything until data is loaded from database
            // This keeps the splash screen visible
            if (!dataReady) {
                return@setContent
            }

            // Now data is loaded, render the UI
            App(
                deepLinkHabitId = deepLinkHabitId,
                shouldNavigateToHabit = shouldNavigateToHabit,
                onDeepLinkHandled = {
                    deepLinkHabitId = null
                    shouldNavigateToHabit = false
                },
                onFirstFrameRendered = {
                    // This is called when the first HabitCard is positioned
                    // meaning the UI is actually rendered on screen
                    isAppReady.value = true
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