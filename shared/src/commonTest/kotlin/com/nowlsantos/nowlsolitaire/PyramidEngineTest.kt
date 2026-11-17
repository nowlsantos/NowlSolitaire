package com.nowlsantos.nowlsolitaire

import com.nowlsantos.nowlsolitaire.domain.engine.PyramidEngine
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidInteractionResult
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidRules
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.domain.model.Rank
import com.nowlsantos.nowlsolitaire.domain.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PyramidEngineTest {

    @Test
    fun dealAndRecycleKeepStockCardsFaceUp() {
        val initial = PyramidEngine.dealNewGame(seed = 42L)
        assertEquals(PyramidGameState.STOCK_CARD_COUNT, initial.stock.size)
        assertTrue(initial.stock.all { it.isFaceUp })

        var state = initial
        while (state.stock.isNotEmpty()) {
            state = PyramidEngine.drawStock(state)!!
        }
        assertTrue(state.stock.isEmpty())
        assertEquals(PyramidGameState.STOCK_CARD_COUNT, state.waste.size)

        val recycled = PyramidEngine.recycleStock(state)!!
        assertEquals(PyramidGameState.STOCK_CARD_COUNT, recycled.stock.size)
        assertTrue(recycled.stock.all { it.isFaceUp })
    }

    @Test
    fun stockTopCardPairsWithWasteTopCard() {
        val stockCard = Card(Suit.HEARTS, Rank.SIX, isFaceUp = true)
        val wasteCard = Card(Suit.SPADES, Rank.SEVEN, isFaceUp = true)
        val state = PyramidGameState(
            stock = listOf(Card(Suit.CLUBS, Rank.TWO, isFaceUp = true), stockCard),
            waste = listOf(wasteCard)
        )

        val moves = PyramidRules.findAvailableMoves(state)
        assertTrue(
            moves.any {
                it is PyramidMatch.PairMatch &&
                    it.firstLocation == PyramidLocation.Stock &&
                    it.secondLocation == PyramidLocation.Waste
            }
        )

        val selected = PyramidEngine.interactCard(state, PyramidLocation.Stock)
        assertIs<PyramidInteractionResult.Selected>(selected)

        val matched = PyramidEngine.interactCard(selected.state, PyramidLocation.Waste)
        assertIs<PyramidInteractionResult.Matched>(matched)
        assertEquals(1, matched.state.stock.size)
        assertTrue(matched.state.waste.isEmpty())
        assertEquals(listOf(stockCard, wasteCard), matched.state.discardPile)
    }

    @Test
    fun stockKingCanBeRemovedDirectly() {
        val king = Card(Suit.DIAMONDS, Rank.KING, isFaceUp = true)
        val state = PyramidGameState(
            stock = listOf(king)
        )

        val matched = PyramidEngine.interactCard(state, PyramidLocation.Stock)
        assertIs<PyramidInteractionResult.Matched>(matched)
        assertTrue(matched.state.stock.isEmpty())
        assertEquals(listOf(king), matched.state.discardPile)
    }
}
