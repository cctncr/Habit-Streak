package org.example.habitstreak.data.mapper.frequency

import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.RepeatUnit

class CustomFrequencySerializer : FrequencySerializer {

    override fun serialize(frequency: HabitFrequency): Pair<String, String> {
        return if (frequency is HabitFrequency.Custom) {
            "CUSTOM" to "${frequency.repeatInterval},${frequency.repeatUnit.name}"
        } else {
            throw IllegalArgumentException("Expected Custom frequency, got ${frequency::class.simpleName}")
        }
    }

    override fun deserialize(type: String, data: String): HabitFrequency? {
        return if (canHandle(type)) {
            if (data.isNotEmpty()) {
                val parts = data.split(",")
                val interval = parts.getOrNull(0)?.toIntOrNull() ?: 1
                val unit = parts.getOrNull(1)?.let { unitName ->
                    try {
                        RepeatUnit.valueOf(unitName)
                    } catch (e: IllegalArgumentException) {
                        RepeatUnit.DAYS
                    }
                } ?: RepeatUnit.DAYS

                HabitFrequency.Custom(interval, unit)
            } else {
                HabitFrequency.Custom(1, RepeatUnit.DAYS)
            }
        } else null
    }

    override fun canHandle(type: String): Boolean = type == "CUSTOM"
}