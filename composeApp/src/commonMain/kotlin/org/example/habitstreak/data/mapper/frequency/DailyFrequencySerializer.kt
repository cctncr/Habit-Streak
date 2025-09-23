package org.example.habitstreak.data.mapper.frequency

import org.example.habitstreak.domain.model.HabitFrequency

/**
 * Serializer for Daily frequency type following Single Responsibility Principle.
 */
class DailyFrequencySerializer : FrequencySerializer {

    override fun serialize(frequency: HabitFrequency): Pair<String, String> {
        return if (frequency is HabitFrequency.Daily) {
            "DAILY" to ""
        } else {
            throw IllegalArgumentException("Expected Daily frequency, got ${frequency::class.simpleName}")
        }
    }

    override fun deserialize(type: String, data: String): HabitFrequency? {
        return if (canHandle(type)) {
            HabitFrequency.Daily
        } else null
    }

    override fun canHandle(type: String): Boolean = type == "DAILY"
}