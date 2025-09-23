package org.example.habitstreak.data.mapper.frequency

import org.example.habitstreak.domain.model.HabitFrequency

/**
 * Strategy interface for frequency serialization following Open/Closed Principle.
 * Each frequency type has its own serializer implementation.
 */
interface FrequencySerializer {
    fun serialize(frequency: HabitFrequency): Pair<String, String>
    fun deserialize(type: String, data: String): HabitFrequency?
    fun canHandle(type: String): Boolean
}

/**
 * Registry for frequency serializers following Strategy Pattern.
 * Extensible design - new frequency types can be added without modifying existing code.
 */
class FrequencySerializationService {
    private val serializers = mutableListOf<FrequencySerializer>()

    init {
        // Register default serializers
        registerSerializer(DailyFrequencySerializer())
        registerSerializer(WeeklyFrequencySerializer())
        registerSerializer(MonthlyFrequencySerializer())
        registerSerializer(CustomFrequencySerializer())
    }

    fun registerSerializer(serializer: FrequencySerializer) {
        serializers.add(serializer)
    }

    fun serialize(frequency: HabitFrequency): Pair<String, String> {
        return serializers.firstOrNull { serializer ->
            // Check if serializer can handle this frequency type
            when (frequency) {
                is HabitFrequency.Daily -> serializer is DailyFrequencySerializer
                is HabitFrequency.Weekly -> serializer is WeeklyFrequencySerializer
                is HabitFrequency.Monthly -> serializer is MonthlyFrequencySerializer
                is HabitFrequency.Custom -> serializer is CustomFrequencySerializer
            }
        }?.serialize(frequency) ?: throw IllegalArgumentException("Unsupported frequency type: ${frequency::class.simpleName}")
    }

    fun deserialize(type: String, data: String): HabitFrequency {
        return serializers.firstOrNull { it.canHandle(type) }
            ?.deserialize(type, data)
            ?: HabitFrequency.Daily // Fallback
    }
}