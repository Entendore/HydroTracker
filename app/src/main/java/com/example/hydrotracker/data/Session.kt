package com.example.hydrotracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: String,
    val intensity: String,
    val durationMin: Int,
    val startedAt: Long,
    val endedAt: Long? = null,
    val suggestedMl: Int,
    val sweatMl: Int? = null,
    val rpe: Int? = null,
    val drinks: Int = 0,
    val hotDay: Boolean = false
)
