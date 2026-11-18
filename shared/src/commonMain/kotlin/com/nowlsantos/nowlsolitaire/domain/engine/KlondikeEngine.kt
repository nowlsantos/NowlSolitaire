package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.DrawMode
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.HintMove
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.domain.model.Rank

object KlondikeEngine {

    private const val MAX_HINT_SEARCH_STATES = 800
    private const val MAX_HINT_SEARCH_DEPTH = 8
    private const val MAX_FOUNDATION_PULLS = 2

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

    fun findHint(
        state: GameState,
        drawCount: Int = state.drawMode.drawCount,
        isCancelled: () -> Boolean = { false }
    ): HintMove? {
        if (isCancelled()) return null

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

        // 3b. Waste -> Tableau
        if (state.waste.isNotEmpty()) {
            val wasteIndex = state.waste.lastIndex
            for (targetCol in 0 until GameState.TABLEAU_COUNT) {
                val target = PileLocation.Tableau(targetCol)
                if (KlondikeRules.isValidMove(state, PileLocation.Waste, wasteIndex, target)) {
                    return HintMove(PileLocation.Waste, wasteIndex, target)
                }
            }
        }

        // 4. Draw from Stock or recycle Waste if stock/waste passes can produce a playable card
        if (canStockProduceMove(state, drawCount)) {
            return HintMove(PileLocation.Stock, 0, PileLocation.Waste)
        }

        if (isCancelled()) return null

        // 5. Multi-step Tableau reorganizations and Foundation -> Tableau pull-downs that unlock progress
        val sequenceMove = findProgressiveSequenceFirstMove(state, drawCount, isCancelled)
        if (sequenceMove != null) {
            return sequenceMove
        }

        // 6. If stock has un-opened cards, suggest drawing from Stock
        if (state.stock.isNotEmpty()) {
            return HintMove(PileLocation.Stock, 0, PileLocation.Waste)
        }

        return null
    }

    fun hasAvailableMoves(
        state: GameState,
        drawCount: Int = state.drawMode.drawCount,
        isCancelled: () -> Boolean = { false }
    ): Boolean {
        if (state.isWon) return false
        if (state.isAutoCompleteReady) return true
        if (state.stock.isNotEmpty()) return true
        if (state.canRecycleStock && canStockProduceMove(state, drawCount)) return true
        return findHint(state, drawCount, isCancelled) != null
    }

    fun canStockProduceMove(state: GameState, drawCount: Int = state.drawMode.drawCount): Boolean {
        val reachableCards = collectReachableStockCards(state, drawCount)
        for (i in reachableCards.indices) {
            if (isCardPlayable(state, reachableCards[i])) return true
        }
        return false
    }

    private fun collectReachableStockCards(state: GameState, drawCount: Int): List<Card> {
        val result = mutableListOf<Card>()
        val seenCards = HashSet<Card>()
        state.waste.lastOrNull()?.let { topWaste ->
            val playable = if (topWaste.isFaceUp) topWaste else topWaste.copy(isFaceUp = true)
            if (seenCards.add(playable)) {
                result.add(playable)
            }
        }

        if (state.stock.isEmpty() && !state.canRecycleStock) return result

        var sim = if (state.drawMode.drawCount == drawCount) {
            state
        } else {
            state.copy(drawMode = if (drawCount >= 3) DrawMode.DRAW_THREE else DrawMode.DRAW_ONE)
        }
        val seenCycles = HashSet<Pair<List<Card>, List<Card>>>()
        while (true) {
            val next = drawOrRecycle(sim) ?: break
            val nextTop = next.waste.lastOrNull()
            if (nextTop != null) {
                val playable = if (nextTop.isFaceUp) nextTop else nextTop.copy(isFaceUp = true)
                if (seenCards.add(playable)) {
                    result.add(playable)
                }
            }
            val cycleKey = next.stock to next.waste
            if (!seenCycles.add(cycleKey)) {
                break
            }
            sim = next
        }
        return result
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

    private data class BoardKey(
        val tableau: List<List<Card>>,
        val foundationTops: List<Card>
    ) {
        companion object {
            fun from(state: GameState): BoardKey = BoardKey(
                tableau = state.tableau,
                foundationTops = state.foundations
                    .mapNotNull { it.lastOrNull() }
                    .sortedBy { it.suit.ordinal }
            )
        }
    }

    private data class SearchNode(
        val state: GameState,
        val firstMove: HintMove?,
        val depth: Int,
        val foundationPulls: Int
    )

    private fun countFaceDownCards(state: GameState): Int {
        var count = 0
        for (col in state.tableau) {
            for (card in col) {
                if (!card.isFaceUp) count++
            }
        }
        return count
    }

    private fun countFoundationCards(state: GameState): Int {
        var count = 0
        for (pile in state.foundations) {
            count += pile.size
        }
        return count
    }

    private fun findProgressiveSequenceFirstMove(
        initialState: GameState,
        drawCount: Int,
        isCancelled: () -> Boolean = { false }
    ): HintMove? {
        val initialFaceDownCount = countFaceDownCards(initialState)
        val initialFoundationCount = countFoundationCards(initialState)
        val reachableStockCards = collectReachableStockCards(initialState, drawCount)

        val queue = ArrayDeque<SearchNode>()
        val visited = HashSet<BoardKey>()
        visited.add(BoardKey.from(initialState))
        queue.addLast(SearchNode(state = initialState, firstMove = null, depth = 0, foundationPulls = 0))

        while (queue.isNotEmpty() && visited.size < MAX_HINT_SEARCH_STATES) {
            if (isCancelled()) return null
            val node = queue.removeFirst()
            if (node.depth >= MAX_HINT_SEARCH_DEPTH) continue
            val curr = node.state

            // 1. Tableau -> Tableau moves (including splitting face-up runs & freeing columns)
            for (col in 0 until GameState.TABLEAU_COUNT) {
                val column = curr.tableau[col]
                for (cardIdx in column.indices) {
                    if (!column[cardIdx].isFaceUp) continue
                    val isBaseKing = cardIdx == 0 && column[cardIdx].rank == Rank.KING
                    for (targetCol in 0 until GameState.TABLEAU_COUNT) {
                        if (targetCol == col) continue
                        if (isBaseKing && curr.tableau[targetCol].isEmpty()) continue
                        if (cardIdx == 0 && curr.tableau[targetCol].isEmpty()) continue

                        val from = PileLocation.Tableau(col)
                        val to = PileLocation.Tableau(targetCol)
                        val nextState = moveCards(curr, from, cardIdx, to) ?: continue
                        val firstMove = node.firstMove ?: HintMove(from, cardIdx, to)

                        if (isProgressAchieved(nextState, initialFaceDownCount, initialFoundationCount, reachableStockCards)) {
                            return firstMove
                        }
                        if (visited.size < MAX_HINT_SEARCH_STATES && visited.add(BoardKey.from(nextState))) {
                            queue.addLast(SearchNode(nextState, firstMove, node.depth + 1, node.foundationPulls))
                        }
                    }
                }
            }

            // 2. Tableau -> Foundation (can combine with Foundation pull-downs)
            for (col in 0 until GameState.TABLEAU_COUNT) {
                val column = curr.tableau[col]
                if (column.isEmpty()) continue
                val topIdx = column.lastIndex
                val target = KlondikeRules.findBestFoundationTarget(curr, column[topIdx]) ?: continue
                val from = PileLocation.Tableau(col)
                val nextState = moveCards(curr, from, topIdx, target) ?: continue
                val firstMove = node.firstMove ?: HintMove(from, topIdx, target)

                if (isProgressAchieved(nextState, initialFaceDownCount, initialFoundationCount, reachableStockCards)) {
                    return firstMove
                }
                if (visited.size < MAX_HINT_SEARCH_STATES && visited.add(BoardKey.from(nextState))) {
                    queue.addLast(SearchNode(nextState, firstMove, node.depth + 1, node.foundationPulls))
                }
            }

            // 3. Foundation -> Tableau pull-downs (bounded to avoid pointless cascading)
            if (node.foundationPulls < MAX_FOUNDATION_PULLS) {
                for (fIdx in 0 until GameState.FOUNDATION_COUNT) {
                    val pile = curr.foundations[fIdx]
                    if (pile.isEmpty()) continue
                    val topIdx = pile.lastIndex
                    val from = PileLocation.Foundation(fIdx)
                    for (targetCol in 0 until GameState.TABLEAU_COUNT) {
                        val to = PileLocation.Tableau(targetCol)
                        val nextState = moveCards(curr, from, topIdx, to) ?: continue
                        val firstMove = node.firstMove ?: HintMove(from, topIdx, to)

                        if (isProgressAchieved(nextState, initialFaceDownCount, initialFoundationCount, reachableStockCards)) {
                            return firstMove
                        }
                        if (visited.size < MAX_HINT_SEARCH_STATES && visited.add(BoardKey.from(nextState))) {
                            queue.addLast(
                                SearchNode(nextState, firstMove, node.depth + 1, node.foundationPulls + 1)
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    private fun isProgressAchieved(
        state: GameState,
        initialFaceDownCount: Int,
        initialFoundationCount: Int,
        reachableStockCards: List<Card>
    ): Boolean {
        if (countFaceDownCards(state) < initialFaceDownCount) return true
        if (countFoundationCards(state) > initialFoundationCount) return true
        for (i in reachableStockCards.indices) {
            if (isCardPlayable(state, reachableStockCards[i])) {
                return true
            }
        }
        return false
    }
}
