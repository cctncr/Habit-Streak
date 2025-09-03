package org.example.habitstreak.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.habitstreak.domain.model.Category

interface CategoryRepository {
    suspend fun createCategory(category: Category): Result<Category>
    suspend fun getCategoryById(id: String): Result<Category?>
    suspend fun getCategoryByName(name: String): Result<Category?>
    suspend fun getAllCategories(): Result<List<Category>>
    suspend fun getUsedCategories(): Result<List<Category>>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun deleteUnusedCustomCategories(): Result<Unit>
    suspend fun incrementUsageCount(categoryId: String): Result<Unit>
    suspend fun decrementUsageCount(categoryId: String): Result<Unit>

    // Habit-Category relations
    suspend fun getCategoriesForHabit(habitId: String): Result<List<Category>>
    suspend fun addCategoryToHabit(habitId: String, categoryId: String): Result<Unit>
    suspend fun removeCategoryFromHabit(habitId: String, categoryId: String): Result<Unit>
    suspend fun updateHabitCategories(habitId: String, categoryIds: List<String>): Result<Unit>

    // Observables
    fun observeAllCategories(): Flow<List<Category>>
    fun observeUsedCategories(): Flow<List<Category>>
    fun observeCategoriesForHabit(habitId: String): Flow<List<Category>>

    // Initialize predefined categories
    suspend fun initializePredefinedCategories(): Result<Unit>
}