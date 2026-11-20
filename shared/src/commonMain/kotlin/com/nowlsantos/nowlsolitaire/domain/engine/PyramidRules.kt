package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.PyramidCard
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.domain.model.Rank

object PyramidRules {

    private fun findSlot(pyramid: List<PyramidCard>, row: Int, col: Int): PyramidCard? {
        val expectedIdx = row * (row + 1) / 2 + col
        val direct = pyramid.getOrNull(expectedIdx)
        if (direct != null && direct.row == row && direct.col == col) {
            return direct
        }
        return pyramid.firstOrNull { it.row == row && it.col == col }
    }

    fun isExposed(pyramid: List<PyramidCard>, row: Int, col: Int): Boolean {
        if (row < 0 || row >= PyramidGameState.PYRAMID_ROW_COUNT || col < 0 || col > row) {
            return false
        }
        val targetCard = findSlot(pyramid, row, col) ?: return false
        if (targetCard.isRemoved) return false

        // Bottom row cards are never covered by children
        if (row == PyramidGameState.PYRAMID_ROW_COUNT - 1) {
            return true
        }

        val leftChild = findSlot(pyramid, row + 1, col)
        val rightChild = findSlot(pyramid, row + 1, col + 1)

        val leftRemoved = leftChild == null || leftChild.isRemoved
        val rightRemoved = rightChild == null || rightChild.isRemoved

        return leftRemoved && rightRemoved
    }

    fun isExposed(pyramid: List<PyramidCard>, slot: PyramidLocation.PyramidSlot): Boolean =
        isExposed(pyramid, slot.row, slot.col)

    fun getExposedCards(pyramid: List<PyramidCard>): List<PyramidCard> =
        pyramid.filter { !it.isRemoved && isExposed(pyramid, it.row, it.col) }

    fun isKing(card: Card): Boolean = card.rank == Rank.KING

    fun canPair(cardA: Card, cardB: Card): Boolean =
        (cardA.rank.value + cardB.rank.value) == PyramidGameState.TARGET_SUM

    fun isRowCleared(pyramid: List<PyramidCard>, row: Int): Boolean {
        val rowCards = pyramid.filter { it.row == row }
        return rowCards.isNotEmpty() && rowCards.all { it.isRemoved }
    }

    fun findAvailableMoves(state: PyramidGameState): List<PyramidMatch> {
        val moves = mutableListOf<PyramidMatch>()
        val exposed = getExposedCards(state.pyramid)

        // 1. Kings in pyramid
        for ((row, col, card) in exposed) {
            if (isKing(card)) {
                moves.add(
                    PyramidMatch.KingMatch(
                        location = PyramidLocation.PyramidSlot(row, col),
                        card = card
                    )
                )
            }
        }

        // 2. King on waste
        state.topWasteCard?.let { wasteCard ->
            if (isKing(wasteCard)) {
                moves.add(
                    PyramidMatch.KingMatch(
                        location = PyramidLocation.Waste,
                        card = wasteCard
                    )
                )
            }
        }

        // 3. King on stock
        state.topStockCard?.let { stockCard ->
            if (isKing(stockCard)) {
                moves.add(
                    PyramidMatch.KingMatch(
                        location = PyramidLocation.Stock,
                        card = stockCard
                    )
                )
            }
        }

        // 4. Pairs among exposed pyramid cards
        for (i in exposed.indices) {
            for (j in i + 1 until exposed.size) {
                val a = exposed[i]
                val b = exposed[j]
                if (canPair(a.card, b.card)) {
                    moves.add(
                        PyramidMatch.PairMatch(
                            firstLocation = PyramidLocation.PyramidSlot(a.row, a.col),
                            firstCard = a.card,
                            secondLocation = PyramidLocation.PyramidSlot(b.row, b.col),
                            secondCard = b.card
                        )
                    )
                }
            }
        }

        // 5. Pairs between an exposed pyramid card and top of waste
        state.topWasteCard?.let { wasteCard ->
            for ((row, col, card) in exposed) {
                if (canPair(card, wasteCard)) {
                    moves.add(
                        PyramidMatch.PairMatch(
                            firstLocation = PyramidLocation.PyramidSlot(row, col),
                            firstCard = card,
                            secondLocation = PyramidLocation.Waste,
                            secondCard = wasteCard
                        )
                    )
                }
            }
        }

        // 6. Pairs between an exposed pyramid card and top of stock
        state.topStockCard?.let { stockCard ->
            for ((row, col, card) in exposed) {
                if (canPair(card, stockCard)) {
                    moves.add(
                        PyramidMatch.PairMatch(
                            firstLocation = PyramidLocation.PyramidSlot(row, col),
                            firstCard = card,
                            secondLocation = PyramidLocation.Stock,
                            secondCard = stockCard
                        )
                    )
                }
            }
        }

        // 7. Pair between top of stock and top of waste
        val stockCard = state.topStockCard
        val wasteCard = state.topWasteCard
        if (stockCard != null && wasteCard != null && canPair(stockCard, wasteCard)) {
            moves.add(
                PyramidMatch.PairMatch(
                    firstLocation = PyramidLocation.Stock,
                    firstCard = stockCard,
                    secondLocation = PyramidLocation.Waste,
                    secondCard = wasteCard
                )
            )
        }

        return moves
    }

    fun hasAnyMovesLeft(
        state: PyramidGameState,
        isCancelled: () -> Boolean = { false }
    ): Boolean {
        if (state.isWon) return false
        if (findAvailableMoves(state).isNotEmpty()) return true
        if (state.stock.isNotEmpty()) return true
        if (!state.canRecycleStock) return false

        var sim = state
        val seenCycles = HashSet<Pair<List<Card>, List<Card>>>()
        while (!isCancelled()) {
            val next = PyramidEngine.drawOrRecycle(sim) ?: return false
            if (isCancelled()) return true
            if (findAvailableMoves(next).isNotEmpty()) {
                return true
            }
            val cycleKey = next.stock to next.waste
            if (!seenCycles.add(cycleKey)) {
                return false
            }
            sim = next
        }
        return true
    }

    fun findBestHint(state: PyramidGameState): PyramidMatch? {
        val moves = findAvailableMoves(state)
        if (moves.isEmpty()) return null

        // Priority order:
        // 1. Pyramid King (clears pyramid card immediately)
        // 2. Pyramid + Pyramid pair (clears two pyramid cards)
        // 3. Pyramid + Stock/Waste pair (clears one pyramid card)
        // 4. Stock + Waste pair (clears two pile cards)
        // 5. Stock/Waste King
        val pyramidKing = moves.firstOrNull { it is PyramidMatch.KingMatch && it.location is PyramidLocation.PyramidSlot }
        if (pyramidKing != null) return pyramidKing

        val pyramidPair = moves.firstOrNull {
            it is PyramidMatch.PairMatch &&
                it.firstLocation is PyramidLocation.PyramidSlot &&
                it.secondLocation is PyramidLocation.PyramidSlot
        }
        if (pyramidPair != null) return pyramidPair

        val pyramidPilePair = moves.firstOrNull {
            it is PyramidMatch.PairMatch &&
                (it.firstLocation is PyramidLocation.PyramidSlot || it.secondLocation is PyramidLocation.PyramidSlot)
        }
        if (pyramidPilePair != null) return pyramidPilePair

        val stockWastePair = moves.firstOrNull { it is PyramidMatch.PairMatch }
        if (stockWastePair != null) return stockWastePair

        return moves.firstOrNull()
    }
}
