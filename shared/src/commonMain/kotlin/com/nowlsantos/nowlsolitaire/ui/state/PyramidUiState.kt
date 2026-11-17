package com.nowlsantos.nowlsolitaire.ui.state

import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.DealMode
import com.nowlsantos.nowlsolitaire.domain.model.GameStats
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.domain.model.PyramidSettings
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme

data class PyramidUiState(
    val isLoading: Boolean = true,
    val gameState: PyramidGameState = PyramidGameState(),
    val canUndo: Boolean = false,
    val elapsedSeconds: Int = 0,
    val activeHint: PyramidMatch? = null,
    val showNoMovesDialog: Boolean = false,
    val hintMessage: String? = null,
    val settings: PyramidSettings = PyramidSettings(),
    val stats: GameStats = GameStats(),
    val lastMatchedResult: String? = null,
    val activeMatch: PyramidMatch? = null,
    val activeDrawCard: Card? = null,
    val dealId: Long = 0L
) {
    val isWon: Boolean
        get() = gameState.isWon

    val score: Int
        get() = gameState.score

    val moveCount: Int
        get() = gameState.moveCount

    val remainingPyramidCount: Int
        get() = gameState.remainingPyramidCount
}

sealed interface PyramidIntent {
    data object NewGame : PyramidIntent
    data object ReplayDeal : PyramidIntent
    data object DrawOrRecycle : PyramidIntent
    data object Undo : PyramidIntent
    data object RequestHint : PyramidIntent
    data object ClearHint : PyramidIntent
    data object DismissNoMovesDialog : PyramidIntent
    data object ClearHintMessage : PyramidIntent
    data object ClearActiveMatch : PyramidIntent
    data object ClearActiveDrawCard : PyramidIntent
    data class TapCard(val location: PyramidLocation) : PyramidIntent
    data class SetTimerPaused(val paused: Boolean) : PyramidIntent
    data class UpdateTableTheme(val theme: TableTheme) : PyramidIntent
    data class UpdateCardBackTheme(val cardBackTheme: CardBackTheme) : PyramidIntent
    data class UpdateMaxRecycles(val maxRecycles: Int) : PyramidIntent
    data class UpdateDealMode(val dealMode: DealMode) : PyramidIntent
    data class UpdateShowTimer(val enabled: Boolean) : PyramidIntent
    data object ResetStats : PyramidIntent
}
