package com.nowlsantos.nowlsolitaire.ui.components.pyramid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.ui.components.CARD_ASPECT_RATIO
import com.nowlsantos.nowlsolitaire.ui.state.PyramidIntent
import com.nowlsantos.nowlsolitaire.ui.theme.toColors
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PyramidBoard(
    boardState: PyramidBoardState,
    onIntent: (PyramidIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBoardSettledChanged: (Boolean) -> Unit = {},
    onWinFlightsSettled: () -> Unit = {},
) {
    val colors = boardState.tableTheme.toColors()
    val gameState = boardState.gameState

    // Non-snapshot collections avoid layout/recomposition churn during positioning & flights.
    val boardOriginHolder = remember { PyramidOffsetHolder() }
    val slotPositions = remember { HashMap<PyramidLocation, Offset>(32) }
    val cardOrigins = remember { HashMap<String, Offset>(64) }
    val stockSlotOriginHolder = remember { PyramidOffsetHolder(Offset.Unspecified) }
    val wasteSlotOriginHolder = remember { PyramidOffsetHolder(Offset.Unspecified) }
    val discardSlotOriginHolder = remember { PyramidOffsetHolder(Offset.Unspecified) }
    val lastDealtIdHolder = remember { PyramidLongHolder(-1L) }

    var isStockSlotReady by remember { mutableStateOf(false) }
    var arePyramidSlotsReady by remember { mutableStateOf(false) }

    val activeFlightsState = remember { mutableStateOf<List<PyramidFlight>>(emptyList()) }
    val isDealingState = remember { mutableStateOf(false) }
    val hideAllForPendingDealState = remember { mutableStateOf(false) }
    val cardFlyingHiddenMap = remember { HashMap<String, MutableState<Boolean>>(64) }

    val latestBoardState by rememberUpdatedState(boardState)
    val latestOnIntent by rememberUpdatedState(onIntent)
    val latestOnWinFlightsSettled by rememberUpdatedState(onWinFlightsSettled)
    val latestOnBoardSettledChanged by rememberUpdatedState(onBoardSettledChanged)

    LaunchedEffect(Unit) {
        snapshotFlow { activeFlightsState.value.isEmpty() && !isDealingState.value }
            .distinctUntilChanged()
            .collect { settled ->
                latestOnBoardSettledChanged(settled)
            }
    }

    val getFlyingHiddenState = remember {
        { cardId: String ->
            cardFlyingHiddenMap.getOrPut(cardId) { mutableStateOf(false) }
        }
    }

    fun syncFlyingHiddenStates(flights: List<PyramidFlight>) {
        if (flights.isEmpty()) {
            for (state in cardFlyingHiddenMap.values) {
                if (state.value) state.value = false
            }
            return
        }
        val activeIds = HashSet<String>(flights.size * 2)
        for (i in flights.indices) {
            val flight = flights[i]
            if (flight.progressState.floatValue < 1f) {
                activeIds.add(flight.card.id)
            }
        }
        for ((id, state) in cardFlyingHiddenMap) {
            val shouldHide = id in activeIds
            if (state.value != shouldHide) {
                state.value = shouldHide
            }
        }
        for (id in activeIds) {
            val state = cardFlyingHiddenMap.getOrPut(id) { mutableStateOf(true) }
            if (!state.value) {
                state.value = true
            }
        }
    }

    val activeDrawCard = boardState.activeDrawCard
    val activeMatch = boardState.activeMatch

    // Synchronous flight initialization for drawn card to eliminate 1-frame flicker
    val prevActiveDrawKey = remember { mutableStateOf<Pair<String, Int>?>(null) }
    val currentDrawKey = activeDrawCard?.let { it.id to gameState.moveCount }
    if (activeDrawCard != null && currentDrawKey != null && prevActiveDrawKey.value != currentDrawKey) {
        prevActiveDrawKey.value = currentDrawKey
        val stockOrigin = stockSlotOriginHolder.value
        val wasteOrigin = wasteSlotOriginHolder.value
        val start = if (stockOrigin.isSpecified) stockOrigin else wasteOrigin
        val flight = PyramidFlight(
            id = "draw_${activeDrawCard.id}_${gameState.moveCount}",
            card = activeDrawCard,
            startOrigin = start,
            targetOrigin = wasteOrigin,
            durationMs = 280f,
            flipFromBack = false,
            startFromStock = true
        )
        Snapshot.withoutReadObservation {
            val merged = activeFlightsState.value.filterNot { existing ->
                existing.progressState.floatValue >= 1f ||
                    (existing.startFromStock && !existing.isDeal) ||
                    existing.card.id == activeDrawCard.id
            } + flight
            activeFlightsState.value = merged
            syncFlyingHiddenStates(merged)
        }
    }
    if (activeDrawCard == null && prevActiveDrawKey.value != null) {
        prevActiveDrawKey.value = null
    }

    // Synchronous flight initialization for matches
    val prevActiveMatch = remember { mutableStateOf<PyramidMatch?>(null) }
    if (activeMatch != null && prevActiveMatch.value != activeMatch) {
        prevActiveMatch.value = activeMatch
        val target = discardSlotOriginHolder.value
        val newFlights = when (activeMatch) {
            is PyramidMatch.KingMatch -> {
                val start = cardOrigins[activeMatch.card.id] ?: slotPositions[activeMatch.location] ?: target
                listOf(
                    PyramidFlight(
                        id = "king_${activeMatch.card.id}_${gameState.moveCount}",
                        card = activeMatch.card,
                        startOrigin = start,
                        targetOrigin = target,
                        durationMs = 280f,
                        flipFromBack = false
                    )
                )
            }
            is PyramidMatch.PairMatch -> {
                val start1 = cardOrigins[activeMatch.firstCard.id] ?: slotPositions[activeMatch.firstLocation] ?: target
                val start2 = cardOrigins[activeMatch.secondCard.id] ?: slotPositions[activeMatch.secondLocation] ?: target
                listOf(
                    PyramidFlight(
                        id = "pair1_${activeMatch.firstCard.id}_${gameState.moveCount}",
                        card = activeMatch.firstCard,
                        startOrigin = start1,
                        targetOrigin = target,
                        durationMs = 280f,
                        flipFromBack = false
                    ),
                    PyramidFlight(
                        id = "pair2_${activeMatch.secondCard.id}_${gameState.moveCount}",
                        card = activeMatch.secondCard,
                        startOrigin = start2,
                        targetOrigin = target,
                        durationMs = 280f,
                        flipFromBack = false
                    )
                )
            }
        }
        Snapshot.withoutReadObservation {
            val newIds = newFlights.mapTo(HashSet()) { it.card.id }
            val merged = activeFlightsState.value.filterNot { existing ->
                existing.progressState.floatValue >= 1f || existing.card.id in newIds
            } + newFlights
            activeFlightsState.value = merged
            syncFlyingHiddenStates(merged)
        }
    }
    if (activeMatch == null && prevActiveMatch.value != null) {
        prevActiveMatch.value = null
    }

    val prevGameStateHolder = remember { PyramidGameStateHolder(null) }
    val currentDealId = boardState.dealId

    // Track deal state for initial deal and replays
    val isDealState = gameState.moveCount == 0 && gameState.waste.isEmpty() && gameState.discardPile.isEmpty()
    val needsDealAnimation = currentDealId > 0L && currentDealId != lastDealtIdHolder.value && isDealState
    Snapshot.withoutReadObservation {
        if (needsDealAnimation && !hideAllForPendingDealState.value) {
            hideAllForPendingDealState.value = true
        } else if (!needsDealAnimation && hideAllForPendingDealState.value) {
            hideAllForPendingDealState.value = false
        }
    }

    val previousGameState = prevGameStateHolder.value
    if (previousGameState !== gameState) {
        prevGameStateHolder.value = gameState
        if (slotPositions.isNotEmpty()) {
            for (i in gameState.pyramid.indices) {
                val pc = gameState.pyramid[i]
                val slotPos = slotPositions[PyramidLocation.PyramidSlot(pc.row, pc.col)]
                if (slotPos != null) {
                    cardOrigins[pc.card.id] = slotPos
                }
            }
        }
        val isRecycleMove = previousGameState != null &&
            previousGameState.seed == gameState.seed &&
            !needsDealAnimation &&
            activeDrawCard == null &&
            activeMatch == null &&
            gameState.moveCount > previousGameState.moveCount &&
            previousGameState.stock.isEmpty() &&
            gameState.stock.isNotEmpty()
        if (isRecycleMove) {
            Snapshot.withoutReadObservation {
                val remaining = activeFlightsState.value.filterNot { existing ->
                    existing.progressState.floatValue >= 1f || (existing.startFromStock && !existing.isDeal)
                }
                if (remaining.size != activeFlightsState.value.size) {
                    activeFlightsState.value = remaining
                    syncFlyingHiddenStates(remaining)
                }
            }
        } else if (previousGameState != null &&
            previousGameState.seed == gameState.seed &&
            !needsDealAnimation &&
            activeDrawCard == null &&
            activeMatch == null &&
            gameState.moveCount < previousGameState.moveCount
        ) {
            val undoFlights = buildPyramidUndoFlights(
                oldState = previousGameState,
                newState = gameState,
                slotPositions = slotPositions,
                cardOrigins = cardOrigins,
                stockSlotPosition = stockSlotOriginHolder.value,
                wasteSlotPosition = wasteSlotOriginHolder.value,
                discardSlotPosition = discardSlotOriginHolder.value
            )
            if (undoFlights.isNotEmpty()) {
                Snapshot.withoutReadObservation {
                    val newIds = undoFlights.mapTo(HashSet()) { it.card.id }
                    val merged = activeFlightsState.value.filterNot {
                        it.progressState.floatValue >= 1f || it.card.id in newIds
                    } + undoFlights
                    activeFlightsState.value = merged
                    syncFlyingHiddenStates(merged)
                }
            }
        }
    }

    LaunchedEffect(needsDealAnimation, currentDealId, isStockSlotReady, arePyramidSlotsReady) {
        val stockOrigin = stockSlotOriginHolder.value
        if (needsDealAnimation && stockOrigin.isSpecified && slotPositions.size >= 28) {
            // Skip one frame so initial layout & screen transition settle cleanly.
            withFrameNanos {}
            lastDealtIdHolder.value = currentDealId
            val currentGameState = latestBoardState.gameState
            val latestStockOrigin = stockSlotOriginHolder.value

            val dealFlights = mutableListOf<PyramidFlight>()
            var dealIndex = 0
            for (row in 0 until PyramidGameState.PYRAMID_ROW_COUNT) {
                for (col in 0..row) {
                    val slot = PyramidLocation.PyramidSlot(row, col)
                    val expectedIdx = row * (row + 1) / 2 + col
                    val card = currentGameState.pyramid.getOrNull(expectedIdx)
                        ?.takeIf { it.row == row && it.col == col }?.card
                        ?: currentGameState.pyramid.firstOrNull { it.row == row && it.col == col }?.card
                        ?: continue
                    val target = slotPositions[slot] ?: continue

                    dealFlights.add(
                        PyramidFlight(
                            id = "deal_${card.id}_${currentGameState.seed}_$dealIndex",
                            card = card,
                            startOrigin = latestStockOrigin,
                            targetOrigin = target,
                            durationMs = 460f,
                            delayMs = dealIndex * 38f,
                            flipFromBack = true,
                            isDeal = true,
                            startFromStock = true
                        )
                    )
                    dealIndex++
                }
            }
            isDealingState.value = dealFlights.isNotEmpty()
            activeFlightsState.value = dealFlights
            syncFlyingHiddenStates(dealFlights)
            hideAllForPendingDealState.value = false
        }
    }

    val onSlotPositioned = remember {
        { slotLocation: PyramidLocation.PyramidSlot, pos: Offset ->
            if (slotPositions[slotLocation] != pos) {
                slotPositions[slotLocation] = pos
            }
            val expectedIdx = slotLocation.row * (slotLocation.row + 1) / 2 + slotLocation.col
            val pyramidCard = latestBoardState.gameState.pyramid.getOrNull(expectedIdx)
                ?.takeIf { it.row == slotLocation.row && it.col == slotLocation.col }
                ?: latestBoardState.gameState.pyramid.firstOrNull {
                    it.row == slotLocation.row && it.col == slotLocation.col
                }
            if (pyramidCard != null) {
                cardOrigins[pyramidCard.card.id] = pos
            }
            if (!arePyramidSlotsReady && slotPositions.size >= 28) {
                arePyramidSlotsReady = true
            }
        }
    }

    val onStockPositioned = remember {
        { pos: Offset ->
            stockSlotOriginHolder.value = pos
            if (slotPositions[PyramidLocation.Stock] != pos) {
                slotPositions[PyramidLocation.Stock] = pos
            }
            val topStock = latestBoardState.gameState.topStockCard
            if (topStock != null) {
                cardOrigins[topStock.id] = pos
            }
            if (pos.isSpecified) {
                if (!isStockSlotReady) {
                    isStockSlotReady = true
                }
                val flights = activeFlightsState.value
                for (i in flights.indices) {
                    val flight = flights[i]
                    if (flight.startFromStock) {
                        flight.startOrigin = pos
                    }
                    if (flight.endAtStock) {
                        flight.targetOrigin = pos
                    }
                }
            }
        }
    }

    val onWastePositioned = remember {
        { pos: Offset ->
            wasteSlotOriginHolder.value = pos
            if (slotPositions[PyramidLocation.Waste] != pos) {
                slotPositions[PyramidLocation.Waste] = pos
            }
            val topWaste = latestBoardState.gameState.waste.lastOrNull()
            if (topWaste != null) {
                cardOrigins[topWaste.id] = pos
            }
        }
    }

    val onDiscardPositioned = remember {
        { pos: Offset ->
            discardSlotOriginHolder.value = pos
        }
    }

    val onTapCard = remember {
        { slotLocation: PyramidLocation ->
            if (!isDealingState.value) {
                latestOnIntent(PyramidIntent.TapCard(slotLocation))
            }
        }
    }

    val onTapStock = remember {
        {
            if (!isDealingState.value) {
                latestOnIntent(PyramidIntent.TapCard(PyramidLocation.Stock))
            }
        }
    }

    val onDrawOrRecycle = remember {
        {
            if (!isDealingState.value) {
                latestOnIntent(PyramidIntent.DrawOrRecycle)
            }
        }
    }

    val onTapWaste = remember {
        {
            if (!isDealingState.value) {
                latestOnIntent(PyramidIntent.TapCard(PyramidLocation.Waste))
            }
        }
    }

    val onFlyingCardsUpdated = remember {
        { currentFlights: List<PyramidFlight> ->
            syncFlyingHiddenStates(currentFlights)
            if (isDealingState.value) {
                var anyDealing = false
                for (i in currentFlights.indices) {
                    val f = currentFlights[i]
                    if (f.isDeal && f.progressState.floatValue < 1f) {
                        anyDealing = true
                        break
                    }
                }
                if (!anyDealing) {
                    isDealingState.value = false
                }
            }
        }
    }

    val onAllFlightsFinished = remember {
        {
            activeFlightsState.value = emptyList()
            isDealingState.value = false
            syncFlyingHiddenStates(emptyList())
            val latest = latestBoardState
            if (latest.activeDrawCard != null) {
                latestOnIntent(PyramidIntent.ClearActiveDrawCard)
            }
            if (latest.activeMatch != null) {
                latestOnIntent(PyramidIntent.ClearActiveMatch)
            }
            if (latest.gameState.isWon) {
                latestOnWinFlightsSettled()
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            latestBoardState.gameState.isWon && activeFlightsState.value.isEmpty()
        }
            .distinctUntilChanged()
            .collect { winSettled ->
                if (winSettled) {
                    latestOnWinFlightsSettled()
                }
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    boardOriginHolder.value = coords.positionInRoot()
                }
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        val hPadding = if (isLandscape) 12.dp else 4.dp
        val vPadding = if (isLandscape) 4.dp else 6.dp
        val availableW = (maxWidth - (hPadding * 2)).coerceAtLeast(0.dp)
        val availableH = (maxHeight - (vPadding * 2)).coerceAtLeast(0.dp)

        // Dynamic card sizing:
        // Tighter horizontal and vertical spacing in the pyramid tableau lets cards scale larger dynamically.
        val horizontalGap = (availableW * 0.004f).coerceIn(1.5.dp, 4.dp)
        val totalHorizontalGaps = horizontalGap * 6

        // Width-derived limit: 7 cards in bottom tier
        val maxCardWidthFromWidth = (availableW - totalHorizontalGaps) / 7f

        // Height-derived limit:
        // Total height = pyramidTotalHeight + tableauPilesGap + playAreaHeight
        // pyramidTotalHeight = (6 * verticalOverlap) + cardHeight
        // Portrait: verticalOverlap = 0.44f * cardHeight -> pyramid = 3.64f * cardHeight, playArea = 1.0f * cardHeight + ~30.dp, gap = 6.dp
        // Landscape: verticalOverlap = 0.37f * cardHeight -> pyramid = 3.22f * cardHeight, playArea = 1.0f * cardHeight + ~24.dp, gap = 10.dp
        val verticalOverlapRatio = if (isLandscape) 0.37f else 0.44f
        val tableauPilesGap = if (isLandscape) 10.dp else 6.dp
        val maxCardHeightFromHeight = if (isLandscape) {
            (availableH - 34.dp).coerceAtLeast(0.dp) / 4.22f
        } else {
            (availableH - 36.dp).coerceAtLeast(0.dp) / 4.64f
        }
        val maxCardWidthFromHeight = maxCardHeightFromHeight / CARD_ASPECT_RATIO

        val calculatedWidth = minOf(maxCardWidthFromWidth, maxCardWidthFromHeight)
        val cardWidth = calculatedWidth.coerceAtLeast(28.dp)
        val cardHeight = cardWidth * CARD_ASPECT_RATIO
        val verticalOverlap = cardHeight * verticalOverlapRatio
        val pyramidTotalHeight = (verticalOverlap * 6) + cardHeight
        val boardContentWidth = (cardWidth * 7) + totalHorizontalGaps

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPadding, vertical = vPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.SpaceEvenly
        ) {
            // 1. The 7-tier Pyramid
            PyramidGrid(
                gameState = gameState,
                activeHint = boardState.activeHint,
                cardBackTheme = boardState.cardBackTheme,
                colors = colors,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                horizontalGap = horizontalGap,
                verticalOverlap = verticalOverlap,
                pyramidTotalHeight = pyramidTotalHeight,
                isDealingState = isDealingState,
                hideAllForPendingDealState = hideAllForPendingDealState,
                getFlyingHiddenState = getFlyingHiddenState,
                onSlotPositioned = onSlotPositioned,
                onTapCard = onTapCard
            )

            Spacer(modifier = Modifier.height(tableauPilesGap))

            // 2. Play Area: Discard/Foundation and Waste + Draw Button + Stock
            val wastePileSlot: @Composable () -> Unit = {
                WastePileView(
                    gameState = gameState,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    colors = colors,
                    cardBackTheme = boardState.cardBackTheme,
                    activeHint = boardState.activeHint,
                    isLandscape = isLandscape,
                    isDealingState = isDealingState,
                    getFlyingHiddenState = getFlyingHiddenState,
                    onTapWaste = onTapWaste,
                    onPositioned = onWastePositioned
                )
            }

            val stockPileSlot: @Composable () -> Unit = {
                StockPileView(
                    gameState = gameState,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    colors = colors,
                    cardBackTheme = boardState.cardBackTheme,
                    activeHint = boardState.activeHint,
                    isLandscape = isLandscape,
                    isDealingState = isDealingState,
                    getFlyingHiddenState = getFlyingHiddenState,
                    onTapStock = onTapStock,
                    onDrawOrRecycle = onDrawOrRecycle,
                    onPositioned = onStockPositioned
                )
            }

            val stockAndWasteRow: @Composable () -> Unit = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (boardState.leftHanded) {
                        stockPileSlot()
                        StockDrawButton(
                            hasStockCards = gameState.stock.isNotEmpty(),
                            canRecycleStock = gameState.canRecycleStock,
                            cardHeight = cardHeight,
                            colors = colors,
                            isLandscape = isLandscape,
                            pointsLeft = false,
                            onDrawOrRecycle = onDrawOrRecycle
                        )
                        wastePileSlot()
                    } else {
                        wastePileSlot()
                        StockDrawButton(
                            hasStockCards = gameState.stock.isNotEmpty(),
                            canRecycleStock = gameState.canRecycleStock,
                            cardHeight = cardHeight,
                            colors = colors,
                            isLandscape = isLandscape,
                            pointsLeft = true,
                            onDrawOrRecycle = onDrawOrRecycle
                        )
                        stockPileSlot()
                    }
                }
            }

            val discardPileSlot: @Composable () -> Unit = {
                DiscardPileView(
                    gameState = gameState,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    colors = colors,
                    cardBackTheme = boardState.cardBackTheme,
                    isLandscape = isLandscape,
                    getFlyingHiddenState = getFlyingHiddenState,
                    onPositioned = onDiscardPositioned
                )
            }

            Row(
                modifier = Modifier
                    .width(boardContentWidth)
                    .padding(
                        horizontal = if (isLandscape) 12.dp else 8.dp,
                        vertical = if (isLandscape) 2.dp else 4.dp
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (boardState.leftHanded) {
                    stockAndWasteRow()
                    discardPileSlot()
                } else {
                    discardPileSlot()
                    stockAndWasteRow()
                }
            }
        }

        // 3. Isolated Flight Overlay (Deal + Draw Flip + Match + Undo)
        PyramidFlightOverlay(
            activeFlightsState = activeFlightsState,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            cardBackTheme = boardState.cardBackTheme,
            boardOriginHolder = boardOriginHolder,
            onFlyingCardsUpdated = onFlyingCardsUpdated,
            onAllFlightsFinished = onAllFlightsFinished
        )
    }
}
