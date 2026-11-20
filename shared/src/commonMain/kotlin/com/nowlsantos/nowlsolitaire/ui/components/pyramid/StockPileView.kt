package com.nowlsantos.nowlsolitaire.ui.components.pyramid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.PyramidGameState
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidMatch
import com.nowlsantos.nowlsolitaire.ui.components.EmptyPileSlot
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
internal fun StockPileView(
    gameState: PyramidGameState,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    cardBackTheme: CardBackTheme,
    activeHint: PyramidMatch?,
    isLandscape: Boolean = false,
    isDealingState: State<Boolean>,
    getFlyingHiddenState: (String) -> State<Boolean>,
    onTapStock: () -> Unit,
    onDrawOrRecycle: () -> Unit,
    onPositioned: (Offset) -> Unit
) {
    val isSelected = gameState.selectedLocation == PyramidLocation.Stock
    val isHinted = isLocationHinted(activeHint, PyramidLocation.Stock)
    val stock = gameState.stock
    val visibleStockInfo by remember(stock) {
        derivedStateOf {
            var top: Card? = null
            var count = 0
            for (i in stock.indices) {
                val card = stock[i]
                if (!getFlyingHiddenState(card.id).value) {
                    top = card
                    count++
                }
            }
            top to count
        }
    }
    val topVisibleStock = visibleStockInfo.first ?: stock.lastOrNull()
    val visibleStockCount = visibleStockInfo.second

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
            if (topVisibleStock != null) {
                val onCardClick = remember(onTapStock) {
                    {
                        if (!isDealingState.value) {
                            onTapStock()
                        }
                    }
                }
                PlayingCardView(
                    card = topVisibleStock,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    colors = colors,
                    isSelected = isSelected,
                    isHinted = isHinted,
                    cardBackTheme = cardBackTheme,
                    onClick = onCardClick
                )

                // Remaining stock badge
                /*Surface(
                    shape = CircleShape,
                    color = Color(0xCC000000),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = visibleStockCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }*/
            } else if (gameState.canRecycleStock) {
                EmptyPileSlot(
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    colors = colors,
                    showRefreshIcon = true,
                    label = "Recycle",
                    onClick = onDrawOrRecycle
                )
            } else {
                EmptyPileSlot(
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    colors = colors,
                    label = "Empty",
                    onClick = null
                )
            }
        }

        Text(
            text = "Stock",
            color = colors.barContent.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun StockDrawButton(
    hasStockCards: Boolean,
    canRecycleStock: Boolean,
    cardHeight: Dp,
    colors: TableColors,
    isLandscape: Boolean = false,
    pointsLeft: Boolean = false,
    onDrawOrRecycle: () -> Unit
) {
    val canAct = hasStockCards || canRecycleStock
    val buttonSize = if (isLandscape) 34.dp else 40.dp
    val iconSize = if (isLandscape) 18.dp else 22.dp
    val labelText = when {
        hasStockCards -> "Draw"
        canRecycleStock -> "Redeal"
        else -> "Draw"
    }
    val actionDescription = when {
        hasStockCards -> "Draw card from stock to waste"
        canRecycleStock -> "Recycle waste pile back into stock"
        else -> "No draws remaining"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 2.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier.height(cardHeight),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = colors.barSurface.copy(alpha = if (canAct) 0.85f else 0.45f),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (canAct) colors.barContent.copy(alpha = 0.45f) else colors.slotStroke.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .alpha(if (canAct) 1f else 0.45f)
                    .semantics { contentDescription = actionDescription }
                    .then(
                        if (canAct) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClick = onDrawOrRecycle
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val drawIcon = if (pointsLeft) {
                        Icons.AutoMirrored.Rounded.ArrowBack
                    } else {
                        Icons.AutoMirrored.Rounded.ArrowForward
                    }
                    Icon(
                        imageVector = if (!hasStockCards && canRecycleStock) {
                            Icons.Rounded.Refresh
                        } else {
                            drawIcon
                        },
                        contentDescription = null,
                        tint = colors.barContent,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        Text(
            text = labelText,
            color = colors.barContent.copy(alpha = if (canAct) 0.8f else 0.35f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
