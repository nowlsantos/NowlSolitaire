package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation

internal fun buildDealFlights(
    state: GameState,
    stockOrigin: Offset?,
    defaultStackStepPx: Float,
    dealBatchId: Int
): List<CardFlight> {
    val maxRows = state.tableau.maxOfOrNull { it.size } ?: 0
    if (maxRows == 0) return emptyList()

    val startOrigin = stockOrigin ?: Offset.Unspecified
    val flights = mutableListOf<CardFlight>()
    var dealIndex = 0
    for (row in 0 until maxRows) {
        for (col in 0 until GameState.TABLEAU_COUNT) {
            val column = state.tableau[col]
            if (row >= column.size) continue
            val card = column[row]
            flights.add(
                CardFlight(
                    id = "${card.id}_deal_$dealBatchId",
                    cards = listOf(card),
                    startOrigin = startOrigin,
                    targetOrigin = Offset.Unspecified,
                    stackStepPx = defaultStackStepPx,
                    durationMs = 480f,
                    fromDragDrop = false,
                    delayMs = dealIndex * 46f,
                    flipFromBack = card.isFaceUp,
                    startFromStock = true,
                    isDeal = true
                )
            )
            dealIndex++
        }
    }
    return flights
}

internal fun buildFlightsForMove(
    oldState: GameState,
    newState: GameState,
    cardOrigins: Map<String, Offset>,
    stockOrigin: Offset?,
    dropTargetBounds: Map<PileLocation, Rect>,
    defaultStackStepPx: Float,
    lastDragDrop: DragDropSnapshot?,
    isAutoCompleting: Boolean = false
): List<CardFlight> {
    val oldLocations = HashMap<String, CardSlotInfo>(52)
    for (card in oldState.waste) {
        oldLocations[card.id] = CardSlotInfo(PileLocation.Waste, card)
    }
    oldState.foundations.forEachIndexed { index, pile ->
        val loc = PileLocation.Foundation(index)
        for (card in pile) {
            oldLocations[card.id] = CardSlotInfo(loc, card)
        }
    }
    oldState.tableau.forEachIndexed { colIndex, column ->
        val loc = PileLocation.Tableau(colIndex)
        for (card in column) {
            oldLocations[card.id] = CardSlotInfo(loc, card)
        }
    }

    val flights = mutableListOf<CardFlight>()

    if (newState.stock.size < oldState.stock.size &&
        newState.waste.size > oldState.waste.size &&
        stockOrigin != null
    ) {
        val drawnCount = newState.waste.size - oldState.waste.size
        val animateCount = if (drawnCount <= 3) {
            drawnCount
        } else {
            newState.wasteVisibleCount.coerceIn(1, minOf(3, newState.waste.size))
        }
        val drawnCards = newState.waste.takeLast(animateCount)
        val wasteFallback = dropTargetBounds[PileLocation.Waste]?.topLeft ?: stockOrigin
        drawnCards.forEachIndexed { index, card ->
            flights.add(
                CardFlight(
                    id = "${card.id}_${newState.moveCount}",
                    cards = listOf(card),
                    startOrigin = stockOrigin,
                    targetOrigin = wasteFallback,
                    stackStepPx = defaultStackStepPx,
                    durationMs = 240f,
                    fromDragDrop = false,
                    delayMs = index * 40f,
                    flipFromBack = true,
                    startFromStock = true
                )
            )
        }
    } else if (newState.stock.size > oldState.stock.size &&
        newState.waste.size < oldState.waste.size &&
        stockOrigin != null
    ) {
        val returnedCount = oldState.waste.size - newState.waste.size
        val animateCount = if (returnedCount <= 3) {
            returnedCount
        } else {
            oldState.wasteVisibleCount.coerceIn(1, minOf(3, oldState.waste.size))
        }
        val returnedCards = oldState.waste.takeLast(animateCount).asReversed()
        val wasteFallback = dropTargetBounds[PileLocation.Waste]?.topLeft ?: stockOrigin
        returnedCards.forEachIndexed { index, card ->
            val startOrigin = cardOrigins[card.id] ?: wasteFallback
            flights.add(
                CardFlight(
                    id = "${card.id}_to_stock_${newState.moveCount}",
                    cards = listOf(card),
                    startOrigin = startOrigin,
                    targetOrigin = stockOrigin,
                    stackStepPx = defaultStackStepPx,
                    durationMs = 240f,
                    fromDragDrop = false,
                    delayMs = index * 40f,
                    flipToBack = true,
                    endAtStock = true
                )
            )
        }
    }

    fun createFlight(
        cards: List<Card>,
        targetLocation: PileLocation
    ) {
        val headCard = cards.firstOrNull() ?: return
        val fromDragDrop = lastDragDrop?.headCardId == headCard.id
        val startOrigin = if (fromDragDrop) {
            lastDragDrop.releaseOrigin
        } else {
            cardOrigins[headCard.id] ?: return
        }
        val targetFallback = dropTargetBounds[targetLocation]?.topLeft ?: startOrigin
        val stepPx = if (fromDragDrop) lastDragDrop.stackStepPx else defaultStackStepPx
        val flightDuration = when {
            isAutoCompleting -> 220f
            fromDragDrop -> 130f
            else -> 240f
        }
        flights.add(
            CardFlight(
                id = "${headCard.id}_${newState.moveCount}",
                cards = cards,
                startOrigin = startOrigin,
                targetOrigin = targetFallback,
                stackStepPx = stepPx,
                durationMs = flightDuration,
                fromDragDrop = fromDragDrop
            )
        )
    }

    newState.foundations.forEachIndexed { index, pile ->
        val targetLoc = PileLocation.Foundation(index)
        for (card in pile) {
            val oldInfo = oldLocations[card.id] ?: continue
            if (oldInfo.location != targetLoc) {
                createFlight(listOf(card), targetLoc)
            }
        }
    }

    newState.tableau.forEachIndexed { colIndex, column ->
        val targetLoc = PileLocation.Tableau(colIndex)
        var currentSource: PileLocation? = null
        val group = mutableListOf<Card>()

        fun flushGroup() {
            if (group.isNotEmpty()) {
                createFlight(group.toList(), targetLoc)
                group.clear()
            }
            currentSource = null
        }

        for (card in column) {
            val oldInfo = oldLocations[card.id]
            if (oldInfo == null || oldInfo.location == targetLoc) {
                flushGroup()
            } else {
                if (currentSource != null && currentSource != oldInfo.location) {
                    flushGroup()
                }
                currentSource = oldInfo.location
                group.add(card)
            }
        }
        flushGroup()
    }

    val topWaste = newState.waste.lastOrNull()
    if (topWaste != null) {
        val oldInfo = oldLocations[topWaste.id]
        if (oldInfo != null && oldInfo.location != PileLocation.Waste) {
            createFlight(listOf(topWaste), PileLocation.Waste)
        }
    }

    return flights
}
