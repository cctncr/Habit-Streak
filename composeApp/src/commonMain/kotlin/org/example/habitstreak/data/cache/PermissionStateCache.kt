package org.example.habitstreak.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory cache for permission state following Single Responsibility Principle
 * Reduces frequent permission checks while maintaining accuracy
 */
class PermissionStateCache {

    private val _permissionState = MutableStateFlow<Boolean?>(null)
    val permissionState: StateFlow<Boolean?> = _permissionState.asStateFlow()

    /**
     * Cache permission state
     * @param hasPermission Current permission status
     */
    fun cachePermissionState(hasPermission: Boolean) {
        _permissionState.value = hasPermission
    }

    /**
     * Get cached permission state
     * @return Cached permission status or null if not cached
     */
    fun getCachedPermissionState(): Boolean? {
        return _permissionState.value
    }

    /**
     * Manually invalidate the cache
     * Should be called when app comes to foreground or permission status might have changed
     */
    fun invalidateCache() {
        _permissionState.value = null
    }

    /**
     * Force refresh permission state in cache
     * Used when we know permission status has changed
     */
    fun refreshCache(hasPermission: Boolean) {
        cachePermissionState(hasPermission)
    }

    /**
     * Check if we have any cached state
     */
    fun hasCachedState(): Boolean {
        return _permissionState.value != null
    }
}