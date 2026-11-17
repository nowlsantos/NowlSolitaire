package com.nowlsantos.nowlsolitaire.ui.components.pyramid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.ui.components.EmptyPileSlot
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
internal fun DiscardPileView(
    gameState: PyramidGameState,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    cardBackTheme: CardBackTheme,
    isLandscape: Boolean = false,
    getFlyingHiddenState: (String) -> State<Boolean>,
    onPositioned: (Offset) -> Unit
) {
    val discardPile = gameState.discardPile
    val topDiscard = discardPile.lastOrNull()
    val topDiscardFlyingState = topDiscard?.let { card ->
        remember(card.id) { getFlyingHiddenState(card.id) }
    }
    val visibleCount by remember(discardPile) {
        derivedStateOf {
            var count = 0
            for (i in discardPile.indices) {
                if (!getFlyingHiddenState(discardPile[i].id).value) {
                    count++
                }
            }
            count
        }
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
            EmptyPileSlot(
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                colors = colors,
                label = "13",
                onClick = null,
                modifier = Modifier.graphicsLayer {
                    alpha = if (visibleCount == 0) 1f else 0f
                }
            )

            if (topDiscard != null && topDiscardFlyingState != null) {
                key(topDiscard.id) {
                    PlayingCardView(
                        card = topDiscard,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        colors = colors,
                        cardBackTheme = cardBackTheme,
                        onClick = null,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (topDiscardFlyingState.value) 0f else 1f
                        }
                    )
                }
            }

            if (visibleCount > 0) {
                // Count badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC000000),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = visibleCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            text = "Discard",
            color = colors.barContent.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
