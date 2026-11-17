package com.nowlsantos.nowlsolitaire

import com.nowlsantos.nowlsolitaire.domain.engine.KlondikeEngine
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.DeckPasses
import com.nowlsantos.nowlsolitaire.domain.model.DrawMode
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.domain.model.Rank
import com.nowlsantos.nowlsolitaire.domain.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KlondikeEngineTest {

    private fun card(suit: Suit, rank: Rank, faceUp: Boolean = true) =
        Card(suit = suit, rank = rank, isFaceUp = faceUp)

    private fun emptyTableau(): List<List<Card>> = List(GameState.TABLEAU_COUNT) { emptyList() }

    private fun emptyFoundations(): List<List<Card>> = List(GameState.FOUNDATION_COUNT) { emptyList() }

    @Test
    fun draw1_nonEmptyUnplayableStock_withPlayableCardBuriedInWaste_andRecyclesLeft_hasMoves() {
        val state = GameState(
            stock = listOf(card(Suit.CLUBS, Rank.NINE, faceUp = false)),
            waste = listOf(
                card(Suit.SPADES, Rank.ACE, faceUp = true),
                card(Suit.DIAMONDS, Rank.SEVEN, faceUp = true)
            ),
            foundations = emptyFoundations(),
            tableau = emptyTableau(),
            drawMode = DrawMode.DRAW_ONE,
            deckPasses = DeckPasses.UNLIMITED
        )

        assertTrue(KlondikeEngine.canStockProduceMove(state))
        val hint = KlondikeEngine.findHint(state)
        assertNotNull(hint)
        assertEquals(PileLocation.Stock, hint.from)
        assertTrue(KlondikeEngine.hasAvailableMoves(state))
    }

    @Test
    fun draw1_nonEmptyUnplayableStock_withPlayableCardBuriedInWaste_andNoRecyclesLeft_isDeadlocked() {
        val state = GameState(
            stock = listOf(card(Suit.CLUBS, Rank.NINE, faceUp = false)),
            waste = listOf(
                card(Suit.SPADES, Rank.ACE, faceUp = true),
                card(Suit.DIAMONDS, Rank.SEVEN, faceUp = true)
            ),
            foundations = emptyFoundations(),
            tableau = emptyTableau(),
            drawMode = DrawMode.DRAW_ONE,
            deckPasses = DeckPasses.ONE_PASS,
            recycleCount = 1
        )

        assertFalse(KlondikeEngine.canStockProduceMove(state))
        assertNull(KlondikeEngine.findHint(state))
        assertFalse(KlondikeEngine.hasAvailableMoves(state))
    }

    @Test
    fun draw3_phaseShiftAfterRecycle_revealsPlayableStockCard() {
        // stock = [C0, C1, C2] -> pass 1 draws all 3, putting C0 (8D) on top of waste.
        // waste = [9C] -> after pass 1, waste = [9C, C2, C1, C0].
        // After recycle, stock = [C0, C1, C2, 9C] (size 4).
        // Pass 2 draw 1 takes [C1, C2, 9C] -> C1 (AS) lands on top and is playable to Foundation!
        val state = GameState(
            stock = listOf(
                card(Suit.DIAMONDS, Rank.EIGHT, faceUp = false),
                card(Suit.SPADES, Rank.ACE, faceUp = false),
                card(Suit.HEARTS, Rank.FOUR, faceUp = false)
            ),
            waste = listOf(card(Suit.CLUBS, Rank.NINE, faceUp = true)),
            foundations = emptyFoundations(),
            tableau = emptyTableau(),
            drawMode = DrawMode.DRAW_THREE,
            deckPasses = DeckPasses.UNLIMITED
        )

        assertTrue(KlondikeEngine.canStockProduceMove(state))
        val hint = KlondikeEngine.findHint(state)
        assertNotNull(hint)
        assertEquals(PileLocation.Stock, hint.from)
    }

    @Test
    fun draw3_unreachableBuriedWasteCard_isDeadlocked() {
        // After recycle, stock = [3D, 2C, AS] (size 3). Draw 3 always exposes 3D on top.
        val state = GameState(
            stock = emptyList(),
            waste = listOf(
                card(Suit.SPADES, Rank.ACE, faceUp = true),
                card(Suit.CLUBS, Rank.TWO, faceUp = true),
                card(Suit.DIAMONDS, Rank.THREE, faceUp = true)
            ),
            foundations = emptyFoundations(),
            tableau = emptyTableau(),
            drawMode = DrawMode.DRAW_THREE,
            deckPasses = DeckPasses.UNLIMITED
        )

        assertFalse(KlondikeEngine.canStockProduceMove(state))
        assertNull(KlondikeEngine.findHint(state))
    }

    @Test
    fun foundationToTableauMove_unlocksWastePlay_isNotDeadlocked() {
        val tableau = emptyTableau().toMutableList()
        tableau[0] = listOf(card(Suit.SPADES, Rank.KING))

        val foundations = emptyFoundations().toMutableList()
        foundations[0] = listOf(
            card(Suit.HEARTS, Rank.ACE),
            card(Suit.HEARTS, Rank.TWO),
            card(Suit.HEARTS, Rank.THREE),
            card(Suit.HEARTS, Rank.FOUR),
            card(Suit.HEARTS, Rank.FIVE),
            card(Suit.HEARTS, Rank.SIX),
            card(Suit.HEARTS, Rank.SEVEN),
            card(Suit.HEARTS, Rank.EIGHT),
            card(Suit.HEARTS, Rank.NINE),
            card(Suit.HEARTS, Rank.TEN),
            card(Suit.HEARTS, Rank.JACK),
            card(Suit.HEARTS, Rank.QUEEN)
        )

        val state = GameState(
            stock = emptyList(),
            waste = listOf(card(Suit.SPADES, Rank.JACK)),
            foundations = foundations,
            tableau = tableau,
            drawMode = DrawMode.DRAW_ONE,
            deckPasses = DeckPasses.ONE_PASS,
            recycleCount = 1
        )

        val hint = KlondikeEngine.findHint(state)
        assertNotNull(hint)
        assertEquals(PileLocation.Foundation(0), hint.from)
        assertEquals(PileLocation.Tableau(0), hint.to)
    }

    @Test
    fun multiStepTableauReorganization_unlocksStockKing_isDetected() {
        val tableau = emptyTableau().toMutableList()
        // Col 0 has sole card 10♥ (red) — needs a black Jack to free Col 0 for Stock's K♦
        tableau[0] = listOf(card(Suit.HEARTS, Rank.TEN))
        // Col 1 has black J♠ covered by black 10♣ (face-down underneath so J♠ can't move)
        tableau[1] = listOf(
            card(Suit.HEARTS, Rank.TWO, faceUp = false),
            card(Suit.SPADES, Rank.JACK),
            card(Suit.CLUBS, Rank.TEN)
        )
        // Col 2 has red J♦ (10♣ can move onto J♦, but 10♥ cannot!)
        tableau[2] = listOf(
            card(Suit.SPADES, Rank.FOUR, faceUp = false),
            card(Suit.DIAMONDS, Rank.JACK)
        )
        // Fill remaining columns with blocked face-up Kings so no empty column exists yet
        tableau[3] = listOf(card(Suit.SPADES, Rank.KING))
        tableau[4] = listOf(card(Suit.HEARTS, Rank.KING))
        tableau[5] = listOf(card(Suit.CLUBS, Rank.KING))
        tableau[6] = listOf(card(Suit.SPADES, Rank.FOUR, faceUp = false), card(Suit.DIAMONDS, Rank.SIX))

        val state = GameState(
            stock = listOf(card(Suit.DIAMONDS, Rank.KING, faceUp = false)),
            waste = emptyList(),
            foundations = emptyFoundations(),
            tableau = tableau,
            drawMode = DrawMode.DRAW_ONE
        )

        // 10♣ (Col 1, idx 2) -> J♦ (Col 2), then 10♥ (Col 0) -> J♠ (Col 1), freeing Col 0 for Stock K♦!
        val hint = KlondikeEngine.findHint(state)
        assertNotNull(hint)
        assertEquals(PileLocation.Tableau(1), hint.from)
        assertEquals(2, hint.cardIndex)
        assertEquals(PileLocation.Tableau(2), hint.to)
    }

    @Test
    fun multiStepTableauReorganization_emptyingColumnForKing_isDetected() {
        val tableau = emptyTableau().toMutableList()
        // Col 0 has K♦ hiding a face-down card
        tableau[0] = listOf(
            card(Suit.CLUBS, Rank.THREE, faceUp = false),
            card(Suit.DIAMONDS, Rank.KING)
        )
        // Col 1 has black J♠ covered by black 10♣ (face-down underneath)
        tableau[1] = listOf(
            card(Suit.HEARTS, Rank.TWO, faceUp = false),
            card(Suit.SPADES, Rank.JACK),
            card(Suit.CLUBS, Rank.TEN)
        )
        // Col 2 has sole card red 10♥ (must wait for 10♣ -> J♦ before moving onto J♠ to free Col 2 for K♦)
        tableau[2] = listOf(
            card(Suit.HEARTS, Rank.TEN)
        )
        // Col 3 has red J♦ covering a face-down card
        tableau[3] = listOf(
            card(Suit.SPADES, Rank.FOUR, faceUp = false),
            card(Suit.DIAMONDS, Rank.JACK)
        )
        // Fill remaining columns with blocked face-up Kings so no empty column exists yet
        tableau[4] = listOf(card(Suit.SPADES, Rank.KING))
        tableau[5] = listOf(card(Suit.HEARTS, Rank.KING))
        tableau[6] = listOf(card(Suit.CLUBS, Rank.KING))

        val state = GameState(
            stock = emptyList(),
            waste = emptyList(),
            foundations = emptyFoundations(),
            tableau = tableau
        )

        // BFS finds: 10♣ (Col 1, idx 2) -> J♦ (Col 3), then 10♥ (Col 2) -> J♠ (Col 1), then K♦ -> Col 2!
        val hint = KlondikeEngine.findHint(state)
        assertNotNull(hint)
        assertEquals(PileLocation.Tableau(1), hint.from)
        assertEquals(2, hint.cardIndex)
        assertEquals(PileLocation.Tableau(3), hint.to)
    }

    @Test
    fun pointlessOscillationsOnly_correctlyDetectedAsNoMoves() {
        val tableau = emptyTableau().toMutableList()
        tableau[0] = listOf(
            card(Suit.CLUBS, Rank.THREE, faceUp = false),
            card(Suit.HEARTS, Rank.EIGHT),
            card(Suit.SPADES, Rank.SEVEN)
        )
        tableau[1] = listOf(
            card(Suit.SPADES, Rank.FOUR, faceUp = false),
            card(Suit.DIAMONDS, Rank.EIGHT)
        )
        tableau[2] = listOf(
            card(Suit.SPADES, Rank.KING)
        )
        // Only foundation has A♠ and 2♠; pulling 2♠ onto 3♦ (not present) or 7♠ <-> 8♦ achieves nothing
        val foundations = emptyFoundations().toMutableList()
        foundations[0] = listOf(
            card(Suit.SPADES, Rank.ACE),
            card(Suit.SPADES, Rank.TWO)
        )

        val state = GameState(
            stock = listOf(card(Suit.HEARTS, Rank.FIVE, faceUp = false)),
            waste = listOf(card(Suit.CLUBS, Rank.NINE)),
            foundations = foundations,
            tableau = tableau,
            drawMode = DrawMode.DRAW_ONE,
            deckPasses = DeckPasses.UNLIMITED
        )

        assertNull(KlondikeEngine.findHint(state))
        assertFalse(KlondikeEngine.hasAvailableMoves(state))
    }
}
