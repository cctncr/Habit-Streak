package org.example.habitstreak.domain.provider

object PredefinedCategoryProvider {

    data class PredefinedCategory(
        val key: String,
        val order: Int
    )

    val PREDEFINED_CATEGORIES = listOf(
        PredefinedCategory("nutrition", 1),
        PredefinedCategory("finance", 2),
        PredefinedCategory("fitness", 3),
        PredefinedCategory("art", 4),
        PredefinedCategory("health", 5),
        PredefinedCategory("social", 6),
        PredefinedCategory("work", 7),
        PredefinedCategory("business", 8),
        PredefinedCategory("morning", 9),
        PredefinedCategory("evening", 10),
        PredefinedCategory("afternoon", 11),
        PredefinedCategory("other", 12)
    )

    fun getCategoryKeys(): List<String> {
        return PREDEFINED_CATEGORIES.map { it.key }
    }

    fun isPredefinedKey(key: String): Boolean {
        return PREDEFINED_CATEGORIES.any { it.key.equals(key, ignoreCase = true) }
    }
}