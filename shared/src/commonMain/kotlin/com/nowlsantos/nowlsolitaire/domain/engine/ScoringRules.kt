package com.nowlsantos.nowlsolitaire.domain.engine

import com.nowlsantos.nowlsolitaire.domain.model.DrawMode
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation

object ScoringRules {

    private const val WASTE_TO_TABLEAU = 5
    private const val WASTE_TO_FOUNDATION = 10
    private const val TABLEAU_TO_FOUNDATION = 10
    private const val FLIP_TABLEAU_CARD = 5
    private const val FOUNDATION_TO_TABLEAU = -15
    private const val RECYCLE_PENALTY_DRAW_ONE = -100
    private const val RECYCLE_PENALTY_DRAW_THREE = -20

    fun scoreForMove(
        from: PileLocation,
        to: PileLocation,
        flippedTableauCard: Boolean
    ): Int {
        val moveDelta = when {
            from is PileLocation.Waste && to is PileLocation.Tableau -> WASTE_TO_TABLEAU
            from is PileLocation.Waste && to is PileLocation.Foundation -> WASTE_TO_FOUNDATION
            from is PileLocation.Tableau && to is PileLocation.Foundation -> TABLEAU_TO_FOUNDATION
            from is PileLocation.Foundation && to is PileLocation.Tableau -> FOUNDATION_TO_TABLEAU
            else -> 0
        }
        val flipBonus = if (flippedTableauCard) FLIP_TABLEAU_CARD else 0
        return moveDelta + flipBonus
    }

    fun scoreForRecycle(drawMode: DrawMode, completedPasses: Int): Int = when (drawMode) {
        DrawMode.DRAW_ONE -> if (completedPasses >= 1) RECYCLE_PENALTY_DRAW_ONE else 0
        DrawMode.DRAW_THREE -> if (completedPasses >= 3) RECYCLE_PENALTY_DRAW_THREE else 0
    }

    fun applyScoreDelta(currentScore: Int, delta: Int): Int =
        (currentScore + delta).coerceAtLeast(0)
}
