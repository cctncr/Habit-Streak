package org.example.habitstreak.domain.helper

import habitstreak.composeapp.generated.resources.*
import habitstreak.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.StringResource

object CategoryLocalizationHelper {

    fun getCategoryStringResource(key: String): StringResource? {
        return when (key.lowercase()) {
            "nutrition" -> Res.string.category_nutrition
            "finance" -> Res.string.category_finance
            "fitness" -> Res.string.category_fitness
            "art" -> Res.string.category_art
            "health" -> Res.string.category_health
            "social" -> Res.string.category_social
            "work" -> Res.string.category_work
            "business" -> Res.string.category_business
            "morning" -> Res.string.category_morning
            "evening" -> Res.string.category_evening
            "afternoon" -> Res.string.category_afternoon
            "other" -> Res.string.category_other
            else -> null
        }
    }
}
