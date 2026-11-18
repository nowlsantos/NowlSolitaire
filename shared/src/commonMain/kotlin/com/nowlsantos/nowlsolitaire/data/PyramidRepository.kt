package com.nowlsantos.nowlsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nowlsantos.nowlsolitaire.domain.model.GameStats
import com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession
import com.nowlsantos.nowlsolitaire.domain.model.PyramidSettings
import com.nowlsantos.nowlsolitaire.domain.model.SavedPyramidGameSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface PyramidRepository {
    val settingsFlow: Flow<PyramidSettings>
    val statsFlow: Flow<GameStats>
    val runSessionFlow: Flow<PyramidRunSession>

    suspend fun loadSavedGame(): SavedPyramidGameSnapshot?
    suspend fun saveGame(snapshot: SavedPyramidGameSnapshot?)
    suspend fun loadRunSession(): PyramidRunSession
    suspend fun saveRunSession(session: PyramidRunSession)
    suspend fun updateSettings(transform: (PyramidSettings) -> PyramidSettings)
    suspend fun recordGamePlayed()
    suspend fun recordGameWon(score: Int, elapsedSeconds: Int)
    suspend fun resetStats()
}

class DataStorePyramidRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : PyramidRepository {

    override val settingsFlow: Flow<PyramidSettings> = dataStore.data
        .map { prefs -> prefs[KEY_SETTINGS] to (prefs[KEY_TIMER_MIGRATED] == true) }
        .distinctUntilChanged()
        .map { (raw, migrated) ->
            val settings = raw?.let {
                runCatching { json.decodeFromString<PyramidSettings>(it) }.getOrNull()
            } ?: PyramidSettings()
            if (!migrated) {
                settings.copy(showTimer = false)
            } else {
                settings
            }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    override val statsFlow: Flow<GameStats> = dataStore.data
        .map { prefs -> prefs[KEY_STATS] }
        .distinctUntilChanged()
        .map { raw ->
            raw?.let {
                runCatching { json.decodeFromString<GameStats>(it) }.getOrNull()
            } ?: GameStats()
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    override val runSessionFlow: Flow<PyramidRunSession> = dataStore.data
        .map { prefs -> prefs[KEY_RUN_SESSION] }
        .distinctUntilChanged()
        .map { raw ->
            raw?.let {
                runCatching { json.decodeFromString<PyramidRunSession>(it) }.getOrNull()
            } ?: PyramidRunSession()
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    override suspend fun loadRunSession(): PyramidRunSession {
        val raw = dataStore.data.first()[KEY_RUN_SESSION] ?: return PyramidRunSession()
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<PyramidRunSession>(raw) }.getOrNull() ?: PyramidRunSession()
        }
    }

    override suspend fun saveRunSession(session: PyramidRunSession) {
        val encoded = withContext(Dispatchers.Default) {
            json.encodeToString(session)
        }
        dataStore.edit { prefs ->
            prefs[KEY_RUN_SESSION] = encoded
        }
    }

    override suspend fun loadSavedGame(): SavedPyramidGameSnapshot? {
        val raw = dataStore.data.first()[KEY_SAVED_GAME] ?: return null
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<SavedPyramidGameSnapshot>(raw) }.getOrNull()
        }
    }

    override suspend fun saveGame(snapshot: SavedPyramidGameSnapshot?) {
        val encoded = if (snapshot == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                json.encodeToString(snapshot)
            }
        }
        dataStore.edit { prefs ->
            if (encoded == null) {
                prefs.remove(KEY_SAVED_GAME)
            } else {
                prefs[KEY_SAVED_GAME] = encoded
            }
        }
    }

    override suspend fun updateSettings(transform: (PyramidSettings) -> PyramidSettings) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_SETTINGS]
            val current = raw?.let {
                runCatching { json.decodeFromString<PyramidSettings>(it) }.getOrNull()
            } ?: PyramidSettings()
            val effective = if (prefs[KEY_TIMER_MIGRATED] != true) {
                current.copy(showTimer = false)
            } else {
                current
            }
            prefs[KEY_SETTINGS] = json.encodeToString(transform(effective))
            prefs[KEY_TIMER_MIGRATED] = true
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
                bestTimeSeconds = if (elapsedSeconds > 0) {
                    current.bestTimeSeconds?.let { minOf(it, elapsedSeconds) } ?: elapsedSeconds
                } else {
                    current.bestTimeSeconds
                }
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
        val KEY_RUN_SESSION = stringPreferencesKey("pyramid_run_session_json")
        val KEY_TIMER_MIGRATED = booleanPreferencesKey("pyramid_timer_default_migrated_v1")
    }
}
