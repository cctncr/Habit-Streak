package org.example.habitstreak.presentation.ui.components.category

import androidx.compose.runtime.Composable
import org.example.habitstreak.domain.helper.CategoryLocalizationHelper
import org.example.habitstreak.domain.model.Category
import org.jetbrains.compose.resources.stringResource

@Composable
fun getLocalizedCategoryName(category: Category): String {
    return if (category.key != null) {
        val stringResource = CategoryLocalizationHelper.getCategoryStringResource(category.key)
        if (stringResource != null) {
            stringResource(stringResource)
        } else {
            category.name
        }
    } else {
        category.name
    }
}
