package com.nowlsantos.nowlsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nowlsantos.nowlsolitaire.domain.model.GameStats
import com.nowlsantos.nowlsolitaire.domain.model.PyramidSettings
import com.nowlsantos.nowlsolitaire.domain.model.SavedPyramidGameSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface PyramidRepository {
    val settingsFlow: Flow<PyramidSettings>
    val statsFlow: Flow<GameStats>

    suspend fun loadSavedGame(): SavedPyramidGameSnapshot?
    suspend fun saveGame(snapshot: SavedPyramidGameSnapshot?)
    suspend fun updateSettings(transform: (PyramidSettings) -> PyramidSettings)
    suspend fun recordGamePlayed()
    suspend fun recordGameWon(score: Int, elapsedSeconds: Int)
    suspend fun resetStats()
}

class DataStorePyramidRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : PyramidRepository {

    override val settingsFlow: Flow<PyramidSettings> = dataStore.data.map { prefs ->
        prefs[KEY_SETTINGS]?.let { raw ->
            runCatching { json.decodeFromString<PyramidSettings>(raw) }.getOrNull()
        } ?: PyramidSettings()
    }

    override val statsFlow: Flow<GameStats> = dataStore.data.map { prefs ->
        prefs[KEY_STATS]?.let { raw ->
            runCatching { json.decodeFromString<GameStats>(raw) }.getOrNull()
        } ?: GameStats()
    }

    override suspend fun loadSavedGame(): SavedPyramidGameSnapshot? {
        val raw = dataStore.data.first()[KEY_SAVED_GAME] ?: return null
        return runCatching { json.decodeFromString<SavedPyramidGameSnapshot>(raw) }.getOrNull()
    }

    override suspend fun saveGame(snapshot: SavedPyramidGameSnapshot?) {
        dataStore.edit { prefs ->
            if (snapshot == null) {
                prefs.remove(KEY_SAVED_GAME)
            } else {
                prefs[KEY_SAVED_GAME] = json.encodeToString(snapshot)
            }
        }
    }

    override suspend fun updateSettings(transform: (PyramidSettings) -> PyramidSettings) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_SETTINGS]?.let { raw ->
                runCatching { json.decodeFromString<PyramidSettings>(raw) }.getOrNull()
            } ?: PyramidSettings()
            prefs[KEY_SETTINGS] = json.encodeToString(transform(current))
        }
    }

    override suspend fun recordGamePlayed() {
        dataStore.edit { prefs ->
            val current = prefs[KEY_STATS]?.let { raw ->
                runCatching { json.decodeFromString<GameStats>(raw) }.getOrNull()
            } ?: GameStats()
            val updated = current.copy(
                gamesPlayed = current.gamesPlayed + 1
            )
            prefs[KEY_STATS] = json.encodeToString(updated)
        }
    }

    override suspend fun recordGameWon(score: Int, elapsedSeconds: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_STATS]?.let { raw ->
                runCatching { json.decodeFromString<GameStats>(raw) }.getOrNull()
            } ?: GameStats()
            val newStreak = current.currentStreak + 1
            val updated = current.copy(
                gamesWon = current.gamesWon + 1,
                currentStreak = newStreak,
                bestStreak = maxOf(current.bestStreak, newStreak),
                bestScore = maxOf(current.bestScore, score),
                bestTimeSeconds = current.bestTimeSeconds?.let { minOf(it, elapsedSeconds) }
                    ?: elapsedSeconds
            )
            prefs[KEY_STATS] = json.encodeToString(updated)
        }
    }

    override suspend fun resetStats() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_STATS)
        }
    }

    private companion object {
        val KEY_SETTINGS = stringPreferencesKey("pyramid_settings_json")
        val KEY_STATS = stringPreferencesKey("pyramid_stats_json")
        val KEY_SAVED_GAME = stringPreferencesKey("pyramid_saved_game_json")
    }
}
