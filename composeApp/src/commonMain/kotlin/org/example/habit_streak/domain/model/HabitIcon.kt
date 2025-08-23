package org.example.habit_streak.domain.model

enum class HabitIcon(val emoji: String, val category: IconCategory) {
    // Health & Fitness
    WATER("💧", IconCategory.HEALTH),
    EXERCISE("🏃", IconCategory.HEALTH),
    SLEEP("😴", IconCategory.HEALTH),
    MEDITATION("🧘", IconCategory.HEALTH),
    VITAMINS("💊", IconCategory.HEALTH),
    HEART("❤️", IconCategory.HEALTH),

    // Productivity
    BOOK("📚", IconCategory.PRODUCTIVITY),
    WRITE("✍️", IconCategory.PRODUCTIVITY),
    CODE("💻", IconCategory.PRODUCTIVITY),
    STUDY("📖", IconCategory.PRODUCTIVITY),
    WORK("💼", IconCategory.PRODUCTIVITY),
    TARGET("🎯", IconCategory.PRODUCTIVITY),

    // Lifestyle
    COFFEE("☕", IconCategory.LIFESTYLE),
    FOOD("🍎", IconCategory.LIFESTYLE),
    MUSIC("🎵", IconCategory.LIFESTYLE),
    PHOTO("📸", IconCategory.LIFESTYLE),
    PLANT("🌱", IconCategory.LIFESTYLE),
    CLEAN("🧹", IconCategory.LIFESTYLE),

    // Social
    CHAT("💬", IconCategory.SOCIAL),
    CALL("📞", IconCategory.SOCIAL),
    GIFT("🎁", IconCategory.SOCIAL),
    FAMILY("👨‍👩‍👧‍👦", IconCategory.SOCIAL),

    // Finance
    MONEY("💰", IconCategory.FINANCE),
    SAVE("🏦", IconCategory.FINANCE),
    CHART("📊", IconCategory.FINANCE),

    // Creativity
    ART("🎨", IconCategory.CREATIVITY),
    GUITAR("🎸", IconCategory.CREATIVITY),
    CAMERA("📷", IconCategory.CREATIVITY),

    // Other
    STAR("⭐", IconCategory.OTHER),
    FIRE("🔥", IconCategory.OTHER),
    ROCKET("🚀", IconCategory.OTHER),
    LIGHTNING("⚡", IconCategory.OTHER)
}