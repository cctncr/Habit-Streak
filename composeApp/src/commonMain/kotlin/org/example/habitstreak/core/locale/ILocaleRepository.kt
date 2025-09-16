package org.example.habitstreak.core.locale

import kotlinx.coroutines.flow.Flow

interface ILocaleRepository {
    suspend fun getLocale(): String
    suspend fun setLocale(localeCode: String)
    fun observeLocale(): Flow<String>
}