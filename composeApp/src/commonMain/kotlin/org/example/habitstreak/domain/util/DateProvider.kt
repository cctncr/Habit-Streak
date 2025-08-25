package org.example.habitstreak.domain.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface DateProvider {
    fun today(): LocalDate

    @OptIn(ExperimentalTime::class)
    fun now(): Instant
}

class DateProviderImpl : DateProvider {
    @OptIn(ExperimentalTime::class)
    override fun today(): LocalDate {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    @OptIn(ExperimentalTime::class)
    override fun now(): Instant {
        return Clock.System.now()
    }
}