package com.nowlsantos.nowlsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nowlsantos.nowlsolitaire.domain.model.GameSettings
import com.nowlsantos.nowlsolitaire.domain.model.GameStats
import com.nowlsantos.nowlsolitaire.domain.model.SavedGameSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface KlondikeRepository {
    val settingsFlow: Flow<GameSettings>
    val statsFlow: Flow<GameStats>

    suspend fun loadSavedGame(): SavedGameSnapshot?
    suspend fun saveGame(snapshot: SavedGameSnapshot?)
    suspend fun updateSettings(transform: (GameSettings) -> GameSettings)
    suspend fun recordGamePlayed()
    suspend fun recordGameWon(score: Int, elapsedSeconds: Int)
    suspend fun resetStats()
}

class DataStoreKlondikeRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : KlondikeRepository {

    override val settingsFlow: Flow<GameSettings> = dataStore.data
        .map { prefs -> prefs[KEY_SETTINGS] to (prefs[KEY_TIMER_MIGRATED] == true) }
        .distinctUntilChanged()
        .map { (raw, migrated) ->
            val settings = raw?.let {
                runCatching { json.decodeFromString<GameSettings>(it) }.getOrNull()
            } ?: GameSettings()
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

    override suspend fun loadSavedGame(): SavedGameSnapshot? {
        val raw = dataStore.data.first()[KEY_SAVED_GAME] ?: return null
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<SavedGameSnapshot>(raw) }.getOrNull()
        }
    }

    override suspend fun saveGame(snapshot: SavedGameSnapshot?) {
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

    override suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_SETTINGS]
            val current = raw?.let {
                runCatching { json.decodeFromString<GameSettings>(it) }.getOrNull()
            } ?: GameSettings()
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
        val KEY_SETTINGS = stringPreferencesKey("game_settings_json")
        val KEY_STATS = stringPreferencesKey("game_stats_json")
        val KEY_SAVED_GAME = stringPreferencesKey("saved_game_json")
        val KEY_TIMER_MIGRATED = booleanPreferencesKey("timer_default_migrated_v1")
    }
}
