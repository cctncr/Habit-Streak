package org.example.habitstreak.presentation.ui.utils

interface PlatformCapabilities {
    val supportsVibrationControl: Boolean
}

expect fun getPlatformCapabilities(): PlatformCapabilities
