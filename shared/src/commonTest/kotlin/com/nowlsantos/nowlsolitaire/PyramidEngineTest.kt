package com.nowlsantos.nowlsolitaire

import com.nowlsantos.nowlsolitaire.domain.engine.PyramidEngine
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidInteractionResult
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidRules
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidSession
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.DealMode
import com.nowlsantos.nowlsolitaire.domain.model.PyramidCard
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.domain.model.PyramidWinnableSeeds
import com.nowlsantos.nowlsolitaire.domain.model.Rank
import com.nowlsantos.nowlsolitaire.domain.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PyramidEngineTest {

    @Test
    fun winnableDealMode_selectsFromCuratedSeeds() {
        val session = PyramidSession.newGame(dealMode = DealMode.WINNABLE)
        assertTrue(PyramidWinnableSeeds.SEEDS.contains(session.currentState.seed))
    }

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

    @Test
    fun unlimitedRecyclesWithNoReachableMatches_isDeadlocked() {
        val state = PyramidGameState(
            pyramid = listOf(
                PyramidCard(0, 0, Card(Suit.SPADES, Rank.TWO, isFaceUp = true), isRemoved = false)
            ),
            stock = emptyList(),
            waste = listOf(
                Card(Suit.HEARTS, Rank.THREE, isFaceUp = true),
                Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true),
                Card(Suit.DIAMONDS, Rank.FIVE, isFaceUp = true)
            ),
            maxRecycles = -1,
            recyclesUsed = 0
        )

        assertFalse(PyramidRules.hasAnyMovesLeft(state))
    }

    @Test
    fun nonEmptyStock_isNotDeadlocked() {
        val state = PyramidGameState(
            pyramid = listOf(
                PyramidCard(0, 0, Card(Suit.SPADES, Rank.TWO, isFaceUp = true), isRemoved = false)
            ),
            stock = listOf(
                Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)
            ),
            waste = listOf(
                Card(Suit.DIAMONDS, Rank.FIVE, isFaceUp = true)
            ),
            maxRecycles = -1,
            recyclesUsed = 0
        )

        assertTrue(PyramidRules.hasAnyMovesLeft(state))
    }

    @Test
    fun buriedWasteMatchReachableAfterRecycle_hasMovesLeft() {
        val state = PyramidGameState(
            pyramid = listOf(
                PyramidCard(0, 0, Card(Suit.SPADES, Rank.THREE, isFaceUp = true), isRemoved = false)
            ),
            stock = listOf(Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true)),
            waste = listOf(
                Card(Suit.HEARTS, Rank.TEN, isFaceUp = true),
                Card(Suit.DIAMONDS, Rank.FIVE, isFaceUp = true)
            ),
            maxRecycles = 1,
            recyclesUsed = 0
        )

        assertTrue(PyramidRules.hasAnyMovesLeft(state))

        val exhausted = state.copy(stock = emptyList(), recyclesUsed = 1)
        assertFalse(PyramidRules.hasAnyMovesLeft(exhausted))
    }

    @Test
    fun adjacentCardsInWasteBecomeStockWastePairAfterRecycle() {
        // After drawing 4C and recycling: stock = [4C, 9H, 4S], waste = []
        // First draw puts 4S in waste and 9H on top of stock -> 4S + 9H = 13!
        val state = PyramidGameState(
            pyramid = listOf(
                PyramidCard(0, 0, Card(Suit.SPADES, Rank.TWO, isFaceUp = true), isRemoved = false)
            ),
            stock = listOf(Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true)),
            waste = listOf(
                Card(Suit.SPADES, Rank.FOUR, isFaceUp = true),
                Card(Suit.HEARTS, Rank.NINE, isFaceUp = true)
            ),
            maxRecycles = 1,
            recyclesUsed = 0
        )

        assertTrue(PyramidRules.hasAnyMovesLeft(state))
    }

    @Test
    fun nonAdjacentStockWasteCardsWithNoPyramidMatch_isDeadlocked() {
        val state = PyramidGameState(
            pyramid = listOf(
                PyramidCard(0, 0, Card(Suit.SPADES, Rank.THREE, isFaceUp = true), isRemoved = false)
            ),
            stock = emptyList(),
            waste = listOf(
                Card(Suit.SPADES, Rank.FOUR, isFaceUp = true),
                Card(Suit.CLUBS, Rank.TWO, isFaceUp = true),
                Card(Suit.HEARTS, Rank.NINE, isFaceUp = true)
            ),
            maxRecycles = -1,
            recyclesUsed = 0
        )

        assertFalse(PyramidRules.hasAnyMovesLeft(state))
    }

    @Test
    fun pyramidRunSession_initialValues_matchDefaults() {
        val session = com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession()
        assertEquals(1, session.currentRound)
        assertEquals(3, session.lives)
        assertEquals(5, session.maxLives)
        assertEquals(0, session.consecutiveWins)
        assertEquals(0, session.totalRunScore)
        assertFalse(session.isGameOver)
    }

    @Test
    fun pyramidRunSession_streakMilestones_grantBonusLivesAndCapAtMax() {
        var session = com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession(lives = 2)
        assertEquals(2, session.lives)

        // 3 consecutive wins awards +1 life
        val streak3 = 3
        val bonus1 = if (streak3 == 3) 1 else 0
        session = session.copy(
            lives = minOf(session.maxLives, session.lives + bonus1),
            consecutiveWins = streak3
        )
        assertEquals(3, session.lives)

        // 6 consecutive wins awards +2 lives -> 3 + 2 = 5 (max)
        val streak6 = 6
        val bonus2 = if (streak6 == 6) 2 else 0
        session = session.copy(
            lives = minOf(session.maxLives, session.lives + bonus2),
            consecutiveWins = streak6
        )
        assertEquals(5, session.lives)
        assertTrue(session.isMaxLives)

        // Another bonus life capped at 5
        val bonusExtra = 1
        session = session.copy(
            lives = minOf(session.maxLives, session.lives + bonusExtra)
        )
        assertEquals(5, session.lives)
    }

    @Test
    fun pyramidRunSession_progressiveDifficulty_scalesCorrectly() {
        val defaultRecycles = com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState.DEFAULT_MAX_RECYCLES

        // Rounds 1-3: Novice (2 recycles / 3 passes)
        assertEquals(2, com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession.getEffectiveMaxRecycles(1, defaultRecycles))
        assertEquals(2, com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession.getEffectiveMaxRecycles(3, defaultRecycles))

        // Rounds 4-6: Veteran (1 recycle / 2 passes)
        assertEquals(1, com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession.getEffectiveMaxRecycles(4, defaultRecycles))
        assertEquals(1, com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession.getEffectiveMaxRecycles(6, defaultRecycles))

        // Rounds 7+: Master (0 recycles / 1 pass)
        assertEquals(0, com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession.getEffectiveMaxRecycles(7, defaultRecycles))
        assertEquals(0, com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession.getEffectiveMaxRecycles(10, defaultRecycles))

        // Custom override: preserved regardless of round
        assertEquals(-1, com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession.getEffectiveMaxRecycles(8, -1))
        assertEquals(5, com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession.getEffectiveMaxRecycles(12, 5))
    }

    @Test
    fun pyramidRunSession_runRecording_sortsAndLimitsTop5() {
        var session = com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession()

        // 0-score round 1 is ignored
        session = session.withRunRecorded()
        assertTrue(session.topRuns.isEmpty())

        // Run 1: round 2, score 250
        session = session.copy(currentRound = 2, totalRunScore = 250, consecutiveWins = 1).withRunRecorded()
        assertEquals(1, session.topRuns.size)
        assertEquals(2, session.topRuns[0].roundReached)
        assertEquals(250, session.topRuns[0].totalScore)

        // Run 2: round 4, score 600
        session = session.copy(currentRound = 4, totalRunScore = 600, consecutiveWins = 3).withRunRecorded()
        assertEquals(2, session.topRuns.size)
        assertEquals(4, session.topRuns[0].roundReached) // higher round first

        // Run 3: round 4, score 750 (higher score on same round)
        session = session.copy(currentRound = 4, totalRunScore = 750, consecutiveWins = 3).withRunRecorded()
        assertEquals(3, session.topRuns.size)
        assertEquals(750, session.topRuns[0].totalScore)
        assertEquals(600, session.topRuns[1].totalScore)

        // Duplicate recording check: same round, score, streak is not duplicated
        val sizeBefore = session.topRuns.size
        session = session.withRunRecorded()
        assertEquals(sizeBefore, session.topRuns.size)

        // Add 3 more runs to exceed top 5
        session = session.copy(currentRound = 5, totalRunScore = 1000).withRunRecorded()
        session = session.copy(currentRound = 3, totalRunScore = 400).withRunRecorded()
        session = session.copy(currentRound = 1, totalRunScore = 150).withRunRecorded()

        // Should be capped at top 5
        assertEquals(5, session.topRuns.size)
        assertEquals(5, session.topRuns[0].roundReached)
        assertEquals(4, session.topRuns[1].roundReached)
        assertEquals(750, session.topRuns[1].totalScore)
    }
}
