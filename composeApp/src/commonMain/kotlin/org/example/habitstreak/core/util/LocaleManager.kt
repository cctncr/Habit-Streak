package org.example.habitstreak.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow

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
            val locale = LocalCurrentLocale.current
            println("📍 AppLocale.current(): Returning ${locale.code}")
            return locale
        }
    }
}

/**
 * Composition local for current locale
 */
val LocalCurrentLocale = compositionLocalOf { AppLocale.ENGLISH }

/**
 * Locale manager for handling language switching
 */
object LocaleManager {
    private val _currentLocale = MutableStateFlow(AppLocale.ENGLISH)
    val currentLocale: StateFlow<AppLocale> = _currentLocale.asStateFlow()

    fun getCurrentLocale(): AppLocale = _currentLocale.value

    fun setLocale(locale: AppLocale) {
        println("🔧 LocaleManager.setLocale: Changing locale from ${_currentLocale.value.code} to ${locale.code}")
        _currentLocale.value = locale
        println("✅ LocaleManager.setLocale: Locale changed to ${_currentLocale.value.code}")
    }

    fun getAvailableLocales(): List<AppLocale> = AppLocale.values().toList()
}

/**
 * Composable wrapper that provides locale context
 */
@Composable
fun LocaleProvider(
    locale: AppLocale = LocaleManager.getCurrentLocale(),
    content: @Composable () -> Unit
) {
    println("🌐 LocaleProvider: Providing locale ${locale.code} to composition")
    CompositionLocalProvider(
        LocalCurrentLocale provides locale,
        content = content
    )
}