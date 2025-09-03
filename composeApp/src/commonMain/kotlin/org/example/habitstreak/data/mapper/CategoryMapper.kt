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
        isCustom = isCustom == 1L,
        usageCount = usageCount.toInt(),
        createdAt = Instant.parse(createdAt)
    )
}

@OptIn(ExperimentalTime::class)
fun Category.toData(): DataCategory {
    return DataCategory(
        id = id,
        name = name,
        isCustom = if (isCustom) 1L else 0L,
        usageCount = usageCount.toLong(),
        createdAt = createdAt.toString()
    )
}