package org.example.habitstreak.domain.usecase

import org.example.habitstreak.domain.repository.CategoryRepository

class InitializeCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke() {
        categoryRepository.initializePredefinedCategories()
    }
}