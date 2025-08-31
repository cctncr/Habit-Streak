package org.example.habitstreak.data.mapper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.RepeatUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.example.habitstreak.data.local.Habit as DataHabit
import org.example.habitstreak.data.local.HabitRecord as DataHabitRecord
import org.example.habitstreak.domain.model.*

@OptIn(ExperimentalTime::class)
fun DataHabit.toDomain(): Habit {
    return Habit(
        id = id,
        title = title,
        description = description,
        icon = HabitIcon.valueOf(iconName),
        color = HabitColor.valueOf(colorName),
        frequency = deserializeFrequency(frequencyType, frequencyData),
        reminderTime = reminderTime,
        isReminderEnabled = isReminderEnabled == 1L,
        targetCount = targetCount.toInt(),
        unit = unit,
        createdAt = Instant.parse(createdAt),
        isArchived = isArchived == 1L,
        sortOrder = sortOrder.toInt()
    )
}

@OptIn(ExperimentalTime::class)
fun Habit.toData(): DataHabit {
    val (frequencyType, frequencyData) = frequency.serialize()
    return DataHabit(
        id = id,
        title = title,
        description = description,
        iconName = icon.name,
        colorName = color.name,
        frequencyType = frequencyType,
        frequencyData = frequencyData,
        reminderTime = reminderTime,
        isReminderEnabled = if (isReminderEnabled) 1L else 0L,
        targetCount = targetCount.toLong(),
        unit = unit,
        createdAt = createdAt.toString(),
        isArchived = if (isArchived) 1L else 0L,
        sortOrder = sortOrder.toLong()
    )
}

@OptIn(ExperimentalTime::class)
fun DataHabitRecord.toDomain(): HabitRecord {
    return HabitRecord(
        id = id,
        habitId = habitId,
        date = LocalDate.parse(date),
        completedCount = completedCount.toInt(),
        note = note,
        completedAt = Instant.parse(completedAt)
    )
}

@OptIn(ExperimentalTime::class)
fun HabitRecord.toData(): DataHabitRecord {
    return DataHabitRecord(
        id = id,
        habitId = habitId,
        date = date.toString(),
        completedCount = completedCount.toLong(),
        note = note,
        completedAt = completedAt.toString()
    )
}

fun HabitFrequency.serialize(): Pair<String, String> {
    return when (this) {
        is HabitFrequency.Daily -> "DAILY" to ""
        is HabitFrequency.Weekly -> "WEEKLY" to days.joinToString(",") { it.name }
        is HabitFrequency.Custom -> "CUSTOM" to daysOfMonth.joinToString(",")
        is HabitFrequency.Monthly -> "MONTHLY" to "${dayOfMonth},${repeatUnit}"
    }
}


fun deserializeFrequency(type: String, data: String): HabitFrequency {
    return when (type) {
        "DAILY" -> HabitFrequency.Daily
        "WEEKLY" -> {
            val days = if (data.isNotEmpty()) {
                data.split(",").map { DayOfWeek.valueOf(it) }.toSet()
            } else {
                emptySet()
            }
            HabitFrequency.Weekly(days)
        }
        "CUSTOM" -> {
            val daysOfMonth = if (data.isNotEmpty()) {
                data.split(",").map { it.toInt() }.toSet()
            } else {
                emptySet()
            }
            HabitFrequency.Custom(daysOfMonth)
        }
        "MONTHLY" -> {
            if (data.isNotEmpty()) {
                val parts = data.split(",")
                HabitFrequency.Monthly(
                    dayOfMonth = parts[0].toIntOrNull() ?: 1,
                    repeatUnit = parts.getOrNull(1) ?: "months"
                )
            } else {
                HabitFrequency.Monthly(1, "months")
            }
        }
        else -> HabitFrequency.Daily
    }
}


fun parseFrequency(type: String, data: String): HabitFrequency = when (type) {
    "DAILY" -> HabitFrequency.Daily
    "WEEKLY" -> {
        if (data.isBlank()) {
            HabitFrequency.Daily // Fallback
        } else {
            try {
                val days = Json.decodeFromString<List<String>>(data)
                    .map { DayOfWeek.valueOf(it) }
                    .toSet()
                HabitFrequency.Weekly(days)
            } catch (e: Exception) {
                HabitFrequency.Daily
            }
        }
    }
    "MONTHLY" -> {
        if (data.isBlank()) {
            HabitFrequency.Daily // Fallback
        } else {
            try {
                val days = Json.decodeFromString<Set<Int>>(data)
                HabitFrequency.Monthly(days)
            } catch (e: Exception) {
                HabitFrequency.Daily
            }
        }
    }
    "CUSTOM" -> {
        if (data.isBlank()) {
            HabitFrequency.Daily
        } else {
            try {
                val customData = Json.decodeFromString<Map<String, String>>(data)
                HabitFrequency.Custom(
                    customData["interval"]?.toIntOrNull() ?: 1,
                    RepeatUnit.valueOf(customData["unit"] ?: "DAYS")
                )
            } catch (e: Exception) {
                HabitFrequency.Daily
            }
        }
    }
    else -> HabitFrequency.Daily
}