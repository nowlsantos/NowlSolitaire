package com.nowlsantos.nowlsolitaire.ui.components.pyramid

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme

data class PyramidBoardState(
    val gameState: PyramidGameState,
    val tableTheme: TableTheme,
    val cardBackTheme: CardBackTheme,
    val activeHint: PyramidMatch? = null,
    val activeMatch: PyramidMatch? = null,
    val activeDrawCard: Card? = null,
    val dealId: Long = 0L
)

internal class PyramidOffsetHolder(var value: Offset = Offset.Zero)
internal class PyramidLongHolder(var value: Long = -1L)
internal class PyramidGameStateHolder(var value: PyramidGameState? = null)

internal val DealPositionEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

internal data class PyramidFlight(
    val id: String,
    val card: Card,
    var startOrigin: Offset,
    var targetOrigin: Offset,
    val durationMs: Float,
    val flipFromBack: Boolean,
    val flipToBack: Boolean = false,
    val delayMs: Float = 0f,
    val isDeal: Boolean = false,
    val startFromStock: Boolean = false,
    val endAtStock: Boolean = false,
    val progressState: MutableFloatState = mutableFloatStateOf(0f),
    var startTimeNanos: Long = -1L
)

internal fun isLocationHinted(hint: PyramidMatch?, location: PyramidLocation): Boolean {
    if (hint == null) return false
    return when (hint) {
        is PyramidMatch.KingMatch -> hint.location == location
        is PyramidMatch.PairMatch -> hint.firstLocation == location || hint.secondLocation == location
    }
}

internal fun buildPyramidUndoFlights(
    oldState: PyramidGameState,
    newState: PyramidGameState,
    slotPositions: Map<PyramidLocation, Offset>,
    cardOrigins: Map<String, Offset>,
    stockSlotPosition: Offset,
    wasteSlotPosition: Offset,
    discardSlotPosition: Offset
): List<PyramidFlight> {
    if (newState.stock.size > oldState.stock.size && newState.waste.size < oldState.waste.size) {
        val returnedCount = oldState.waste.size - newState.waste.size
        if (returnedCount == 1 && wasteSlotPosition.isSpecified && stockSlotPosition.isSpecified) {
            val returnedCard = oldState.waste.last()
            return listOf(
                PyramidFlight(
                    id = "undo_draw_${returnedCard.id}_${newState.moveCount}",
                    card = returnedCard,
                    startOrigin = wasteSlotPosition,
                    targetOrigin = stockSlotPosition,
                    durationMs = 280f,
                    flipFromBack = false,
                    flipToBack = false,
                    endAtStock = true
                )
            )
        }
    } else if (oldState.stock.isNotEmpty() &&
        oldState.waste.isEmpty() &&
        newState.stock.isEmpty() &&
        newState.waste.isNotEmpty()
    ) {
        if (stockSlotPosition.isSpecified && wasteSlotPosition.isSpecified) {
            val topWaste = newState.waste.last()
            return listOf(
                PyramidFlight(
                    id = "undo_recycle_${topWaste.id}_${newState.moveCount}",
                    card = topWaste,
                    startOrigin = stockSlotPosition,
                    targetOrigin = wasteSlotPosition,
                    durationMs = 280f,
                    flipFromBack = false,
                    startFromStock = true
                )
            )
        }
    } else if (newState.discardPile.size < oldState.discardPile.size && discardSlotPosition.isSpecified) {
        val returnedCards = oldState.discardPile.drop(newState.discardPile.size)
        val flights = mutableListOf<PyramidFlight>()
        for (card in returnedCards) {
            val pyramidSlot = newState.pyramid
                .firstOrNull { !it.isRemoved && it.card.id == card.id }
                ?.let { PyramidLocation.PyramidSlot(it.row, it.col) }
            val isReturnedToStock = newState.stock.any { it.id == card.id }
            val targetPos = when {
                pyramidSlot != null -> slotPositions[pyramidSlot] ?: cardOrigins[card.id]
                isReturnedToStock -> stockSlotPosition
                newState.waste.any { it.id == card.id } -> wasteSlotPosition
                else -> null
            }
            if (targetPos != null && targetPos.isSpecified) {
                flights.add(
                    PyramidFlight(
                        id = "undo_match_${card.id}_${newState.moveCount}",
                        card = card,
                        startOrigin = discardSlotPosition,
                        targetOrigin = targetPos,
                        durationMs = 280f,
                        flipFromBack = false,
                        endAtStock = isReturnedToStock
                    )
                )
            }
        }
        return flights
    }
    return emptyList()
}
