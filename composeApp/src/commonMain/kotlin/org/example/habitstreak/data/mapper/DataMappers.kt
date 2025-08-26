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
import org.example.habitstreak.data.local.Habit as DataHabit
import org.example.habitstreak.data.local.HabitRecord as DataHabitRecord

fun DataHabit.toDomain(): Habit = Habit(
    id = id,
    title = title,
    description = description,
    icon = HabitIcon.valueOf(iconName),
    color = HabitColor.valueOf(colorName),
    frequency = parseFrequency(frequencyType, frequencyData),
    reminderTime = reminderTime?.let { LocalTime.parse(it) },
    isReminderEnabled = isReminderEnabled == 1L,
    targetCount = targetCount.toInt(),
    unit = unit,
    createdAt = LocalDate.parse(createdAt),
    isArchived = isArchived == 1L,
    sortOrder = sortOrder.toInt()
)

fun Habit.toData(): DataHabit {
    val (freqType, freqData) = frequency.serialize()
    return DataHabit(
        id = id,
        title = title,
        description = description,
        iconName = icon.name,
        colorName = color.name,
        frequencyType = freqType,
        frequencyData = freqData,
        reminderTime = reminderTime?.toString(),
        isReminderEnabled = if (isReminderEnabled) 1L else 0L,
        targetCount = targetCount.toLong(),
        unit = unit,
        createdAt = createdAt.toString(),
        isArchived = if (isArchived) 1L else 0L,
        sortOrder = sortOrder.toLong()
    )
}

fun DataHabitRecord.toDomain(): HabitRecord = HabitRecord(
    id = id,
    habitId = habitId,
    date = LocalDate.parse(date),
    completedCount = completedCount.toInt(),
    note = note,
    completedAt = LocalDate.parse(completedAt)
)

fun HabitRecord.toData(): DataHabitRecord = DataHabitRecord(
    id = id,
    habitId = habitId,
    date = date.toString(),
    completedCount = completedCount.toLong(),
    note = note,
    completedAt = completedAt.toString()
)

fun HabitFrequency.serialize(): Pair<String, String> = when (this) {
    is HabitFrequency.Daily -> "DAILY" to ""
    is HabitFrequency.Weekly -> "WEEKLY" to Json.encodeToString(daysOfWeek.map { it.name })
    is HabitFrequency.Monthly -> "MONTHLY" to Json.encodeToString(daysOfMonth)
    is HabitFrequency.Custom -> "CUSTOM" to Json.encodeToString(
        mapOf("interval" to repeatInterval.toString(), "unit" to repeatUnit.name)
    )
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
                HabitFrequency.Daily // Fallback on error
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
                HabitFrequency.Daily // Fallback on error
            }
        }
    }
    "CUSTOM" -> {
        if (data.isBlank()) {
            HabitFrequency.Daily // Fallback
        } else {
            try {
                val customData = Json.decodeFromString<Map<String, String>>(data)
                HabitFrequency.Custom(
                    customData["interval"]?.toIntOrNull() ?: 1,
                    RepeatUnit.valueOf(customData["unit"] ?: "DAYS")
                )
            } catch (e: Exception) {
                HabitFrequency.Daily // Fallback on error
            }
        }
    }
    else -> HabitFrequency.Daily
}