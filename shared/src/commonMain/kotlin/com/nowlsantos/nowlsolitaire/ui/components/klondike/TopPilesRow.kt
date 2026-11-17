package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
internal fun TopPilesRow(
    boardState: KlondikeBoardState,
    cardWidth: Dp,
    cardHeight: Dp,
    horizontalGap: Dp,
    colors: TableColors,
    textMeasurer: TextMeasurer,
    activeDragState: State<ActiveDragState?>,
    getFlyingHiddenState: (String) -> MutableState<Boolean>,
    hoveredTarget: PileLocation?,
    onUpdateTargetBounds: (PileLocation, Rect) -> Unit,
    onRegisterStockOrigin: (Offset) -> Unit,
    onRegisterCardOrigin: (String, Offset) -> Unit,
    onStartDrag: (PileLocation, Int, List<Card>, Offset?) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onIntent: (KlondikeIntent) -> Unit
) {
    val state = boardState.gameState
    val waste = state.waste
    val visibleCount = if (waste.isEmpty()) 0 else state.wasteVisibleCount.coerceIn(1, minOf(3, waste.size))
    val fanStep = cardWidth * 0.28f
    val extraFanWidth = if (visibleCount > 1) fanStep * (visibleCount - 1) else 0.dp
    val spacerWidth = (cardWidth - extraFanWidth).coerceAtLeast(0.dp)

    val stockSlot: @Composable () -> Unit = {
        StockPile(
            boardState = boardState,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            colors = colors,
            textMeasurer = textMeasurer,
            getFlyingHiddenState = getFlyingHiddenState,
            onRegisterStockOrigin = onRegisterStockOrigin,
            onIntent = onIntent
        )
    }

    val wasteSlot: @Composable () -> Unit = {
        WastePile(
            boardState = boardState,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            colors = colors,
            textMeasurer = textMeasurer,
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

    val foundationSlot: @Composable (Int) -> Unit = { index ->
        FoundationPile(
            index = index,
            boardState = boardState,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            colors = colors,
            textMeasurer = textMeasurer,
            activeDragState = activeDragState,
            getFlyingHiddenState = getFlyingHiddenState,
            hoveredTarget = hoveredTarget,
            onUpdateTargetBounds = onUpdateTargetBounds,
            onRegisterCardOrigin = onRegisterCardOrigin,
            onStartDrag = onStartDrag,
            onDragDelta = onDragDelta,
            onEndDrag = onEndDrag,
            onCancelDrag = onCancelDrag,
            onIntent = onIntent
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
        verticalAlignment = Alignment.Top
    ) {
        if (boardState.leftHanded) {
            for (index in 0 until GameState.FOUNDATION_COUNT) {
                foundationSlot(index)
            }
            Spacer(modifier = Modifier.width(spacerWidth))
            wasteSlot()
            stockSlot()
        } else {
            stockSlot()
            wasteSlot()
            Spacer(modifier = Modifier.width(spacerWidth))
            for (index in 0 until GameState.FOUNDATION_COUNT) {
                foundationSlot(index)
            }
        }
    }
}
