package com.nowlsantos.nowlsolitaire.ui.state

import com.nowlsantos.nowlsolitaire.domain.model.DeckPasses
import com.nowlsantos.nowlsolitaire.domain.model.DrawMode
import com.nowlsantos.nowlsolitaire.domain.model.GameSettings
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.GameStats
import com.nowlsantos.nowlsolitaire.domain.model.HintMove
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme

data class CardSelection(
    val location: PileLocation,
    val cardIndex: Int
)

data class KlondikeUiState(
    val isLoading: Boolean = true,
    val gameState: GameState = GameState(),
    val canUndo: Boolean = false,
    val elapsedSeconds: Int = 0,
    val selection: CardSelection? = null,
    val activeHint: HintMove? = null,
    val isAutoCompleting: Boolean = false,
    val showNoMovesDialog: Boolean = false,
    val hintMessage: String? = null,
    val settings: GameSettings = GameSettings(),
    val stats: GameStats = GameStats()
) {
    val isWon: Boolean
        get() = gameState.isWon && !isAutoCompleting

    val isAutoCompleteAvailable: Boolean
        get() = gameState.isAutoCompleteReady && !isAutoCompleting

    val score: Int
        get() = gameState.score

    val moveCount: Int
        get() = gameState.moveCount
}

sealed interface KlondikeIntent {
    data object NewGame : KlondikeIntent
    data object ReplayDeal : KlondikeIntent
    data object DrawOrRecycle : KlondikeIntent
    data object Undo : KlondikeIntent
    data object RequestHint : KlondikeIntent
    data object ClearHint : KlondikeIntent
    data object DismissNoMovesDialog : KlondikeIntent
    data object ClearHintMessage : KlondikeIntent
    data object RunAutoComplete : KlondikeIntent
    data class TapPileOrCard(val location: PileLocation, val cardIndex: Int? = null) : KlondikeIntent
    data class MoveCards(val from: PileLocation, val cardIndex: Int, val to: PileLocation) : KlondikeIntent
    data class SetTimerPaused(val paused: Boolean) : KlondikeIntent
    data class UpdateDrawMode(val drawMode: DrawMode) : KlondikeIntent
    data class UpdateDeckPasses(val deckPasses: DeckPasses) : KlondikeIntent
    data class UpdateTableTheme(val theme: TableTheme) : KlondikeIntent
    data class UpdateCardBackTheme(val cardBackTheme: com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme) : KlondikeIntent
    data class UpdateLeftHanded(val leftHanded: Boolean) : KlondikeIntent
    data class UpdateTapToAutoMove(val enabled: Boolean) : KlondikeIntent
    data class UpdateShowTimer(val enabled: Boolean) : KlondikeIntent
    data object ResetStats : KlondikeIntent
}
