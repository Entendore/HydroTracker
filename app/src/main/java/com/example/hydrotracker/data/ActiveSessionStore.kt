package com.example.hydrotracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hydrotracker.ActiveSession
import com.example.hydrotracker.SessionSummary
import com.example.hydrotracker.logic.Intensity
import com.example.hydrotracker.logic.SessionMode
import com.example.hydrotracker.logic.SipPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.sessionDataStore by preferencesDataStore(name = "active_session")

class ActiveSessionStore(private val context: Context) {

    private val key = stringPreferencesKey("session_json")
    private val summaryKey = stringPreferencesKey("pending_summary_json")

    val activeSession: Flow<ActiveSession?> =
        context.sessionDataStore.data.map { it[key]?.let(::fromJson) }

    val pendingSummary: Flow<SessionSummary?> =
        context.sessionDataStore.data.map { it[summaryKey]?.let(::summaryFromJson) }

    suspend fun save(session: ActiveSession) =
        context.sessionDataStore.edit { it[key] = session.toJson() }

    suspend fun saveSummary(s: SessionSummary) =
        context.sessionDataStore.edit { it[summaryKey] = summaryToJson(s) }

    suspend fun clear() = context.sessionDataStore.edit { it.remove(key) }
    suspend fun clearSummary() = context.sessionDataStore.edit { it.remove(summaryKey) }
}

private fun ActiveSession.toJson(): String = JSONObject().apply {
    put("id", id); put("templateId", templateId); put("intensity", intensity.name)
    put("durationMin", durationMin); put("startedAt", startedAt); put("plannedMl", plannedMl)
    put("mode", mode.name); put("drinks", drinks)
    put("everyMin", plan.everyMin); put("count", plan.count); put("mlPerSip", plan.mlPerSip)
}.toString()

private fun fromJson(json: String): ActiveSession? = try {
    with(JSONObject(json)) {
        ActiveSession(
            id = getLong("id"), templateId = getString("templateId"),
            intensity = Intensity.valueOf(getString("intensity")),
            durationMin = getInt("durationMin"), startedAt = getLong("startedAt"),
            plan = SipPlan(getInt("everyMin"), getInt("count"), getInt("mlPerSip")),
            plannedMl = getInt("plannedMl"),
            mode = SessionMode.valueOf(getString("mode")), drinks = getInt("drinks")
        )
    }
} catch (e: Exception) { null }

private fun summaryToJson(s: SessionSummary): String = JSONObject().apply {
    put("templateId", s.templateId); put("plannedMl", s.plannedMl); put("loggedMl", s.loggedMl)
    put("electrolytes", s.electrolytes); put("recoveryMl", s.recoveryMl); put("mode", s.mode.name)
}.toString()

private fun summaryFromJson(json: String): SessionSummary? = try {
    with(JSONObject(json)) {
        SessionSummary(
            templateId = getString("templateId"), plannedMl = getInt("plannedMl"),
            loggedMl = getInt("loggedMl"), electrolytes = getBoolean("electrolytes"),
            recoveryMl = getInt("recoveryMl"),
            mode = SessionMode.valueOf(getString("mode"))
        )
    }
} catch (e: Exception) { null }
