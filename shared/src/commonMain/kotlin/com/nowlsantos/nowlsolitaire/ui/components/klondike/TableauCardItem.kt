package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors
import kotlin.math.roundToInt

@Composable
internal fun TableauCardItem(
    card: Card,
    cardIndex: Int,
    cards: List<Card>,
    location: PileLocation.Tableau,
    targetYPx: Float,
    faceUpStepPx: Float,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    textMeasurer: TextMeasurer,
    isSelected: Boolean,
    isHinted: Boolean,
    activeDragState: State<ActiveDragState?>,
    getFlyingHiddenState: (String) -> MutableState<Boolean>,
    onRegisterCardOrigin: (String, Offset) -> Unit,
    onStartDrag: (PileLocation, Int, List<Card>, Offset?, Float) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onIntent: (KlondikeIntent) -> Unit
) {
    val flyingHiddenState = remember(card.id) { getFlyingHiddenState(card.id) }

    if (card.isFaceUp) {
        val originHolder = remember { NullableOffsetHolder() }
        val currentCards by rememberUpdatedState(cards)
        val currentCardIndex by rememberUpdatedState(cardIndex)
        val currentStepPx by rememberUpdatedState(faceUpStepPx)
        val onCardClick = remember(card.id, location, onIntent) {
            { onIntent(KlondikeIntent.TapPileOrCard(location, currentCardIndex)) }
        }

        PlayingCardView(
            card = card,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            colors = colors,
            isSelected = isSelected,
            isHinted = isHinted,
            textMeasurer = textMeasurer,
            onClick = onCardClick,
            modifier = Modifier
                .offset { IntOffset(0, targetYPx.roundToInt()) }
                .graphicsLayer {
                    val drag = activeDragState.value
                    val hiddenForDrag = drag != null && drag.from == location && currentCardIndex >= drag.cardIndex
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
                .pointerInput(card.id, location) {
                    detectDragGestures(
                        onDragStart = {
                            val snapshot = currentCards
                            val idx = currentCardIndex
                            if (idx in snapshot.indices && snapshot[idx].isFaceUp) {
                                onStartDrag(
                                    location,
                                    idx,
                                    snapshot.subList(idx, snapshot.size),
                                    originHolder.value,
                                    currentStepPx
                                )
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount)
                        },
                        onDragEnd = onEndDrag,
                        onDragCancel = onCancelDrag
                    )
                }
        )
    } else {
        PlayingCardView(
            card = card,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            colors = colors,
            isSelected = isSelected,
            isHinted = isHinted,
            textMeasurer = textMeasurer,
            modifier = Modifier
                .offset { IntOffset(0, targetYPx.roundToInt()) }
                .graphicsLayer {
                    alpha = if (flyingHiddenState.value) 0f else 1f
                }
                .onPlaced { coords ->
                    if (coords.isAttached) {
                        onRegisterCardOrigin(card.id, coords.positionInRoot())
                    }
                }
        )
    }
}
