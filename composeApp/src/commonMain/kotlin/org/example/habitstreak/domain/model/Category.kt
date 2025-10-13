package org.example.habitstreak.domain.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Category @OptIn(ExperimentalTime::class) constructor(
    val id: String = "",
    val name: String,
    val key: String? = null,
    val isCustom: Boolean = false,
    val usageCount: Int = 0,
    val createdAt: Instant = Clock.System.now(),
    val isDeleted: Boolean = false
)