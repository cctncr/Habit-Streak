package org.example.habitstreak.domain.util

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface DateProvider {
    fun today(): LocalDate

    @OptIn(ExperimentalTime::class)
    fun now(): Instant
}