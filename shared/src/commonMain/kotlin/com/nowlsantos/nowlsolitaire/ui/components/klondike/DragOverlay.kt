package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.ui.components.CardCornerRadius
import com.nowlsantos.nowlsolitaire.ui.components.drawCardOutline
import com.nowlsantos.nowlsolitaire.ui.components.getCardDrawableRes
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors
import org.jetbrains.compose.resources.imageResource
import kotlin.math.roundToInt

private val DragCardShape = RoundedCornerShape(CardCornerRadius)

@Suppress("UNUSED_PARAMETER")
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
            key(card.id) {
                val bitmap = imageResource(getCardDrawableRes(card))
                Canvas(
                    modifier = Modifier
                        .offset(y = stepDp * index)
                        .size(cardWidth, cardHeight)
                        .graphicsLayer {
                            shadowElevation = 6.dp.toPx()
                            shape = DragCardShape
                            clip = true
                        }
                ) {
                    val w = size.width.roundToInt()
                    val h = size.height.roundToInt()
                    if (w > 0 && h > 0) {
                        drawImage(
                            image = bitmap,
                            dstSize = IntSize(w, h)
                        )
                        drawCardOutline(
                            color = colors.selectionGlow,
                            strokeWidthDp = 2.2.dp,
                            cornerDp = CardCornerRadius
                        )
                    }
                }
            }
        }
    }
}
