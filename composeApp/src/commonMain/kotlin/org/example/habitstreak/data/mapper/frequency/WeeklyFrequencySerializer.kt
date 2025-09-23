package org.example.habitstreak.data.mapper.frequency

import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency

/**
 * Serializer for Weekly frequency type following Single Responsibility Principle.
 */
class WeeklyFrequencySerializer : FrequencySerializer {

    override fun serialize(frequency: HabitFrequency): Pair<String, String> {
        return if (frequency is HabitFrequency.Weekly) {
            "WEEKLY" to frequency.daysOfWeek.joinToString(",") { it.name }
        } else {
            throw IllegalArgumentException("Expected Weekly frequency, got ${frequency::class.simpleName}")
        }
    }

    override fun deserialize(type: String, data: String): HabitFrequency? {
        return if (canHandle(type)) {
            val days = if (data.isNotEmpty()) {
                data.split(",").mapNotNull { dayName ->
                    try {
                        DayOfWeek.valueOf(dayName)
                    } catch (e: IllegalArgumentException) {
                        null // Skip invalid day names
                    }
                }.toSet()
            } else {
                emptySet()
            }
            HabitFrequency.Weekly(days)
        } else null
    }

    override fun canHandle(type: String): Boolean = type == "WEEKLY"
}