package org.example.habitstreak.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun setNotificationsEnabled(enabled: Boolean)
    fun isNotificationsEnabled(): Flow<Boolean>
    suspend fun getNotificationsEnabled(): Boolean

    suspend fun setSoundEnabled(enabled: Boolean)
    fun isSoundEnabled(): Flow<Boolean>

    suspend fun setVibrationEnabled(enabled: Boolean)
    fun isVibrationEnabled(): Flow<Boolean>

    suspend fun setTheme(theme: String)
    fun getTheme(): Flow<String>

    suspend fun setLocale(locale: String)
    fun getLocale(): Flow<String>
}