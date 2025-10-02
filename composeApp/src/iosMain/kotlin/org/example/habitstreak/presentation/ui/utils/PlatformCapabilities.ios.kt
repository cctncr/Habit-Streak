package org.example.habitstreak.presentation.ui.utils

actual fun getPlatformCapabilities(): PlatformCapabilities = object : PlatformCapabilities {
    override val supportsVibrationControl: Boolean = false
}
