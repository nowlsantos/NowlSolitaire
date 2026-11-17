package com.nowlsantos.nowlsolitaire.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TableTheme(val displayName: String) {
    CLASSIC_GREEN("Classic Green"),
    MIDNIGHT_BLUE("Midnight Blue"),
    ROYAL_BURGUNDY("Royal Burgundy"),
    SLATE_DARK("Dark Slate")
}

@Serializable
enum class CardBackTheme(val displayName: String) {
    CLASSIC("Classic"),
    BACK_1("Style 1"),
    BACK_2("Style 2"),
    BACK_3("Style 3"),
    BACK_4("Style 4")
}

@Serializable
data class GameSettings(
    val drawMode: DrawMode = DrawMode.DRAW_ONE,
    val deckPasses: DeckPasses = DeckPasses.UNLIMITED,
    val tableTheme: TableTheme = TableTheme.CLASSIC_GREEN,
    val cardBackTheme: CardBackTheme = CardBackTheme.CLASSIC,
    val leftHanded: Boolean = false,
    val tapToAutoMove: Boolean = true,
    val showTimer: Boolean = false,
)

@Serializable
data class GameStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val bestScore: Int = 0,
    val bestTimeSeconds: Int? = null
) {
    val winRatePercent: Int
        get() = if (gamesPlayed == 0) 0 else ((gamesWon * 100f) / gamesPlayed).toInt()
}

@Serializable
data class SavedGameSnapshot(
    val gameState: GameState,
    val undoStack: List<GameState> = emptyList(),
    val elapsedSeconds: Int = 0,
    val hasStartedMoving: Boolean = false,
    val recordedAsPlayed: Boolean = false
)
