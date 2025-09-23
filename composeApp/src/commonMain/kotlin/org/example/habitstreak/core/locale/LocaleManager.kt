package org.example.habitstreak.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * Supported locales for the application
 */
enum class AppLocale(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe");

    companion object {
        fun fromCode(code: String): AppLocale {
            return entries.find { it.code == code } ?: ENGLISH
        }

        @Composable
        fun current(): AppLocale {
            return LocalCurrentLocale.current
        }
    }
}

/**
 * Composition local for current locale
 */
val LocalCurrentLocale = compositionLocalOf { AppLocale.ENGLISH }

/**
 * Composable wrapper that provides locale context
 */
@Composable
fun LocaleProvider(
    locale: AppLocale,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalCurrentLocale provides locale,
        content = content
    )
}