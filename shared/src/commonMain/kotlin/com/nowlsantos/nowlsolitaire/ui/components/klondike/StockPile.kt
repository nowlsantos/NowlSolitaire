package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.ui.components.EmptyPileSlot
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
internal fun StockPile(
    boardState: KlondikeBoardState,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    textMeasurer: TextMeasurer,
    getFlyingHiddenState: (String) -> MutableState<Boolean>,
    onRegisterStockOrigin: (Offset) -> Unit,
    onIntent: (KlondikeIntent) -> Unit
) {
    val stock = boardState.gameState.stock
    val isHinted = boardState.activeHint?.from == PileLocation.Stock
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
    val topStock = visibleStockInfo.first
    val visibleStockCount = visibleStockInfo.second

    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                onRegisterStockOrigin(coords.positionInRoot())
            }
        }
    ) {
        val canRecycle = boardState.gameState.canRecycleStock
        if (topStock == null) {
            EmptyPileSlot(
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                colors = colors,
                showRefreshIcon = canRecycle,
                isHighlighted = isHinted && canRecycle,
                contentDescriptionText = if (canRecycle) "Empty stock pile, tap to recycle waste" else "Empty stock pile, no passes remaining",
                onClick = if (canRecycle) { { onIntent(KlondikeIntent.DrawOrRecycle) } } else null
            )
        } else {
            PlayingCardView(
                card = topStock,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                colors = colors,
                isHinted = isHinted,
                textMeasurer = textMeasurer,
                onClick = { onIntent(KlondikeIntent.DrawOrRecycle) }
            )
            /*Surface(
                color = Color(0xAA000000),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
            ) {
                Text(
                    text = visibleStockCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }*/
        }

        /*val deckPasses = boardState.gameState.deckPasses
        if (deckPasses.maxPasses > 0) {
            val passesLeft = boardState.gameState.remainingPasses
            Surface(
                color = if (passesLeft > 0) Color(0xCC1E293B) else Color(0xCCE11D48),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
            ) {
                Text(
                    text = if (passesLeft > 0) "P:$passesLeft" else "0 left",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }*/
    }
}
