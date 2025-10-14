package org.example.habitstreak.platform.initialization

import android.app.Activity
import android.view.ViewTreeObserver
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller for managing Android SplashScreen API.
 *
 * This class follows SOLID principles:
 * - Single Responsibility: Only manages splash screen visibility
 * - Open/Closed: Can be extended without modification
 * - Dependency Inversion: Depends on StateFlow abstraction for ready state
 *
 * The splash screen will remain visible until the app signals it's ready
 * by updating the isReady StateFlow to true.
 *
 * @param activity The activity to install splash screen on
 * @param isReady StateFlow that indicates when app is ready to be shown
 */
class SplashScreenController(
    private val activity: Activity,
    private val isReady: StateFlow<Boolean>
) {
    private var splashScreen: SplashScreen? = null

    /**
     * Install and configure the splash screen.
     * This should be called in Activity.onCreate() before setContent().
     */
    fun install() {
        splashScreen = activity.installSplashScreen()

        // Keep the splash screen visible until app is ready
        setupSplashScreenExitListener()
    }

    private fun setupSplashScreenExitListener() {
        val contentView = activity.findViewById<android.view.View>(android.R.id.content)

        contentView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (isReady.value) {
                        // App is ready, remove listener and allow drawing
                        contentView.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        // Keep splash screen visible
                        false
                    }
                }
            }
        )
    }
}
