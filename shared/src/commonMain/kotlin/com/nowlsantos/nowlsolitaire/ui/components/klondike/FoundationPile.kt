package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.ui.components.EmptyPileSlot
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
internal fun FoundationPile(
    index: Int,
    boardState: KlondikeBoardState,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    textMeasurer: TextMeasurer,
    activeDragState: State<ActiveDragState?>,
    getFlyingHiddenState: (String) -> MutableState<Boolean>,
    hoveredTarget: PileLocation?,
    onUpdateTargetBounds: (PileLocation, Rect) -> Unit,
    onRegisterCardOrigin: (String, Offset) -> Unit,
    onStartDrag: (PileLocation, Int, List<Card>, Offset?) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onIntent: (KlondikeIntent) -> Unit
) {
    val location = PileLocation.Foundation(index)
    val pile = boardState.gameState.foundations.getOrElse(index) { emptyList() }
    val isSelected = boardState.selection?.location == location
    val isHintSource = boardState.activeHint?.from == location
    val isHintTarget = boardState.activeHint?.to == location
    val isDropTarget = hoveredTarget == location
    val bottomCardFlyingState = if (pile.size == 1) {
        getFlyingHiddenState(pile[0].id)
    } else {
        null
    }

    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                onUpdateTargetBounds(location, coords.boundsInRoot())
            }
        }
    ) {
        EmptyPileSlot(
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            colors = colors,
            label = "A",
            isHighlighted = isHintTarget,
            isDropTarget = isDropTarget,
            contentDescriptionText = if (pile.isEmpty()) "Empty foundation pile ${index + 1}" else null,
            onClick = if (pile.isEmpty()) {
                { onIntent(KlondikeIntent.TapPileOrCard(location, null)) }
            } else {
                null
            },
            modifier = Modifier.graphicsLayer {
                val showEmpty = pile.isEmpty() || (pile.size == 1 && bottomCardFlyingState?.value == true)
                alpha = if (showEmpty) 1f else 0f
            }
        )

        val startIdx = (pile.size - 4).coerceAtLeast(0)
        for (cardIdx in startIdx until pile.size) {
            val card = pile[cardIdx]
            val isTopCard = cardIdx == pile.lastIndex
            key(card.id) {
                val originHolder = remember { NullableOffsetHolder() }
                val currentCard by rememberUpdatedState(card)
                val currentTopIndex by rememberUpdatedState(cardIdx)
                val flyingHiddenState = remember(card.id) { getFlyingHiddenState(card.id) }

                PlayingCardView(
                    card = card,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    colors = colors,
                    isSelected = isTopCard && (isSelected || isDropTarget),
                    isHinted = isTopCard && (isHintSource || isHintTarget),
                    textMeasurer = textMeasurer,
                    onClick = if (isTopCard) {
                        { onIntent(KlondikeIntent.TapPileOrCard(location, currentTopIndex)) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            val hiddenForDrag = isTopCard && activeDragState.value?.from == location
                            alpha = if (hiddenForDrag || flyingHiddenState.value) 0f else 1f
                        }
                        .onGloballyPositioned { coords ->
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
                                Modifier.pointerInput(card.id, location) {
                                    detectDragGestures(
                                        onDragStart = {
                                            onStartDrag(
                                                location,
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
