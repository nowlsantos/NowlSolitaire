package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation

object PyramidScoring {

    const val SCORE_PYRAMID_KING = 10
    const val SCORE_WASTE_KING = 5
    const val SCORE_PYRAMID_PAIR = 20
    const val SCORE_PYRAMID_WASTE_PAIR = 15
    const val SCORE_STOCK_WASTE_PAIR = 10
    const val VICTORY_BONUS = 1000
    const val RECYCLE_PENALTY = -20

    private val ROW_CLEAR_BONUS = mapOf(
        6 to 25,
        5 to 50,
        4 to 75,
        3 to 100,
        2 to 150,
        1 to 250,
        0 to 500
    )

    fun scoreForKing(location: PyramidLocation): Int {
        return when (location) {
            is PyramidLocation.PyramidSlot -> SCORE_PYRAMID_KING
            PyramidLocation.Waste, PyramidLocation.Stock -> SCORE_WASTE_KING
        }
    }

    fun scoreForPair(first: PyramidLocation, second: PyramidLocation): Int {
        val pyramidCount = (if (first is PyramidLocation.PyramidSlot) 1 else 0) +
            (if (second is PyramidLocation.PyramidSlot) 1 else 0)
        return when (pyramidCount) {
            2 -> SCORE_PYRAMID_PAIR
            1 -> SCORE_PYRAMID_WASTE_PAIR
            else -> SCORE_STOCK_WASTE_PAIR
        }
    }

    fun scoreForRowClear(row: Int): Int {
        return ROW_CLEAR_BONUS[row] ?: 0
    }

    fun applyScoreDelta(currentScore: Int, delta: Int): Int =
        (currentScore + delta).coerceAtLeast(0)
}
