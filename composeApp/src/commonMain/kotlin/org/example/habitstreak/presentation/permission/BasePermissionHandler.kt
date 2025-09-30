package org.example.habitstreak.presentation.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Unified permission handler that encapsulates all permission-related logic
 * Eliminates code duplication across screens
 */
class BasePermissionHandler(
    private val flowHandler: PermissionFlowHandler,
    private val platformLauncher: PlatformPermissionLauncher,
    private val coroutineScope: CoroutineScope,
    private val habitName: String? = null
) {

    /**
     * Check if permission is currently granted
     */
    suspend fun hasPermission(): Boolean {
        return flowHandler.hasPermission()
    }

    /**
     * Start permission request flow
     */
    fun requestPermissionFlow(onResult: (PermissionFlowResult) -> Unit) {
        coroutineScope.launch {
            flowHandler.requestPermissionWithFlow(
                habitName = habitName,
                onResult = onResult
            )
        }
    }

    /**
     * Launch platform-specific permission request
     */
    fun launchPlatformPermissionRequest() {
        platformLauncher.requestNotificationPermission()
    }

    /**
     * Handle system permission result
     */
    fun handleSystemResult(granted: Boolean, canAskAgain: Boolean, onResult: (PermissionFlowResult) -> Unit) {
        coroutineScope.launch {
            flowHandler.handleSystemPermissionResult(
                habitName = habitName,
                onResult = onResult
            )
        }
    }

    /**
     * Handle never ask again
     */
    fun handleNeverAskAgain() {
        flowHandler.handleNeverAskAgain()
    }

    /**
     * Open app settings
     */
    fun openSettings(): Boolean {
        return coroutineScope.launch {
            flowHandler.handleOpenSettings()
        }.let { true }
    }

    /**
     * Invalidate permission cache
     */
    fun invalidateCache() {
        flowHandler.invalidateCache()
    }
}

/**
 * Composable to remember BasePermissionHandler instance
 */
@Composable
fun rememberBasePermissionHandler(
    habitName: String? = null,
    onPermissionResult: (granted: Boolean, canAskAgain: Boolean) -> Unit = { _, _ -> }
): BasePermissionHandler {
    val flowHandler: PermissionFlowHandler = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val platformLauncher = rememberPlatformPermissionLauncher { granted, canAskAgain ->
        onPermissionResult(granted, canAskAgain)
    }

    return remember(habitName) {
        BasePermissionHandler(
            flowHandler = flowHandler,
            platformLauncher = platformLauncher,
            coroutineScope = coroutineScope,
            habitName = habitName
        )
    }
}