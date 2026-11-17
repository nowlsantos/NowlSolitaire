package com.nowlsantos.nowlsolitaire.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DrawMode(val drawCount: Int) {
    DRAW_ONE(1),
    DRAW_THREE(3)
}

@Serializable
enum class DeckPasses(val displayName: String, val maxPasses: Int) {
    UNLIMITED("Unlimited", -1),
    ONE_PASS("2 Pass", 2),
    THREE_PASSES("3 Passes", 3);

    val maxRecycles: Int
        get() = if (maxPasses <= 0) -1 else maxPasses - 1
}

@Serializable
sealed interface PileLocation {
    @Serializable
    data object Stock : PileLocation

    @Serializable
    data object Waste : PileLocation

    @Serializable
    data class Foundation(val index: Int) : PileLocation

    @Serializable
    data class Tableau(val column: Int) : PileLocation
}

@Serializable
data class HintMove(
    val from: PileLocation,
    val cardIndex: Int,
    val to: PileLocation
)

@Serializable
data class GameState(
    val stock: List<Card> = emptyList(),
    val waste: List<Card> = emptyList(),
    val wasteVisibleCount: Int = 0,
    val foundations: List<List<Card>> = List(FOUNDATION_COUNT) { emptyList() },
    val tableau: List<List<Card>> = List(TABLEAU_COUNT) { emptyList() },
    val drawMode: DrawMode = DrawMode.DRAW_ONE,
    val deckPasses: DeckPasses = DeckPasses.UNLIMITED,
    val score: Int = 0,
    val moveCount: Int = 0,
    val recycleCount: Int = 0,
    val seed: Long = 0L
) {
    val isWon: Boolean
        get() = foundations.size == FOUNDATION_COUNT &&
            foundations.all { it.size == CARDS_PER_SUIT }

    val isAutoCompleteReady: Boolean
        get() = !isWon &&
            stock.isEmpty() &&
            waste.isEmpty() &&
            tableau.all { column -> column.all { it.isFaceUp } }

    val canRecycleStock: Boolean
        get() = stock.isEmpty() && waste.isNotEmpty() && (deckPasses.maxRecycles !in 0..recycleCount)

    val remainingPasses: Int
        get() {
            if (deckPasses.maxPasses <= 0) return Int.MAX_VALUE
            val futureRecycles = (deckPasses.maxRecycles - recycleCount).coerceAtLeast(0)
            return if (stock.isNotEmpty()) futureRecycles + 1 else futureRecycles
        }

    val remainingRecycles: Int
        get() = if (deckPasses.maxRecycles < 0) Int.MAX_VALUE else (deckPasses.maxRecycles - recycleCount).coerceAtLeast(0)

    companion object {
        const val FOUNDATION_COUNT = 4
        const val TABLEAU_COUNT = 7
        const val CARDS_PER_SUIT = 13
        const val DECK_SIZE = 52
    }
}
