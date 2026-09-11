package com.mani.controlcentre.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store: DataStore<Preferences> by preferencesDataStore("control_centre")

/** Persisted configuration and small tool state. Real configuration only; never per-frame values. */
class Prefs(context: Context) {
    private val store = context.applicationContext.store

    /** The companion-page experiment. Off means the service creates no windows at all. */
    val companionEnabled: Flow<Boolean> = store.data.map { it[COMPANION] ?: true }
    val counter: Flow<Long> = store.data.map { it[COUNTER] ?: 0L }

    suspend fun setCompanionEnabled(value: Boolean) { store.edit { it[COMPANION] = value } }
    suspend fun setCounter(value: Long) { store.edit { it[COUNTER] = value } }

    private companion object {
        val COMPANION = booleanPreferencesKey("companion_enabled")
        val COUNTER = longPreferencesKey("counter")
    }
}
