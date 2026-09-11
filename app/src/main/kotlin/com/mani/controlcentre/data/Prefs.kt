package com.mani.controlcentre.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mani.controlcentre.core.Route
import com.mani.controlcentre.core.TriggerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store: DataStore<Preferences> by preferencesDataStore("control_centre")

/** Which triggers are active. Both default on for the feasibility test; "paused" removes every trigger window. */
data class TriggerSettings(
    val topEnabled: Boolean = true,
    val sideEnabled: Boolean = true,
    val leftHanded: Boolean = false,
    val showHandles: Boolean = true,
    val paused: Boolean = false,
) {
    fun configs(): List<TriggerConfig> = listOf(
        TriggerConfig(enabled = topEnabled && !paused, route = Route.TOP, left = leftHanded, topWidthDp = 96f, showHandle = showHandles),
        TriggerConfig(enabled = sideEnabled && !paused, route = Route.SIDE, left = leftHanded, showHandle = showHandles),
    )
}

/** Persisted configuration and small tool state. Real configuration only; never per-frame values. */
class Prefs(context: Context) {
    private val store = context.applicationContext.store

    val triggers: Flow<TriggerSettings> = store.data.map { p ->
        TriggerSettings(
            topEnabled = p[TOP] ?: true,
            sideEnabled = p[SIDE] ?: true,
            leftHanded = p[LEFT] ?: false,
            showHandles = p[SHOW] ?: true,
            paused = p[PAUSED] ?: false,
        )
    }
    val counter: Flow<Long> = store.data.map { it[COUNTER] ?: 0L }

    suspend fun setTopEnabled(value: Boolean) { store.edit { it[TOP] = value } }
    suspend fun setSideEnabled(value: Boolean) { store.edit { it[SIDE] = value } }
    suspend fun setLeftHanded(value: Boolean) { store.edit { it[LEFT] = value } }
    suspend fun setShowHandles(value: Boolean) { store.edit { it[SHOW] = value } }
    suspend fun setPaused(value: Boolean) { store.edit { it[PAUSED] = value } }
    suspend fun setCounter(value: Long) { store.edit { it[COUNTER] = value } }

    private companion object {
        val TOP = booleanPreferencesKey("top_enabled")
        val SIDE = booleanPreferencesKey("side_enabled")
        val LEFT = booleanPreferencesKey("left_handed")
        val SHOW = booleanPreferencesKey("show_handles")
        val PAUSED = booleanPreferencesKey("paused")
        val COUNTER = longPreferencesKey("counter")
    }
}
