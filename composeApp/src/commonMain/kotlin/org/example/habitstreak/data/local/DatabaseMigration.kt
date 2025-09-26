package org.example.habitstreak.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Database migration utilities
 * Following Single Responsibility Principle - only handles schema migrations
 */
object DatabaseMigration {

    const val CURRENT_VERSION = 1

    /**
     * Setup migration callbacks for the database
     */
    fun setupMigrations(driver: SqlDriver) {
        // Enable foreign keys
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)

        // Future migrations can be added here using AfterVersion callbacks
        // Example for version 2:
        // HabitDatabase.Schema.migrate(driver, 1, 2)
    }

    /**
     * Create migration scripts directory structure (for future use)
     */
    fun getMigrationScripts(): List<Migration> {
        return listOf(
            // Future migrations will be added here
            // Migration(fromVersion = 1, toVersion = 2, script = "ALTER TABLE...")
        )
    }
}

/**
 * Represents a database migration script
 */
data class Migration(
    val fromVersion: Int,
    val toVersion: Int,
    val script: String
)