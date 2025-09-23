package org.example.habitstreak.domain.util

import kotlin.random.Random

object UuidGenerator {
    fun generateUUID(): String {
        val random = Random.Default
        val uuid = buildString {
            // Generate 32 hex characters
            repeat(32) {
                append(random.nextInt(16).toString(16))
            }
        }

        // Format as standard UUID: 8-4-4-4-12
        return buildString {
            append(uuid.substring(0, 8))
            append("-")
            append(uuid.substring(8, 12))
            append("-")
            append("4") // Version 4 UUID
            append(uuid.substring(13, 16))
            append("-")
            append("a") // Variant bits
            append(uuid.substring(17, 20))
            append("-")
            append(uuid.substring(20, 32))
        }
    }
}