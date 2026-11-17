package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.DeckPasses
import com.nowlsantos.nowlsolitaire.domain.model.DrawMode
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.Rank
import com.nowlsantos.nowlsolitaire.domain.model.Suit
import kotlin.random.Random

object DeckDealer {

    fun createStandardDeck(): List<Card> =
        Suit.entries.flatMap { suit ->
            Rank.entries.map { rank ->
                Card(suit = suit, rank = rank, isFaceUp = false)
            }
        }

    fun dealNewGame(
        drawMode: DrawMode = DrawMode.DRAW_ONE,
        deckPasses: DeckPasses = DeckPasses.UNLIMITED,
        seed: Long = Random.nextLong()
    ): GameState {
        val shuffledDeck = createStandardDeck().shuffled(Random(seed))
        val tableau = MutableList(GameState.TABLEAU_COUNT) { emptyList<Card>() }

        var cursor = 0
        for (column in 0 until GameState.TABLEAU_COUNT) {
            val columnSize = column + 1
            val cards = mutableListOf<Card>()
            for (row in 0 until columnSize) {
                val card = shuffledDeck[cursor++]
                cards.add(card.copy(isFaceUp = row == columnSize - 1))
            }
            tableau[column] = cards
        }

        val stock = shuffledDeck.subList(cursor, shuffledDeck.size).map { it.copy(isFaceUp = false) }

        return GameState(
            stock = stock,
            waste = emptyList(),
            wasteVisibleCount = 0,
            foundations = List(GameState.FOUNDATION_COUNT) { emptyList() },
            tableau = tableau,
            drawMode = drawMode,
            deckPasses = deckPasses,
            score = 0,
            moveCount = 0,
            recycleCount = 0,
            seed = seed
        )
    }
}
