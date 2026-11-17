package com.nowlsantos.nowlsolitaire.ui.components.pyramid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.ui.components.EmptyPileSlot
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
internal fun WastePileView(
    gameState: PyramidGameState,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    cardBackTheme: CardBackTheme,
    activeHint: PyramidMatch?,
    isLandscape: Boolean = false,
    isDealingState: State<Boolean>,
    getFlyingHiddenState: (String) -> State<Boolean>,
    onTapWaste: () -> Unit,
    onPositioned: (Offset) -> Unit
) {
    val isSelected = gameState.selectedLocation == PyramidLocation.Waste
    val isHinted = isLocationHinted(activeHint, PyramidLocation.Waste)
    val waste = gameState.waste
    val topWaste = waste.lastOrNull()
    val underneathWaste = if (waste.size >= 2) waste[waste.size - 2] else null
    val topFlyingState = topWaste?.let { card ->
        remember(card.id) { getFlyingHiddenState(card.id) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 2.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier.onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    onPositioned(coords.positionInRoot())
                }
            },
            contentAlignment = Alignment.Center
        ) {
            // Base slot (Empty slot indicator)
            EmptyPileSlot(
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                colors = colors,
                label = "Waste",
                onClick = null,
                modifier = Modifier.graphicsLayer {
                    val showEmpty = waste.isEmpty() || (waste.size == 1 && topFlyingState?.value == true)
                    alpha = if (showEmpty) 1f else 0f
                }
            )

            // Card underneath (revealed while top card is flying away or arriving)
            if (underneathWaste != null) {
                key(underneathWaste.id) {
                    val underFlyingState = remember(underneathWaste.id) {
                        getFlyingHiddenState(underneathWaste.id)
                    }
                    PlayingCardView(
                        card = underneathWaste,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        colors = colors,
                        cardBackTheme = cardBackTheme,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (underFlyingState.value) 0f else 1f
                        }
                    )
                }
            }

            // Top waste card
            if (topWaste != null && topFlyingState != null) {
                key(topWaste.id) {
                    val onCardClick = remember(topWaste.id, onTapWaste) {
                        {
                            if (!isDealingState.value && !topFlyingState.value) {
                                onTapWaste()
                            }
                        }
                    }
                    PlayingCardView(
                        card = topWaste,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        colors = colors,
                        isSelected = isSelected,
                        isHinted = isHinted,
                        cardBackTheme = cardBackTheme,
                        onClick = onCardClick,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (topFlyingState.value) 0f else 1f
                        }
                    )
                }
            }
        }

        Text(
            text = "Waste",
            color = colors.barContent.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
