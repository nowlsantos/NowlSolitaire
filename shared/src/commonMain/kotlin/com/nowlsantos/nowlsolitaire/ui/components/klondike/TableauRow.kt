package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
internal fun TableauRow(
    boardState: KlondikeBoardState,
    dealMountLimitState: State<Int>,
    cardWidth: Dp,
    cardHeight: Dp,
    horizontalGap: Dp,
    availableHeight: Dp,
    colors: TableColors,
    textMeasurer: TextMeasurer,
    activeDragState: State<ActiveDragState?>,
    getFlyingHiddenState: (String) -> MutableState<Boolean>,
    hoveredTargetState: State<PileLocation?>,
    onUpdateTargetBounds: (PileLocation, Rect) -> Unit,
    onRegisterCardOrigin: (String, Offset) -> Unit,
    onStartDrag: (PileLocation, Int, List<Card>, Offset?, Float) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onIntent: (KlondikeIntent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalGap)
    ) {
        var mountStartIndex = 0
        for (columnIndex in 0 until GameState.TABLEAU_COUNT) {
            val location = PileLocation.Tableau(columnIndex)
            val cards = boardState.gameState.tableau.getOrElse(columnIndex) { emptyList() }
            val isHintTarget = boardState.activeHint?.to == location
            val columnMountOffset = mountStartIndex
            mountStartIndex += cards.size

            key(columnIndex) {
                TableauColumn(
                    location = location,
                    columnIndex = columnIndex,
                    cards = cards,
                    mountStartIndex = columnMountOffset,
                    dealMountLimitState = dealMountLimitState,
                    selection = boardState.selection,
                    activeHint = boardState.activeHint,
                    isHintTarget = isHintTarget,
                    hoveredTargetState = hoveredTargetState,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    availableHeight = availableHeight,
                    colors = colors,
                    textMeasurer = textMeasurer,
                    activeDragState = activeDragState,
                    getFlyingHiddenState = getFlyingHiddenState,
                    onUpdateTargetBounds = onUpdateTargetBounds,
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
