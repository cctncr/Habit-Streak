package org.example.habitstreak.core.theme

import kotlinx.coroutines.flow.Flow

interface IThemeRepository {
    suspend fun getTheme(): String
    suspend fun setTheme(themeCode: String)
    fun observeTheme(): Flow<String>
}