package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import kotlin.random.Random

data class PyramidSession(
    val currentState: PyramidGameState,
    val undoStack: List<PyramidGameState> = emptyList()
) {
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    fun drawOrRecycle(): PyramidSession {
        val next = PyramidEngine.drawOrRecycle(currentState) ?: return this
        return pushState(next)
    }

    fun interactCard(location: PyramidLocation): Pair<PyramidSession, PyramidInteractionResult?> {
        val result = PyramidEngine.interactCard(currentState, location) ?: return Pair(this, null)
        return when (result) {
            is PyramidInteractionResult.Matched -> {
                Pair(pushState(result.state), result)
            }
            is PyramidInteractionResult.Selected,
            is PyramidInteractionResult.Deselected,
            is PyramidInteractionResult.InvalidPair -> {
                // Changing card selection does not consume an undo step
                Pair(copy(currentState = result.state), result)
            }
        }
    }

    fun undo(): PyramidSession {
        if (undoStack.isEmpty()) return this
        return PyramidSession(
            currentState = undoStack.last(),
            undoStack = undoStack.dropLast(1)
        )
    }

    fun findHint(): PyramidMatch? = PyramidRules.findBestHint(currentState)

    private fun pushState(nextState: PyramidGameState): PyramidSession {
        val trimmed = if (undoStack.size >= MAX_UNDO_HISTORY) {
            undoStack.drop(undoStack.size - (MAX_UNDO_HISTORY - 1))
        } else {
            undoStack
        }
        return PyramidSession(
            currentState = nextState,
            undoStack = trimmed + currentState
        )
    }

    companion object {
        const val MAX_UNDO_HISTORY = 50

        fun newGame(
            maxRecycles: Int = PyramidGameState.DEFAULT_MAX_RECYCLES,
            seed: Long = Random.nextLong()
        ): PyramidSession = PyramidSession(
            currentState = PyramidEngine.dealNewGame(seed = seed, maxRecycles = maxRecycles)
        )

        fun replayDeal(state: PyramidGameState): PyramidSession = PyramidSession(
            currentState = PyramidEngine.dealNewGame(seed = state.seed, maxRecycles = state.maxRecycles)
        )
    }
}
