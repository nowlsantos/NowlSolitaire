package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.ui.components.getCardBackDrawableRes
import com.nowlsantos.nowlsolitaire.ui.components.getCardDrawableRes
import org.jetbrains.compose.resources.imageResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private val KlondikeFlightCardShape = RoundedCornerShape(6.dp)

@Composable
internal fun KlondikeFlipFlightCard(
    flight: CardFlight,
    cardWidth: Dp,
    cardHeight: Dp,
    cardBackTheme: CardBackTheme,
    boardOriginHolder: OffsetHolder
) {
    val frontCard = flight.cards.firstOrNull()
    val hasFlip = flight.flipFromBack || flight.flipToBack
    val needsFront = hasFlip || frontCard?.isFaceUp == true
    val needsBack = hasFlip || frontCard?.isFaceUp != true

    val backBitmap = if (needsBack) {
        imageResource(getCardBackDrawableRes(cardBackTheme))
    } else {
        null
    }
    val frontBitmap = if (needsFront && frontCard != null) {
        imageResource(getCardDrawableRes(frontCard))
    } else {
        null
    }
    val density = LocalDensity.current
    val staticShowFront = frontCard?.isFaceUp ?: false

    val showFrontState = remember(flight) {
        if (!hasFlip) {
            null
        } else {
            derivedStateOf {
                val rawT = flight.progressState.floatValue
                val flipAngle = when {
                    flight.flipFromBack -> {
                        val flipRaw = if (flight.isDeal) {
                            ((rawT - 0.20f) / 0.72f).coerceIn(0f, 1f)
                        } else {
                            rawT
                        }
                        FastOutSlowInEasing.transform(flipRaw) * 180f
                    }
                    flight.flipToBack -> FastOutSlowInEasing.transform(rawT) * 180f
                    else -> 0f
                }
                when {
                    flight.flipFromBack -> flipAngle > 90f
                    flight.flipToBack -> flipAngle <= 90f
                    else -> staticShowFront
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(cardWidth, cardHeight)
            .graphicsLayer {
                val rawT = flight.progressState.floatValue
                if (rawT <= 0f || rawT >= 1f ||
                    !flight.startOrigin.isSpecified ||
                    !flight.targetOrigin.isSpecified
                ) {
                    alpha = 0f
                    return@graphicsLayer
                }
                alpha = 1f
                val easedT = if (flight.isDeal) {
                    DealPositionEasing.transform(rawT)
                } else {
                    FastOutSlowInEasing.transform(rawT)
                }
                val currentOrigin = lerp(flight.startOrigin, flight.targetOrigin, easedT)
                val boardOrigin = boardOriginHolder.value
                translationX = currentOrigin.x - boardOrigin.x
                translationY = currentOrigin.y - boardOrigin.y

                val arcBoost = sin(easedT * PI).toFloat()
                val scale = if (flight.isDeal) {
                    1f + 0.035f * arcBoost
                } else {
                    1f + 0.06f * arcBoost
                }
                scaleX = scale
                scaleY = scale

                if (hasFlip) {
                    val flipAngle = when {
                        flight.flipFromBack -> {
                            val flipRaw = if (flight.isDeal) {
                                ((rawT - 0.20f) / 0.72f).coerceIn(0f, 1f)
                            } else {
                                rawT
                            }
                            FastOutSlowInEasing.transform(flipRaw) * 180f
                        }
                        flight.flipToBack -> FastOutSlowInEasing.transform(rawT) * 180f
                        else -> 0f
                    }
                    val showFront = when {
                        flight.flipFromBack -> flipAngle > 90f
                        flight.flipToBack -> flipAngle <= 90f
                        else -> staticShowFront
                    }
                    val drawAngle = when {
                        flight.flipFromBack -> if (showFront) flipAngle - 180f else flipAngle
                        flight.flipToBack -> if (showFront) -flipAngle else 180f - flipAngle
                        else -> 0f
                    }
                    if (abs(drawAngle) > 0.5f) {
                        rotationY = drawAngle
                        cameraDistance = 12f * density.density
                    } else {
                        rotationY = 0f
                    }
                } else {
                    rotationY = 0f
                }

                if (!flight.isDeal) {
                    shadowElevation = 4.dp.toPx()
                    shape = KlondikeFlightCardShape
                    clip = false
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val showFront = showFrontState?.value ?: staticShowFront
            val bitmapToDraw = if (showFront) {
                frontBitmap ?: backBitmap
            } else {
                backBitmap ?: frontBitmap
            } ?: return@Canvas
            val w = size.width.roundToInt()
            val h = size.height.roundToInt()
            if (w > 0 && h > 0) {
                drawImage(
                    image = bitmapToDraw,
                    dstSize = IntSize(w, h)
                )
            }
        }
    }
}
