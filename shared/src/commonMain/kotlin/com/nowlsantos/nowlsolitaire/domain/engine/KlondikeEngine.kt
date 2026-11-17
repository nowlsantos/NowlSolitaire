package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.HintMove
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.domain.model.Rank

object KlondikeEngine {

    fun drawOrRecycle(state: GameState): GameState? {
        if (state.stock.isEmpty() && state.waste.isEmpty()) return null

        return if (state.stock.isNotEmpty()) {
            val drawCount = minOf(state.drawMode.drawCount, state.stock.size)
            val drawnCards = state.stock.takeLast(drawCount)
                .reversed()
                .map { it.copy(isFaceUp = true) }

            state.copy(
                stock = state.stock.dropLast(drawCount),
                waste = state.waste + drawnCards,
                wasteVisibleCount = drawCount,
                moveCount = state.moveCount + 1
            )
        } else {
            if (!state.canRecycleStock) return null

            val recycledStock = state.waste
                .reversed()
                .map { it.copy(isFaceUp = false) }
            val newRecycleCount = state.recycleCount + 1
            val penalty = ScoringRules.scoreForRecycle(state.drawMode, newRecycleCount)

            state.copy(
                stock = recycledStock,
                waste = emptyList(),
                wasteVisibleCount = 0,
                score = ScoringRules.applyScoreDelta(state.score, penalty),
                moveCount = state.moveCount + 1,
                recycleCount = newRecycleCount
            )
        }
    }

    fun moveCards(
        state: GameState,
        from: PileLocation,
        cardIndex: Int,
        to: PileLocation
    ): GameState? {
        if (!KlondikeRules.isValidMove(state, from, cardIndex, to)) return null
        val movingCards = KlondikeRules.extractMovingCards(state, from, cardIndex) ?: return null

        var newWaste = state.waste
        var newWasteVisibleCount = state.wasteVisibleCount
        val newFoundations = state.foundations.map { it.toMutableList() }.toMutableList()
        val newTableau = state.tableau.map { it.toMutableList() }.toMutableList()
        var flippedTableauCard = false

        when (from) {
            PileLocation.Stock -> return null
            PileLocation.Waste -> {
                newWaste = state.waste.dropLast(1)
                newWasteVisibleCount = if (newWaste.isEmpty()) {
                    0
                } else {
                    (state.wasteVisibleCount - 1).coerceAtLeast(1)
                }
            }
            is PileLocation.Foundation -> {
                newFoundations[from.index].removeAt(newFoundations[from.index].lastIndex)
            }
            is PileLocation.Tableau -> {
                val sourceCol = newTableau[from.column]
                val remaining = sourceCol.subList(0, cardIndex).toMutableList()
                if (remaining.isNotEmpty() && !remaining.last().isFaceUp) {
                    remaining[remaining.lastIndex] = remaining.last().copy(isFaceUp = true)
                    flippedTableauCard = true
                }
                newTableau[from.column] = remaining
            }
        }

        when (to) {
            is PileLocation.Foundation -> {
                newFoundations[to.index].addAll(movingCards)
            }
            is PileLocation.Tableau -> {
                newTableau[to.column].addAll(movingCards)
            }
            PileLocation.Stock,
            PileLocation.Waste -> return null
        }

        val scoreDelta = ScoringRules.scoreForMove(from, to, flippedTableauCard)

        return state.copy(
            waste = newWaste,
            wasteVisibleCount = newWasteVisibleCount,
            foundations = newFoundations,
            tableau = newTableau,
            score = ScoringRules.applyScoreDelta(state.score, scoreDelta),
            moveCount = state.moveCount + 1
        )
    }

    fun autoMove(
        state: GameState,
        from: PileLocation,
        cardIndex: Int
    ): GameState? {
        val target = KlondikeRules.findAutoMoveTarget(state, from, cardIndex) ?: return null
        return moveCards(state, from, cardIndex, target)
    }

    fun stepAutoComplete(state: GameState): GameState? {
        if (!state.isAutoCompleteReady) return null

        val candidates = mutableListOf<Pair<PileLocation, Int>>()
        if (state.waste.isNotEmpty()) {
            candidates.add(PileLocation.Waste to state.waste.lastIndex)
        }
        state.tableau.forEachIndexed { colIndex, column ->
            if (column.isNotEmpty()) {
                candidates.add(PileLocation.Tableau(colIndex) to column.lastIndex)
            }
        }

        // Always pick the lowest-rank playable card first so foundations build evenly.
        val bestCandidate = candidates
            .mapNotNull { (location, index) ->
                val card = KlondikeRules.extractMovingCards(state, location, index)?.singleOrNull()
                    ?: return@mapNotNull null
                val target = KlondikeRules.findBestFoundationTarget(state, card)
                    ?: return@mapNotNull null
                Triple(location, index, target) to card.rank.value
            }
            .minByOrNull { it.second }
            ?.first
            ?: return null

        return moveCards(state, bestCandidate.first, bestCandidate.second, bestCandidate.third)
    }

    fun findHint(state: GameState, drawCount: Int = state.drawMode.drawCount): HintMove? {
        // 1. Top Tableau cards -> Foundation
        for (col in 0 until GameState.TABLEAU_COUNT) {
            val column = state.tableau[col]
            if (column.isNotEmpty()) {
                val topIndex = column.lastIndex
                val target = KlondikeRules.findBestFoundationTarget(state, column[topIndex])
                if (target != null) {
                    return HintMove(PileLocation.Tableau(col), topIndex, target)
                }
            }
        }

        // 2. Top Waste card -> Foundation
        if (state.waste.isNotEmpty()) {
            val wasteTop = state.waste.last()
            val target = KlondikeRules.findBestFoundationTarget(state, wasteTop)
            if (target != null) {
                return HintMove(PileLocation.Waste, state.waste.lastIndex, target)
            }
        }

        // 3. Tableau -> Tableau moves that uncover a face-down card or free a column for a King
        for (col in 0 until GameState.TABLEAU_COUNT) {
            val column = state.tableau[col]
            val firstFaceUpIndex = column.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIndex == -1) continue

            val movingHead = column[firstFaceUpIndex]
            val uncoversFaceDown = firstFaceUpIndex > 0
            val freesColumn = firstFaceUpIndex == 0 && movingHead.rank != Rank.KING

            if (!uncoversFaceDown && !freesColumn) continue

            for (targetCol in 0 until GameState.TABLEAU_COUNT) {
                if (targetCol == col) continue
                val target = PileLocation.Tableau(targetCol)
                if (KlondikeRules.isValidMove(state, PileLocation.Tableau(col), firstFaceUpIndex, target)) {
                    return HintMove(PileLocation.Tableau(col), firstFaceUpIndex, target)
                }
            }
        }

        // 3b. Any other Tableau -> Tableau moves, ONLY if the move makes real progress
        // (i.e. enables moving the newly exposed card, enables a foundation move, enables uncovering
        // a face-down card elsewhere, or enables playing an unburied waste/stock card).
        for (col in 0 until GameState.TABLEAU_COUNT) {
            val column = state.tableau[col]
            for (cardIdx in column.indices) {
                if (!column[cardIdx].isFaceUp) continue
                if (cardIdx == 0 && column[cardIdx].rank == Rank.KING) continue

                for (targetCol in 0 until GameState.TABLEAU_COUNT) {
                    if (targetCol == col) continue
                    if (state.tableau[targetCol].isEmpty() && cardIdx == 0) continue

                    val target = PileLocation.Tableau(targetCol)
                    if (KlondikeRules.isValidMove(state, PileLocation.Tableau(col), cardIdx, target)) {
                        if (isProgressiveLateralMove(state, col, cardIdx, targetCol)) {
                            return HintMove(PileLocation.Tableau(col), cardIdx, target)
                        }
                    }
                }
            }
        }

        // 4. Waste -> Tableau
        if (state.waste.isNotEmpty()) {
            val wasteIndex = state.waste.lastIndex
            for (targetCol in 0 until GameState.TABLEAU_COUNT) {
                val target = PileLocation.Tableau(targetCol)
                if (KlondikeRules.isValidMove(state, PileLocation.Waste, wasteIndex, target)) {
                    return HintMove(PileLocation.Waste, wasteIndex, target)
                }
            }
        }

        // 5. Draw from Stock or recycle Waste only if stock can produce a playable card
        if (canStockProduceMove(state, drawCount)) {
            return HintMove(PileLocation.Stock, 0, PileLocation.Waste)
        }

        return null
    }

    fun canStockProduceMove(state: GameState, drawCount: Int = state.drawMode.drawCount): Boolean {
        if (state.stock.isEmpty() && !state.canRecycleStock) return false

        // Fast path for Draw 1: every accessible card in stock (and waste if recyclable) will become exposed.
        if (drawCount == 1) {
            val topWaste = state.waste.lastOrNull()
            if (topWaste != null && isCardPlayable(state, topWaste)) return true
            if (state.stock.any { isCardPlayable(state, it) }) return true
            if (state.canRecycleStock && state.waste.any { isCardPlayable(state, it) }) return true
            return false
        }

        // Fast path for Draw 3: check accessible cards without cloning lists
        val totalStock = state.stock.size
        if (totalStock > 0) {
            var idx = totalStock - drawCount
            while (idx >= 0) {
                if (isCardPlayable(state, state.stock[idx])) return true
                idx -= drawCount
            }
            // Remainder card when stock exhausts
            if ((totalStock % drawCount) != 0 && isCardPlayable(state, state.stock[0])) {
                return true
            }
        }
        val topWaste = state.waste.lastOrNull()
        if (topWaste != null && isCardPlayable(state, topWaste)) return true

        if (state.canRecycleStock && state.waste.any { isCardPlayable(state, it) }) return true

        return false
    }

    private fun isCardPlayable(state: GameState, card: Card): Boolean {
        val playableCard = if (card.isFaceUp) card else card.copy(isFaceUp = true)
        for (f in state.foundations) {
            if (KlondikeRules.canMoveToFoundation(playableCard, f)) return true
        }
        for (col in state.tableau) {
            if (KlondikeRules.canMoveToTableau(playableCard, col)) return true
        }
        return false
    }

    private fun isProgressiveLateralMove(
        state: GameState,
        fromCol: Int,
        cardIdx: Int,
        toCol: Int
    ): Boolean {
        val nextState = moveCards(state, PileLocation.Tableau(fromCol), cardIdx, PileLocation.Tableau(toCol)) ?: return false

        // 1. In nextState, does the newly exposed card in fromCol have any valid move?
        val exposedIndex = cardIdx - 1
        if (exposedIndex >= 0 && fromCol < nextState.tableau.size) {
            val exposedColumn = nextState.tableau[fromCol]
            if (exposedIndex in exposedColumn.indices && exposedColumn[exposedIndex].isFaceUp) {
                val exposedCard = exposedColumn[exposedIndex]
                if (KlondikeRules.findBestFoundationTarget(nextState, exposedCard) != null) return true
                for (otherCol in 0 until GameState.TABLEAU_COUNT) {
                    if (otherCol != fromCol && KlondikeRules.isValidMove(nextState, PileLocation.Tableau(fromCol), exposedIndex, PileLocation.Tableau(otherCol))) {
                        return true
                    }
                }
            }
        }

        // 2. Does nextState allow ANY tableau card to move to foundation?
        for (col in 0 until GameState.TABLEAU_COUNT) {
            val colCards = nextState.tableau[col]
            if (colCards.isNotEmpty()) {
                if (KlondikeRules.findBestFoundationTarget(nextState, colCards.last()) != null) return true
            }
        }

        // 3. Does nextState allow top waste to move to foundation?
        if (nextState.waste.isNotEmpty()) {
            if (KlondikeRules.findBestFoundationTarget(nextState, nextState.waste.last()) != null) return true
        }

        // 4. Does nextState uncover a face-down card anywhere?
        for (col in 0 until GameState.TABLEAU_COUNT) {
            val firstFaceUp = nextState.tableau[col].indexOfFirst { it.isFaceUp }
            if (firstFaceUp > 0) {
                for (target in 0 until GameState.TABLEAU_COUNT) {
                    if (target != col && KlondikeRules.isValidMove(nextState, PileLocation.Tableau(col), firstFaceUp, PileLocation.Tableau(target))) {
                        return true
                    }
                }
            }
        }

        // 5. Does nextState allow a waste card to move to tableau?
        if (nextState.waste.isNotEmpty()) {
            for (target in 0 until GameState.TABLEAU_COUNT) {
                if (KlondikeRules.isValidMove(nextState, PileLocation.Waste, nextState.waste.lastIndex, PileLocation.Tableau(target))) {
                    return true
                }
            }
        }

        return false
    }
}
