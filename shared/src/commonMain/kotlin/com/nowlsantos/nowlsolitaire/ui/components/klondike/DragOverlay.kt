package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.nowlsantos.nowlsolitaire.ui.components.PlayingCardView
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors
import kotlin.math.roundToInt

@Composable
internal fun DragOverlay(
    activeDragState: State<ActiveDragState?>,
    dragOffsetState: State<Offset>,
    returnProgressState: State<Float>,
    boardOriginHolder: OffsetHolder,
    textMeasurer: TextMeasurer,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors
) {
    val drag = activeDragState.value ?: return
    val density = LocalDensity.current
    val baseOffset = drag.originInRoot - boardOriginHolder.value
    val stepDp = with(density) { drag.stackStepPx.toDp() }
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = baseOffset.x.roundToInt(),
                    y = baseOffset.y.roundToInt()
                )
            }
            .graphicsLayer {
                val liveOffset = dragOffsetState.value
                translationX = liveOffset.x
                translationY = liveOffset.y
                val scale = 1.03f - (0.03f * returnProgressState.value)
                scaleX = scale
                scaleY = scale
            }
    ) {
        drag.cards.forEachIndexed { index, card ->
            PlayingCardView(
                card = card,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                colors = colors,
                isSelected = true,
                isElevated = true,
                textMeasurer = textMeasurer,
                modifier = Modifier.offset(y = stepDp * index)
            )
        }
    }
}
