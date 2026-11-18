package com.nowlsantos.nowlsolitaire.ui.components.pyramid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidRules
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors
import kotlin.math.roundToInt

@Composable
internal fun PyramidGrid(
    gameState: PyramidGameState,
    activeHint: PyramidMatch?,
    cardBackTheme: CardBackTheme,
    colors: TableColors,
    cardWidth: Dp,
    cardHeight: Dp,
    horizontalGap: Dp,
    verticalOverlap: Dp,
    pyramidTotalHeight: Dp,
    isDealingState: State<Boolean>,
    hideAllForPendingDealState: State<Boolean>,
    getFlyingHiddenState: (String) -> State<Boolean>,
    onSlotPositioned: (PyramidLocation.PyramidSlot, Offset) -> Unit,
    onTapCard: (PyramidLocation) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pyramidTotalHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        for (row in 0 until PyramidGameState.PYRAMID_ROW_COUNT) {
            val countInRow = row + 1
            val offsetY = verticalOverlap * row

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, offsetY.toPx().roundToInt()) }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(horizontalGap),
                    verticalAlignment = Alignment.Top
                ) {
                    for (col in 0 until countInRow) {
                        val slotLocation = PyramidLocation.PyramidSlot(row, col)
                        val expectedIdx = row * (row + 1) / 2 + col
                        val pyramidCard = gameState.pyramid.getOrNull(expectedIdx)
                            ?.takeIf { it.row == row && it.col == col }
                            ?: gameState.pyramid.firstOrNull { it.row == row && it.col == col }

                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                if (coords.isAttached) {
                                    onSlotPositioned(slotLocation, coords.positionInRoot())
                                }
                            }
                        ) {
                            if (pyramidCard != null && !pyramidCard.isRemoved) {
                                key(pyramidCard.card.id) {
                                    val flyingHiddenState = remember(pyramidCard.card.id) {
                                        getFlyingHiddenState(pyramidCard.card.id)
                                    }
                                    val isExposed = PyramidRules.isExposed(gameState.pyramid, row, col)
                                    val isSelected = gameState.selectedLocation == slotLocation
                                    val isHinted = isLocationHinted(activeHint, slotLocation)
                                    val onCardClick = remember(slotLocation, pyramidCard.card.id, isExposed, onTapCard) {
                                        if (isExposed) {
                                            {
                                                if (!isDealingState.value &&
                                                    !hideAllForPendingDealState.value &&
                                                    !flyingHiddenState.value
                                                ) {
                                                    onTapCard(slotLocation)
                                                }
                                            }
                                        } else {
                                            null
                                        }
                                    }

                                    PlayingCardView(
                                        card = pyramidCard.card,
                                        cardWidth = cardWidth,
                                        cardHeight = cardHeight,
                                        colors = colors,
                                        isSelected = isSelected,
                                        isHinted = isHinted,
                                        isElevated = true,
                                        elevation = if (isSelected) 6.dp else 2.5.dp,
                                        cardBackTheme = cardBackTheme,
                                        onClick = onCardClick,
                                        modifier = Modifier.graphicsLayer {
                                            val hidden = hideAllForPendingDealState.value || flyingHiddenState.value
                                            alpha = if (hidden) 0f else 1f
                                        }
                                    )
                                }
                            } else {
                                // Empty placeholder to preserve column positions
                                Spacer(modifier = Modifier.size(cardWidth, cardHeight))
                            }
                        }
                    }
                }
            }
        }
    }
}
