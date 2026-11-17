package com.nowlsantos.nowlsolitaire.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface PyramidLocation {
    @Serializable
    data class PyramidSlot(val row: Int, val col: Int) : PyramidLocation

    @Serializable
    data object Waste : PyramidLocation

    @Serializable
    data object Stock : PyramidLocation
}

@Serializable
data class PyramidCard(
    val row: Int,
    val col: Int,
    val card: Card,
    val isRemoved: Boolean = false
) {
    val id: String
        get() = "pyr_${row}_${col}_${card.id}"
}

sealed interface PyramidMatch {
    val cards: List<Card>

    data class KingMatch(
        val location: PyramidLocation,
        val card: Card
    ) : PyramidMatch {
        override val cards: List<Card> = listOf(card)
    }

    data class PairMatch(
        val firstLocation: PyramidLocation,
        val firstCard: Card,
        val secondLocation: PyramidLocation,
        val secondCard: Card
    ) : PyramidMatch {
        override val cards: List<Card> = listOf(firstCard, secondCard)
    }
}

@Serializable
data class PyramidGameState(
    val pyramid: List<PyramidCard> = emptyList(),
    val stock: List<Card> = emptyList(),
    val waste: List<Card> = emptyList(),
    val discardPile: List<Card> = emptyList(),
    val selectedLocation: PyramidLocation? = null,
    val maxRecycles: Int = DEFAULT_MAX_RECYCLES,
    val recyclesUsed: Int = 0,
    val score: Int = 0,
    val moveCount: Int = 0,
    val seed: Long = 0L
) {
    val isWon: Boolean
        get() = pyramid.isNotEmpty() && pyramid.all { it.isRemoved }

    val remainingPyramidCount: Int
        get() = pyramid.count { !it.isRemoved }

    val topWasteCard: Card?
        get() = waste.lastOrNull()

    val topStockCard: Card?
        get() = stock.lastOrNull()

    val canRecycleStock: Boolean
        get() = stock.isEmpty() && waste.isNotEmpty() && (maxRecycles !in 0..recyclesUsed)

    val remainingRecycles: Int
        get() = if (maxRecycles < 0) Int.MAX_VALUE else (maxRecycles - recyclesUsed).coerceAtLeast(0)

    companion object {
        const val PYRAMID_ROW_COUNT = 7
        const val PYRAMID_CARD_COUNT = 28 // 1 + 2 + 3 + 4 + 5 + 6 + 7
        const val STOCK_CARD_COUNT = 24   // 52 - 28
        const val TARGET_SUM = 13
        const val DEFAULT_MAX_RECYCLES = 2 // 3 passes total through deck
    }
}

@Serializable
data class PyramidSettings(
    val tableTheme: TableTheme = TableTheme.CLASSIC_GREEN,
    val cardBackTheme: CardBackTheme = CardBackTheme.CLASSIC,
    val maxRecycles: Int = PyramidGameState.DEFAULT_MAX_RECYCLES,
    val dealMode: DealMode = DealMode.WINNABLE,
    val showTimer: Boolean = false,
    val tapToAutoMove: Boolean = true
)

@Serializable
data class SavedPyramidGameSnapshot(
    val gameState: PyramidGameState,
    val undoStack: List<PyramidGameState> = emptyList(),
    val elapsedSeconds: Int = 0,
    val hasStartedMoving: Boolean = false,
    val recordedAsPlayed: Boolean = false
)
