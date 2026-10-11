package com.example.hydrotracker

import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val DB = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createV1Db() {
        ctx().deleteDatabase(DB)
        SQLiteDatabase.openOrCreateDatabase(ctx().getDatabasePath(DB), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS water_entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "amountMl INTEGER NOT NULL, timestamp INTEGER NOT NULL)")
            db.execSQL("INSERT INTO water_entries (amountMl, timestamp) VALUES (250, 1700000000000)")
        }
    }

    private fun createV2Db() {
        ctx().deleteDatabase(DB)
        SQLiteDatabase.openOrCreateDatabase(ctx().getDatabasePath(DB), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS water_entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "amountMl INTEGER NOT NULL, timestamp INTEGER NOT NULL)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS workouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, presetId TEXT NOT NULL,
                intensity TEXT NOT NULL, durationMin INTEGER NOT NULL, startedAt INTEGER NOT NULL,
                endedAt INTEGER, suggestedMl INTEGER NOT NULL, sweatMl INTEGER, rpe INTEGER)""")
            db.execSQL("INSERT INTO water_entries (amountMl, timestamp) VALUES (250, 1700000000000)")
            db.execSQL("INSERT INTO workouts (presetId, intensity, durationMin, startedAt, suggestedMl) " +
                    "VALUES ('gym','HIGH',60,1700000000000,360)")
        }
    }

    @Test
    fun migrate1To3_preservesEntries() {
        createV1Db()
        val db = helper.runMigrationsAndValidate(DB, 3, true,
            AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        db.execSQL("INSERT INTO sessions (templateId, intensity, durationMin, startedAt, suggestedMl) " +
                "VALUES ('running','MODERATE',45,1700000000000,360)")
        db.query("SELECT COUNT(*) FROM water_entries").use {
            it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        db.query("SELECT COUNT(*) FROM sessions").use {
            it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        db.close()
    }

    @Test
    fun migrate2To3_renamesWorkoutsAndAddsColumns() {
        createV2Db()
        val db = helper.runMigrationsAndValidate(DB, 3, true, AppDatabase.MIGRATION_2_3)
        db.query("SELECT templateId, drinks, hotDay FROM sessions").use {
            it.moveToFirst()
            assertEquals("gym", it.getString(0))
            assertEquals(0, it.getInt(1))
            assertEquals(0, it.getInt(2))
        }
        db.query("SELECT COUNT(*) FROM water_entries").use {
            it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        db.close()
    }
}
