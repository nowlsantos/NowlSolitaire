package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.ui.components.EmptyPileSlot
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors
import kotlin.math.roundToInt

@Composable
internal fun WastePile(
    boardState: KlondikeBoardState,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    textMeasurer: TextMeasurer,
    activeDragState: State<ActiveDragState?>,
    getFlyingHiddenState: (String) -> MutableState<Boolean>,
    onUpdateTargetBounds: (PileLocation, Rect) -> Unit,
    onRegisterCardOrigin: (String, Offset) -> Unit,
    onStartDrag: (PileLocation, Int, List<Card>, Offset?) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onIntent: (KlondikeIntent) -> Unit
) {
    val waste = boardState.gameState.waste
    val visibleCount = if (waste.isEmpty()) {
        0
    } else {
        boardState.gameState.wasteVisibleCount.coerceIn(1, minOf(3, waste.size))
    }

    val fanStep = cardWidth * WASTE_FAN_STEP_RATIO
    val extraFanWidth = if (visibleCount > 1) fanStep * (visibleCount - 1) else 0.dp
    val baseSlotX = 0.dp

    Box(
        modifier = Modifier
            .width(cardWidth + extraFanWidth)
            .height(cardHeight)
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    onUpdateTargetBounds(PileLocation.Waste, coords.boundsInRoot())
                }
            }
    ) {
        EmptyPileSlot(
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            colors = colors,
            contentDescriptionText = if (waste.isEmpty()) "Empty waste pile" else null,
            modifier = Modifier
                .offset(x = baseSlotX)
                .graphicsLayer {
                    val allVisibleHidden = visibleCount > 0 && waste.takeLast(visibleCount).all { getFlyingHiddenState(it.id).value }
                    val showEmpty = waste.isEmpty() || allVisibleHidden
                    alpha = if (showEmpty) 1f else 0f
                }
        )

        if (waste.isNotEmpty()) {
            val startIndex = waste.size - visibleCount
            val visibleCards = waste.subList(startIndex, waste.size)
            val isSelected = boardState.selection?.location == PileLocation.Waste
            val isHintSource = boardState.activeHint?.from == PileLocation.Waste

            // Keep the card directly beneath the visible slice rendered so dragging the top card
            // reveals the next waste card underneath (especially in Draw-1 mode).
            if (startIndex > 0) {
                val underneathCard = waste[startIndex - 1]
                key(underneathCard.id) {
                    val flyingHiddenState = remember(underneathCard.id) { getFlyingHiddenState(underneathCard.id) }
                    PlayingCardView(
                        card = underneathCard,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        colors = colors,
                        textMeasurer = textMeasurer,
                        modifier = Modifier
                            .offset(x = baseSlotX)
                            .graphicsLayer {
                                alpha = if (flyingHiddenState.value) 0f else 1f
                            }
                            .onGloballyPositioned { coords ->
                                if (coords.isAttached) {
                                    onRegisterCardOrigin(underneathCard.id, coords.positionInRoot())
                                }
                            }
                    )
                }
            }

            visibleCards.forEachIndexed { offsetIndex, card ->
                val isTopCard = offsetIndex == visibleCards.lastIndex
                val targetXOffset = fanStep * offsetIndex
                val targetXPx = with(LocalDensity.current) { targetXOffset.toPx() }

                key(card.id) {
                    val originHolder = remember { NullableOffsetHolder() }
                    val currentTopIndex by rememberUpdatedState(waste.lastIndex)
                    val currentCard by rememberUpdatedState(card)
                    val flyingHiddenState = remember(card.id) { getFlyingHiddenState(card.id) }

                    PlayingCardView(
                        card = card,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        colors = colors,
                        isSelected = isTopCard && isSelected,
                        isHinted = isTopCard && isHintSource,
                        textMeasurer = textMeasurer,
                        onClick = if (isTopCard) {
                            { onIntent(KlondikeIntent.TapPileOrCard(PileLocation.Waste, currentTopIndex)) }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .offset { IntOffset(targetXPx.roundToInt(), 0) }
                            .graphicsLayer {
                                val hiddenForDrag = isTopCard && activeDragState.value?.from == PileLocation.Waste
                                alpha = if (hiddenForDrag || flyingHiddenState.value) 0f else 1f
                            }
                            .onPlaced { coords ->
                                if (coords.isAttached) {
                                    val pos = coords.positionInRoot()
                                    originHolder.value = pos
                                    onRegisterCardOrigin(card.id, pos)
                                } else {
                                    originHolder.value = null
                                }
                            }
                            .then(
                                if (isTopCard) {
                                    Modifier.pointerInput(card.id, PileLocation.Waste) {
                                        detectDragGestures(
                                            onDragStart = {
                                                onStartDrag(
                                                    PileLocation.Waste,
                                                    currentTopIndex,
                                                    listOf(currentCard),
                                                    originHolder.value
                                                )
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                onDragDelta(dragAmount)
                                            },
                                            onDragEnd = onEndDrag,
                                            onDragCancel = onCancelDrag
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}
