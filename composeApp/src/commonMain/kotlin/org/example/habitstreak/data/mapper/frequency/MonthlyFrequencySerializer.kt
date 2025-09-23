package org.example.habitstreak.data.mapper.frequency

import org.example.habitstreak.domain.model.HabitFrequency

/**
 * Serializer for Monthly frequency type following Single Responsibility Principle.
 */
class MonthlyFrequencySerializer : FrequencySerializer {

    override fun serialize(frequency: HabitFrequency): Pair<String, String> {
        return if (frequency is HabitFrequency.Monthly) {
            "MONTHLY" to frequency.daysOfMonth.joinToString(",")
        } else {
            throw IllegalArgumentException("Expected Monthly frequency, got ${frequency::class.simpleName}")
        }
    }

    override fun deserialize(type: String, data: String): HabitFrequency? {
        return if (canHandle(type)) {
            val daysOfMonth = if (data.isNotEmpty()) {
                data.split(",").mapNotNull { dayStr ->
                    dayStr.toIntOrNull()?.takeIf { it in 1..31 }
                }.toSet()
            } else {
                emptySet()
            }
            HabitFrequency.Monthly(daysOfMonth)
        } else null
    }

    override fun canHandle(type: String): Boolean = type == "MONTHLY"
}