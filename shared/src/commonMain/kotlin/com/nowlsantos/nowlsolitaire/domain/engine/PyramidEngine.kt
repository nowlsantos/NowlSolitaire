package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.PyramidCard
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import kotlin.random.Random

sealed interface PyramidInteractionResult {
    val state: PyramidGameState

    data class Selected(
        override val state: PyramidGameState,
        val location: PyramidLocation
    ) : PyramidInteractionResult

    data class Deselected(
        override val state: PyramidGameState
    ) : PyramidInteractionResult

    data class Matched(
        override val state: PyramidGameState,
        val match: PyramidMatch,
        val pointsEarned: Int,
        val clearedRows: List<Int>,
        val isVictory: Boolean
    ) : PyramidInteractionResult

    data class InvalidPair(
        override val state: PyramidGameState,
        val newSelection: PyramidLocation
    ) : PyramidInteractionResult
}

object PyramidEngine {

    private data class PileRemovalResult(
        val pyramid: List<PyramidCard>,
        val stock: List<Card>,
        val waste: List<Card>
    )

    fun dealNewGame(
        seed: Long = Random.nextLong(),
        maxRecycles: Int = PyramidGameState.DEFAULT_MAX_RECYCLES
    ): PyramidGameState {
        val shuffledDeck = DeckDealer.createStandardDeck().shuffled(Random(seed))
        val pyramidCards = mutableListOf<PyramidCard>()

        var cursor = 0
        for (row in 0 until PyramidGameState.PYRAMID_ROW_COUNT) {
            val countInRow = row + 1
            for (col in 0 until countInRow) {
                val card = shuffledDeck[cursor++].copy(isFaceUp = true)
                pyramidCards.add(
                    PyramidCard(
                        row = row,
                        col = col,
                        card = card,
                        isRemoved = false
                    )
                )
            }
        }

        val stockCards = shuffledDeck.subList(cursor, shuffledDeck.size)
            .map { it.copy(isFaceUp = true) }

        return PyramidGameState(
            pyramid = pyramidCards,
            stock = stockCards,
            waste = emptyList(),
            discardPile = emptyList(),
            selectedLocation = null,
            maxRecycles = maxRecycles,
            recyclesUsed = 0,
            score = 0,
            moveCount = 0,
            seed = seed
        )
    }

    fun drawStock(state: PyramidGameState): PyramidGameState? {
        if (state.stock.isEmpty()) return null

        val drawnCard = state.stock.last().copy(isFaceUp = true)
        val newStock = state.stock.dropLast(1)
        val newWaste = state.waste + drawnCard

        return state.copy(
            stock = newStock,
            waste = newWaste,
            selectedLocation = null,
            moveCount = state.moveCount + 1
        )
    }

    fun recycleStock(state: PyramidGameState): PyramidGameState? {
        if (!state.canRecycleStock) return null

        val recycledStock = state.waste
            .reversed()
            .map { it.copy(isFaceUp = true) }

        val newRecyclesUsed = state.recyclesUsed + 1
        val newScore = PyramidScoring.applyScoreDelta(
            state.score,
            PyramidScoring.RECYCLE_PENALTY
        )

        return state.copy(
            stock = recycledStock,
            waste = emptyList(),
            selectedLocation = null,
            recyclesUsed = newRecyclesUsed,
            score = newScore,
            moveCount = state.moveCount + 1
        )
    }

    fun drawOrRecycle(state: PyramidGameState): PyramidGameState? {
        return if (state.stock.isNotEmpty()) {
            drawStock(state)
        } else if (state.canRecycleStock) {
            recycleStock(state)
        } else {
            null
        }
    }

    fun getCardAt(state: PyramidGameState, location: PyramidLocation): Card? {
        return when (location) {
            is PyramidLocation.PyramidSlot -> {
                val pyr = state.pyramid.firstOrNull { it.row == location.row && it.col == location.col }
                if (pyr != null && !pyr.isRemoved && PyramidRules.isExposed(state.pyramid, location.row, location.col)) {
                    pyr.card
                } else {
                    null
                }
            }
            PyramidLocation.Waste -> state.topWasteCard
            PyramidLocation.Stock -> state.topStockCard
        }
    }

    fun interactCard(
        state: PyramidGameState,
        location: PyramidLocation
    ): PyramidInteractionResult? {
        val targetCard = getCardAt(state, location) ?: return null

        // 1. King single-tap removal (rank 13)
        if (PyramidRules.isKing(targetCard)) {
            val removal = removeCardsFromPiles(state, listOf(location))
            val basePoints = PyramidScoring.scoreForKing(location)
            val newlyClearedRows = detectNewlyClearedRows(state.pyramid, removal.pyramid)
            val rowClearBonus = newlyClearedRows.sumOf { PyramidScoring.scoreForRowClear(it) }

            val allPyramidCleared = removal.pyramid.all { it.isRemoved }
            val victoryBonus = if (allPyramidCleared) PyramidScoring.VICTORY_BONUS else 0
            val totalPoints = basePoints + rowClearBonus + victoryBonus

            val nextState = state.copy(
                pyramid = removal.pyramid,
                stock = removal.stock,
                waste = removal.waste,
                discardPile = state.discardPile + targetCard,
                selectedLocation = null,
                score = state.score + totalPoints,
                moveCount = state.moveCount + 1
            )

            return PyramidInteractionResult.Matched(
                state = nextState,
                match = PyramidMatch.KingMatch(location, targetCard),
                pointsEarned = totalPoints,
                clearedRows = newlyClearedRows,
                isVictory = allPyramidCleared
            )
        }

        // 2. No card currently selected
        val currentSelected = state.selectedLocation
        if (currentSelected == null) {
            return PyramidInteractionResult.Selected(
                state = state.copy(selectedLocation = location),
                location = location
            )
        }

        // 3. Tapping the same card deselects it
        if (currentSelected == location) {
            return PyramidInteractionResult.Deselected(
                state = state.copy(selectedLocation = null)
            )
        }

        // 4. Another card was already selected: check pairing
        val firstCard = getCardAt(state, currentSelected)
        if (firstCard == null) {
            // Selected card is no longer valid, select the new one instead
            return PyramidInteractionResult.Selected(
                state = state.copy(selectedLocation = location),
                location = location
            )
        }

        if (PyramidRules.canPair(firstCard, targetCard)) {
            val removal = removeCardsFromPiles(state, listOf(currentSelected, location))
            val basePoints = PyramidScoring.scoreForPair(currentSelected, location)
            val newlyClearedRows = detectNewlyClearedRows(state.pyramid, removal.pyramid)
            val rowClearBonus = newlyClearedRows.sumOf { PyramidScoring.scoreForRowClear(it) }

            val allPyramidCleared = removal.pyramid.all { it.isRemoved }
            val victoryBonus = if (allPyramidCleared) PyramidScoring.VICTORY_BONUS else 0
            val totalPoints = basePoints + rowClearBonus + victoryBonus

            val nextState = state.copy(
                pyramid = removal.pyramid,
                stock = removal.stock,
                waste = removal.waste,
                discardPile = state.discardPile + listOf(firstCard, targetCard),
                selectedLocation = null,
                score = state.score + totalPoints,
                moveCount = state.moveCount + 1
            )

            return PyramidInteractionResult.Matched(
                state = nextState,
                match = PyramidMatch.PairMatch(currentSelected, firstCard, location, targetCard),
                pointsEarned = totalPoints,
                clearedRows = newlyClearedRows,
                isVictory = allPyramidCleared
            )
        }

        // 5. Invalid pair: switch selection to the new card
        return PyramidInteractionResult.InvalidPair(
            state = state.copy(selectedLocation = location),
            newSelection = location
        )
    }

    private fun removeCardsFromPiles(
        state: PyramidGameState,
        locations: List<PyramidLocation>
    ): PileRemovalResult {
        val pyramidSlotsToRemove = locations.filterIsInstance<PyramidLocation.PyramidSlot>()
        val stockRemovals = locations.count { it is PyramidLocation.Stock }
        val wasteRemovals = locations.count { it is PyramidLocation.Waste }

        val newPyramid = if (pyramidSlotsToRemove.isEmpty()) {
            state.pyramid
        } else {
            state.pyramid.map { card ->
                if (pyramidSlotsToRemove.any { it.row == card.row && it.col == card.col }) {
                    card.copy(isRemoved = true)
                } else {
                    card
                }
            }
        }

        val newStock = if (stockRemovals > 0) {
            state.stock.dropLast(stockRemovals)
        } else {
            state.stock
        }

        val newWaste = if (wasteRemovals > 0) {
            state.waste.dropLast(wasteRemovals)
        } else {
            state.waste
        }

        return PileRemovalResult(
            pyramid = newPyramid,
            stock = newStock,
            waste = newWaste
        )
    }

    private fun detectNewlyClearedRows(
        oldPyramid: List<PyramidCard>,
        newPyramid: List<PyramidCard>
    ): List<Int> {
        val cleared = mutableListOf<Int>()
        for (row in 0 until PyramidGameState.PYRAMID_ROW_COUNT) {
            val wasCleared = PyramidRules.isRowCleared(oldPyramid, row)
            val isNowCleared = PyramidRules.isRowCleared(newPyramid, row)
            if (!wasCleared && isNowCleared) {
                cleared.add(row)
            }
        }
        return cleared
    }
}
