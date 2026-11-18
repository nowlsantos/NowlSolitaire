package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.engine.KlondikeRules
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.ui.components.CARD_ASPECT_RATIO
import com.nowlsantos.nowlsolitaire.ui.components.LocalCardBackTheme
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.theme.toColors
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun KlondikeBoard(
    boardState: KlondikeBoardState,
    onIntent: (KlondikeIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBoardSettledChanged: (Boolean) -> Unit = {},
    onWinFlightsSettled: () -> Unit = {},
) {
    val colors = boardState.tableTheme.toColors()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val sharedTextMeasurer = rememberTextMeasurer()

    // Non-snapshot collections avoid layout/recomposition churn during positioning & drag.
    val dropTargetBounds = remember { mutableMapOf<PileLocation, Rect>() }
    val cardOrigins = remember { mutableMapOf<String, Offset>() }
    val stockOriginHolder = remember { NullableOffsetHolder() }
    val boardOriginHolder = remember { OffsetHolder() }
    val isReturningHolder = remember { BooleanHolder() }
    val returnJobHolder = remember { JobHolder() }
    val prevGameStateHolder = remember { GameStateHolder(null) }
    val lastDealtIdHolder = remember { LongHolder(-1L) }
    val dealBatchHolder = remember { IntHolder(0) }
    val lastDragDropHolder = remember { DragDropHolder() }
    val cardFlyingHiddenMap = remember { HashMap<String, MutableState<Boolean>>(64) }

    val activeDragState = remember { mutableStateOf<ActiveDragState?>(null) }
    val dragOffsetState = remember { mutableStateOf(Offset.Zero) }
    val returnProgressState = remember { mutableFloatStateOf(0f) }
    val activeFlightsState = remember { mutableStateOf<List<CardFlight>>(emptyList()) }
    val isReturningState = remember { mutableStateOf(false) }
    val dealMountLimitState = remember { mutableIntStateOf(Int.MAX_VALUE) }
    val dealBatchState = remember { mutableIntStateOf(0) }
    val hoveredTargetState = remember { mutableStateOf<PileLocation?>(null) }

    val latestBoardState by rememberUpdatedState(boardState)
    val latestOnIntent by rememberUpdatedState(onIntent)
    val latestOnWinFlightsSettled by rememberUpdatedState(onWinFlightsSettled)
    val latestOnBoardSettledChanged by rememberUpdatedState(onBoardSettledChanged)

    LaunchedEffect(Unit) {
        snapshotFlow {
            activeFlightsState.value.isEmpty() &&
                activeDragState.value == null &&
                !isReturningState.value &&
                !latestBoardState.isAutoCompleting
        }
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

    fun syncFlyingHiddenStates(flights: List<CardFlight>) {
        if (flights.isEmpty()) {
            for (state in cardFlyingHiddenMap.values) {
                if (state.value) state.value = false
            }
            return
        }
        val activeIds = HashSet<String>(flights.size * 2)
        for (flight in flights) {
            if (flight.progressState.floatValue < 1f) {
                for (card in flight.cards) {
                    activeIds.add(card.id)
                }
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

    fun updateHoveredTarget(drag: ActiveDragState, currentOffset: Offset) {
        val currentLeft = drag.originInRoot.x + currentOffset.x
        val currentTop = drag.originInRoot.y + currentOffset.y
        val draggedRect = Rect(
            left = currentLeft,
            top = currentTop,
            right = currentLeft + drag.cardWidthPx,
            bottom = currentTop + drag.cardHeightPx
        )

        var bestTarget: PileLocation? = null
        var bestArea = 0f
        for ((location, rect) in dropTargetBounds) {
            if (!KlondikeRules.isValidMove(latestBoardState.gameState, drag.from, drag.cardIndex, location)) {
                continue
            }
            val intersection = draggedRect.intersect(rect)
            if ((intersection.width > 0f) && (intersection.height > 0f)) {
                val area = intersection.width * intersection.height
                if (area > bestArea) {
                    bestArea = area
                    bestTarget = location
                }
            }
        }
        if (hoveredTargetState.value != bestTarget) {
            hoveredTargetState.value = bestTarget
        }
    }

    fun startCardDrag(
        from: PileLocation,
        cardIndex: Int,
        cards: List<Card>,
        originInRoot: Offset?,
        cardWidthPx: Float,
        cardHeightPx: Float,
        stackStepPx: Float
    ) {
        if (latestBoardState.isAutoCompleting || latestBoardState.isWon) return
        if (activeDragState.value != null) {
            if (isReturningHolder.value || returnJobHolder.value?.isActive == true) {
                returnJobHolder.value?.cancel()
                returnJobHolder.value = null
                isReturningHolder.value = false
                isReturningState.value = false
                returnProgressState.floatValue = 0f
                activeDragState.value = null
                dragOffsetState.value = Offset.Zero
            } else {
                return
            }
        }
        val origin = originInRoot?.takeIf { it.isSpecified } ?: return

        val draggedIds = cards.mapTo(HashSet()) { it.id }
        if (activeFlightsState.value.any { f -> f.cards.any { it.id in draggedIds } }) {
            val remaining = activeFlightsState.value.filterNot { f -> f.cards.any { it.id in draggedIds } }
            activeFlightsState.value = remaining
            syncFlyingHiddenStates(remaining)
        }

        dragOffsetState.value = Offset.Zero
        returnProgressState.floatValue = 0f
        isReturningHolder.value = false
        isReturningState.value = false
        activeDragState.value = ActiveDragState(
            from = from,
            cardIndex = cardIndex,
            cards = cards,
            originInRoot = origin,
            cardWidthPx = cardWidthPx,
            cardHeightPx = cardHeightPx,
            stackStepPx = stackStepPx
        )
        hoveredTargetState.value = null
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val onDragDelta = remember {
        { delta: Offset ->
            val drag = activeDragState.value
            if (drag != null && !isReturningHolder.value && returnJobHolder.value?.isActive != true) {
                val nextOffset = dragOffsetState.value + delta
                dragOffsetState.value = nextOffset
                updateHoveredTarget(drag, nextOffset)
            }
        }
    }

    val onEndDrag = remember {
        {
            val drag = activeDragState.value
            if (drag != null && !isReturningHolder.value && returnJobHolder.value?.isActive != true) {
                val target = hoveredTargetState.value
                if (target != null && KlondikeRules.isValidMove(latestBoardState.gameState, drag.from, drag.cardIndex, target)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastDragDropHolder.value = DragDropSnapshot(
                        headCardId = drag.cards.first().id,
                        releaseOrigin = drag.originInRoot + dragOffsetState.value,
                        stackStepPx = drag.stackStepPx
                    )
                    activeDragState.value = null
                    hoveredTargetState.value = null
                    dragOffsetState.value = Offset.Zero
                    returnProgressState.floatValue = 0f
                    isReturningHolder.value = false
                    isReturningState.value = false
                    latestOnIntent(KlondikeIntent.MoveCards(drag.from, drag.cardIndex, target))
                } else {
                    val releaseDelta = dragOffsetState.value
                    hoveredTargetState.value = null
                    val distance = releaseDelta.getDistance()
                    val tapThresholdPx = with(density) { 22.dp.toPx() }
                    if (distance < tapThresholdPx) {
                        isReturningHolder.value = false
                        isReturningState.value = false
                        returnProgressState.floatValue = 0f
                        dragOffsetState.value = Offset.Zero
                        activeDragState.value = null
                        latestOnIntent(KlondikeIntent.TapPileOrCard(drag.from, drag.cardIndex))
                    } else {
                        isReturningHolder.value = true
                        isReturningState.value = true
                        returnJobHolder.value?.cancel()
                        returnJobHolder.value = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            val anim = TargetBasedAnimation(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                typeConverter = Offset.VectorConverter,
                                initialValue = releaseDelta,
                                targetValue = Offset.Zero
                            )
                            var startNanos = -1L
                            while (true) {
                                val finished = withFrameNanos { frameNanos ->
                                    if (startNanos < 0L) {
                                        // Advance by one frame interval so motion begins on the very first frame after release.
                                        startNanos = frameNanos - 16_666_667L
                                    }
                                    val playTimeNanos = frameNanos - startNanos
                                    val value = anim.getValueFromNanos(playTimeNanos)
                                    dragOffsetState.value = value
                                    returnProgressState.floatValue =
                                        (1f - (value.getDistance() / distance)).coerceIn(0f, 1f)
                                    anim.isFinishedFromNanos(playTimeNanos)
                                }
                                if (finished) break
                            }
                            if (isActive) {
                                isReturningHolder.value = false
                                isReturningState.value = false
                                returnProgressState.floatValue = 0f
                                dragOffsetState.value = Offset.Zero
                                activeDragState.value = null
                                returnJobHolder.value = null
                            }
                        }
                    }
                }
            }
        }
    }

    val onCancelDrag = remember {
        {
            returnJobHolder.value?.cancel()
            returnJobHolder.value = null
            isReturningHolder.value = false
            isReturningState.value = false
            returnProgressState.floatValue = 0f
            activeDragState.value = null
            hoveredTargetState.value = null
            dragOffsetState.value = Offset.Zero
        }
    }

    val onUpdateTargetBounds = remember {
        { loc: PileLocation, rect: Rect ->
            dropTargetBounds[loc] = rect
        }
    }

    val onRegisterStockOrigin = remember {
        { origin: Offset ->
            stockOriginHolder.value = origin
            val flights = activeFlightsState.value
            for (flight in flights) {
                if (flight.startFromStock) {
                    flight.startOrigin = origin
                }
                if (flight.endAtStock) {
                    flight.targetOrigin = origin
                }
            }
        }
    }

    val onRegisterCardOrigin = remember {
        { cardId: String, origin: Offset ->
            cardOrigins[cardId] = origin
            val flights = activeFlightsState.value
            for (flight in flights) {
                if (flight.cards.firstOrNull()?.id == cardId) {
                    flight.targetOrigin = origin
                }
            }
        }
    }

    val onFlyingCardsUpdated = remember {
        { currentFlights: List<CardFlight> ->
            syncFlyingHiddenStates(currentFlights)
        }
    }

    val onFlightsUpdated = remember {
        { remaining: List<CardFlight> ->
            activeFlightsState.value = remaining
            syncFlyingHiddenStates(remaining)
            if (remaining.isEmpty() && latestBoardState.isWon && !latestBoardState.isAutoCompleting) {
                latestOnWinFlightsSettled()
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            latestBoardState.isWon && !latestBoardState.isAutoCompleting && activeFlightsState.value.isEmpty()
        }
            .distinctUntilChanged()
            .collect { winSettled ->
                if (winSettled) {
                    latestOnWinFlightsSettled()
                }
            }
    }

    val onBoardIntent = remember(haptic) {
        { intent: KlondikeIntent ->
            if (intent is KlondikeIntent.DrawOrRecycle || intent is KlondikeIntent.TapPileOrCard) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            latestOnIntent(intent)
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
        val hPadding = if (isLandscape) 16.dp else 6.dp
        val vPadding = if (isLandscape) 4.dp else 8.dp
        val availableW = (maxWidth - hPadding * 2).coerceAtLeast(0.dp)
        val availableH = (maxHeight - vPadding * 2).coerceAtLeast(0.dp)

        val topRowGap = if (isLandscape) 6.dp else 12.dp
        val heightDenominator = if (isLandscape) {
            when {
                availableH < 260.dp -> 5.2f
                availableH < 420.dp -> 5.0f
                else -> 4.85f
            }
        } else {
            3.2f
        }
        val maxCardHeightFromHeight = (availableH - topRowGap) / heightDenominator
        val maxCardWidthFromHeight = maxCardHeightFromHeight / CARD_ASPECT_RATIO

        val horizontalGap = if (isLandscape) {
            (maxCardWidthFromHeight * 0.12f).coerceIn(4.dp, 10.dp)
        } else {
            (availableW * 0.012f).coerceIn(4.dp, 10.dp)
        }
        val totalGaps = horizontalGap * (GameState.TABLEAU_COUNT - 1)
        val maxCardWidthFromWidth = (availableW - totalGaps) / GameState.TABLEAU_COUNT

        val cardWidth = minOf(maxCardWidthFromWidth, maxCardWidthFromHeight)
        val cardHeight = cardWidth * CARD_ASPECT_RATIO
        val boardContentWidth = (cardWidth * GameState.TABLEAU_COUNT) + totalGaps
        val tableauAvailableHeight = (availableH - cardHeight - topRowGap).coerceAtLeast(cardHeight)

        val cardWidthPx = with(density) { cardWidth.toPx() }
        val cardHeightPx = with(density) { cardHeight.toPx() }
        val defaultFaceUpStepPx = with(density) { (cardHeight * 0.32f).coerceAtLeast(10.dp).toPx() }

        val previousState = prevGameStateHolder.value
        val currentDealId = boardState.dealId
        if (previousState !== boardState.gameState) {
            prevGameStateHolder.value = boardState.gameState
            val currentDragDrop = lastDragDropHolder.value
            lastDragDropHolder.value = null
            val currentState = boardState.gameState
            val isCleanDealState = currentState.moveCount == 0 &&
                currentState.waste.isEmpty() &&
                currentState.foundations.all { it.isEmpty() }
            val isFreshDeal = isCleanDealState && (
                (currentDealId > 0L && currentDealId != lastDealtIdHolder.value) ||
                    (currentDealId <= 0L && previousState == null)
                )
            lastDealtIdHolder.value = currentDealId
            if (isFreshDeal) {
                val dealBatchId = ++dealBatchHolder.value
                val totalDealCards = currentState.tableau.sumOf { it.size }
                val dealFlights = buildDealFlights(
                    state = currentState,
                    stockOrigin = stockOriginHolder.value,
                    defaultStackStepPx = defaultFaceUpStepPx,
                    dealBatchId = dealBatchId
                )
                Snapshot.withoutReadObservation {
                    activeFlightsState.value = dealFlights
                    syncFlyingHiddenStates(dealFlights)
                    dealBatchState.intValue = dealBatchId
                    dealMountLimitState.intValue = if (totalDealCards > 0) 0 else Int.MAX_VALUE
                }
            } else if (currentState.seed != previousState?.seed) {
                Snapshot.withoutReadObservation {
                    dealMountLimitState.intValue = Int.MAX_VALUE
                    if (activeFlightsState.value.isNotEmpty()) {
                        activeFlightsState.value = emptyList()
                        syncFlyingHiddenStates(emptyList())
                    }
                }
            } else {
                val newFlights = buildFlightsForMove(
                    oldState = previousState,
                    newState = currentState,
                    cardOrigins = cardOrigins,
                    stockOrigin = stockOriginHolder.value,
                    dropTargetBounds = dropTargetBounds,
                    defaultStackStepPx = defaultFaceUpStepPx,
                    lastDragDrop = currentDragDrop,
                    isAutoCompleting = boardState.isAutoCompleting
                )
                Snapshot.withoutReadObservation {
                    dealMountLimitState.intValue = Int.MAX_VALUE
                }
                if (newFlights.isNotEmpty()) {
                    Snapshot.withoutReadObservation {
                        val newIds = newFlights.flatMap { f -> f.cards.map { it.id } }.toSet()
                        val hasNewFlipFromBack = newFlights.any { it.flipFromBack }
                        val merged = activeFlightsState.value.filterNot { existing ->
                            existing.progressState.floatValue >= 1f ||
                                (hasNewFlipFromBack && existing.flipFromBack && !existing.isDeal) ||
                                existing.cards.any { it.id in newIds }
                        } + newFlights
                        activeFlightsState.value = merged
                        syncFlyingHiddenStates(merged)
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { dealBatchState.intValue }
                .collectLatest { currentDealBatch ->
                    if (currentDealBatch > 0 && dealMountLimitState.intValue < Int.MAX_VALUE) {
                        // Skip the first frame so dialog dismissal & previous board teardown settle cleanly.
                        withFrameNanos {}
                        var mounted = 0
                        val totalToMount = latestBoardState.gameState.tableau.sumOf { it.size }
                        while (mounted < totalToMount) {
                            withFrameNanos {
                                mounted += 7
                                dealMountLimitState.intValue = if (mounted >= totalToMount) Int.MAX_VALUE else mounted
                            }
                        }
                        dealMountLimitState.intValue = Int.MAX_VALUE
                    }
                }
        }

        val onStartTopDrag = remember(cardWidthPx, cardHeightPx, defaultFaceUpStepPx) {
            { from: PileLocation, idx: Int, cards: List<Card>, origin: Offset? ->
                startCardDrag(from, idx, cards, origin, cardWidthPx, cardHeightPx, defaultFaceUpStepPx)
            }
        }
        val onStartTableauDrag = remember(cardWidthPx, cardHeightPx) {
            { from: PileLocation, idx: Int, cards: List<Card>, origin: Offset?, stepPx: Float ->
                startCardDrag(from, idx, cards, origin, cardWidthPx, cardHeightPx, stepPx)
            }
        }

        CompositionLocalProvider(LocalCardBackTheme provides boardState.cardBackTheme) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(boardContentWidth)
                    .fillMaxHeight()
                    .padding(vertical = vPadding)
            ) {
                TopPilesRow(
                    boardState = boardState,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    horizontalGap = horizontalGap,
                    colors = colors,
                    textMeasurer = sharedTextMeasurer,
                    activeDragState = activeDragState,
                    getFlyingHiddenState = getFlyingHiddenState,
                    hoveredTargetState = hoveredTargetState,
                    onUpdateTargetBounds = onUpdateTargetBounds,
                    onRegisterStockOrigin = onRegisterStockOrigin,
                    onRegisterCardOrigin = onRegisterCardOrigin,
                    onStartDrag = onStartTopDrag,
                    onDragDelta = onDragDelta,
                    onEndDrag = onEndDrag,
                    onCancelDrag = onCancelDrag,
                    onIntent = onBoardIntent
                )

                Spacer(modifier = Modifier.height(topRowGap))

                TableauRow(
                    boardState = boardState,
                    dealMountLimitState = dealMountLimitState,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    horizontalGap = horizontalGap,
                    availableHeight = tableauAvailableHeight,
                    colors = colors,
                    textMeasurer = sharedTextMeasurer,
                    activeDragState = activeDragState,
                    getFlyingHiddenState = getFlyingHiddenState,
                    hoveredTargetState = hoveredTargetState,
                    onUpdateTargetBounds = onUpdateTargetBounds,
                    onRegisterCardOrigin = onRegisterCardOrigin,
                    onStartDrag = onStartTableauDrag,
                    onDragDelta = onDragDelta,
                    onEndDrag = onEndDrag,
                    onCancelDrag = onCancelDrag,
                    onIntent = onBoardIntent
                )
            }

            FlightOverlay(
                activeFlightsState = activeFlightsState,
                onFlyingCardsUpdated = onFlyingCardsUpdated,
                onFlightsUpdated = onFlightsUpdated,
                boardOriginHolder = boardOriginHolder,
                textMeasurer = sharedTextMeasurer,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                colors = colors,
                cardBackTheme = boardState.cardBackTheme
            )

            DragOverlay(
                activeDragState = activeDragState,
                dragOffsetState = dragOffsetState,
                returnProgressState = returnProgressState,
                boardOriginHolder = boardOriginHolder,
                textMeasurer = sharedTextMeasurer,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                colors = colors
            )
        }
    }
}
