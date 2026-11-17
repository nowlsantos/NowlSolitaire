package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.domain.model.Rank

object KlondikeRules {

    fun canMoveToFoundation(card: Card, foundationPile: List<Card>): Boolean {
        if (!card.isFaceUp) return false
        val top = foundationPile.lastOrNull() ?: return card.rank == Rank.ACE
        return top.suit == card.suit && card.rank.value == top.rank.value + 1
    }

    fun canMoveToTableau(movingHead: Card, targetColumn: List<Card>): Boolean {
        if (!movingHead.isFaceUp) return false
        val top = targetColumn.lastOrNull() ?: return movingHead.rank == Rank.KING
        return top.isFaceUp &&
            top.color != movingHead.color &&
            top.rank.value == movingHead.rank.value + 1
    }

    fun isValidMove(
        state: GameState,
        from: PileLocation,
        cardIndex: Int,
        to: PileLocation
    ): Boolean {
        if (from == to) return false
        if (to is PileLocation.Stock || to is PileLocation.Waste) return false

        val movingCards = extractMovingCards(state, from, cardIndex) ?: return false
        val headCard = movingCards.first()

        return when (to) {
            is PileLocation.Foundation -> {
                if (to.index !in 0 until GameState.FOUNDATION_COUNT) return false
                if (from is PileLocation.Foundation) return false
                if (movingCards.size != 1) return false
                canMoveToFoundation(headCard, state.foundations[to.index])
            }
            is PileLocation.Tableau -> {
                if (to.column !in 0 until GameState.TABLEAU_COUNT) return false
                canMoveToTableau(headCard, state.tableau[to.column])
            }
            PileLocation.Stock,
            PileLocation.Waste -> false
        }
    }

    fun extractMovingCards(
        state: GameState,
        from: PileLocation,
        cardIndex: Int
    ): List<Card>? {
        return when (from) {
            PileLocation.Stock -> null
            PileLocation.Waste -> {
                if (state.waste.isNotEmpty() && cardIndex == state.waste.lastIndex) {
                    listOf(state.waste.last())
                } else {
                    null
                }
            }
            is PileLocation.Foundation -> {
                if (from.index !in 0 until GameState.FOUNDATION_COUNT) return null
                val pile = state.foundations[from.index]
                if (pile.isNotEmpty() && cardIndex == pile.lastIndex) {
                    listOf(pile.last())
                } else {
                    null
                }
            }
            is PileLocation.Tableau -> {
                if (from.column !in 0 until GameState.TABLEAU_COUNT) return null
                val column = state.tableau[from.column]
                if (cardIndex !in column.indices) return null
                if (!column[cardIndex].isFaceUp) return null
                column.subList(cardIndex, column.size)
            }
        }
    }

    fun findBestFoundationTarget(state: GameState, card: Card): PileLocation.Foundation? {
        // Prefer an existing pile of the same suit, then first empty pile for an Ace.
        state.foundations.forEachIndexed { index, pile ->
            if (pile.isNotEmpty() && canMoveToFoundation(card, pile)) {
                return PileLocation.Foundation(index)
            }
        }
        state.foundations.forEachIndexed { index, pile ->
            if (pile.isEmpty() && canMoveToFoundation(card, pile)) {
                return PileLocation.Foundation(index)
            }
        }
        return null
    }

    fun findAutoMoveTarget(
        state: GameState,
        from: PileLocation,
        cardIndex: Int
    ): PileLocation? {
        val movingCards = extractMovingCards(state, from, cardIndex) ?: return null
        val headCard = movingCards.first()

        // 1. Single card from Waste or top of Tableau -> try Foundation first.
        if (movingCards.size == 1 && from !is PileLocation.Foundation) {
            findBestFoundationTarget(state, headCard)?.let { return it }
        }

        // 2. Try non-empty Tableau columns first.
        for (col in 0 until GameState.TABLEAU_COUNT) {
            val target = PileLocation.Tableau(col)
            if (target == from) continue
            if (state.tableau[col].isNotEmpty() && isValidMove(state, from, cardIndex, target)) {
                return target
            }
        }

        // 3. Try empty Tableau columns (avoid bouncing a base King between empty columns).
        val isBaseKingInTableau = from is PileLocation.Tableau && cardIndex == 0 && headCard.rank == Rank.KING
        if (!isBaseKingInTableau) {
            for (col in 0 until GameState.TABLEAU_COUNT) {
                val target = PileLocation.Tableau(col)
                if (target == from) continue
                if (state.tableau[col].isEmpty() && isValidMove(state, from, cardIndex, target)) {
                    return target
                }
            }
        }

        return null
    }
}
