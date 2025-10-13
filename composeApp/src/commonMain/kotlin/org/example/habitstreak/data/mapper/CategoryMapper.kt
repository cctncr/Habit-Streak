package org.example.habitstreak.data.mapper

import org.example.habitstreak.domain.model.Category
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.example.habitstreak.data.local.Category as DataCategory

@OptIn(ExperimentalTime::class)
fun DataCategory.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        key = key,
        isCustom = isCustom == 1L,
        usageCount = usageCount.toInt(),
        createdAt = Instant.parse(createdAt),
        isDeleted = isDeleted == 1L
    )
}

@OptIn(ExperimentalTime::class)
fun Category.toData(): DataCategory {
    return DataCategory(
        id = id,
        name = name,
        key = key,
        isCustom = if (isCustom) 1L else 0L,
        usageCount = usageCount.toLong(),
        createdAt = createdAt.toString(),
        isDeleted = if (isDeleted) 1L else 0L
    )
}