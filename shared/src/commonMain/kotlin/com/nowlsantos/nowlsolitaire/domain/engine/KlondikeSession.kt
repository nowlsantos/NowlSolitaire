package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.DealMode
import com.nowlsantos.nowlsolitaire.domain.model.DeckPasses
import com.nowlsantos.nowlsolitaire.domain.model.DrawMode
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.HintMove
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation

data class KlondikeSession(
    val currentState: GameState,
    val undoStack: List<GameState> = emptyList()
) {
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    fun drawOrRecycle(): KlondikeSession {
        val next = KlondikeEngine.drawOrRecycle(currentState) ?: return this
        return pushState(next)
    }

    fun moveCards(
        from: PileLocation,
        cardIndex: Int,
        to: PileLocation
    ): KlondikeSession {
        val next = KlondikeEngine.moveCards(currentState, from, cardIndex, to) ?: return this
        return pushState(next)
    }

    fun autoMove(
        from: PileLocation,
        cardIndex: Int
    ): KlondikeSession {
        val next = KlondikeEngine.autoMove(currentState, from, cardIndex) ?: return this
        return pushState(next)
    }

    fun stepAutoComplete(): KlondikeSession {
        val next = KlondikeEngine.stepAutoComplete(currentState) ?: return this
        return pushState(next)
    }

    fun undo(): KlondikeSession {
        if (undoStack.isEmpty()) return this
        return KlondikeSession(
            currentState = undoStack.last(),
            undoStack = undoStack.dropLast(1)
        )
    }

    fun findHint(): HintMove? = KlondikeEngine.findHint(currentState)

    private fun pushState(nextState: GameState): KlondikeSession {
        val trimmed = if (undoStack.size >= MAX_UNDO_HISTORY) {
            undoStack.drop(undoStack.size - (MAX_UNDO_HISTORY - 1))
        } else {
            undoStack
        }
        return KlondikeSession(
            currentState = nextState,
            undoStack = trimmed + currentState
        )
    }

    companion object {
        const val MAX_UNDO_HISTORY = 50

        fun newGame(
            drawMode: DrawMode = DrawMode.DRAW_ONE,
            deckPasses: DeckPasses = DeckPasses.UNLIMITED,
            dealMode: DealMode = DealMode.WINNABLE,
            seed: Long? = null
        ): KlondikeSession = KlondikeSession(
            currentState = DeckDealer.dealNewGame(
                drawMode = drawMode,
                deckPasses = deckPasses,
                dealMode = dealMode,
                seed = seed
            )
        )

        fun replayDeal(state: GameState): KlondikeSession = KlondikeSession(
            currentState = DeckDealer.dealNewGame(
                drawMode = state.drawMode,
                deckPasses = state.deckPasses,
                seed = state.seed
            )
        )
    }
}
