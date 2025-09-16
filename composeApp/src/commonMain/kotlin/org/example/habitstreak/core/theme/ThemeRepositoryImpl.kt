package org.example.habitstreak.core.theme

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.example.habitstreak.domain.repository.PreferencesRepository

class ThemeRepositoryImpl(
    private val preferencesRepository: PreferencesRepository
) : IThemeRepository {

    override suspend fun getTheme(): String {
        return preferencesRepository.getTheme().first()
    }

    override suspend fun setTheme(themeCode: String) {
        preferencesRepository.setTheme(themeCode)
    }

    override fun observeTheme(): Flow<String> {
        return preferencesRepository.getTheme()
    }
}