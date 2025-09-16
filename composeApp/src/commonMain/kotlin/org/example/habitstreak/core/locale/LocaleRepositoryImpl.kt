package org.example.habitstreak.core.locale

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.example.habitstreak.domain.repository.PreferencesRepository

class LocaleRepositoryImpl(
    private val preferencesRepository: PreferencesRepository
) : ILocaleRepository {

    override suspend fun getLocale(): String {
        return preferencesRepository.getLocale().first()
    }

    override suspend fun setLocale(localeCode: String) {
        preferencesRepository.setLocale(localeCode)
    }

    override fun observeLocale(): Flow<String> {
        return preferencesRepository.getLocale()
    }
}