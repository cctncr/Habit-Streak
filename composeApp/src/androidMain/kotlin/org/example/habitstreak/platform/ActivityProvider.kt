package org.example.habitstreak.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Provides access to the current Activity in a safe manner
 * Following Single Responsibility Principle - only handles Activity tracking
 */
interface ActivityProvider {
    fun getCurrentActivity(): Activity?
}

/**
 * Implementation using ActivityLifecycleCallbacks to track current Activity safely
 * Following Open/Closed Principle - extensible for future Activity tracking needs
 */
class ActivityProviderImpl(application: Application) : ActivityProvider {

    private var currentActivityRef: WeakReference<Activity>? = null

    init {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                // Keep reference during pause
            }

            override fun onActivityStopped(activity: Activity) {
                // Keep reference during stop in case of configuration change
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                // Keep reference during state save
            }

            override fun onActivityDestroyed(activity: Activity) {
                // Clear reference only if this is the current activity
                if (currentActivityRef?.get() == activity) {
                    currentActivityRef = null
                }
            }
        })
    }

    override fun getCurrentActivity(): Activity? {
        return currentActivityRef?.get()
    }
}