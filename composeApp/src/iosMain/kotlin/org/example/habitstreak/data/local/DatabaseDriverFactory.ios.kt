package org.example.habitstreak.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(HabitDatabase.Schema, "habit.db").also { driver ->
            // Enable foreign key constraints
            driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        }
    }
}