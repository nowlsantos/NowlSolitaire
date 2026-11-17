package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.HintMove
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.ui.components.EmptyPileSlot
import com.nowlsantos.nowlsolitaire.ui.state.CardSelection
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
internal fun TableauColumn(
    location: PileLocation.Tableau,
    columnIndex: Int,
    cards: List<Card>,
    mountStartIndex: Int,
    dealMountLimitState: State<Int>,
    selection: CardSelection?,
    activeHint: HintMove?,
    isHintTarget: Boolean,
    isDropTarget: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    availableHeight: Dp,
    colors: TableColors,
    textMeasurer: TextMeasurer,
    activeDragState: State<ActiveDragState?>,
    getFlyingHiddenState: (String) -> MutableState<Boolean>,
    onUpdateTargetBounds: (PileLocation, Rect) -> Unit,
    onRegisterCardOrigin: (String, Offset) -> Unit,
    onStartDrag: (PileLocation, Int, List<Card>, Offset?, Float) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onIntent: (KlondikeIntent) -> Unit
) {
    val columnVisibleLimit by remember(mountStartIndex, cards.size) {
        derivedStateOf {
            val limit = dealMountLimitState.value
            if (limit == Int.MAX_VALUE) {
                cards.size
            } else {
                (limit - mountStartIndex).coerceIn(0, cards.size)
            }
        }
    }
    val density = LocalDensity.current
    val offsets = remember(cards, cardHeight, availableHeight) {
        if (cards.isNotEmpty()) {
            calculateColumnOffsets(
                cards = cards,
                cardHeight = cardHeight,
                availableHeight = availableHeight
            )
        } else {
            emptyList()
        }
    }
    val registrationHolder = remember { ColumnOriginRegistrationHolder() }
    val firstCardFlyingState = cards.firstOrNull()?.let { firstCard ->
        remember(firstCard.id) { getFlyingHiddenState(firstCard.id) }
    }

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(availableHeight)
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    onUpdateTargetBounds(location, coords.boundsInRoot())
                    if (cards.isNotEmpty()) {
                        val colTopLeft = coords.positionInRoot()
                        if (registrationHolder.colTopLeft != colTopLeft ||
                            registrationHolder.cards !== cards ||
                            registrationHolder.offsets !== offsets
                        ) {
                            registrationHolder.colTopLeft = colTopLeft
                            registrationHolder.cards = cards
                            registrationHolder.offsets = offsets
                            for (i in cards.indices) {
                                val yPx = with(density) { offsets[i].toPx() }
                                onRegisterCardOrigin(cards[i].id, Offset(colTopLeft.x, colTopLeft.y + yPx))
                            }
                        }
                    } else {
                        registrationHolder.cards = cards
                    }
                }
            }
    ) {
        EmptyPileSlot(
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            colors = colors,
            label = "K",
            isHighlighted = isHintTarget,
            isDropTarget = isDropTarget,
            contentDescriptionText = if (cards.isEmpty()) "Empty tableau column ${columnIndex + 1}" else null,
            onClick = if (cards.isEmpty()) {
                { onIntent(KlondikeIntent.TapPileOrCard(location, null)) }
            } else {
                null
            },
            modifier = Modifier.graphicsLayer {
                val showEmpty = cards.isEmpty() || firstCardFlyingState?.value == true
                alpha = if (showEmpty) 1f else 0f
            }
        )

        if (cards.isNotEmpty() && columnVisibleLimit > 0) {
            val faceUpStepDp = if (cards.size >= 2) {
                val lastFaceUpGap = offsets.last() - offsets[offsets.lastIndex - 1]
                if (lastFaceUpGap > 0.dp) lastFaceUpGap else (cardHeight * 0.32f).coerceAtLeast(10.dp)
            } else {
                (cardHeight * 0.32f).coerceAtLeast(10.dp)
            }
            val faceUpStepPx = with(density) { faceUpStepDp.toPx() }

            for (cardIndex in 0 until columnVisibleLimit) {
                val card = cards[cardIndex]
                val isSelected = selection?.let { sel ->
                    sel.location == location && cardIndex >= sel.cardIndex
                } ?: false

                val isHinted = activeHint?.let { hint ->
                    (hint.from == location && cardIndex >= hint.cardIndex) ||
                        (hint.to == location && cardIndex == cards.lastIndex)
                } ?: false

                val isDropHighlighted = isDropTarget && cardIndex == cards.lastIndex
                val targetYPx = with(density) { offsets[cardIndex].toPx() }

                key(card.id) {
                    TableauCardItem(
                        card = card,
                        cardIndex = cardIndex,
                        cards = cards,
                        location = location,
                        targetYPx = targetYPx,
                        faceUpStepPx = faceUpStepPx,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        colors = colors,
                        textMeasurer = textMeasurer,
                        isSelected = isSelected || isDropHighlighted,
                        isHinted = isHinted,
                        activeDragState = activeDragState,
                        getFlyingHiddenState = getFlyingHiddenState,
                        onRegisterCardOrigin = onRegisterCardOrigin,
                        onStartDrag = onStartDrag,
                        onDragDelta = onDragDelta,
                        onEndDrag = onEndDrag,
                        onCancelDrag = onCancelDrag,
                        onIntent = onIntent
                    )
                }
            }
        }
    }
}

internal fun calculateColumnOffsets(
    cards: List<Card>,
    cardHeight: Dp,
    availableHeight: Dp
): List<Dp> {
    if (cards.size <= 1) return listOf(0.dp)

    val defaultFaceDownStep = (cardHeight * 0.18f).coerceAtLeast(4.dp)
    val defaultFaceUpStep = (cardHeight * 0.32f).coerceAtLeast(10.dp)

    val rawSteps = List(cards.size - 1) { index ->
        if (cards[index].isFaceUp) defaultFaceUpStep else defaultFaceDownStep
    }

    val totalRawStepHeight = rawSteps.fold(0.dp) { acc, dp -> acc + dp }
    val maxStepsHeight = (availableHeight - cardHeight).coerceAtLeast(0.dp)
    val scale = if (totalRawStepHeight > maxStepsHeight && totalRawStepHeight > 0.dp) {
        maxStepsHeight / totalRawStepHeight
    } else {
        1f
    }

    val offsets = mutableListOf(0.dp)
    var currentY = 0.dp
    for (step in rawSteps) {
        currentY += step * scale
        offsets.add(currentY)
    }
    return offsets
}
