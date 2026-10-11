package com.example.hydrotracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterEntryDao {
    @Insert suspend fun insert(entry: WaterEntry)
    @Delete suspend fun delete(entry: WaterEntry)

    @Query("SELECT * FROM water_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<WaterEntry>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE timestamp >= :startMs")
    suspend fun totalSince(startMs: Long): Int

    @Insert suspend fun insertSession(session: Session): Long

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("UPDATE sessions SET endedAt = :endedAt, durationMin = :actualMin WHERE id = :id")
    suspend fun endSession(id: Long, endedAt: Long, actualMin: Int)

    @Query("UPDATE sessions SET drinks = drinks + 1 WHERE id = :id")
    suspend fun addDrink(id: Long)

    @Query("UPDATE sessions SET rpe = :rpe WHERE id = (SELECT MAX(id) FROM sessions)")
    suspend fun setLastRpe(rpe: Int)
}
