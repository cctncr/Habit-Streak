package org.example.habitstreak.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = HabitDatabase.Schema,
            context = context,
            name = "habit.db",
            callback = object : AndroidSqliteDriver.Callback(HabitDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Enable foreign keys
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            }
        ).also { driver ->
            DatabaseMigration.setupMigrations(driver)
        }
    }
}