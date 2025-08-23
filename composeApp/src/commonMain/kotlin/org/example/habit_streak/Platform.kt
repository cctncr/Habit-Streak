package org.example.habit_streak

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform