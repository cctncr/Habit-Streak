package org.example.habitstreak.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.example.habitstreak.core.util.UuidGenerator
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.data.mapper.toData
import org.example.habitstreak.data.mapper.toDomain
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.repository.CategoryRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CategoryRepositoryImpl(
    private val database: HabitDatabase
) : CategoryRepository {

    private val categoryQueries = database.categoryQueries
    private val habitCategoryQueries = database.habitCategoryQueries

    @OptIn(ExperimentalTime::class)
    override suspend fun createCategory(category: Category): Result<Category> = withContext(Dispatchers.IO) {
        try {
            val categoryWithId = if (category.id.isEmpty()) {
                category.copy(id = UuidGenerator.generateUUID())
            } else category

            database.transaction {
                // Check if category with same name exists
                val existing = categoryQueries.selectByName(categoryWithId.name).executeAsOneOrNull()
                if (existing != null) {
                    throw IllegalArgumentException("Kategori zaten mevcut: ${categoryWithId.name}")
                }

                categoryQueries.insert(categoryWithId.toData())
            }
            Result.success(categoryWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategoryById(id: String): Result<Category?> = withContext(Dispatchers.IO) {
        try {
            val category = categoryQueries.selectById(id).executeAsOneOrNull()?.toDomain()
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategoryByName(name: String): Result<Category?> = withContext(Dispatchers.IO) {
        try {
            val category = categoryQueries.selectByName(name).executeAsOneOrNull()?.toDomain()
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val categories = categoryQueries.selectAll().executeAsList().map { it.toDomain() }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUsedCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val categories = categoryQueries.selectUsed().executeAsList().map { it.toDomain() }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                categoryQueries.selectById(category.id).executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Kategori bulunamadı")
                categoryQueries.insert(category.toData())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                categoryQueries.delete(categoryId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUnusedCustomCategories(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            categoryQueries.deleteUnused()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun incrementUsageCount(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            categoryQueries.updateUsageCount(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun decrementUsageCount(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            categoryQueries.decrementUsageCount(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategoriesForHabit(habitId: String): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val categories = habitCategoryQueries.selectByHabit(habitId).executeAsList().map { it.toDomain() }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCategoryToHabit(habitId: String, categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            habitCategoryQueries.insert(habitId, categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeCategoryFromHabit(habitId: String, categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            habitCategoryQueries.delete(habitId, categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHabitCategories(habitId: String, categoryIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                // Remove all existing categories for this habit
                habitCategoryQueries.deleteByHabit(habitId)

                // Add new categories
                categoryIds.forEach { categoryId ->
                    habitCategoryQueries.insert(habitId, categoryId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllCategories(): Flow<List<Category>> {
        return categoryQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeUsedCategories(): Flow<List<Category>> {
        return categoryQueries.selectUsed()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeCategoriesForHabit(habitId: String): Flow<List<Category>> {
        return habitCategoryQueries.selectByHabit(habitId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun initializePredefinedCategories(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                Category.PREDEFINED_CATEGORIES.forEach { categoryName ->
                    // Check if category already exists
                    val existing = categoryQueries.selectByName(categoryName).executeAsOneOrNull()
                    if (existing == null) {
                        val category = Category(
                            id = UuidGenerator.generateUUID(),
                            name = categoryName,
                            isCustom = false,
                            usageCount = 0,
                            createdAt = Clock.System.now()
                        )
                        categoryQueries.insert(category.toData())
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}