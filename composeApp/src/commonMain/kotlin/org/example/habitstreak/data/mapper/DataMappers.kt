package org.example.habitstreak.data.mapper

import kotlinx.datetime.LocalDate
import org.example.habitstreak.data.mapper.frequency.FrequencySerializationService
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon
import org.example.habitstreak.domain.model.HabitRecord
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.example.habitstreak.data.local.Habit as DataHabit
import org.example.habitstreak.data.local.HabitRecord as DataHabitRecord

// Singleton instance of frequency serialization service following SOLID principles
private val frequencySerializationService = FrequencySerializationService()

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
    return frequencySerializationService.serialize(this)
}

fun deserializeFrequency(type: String, data: String): HabitFrequency {
    return frequencySerializationService.deserialize(type, data)
}

fun parseFrequency(type: String, data: String): HabitFrequency {
    return frequencySerializationService.deserialize(type, data)
}