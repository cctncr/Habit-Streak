package org.example.habitstreak.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.*

actual object LocalAppLocale {
    actual val current: String
        @Composable
        get() = Locale.getDefault().language

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val newLocale = when (value) {
            null -> Locale.getDefault()
            else -> Locale(value)
        }

        // Update the default locale
        Locale.setDefault(newLocale)

        // Create new configuration with updated locale
        val newConfiguration = android.content.res.Configuration(configuration).apply {
            setLocale(newLocale)
        }

        return androidx.compose.ui.platform.LocalConfiguration provides newConfiguration
    }
}