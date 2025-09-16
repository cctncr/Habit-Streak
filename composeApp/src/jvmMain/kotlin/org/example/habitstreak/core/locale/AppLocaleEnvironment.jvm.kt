package org.example.habitstreak.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import java.util.*

actual object LocalAppLocale {
    actual val current: String
        @Composable
        get() = Locale.getDefault().language

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val newLocale = when (value) {
            null -> Locale.getDefault()
            else -> Locale(value)
        }

        // Update the JVM's default locale
        Locale.setDefault(newLocale)

        // Return empty ProvidedValue since JVM locale changes take effect globally
        return compositionLocalOf { Unit } provides Unit
    }
}