package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.ui.components.CardBorderWidth
import com.nowlsantos.nowlsolitaire.ui.components.CardCornerRadius
import com.nowlsantos.nowlsolitaire.ui.components.drawCardOutline
import com.nowlsantos.nowlsolitaire.ui.components.getCardDrawableRes
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors
import org.jetbrains.compose.resources.imageResource
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val FlightCardShape = RoundedCornerShape(CardCornerRadius)

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun FlightOverlay(
    activeFlightsState: State<List<CardFlight>>,
    onFlyingCardsUpdated: (List<CardFlight>) -> Unit,
    onFlightsUpdated: (List<CardFlight>) -> Unit,
    boardOriginHolder: OffsetHolder,
    textMeasurer: TextMeasurer,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    cardBackTheme: CardBackTheme
) {
    val flights = activeFlightsState.value
    if (flights.isEmpty()) return

    LaunchedEffect(true) {
        var prevFrameNanos = -1L
        var tickCount = 0
        while (true) {
            val shouldContinue = withFrameNanos { frameNanos ->
                val currentFlights = activeFlightsState.value
                if (currentFlights.isEmpty()) return@withFrameNanos false

                val gapNanos = if (prevFrameNanos > 0L) frameNanos - prevFrameNanos else 0L
                val shouldRebaseDealStart = tickCount == 1 && gapNanos > 50_000_000L
                prevFrameNanos = frameNanos
                tickCount++

                var anyNewlyLanded = false
                var allFinished = true
                for (flight in currentFlights) {
                    if (flight.progressState.floatValue >= 1f) {
                        continue
                    }
                    if (flight.startTimeNanos < 0L) {
                        flight.startTimeNanos = frameNanos - 16_666_667L
                    } else if (shouldRebaseDealStart && flight.isDeal) {
                        flight.startTimeNanos += gapNanos - 16_666_667L
                    }
                    if (!flight.startOrigin.isSpecified || !flight.targetOrigin.isSpecified) {
                        val waitingMs = ((frameNanos - flight.startTimeNanos) / 1_000_000f)
                        val maxWaitMs = if (flight.isDeal) 800f else 300f
                        if (waitingMs > maxWaitMs) {
                            flight.progressState.floatValue = 1f
                            anyNewlyLanded = true
                        } else {
                            flight.progressState.floatValue = 0f
                            allFinished = false
                        }
                        continue
                    }
                    val distance = (flight.targetOrigin - flight.startOrigin).getDistance()
                    if (flight.fromDragDrop && distance < 2f) {
                        flight.progressState.floatValue = 1f
                        anyNewlyLanded = true
                    } else {
                        val elapsedMs = ((frameNanos - flight.startTimeNanos) / 1_000_000f) - flight.delayMs
                        val progress = if (elapsedMs <= 0f) {
                            0f
                        } else {
                            (elapsedMs / flight.durationMs).coerceIn(0f, 1f)
                        }
                        flight.progressState.floatValue = progress
                        if (progress >= 1f) {
                            anyNewlyLanded = true
                        } else {
                            allFinished = false
                        }
                    }
                }

                if (allFinished) {
                    onFlightsUpdated(emptyList())
                    false
                } else {
                    if (anyNewlyLanded) {
                        onFlyingCardsUpdated(currentFlights)
                    }
                    true
                }
            }
            if (!shouldContinue) break
        }
    }

    val density = LocalDensity.current
    val canvasFlights = remember(flights) {
        flights.filter { it.isDeal || it.flipFromBack || it.flipToBack }
    }
    val composableFlights = remember(flights) {
        flights.filterNot { it.isDeal || it.flipFromBack || it.flipToBack }
    }

    canvasFlights.forEach { flight ->
        key(flight.id) {
            KlondikeFlipFlightCard(
                flight = flight,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                cardBackTheme = cardBackTheme,
                boardOriginHolder = boardOriginHolder
            )
        }
    }

    composableFlights.forEach { flight ->
        key(flight.id) {
            val stepDp = with(density) { flight.stackStepPx.toDp() }
            Box(
                modifier = Modifier.graphicsLayer {
                    val rawT = flight.progressState.floatValue
                    val waitingForStagger = flight.delayMs > 0f && rawT <= 0f
                    if (!flight.startOrigin.isSpecified ||
                        !flight.targetOrigin.isSpecified ||
                        waitingForStagger ||
                        rawT >= 1f
                    ) {
                        alpha = 0f
                        return@graphicsLayer
                    }
                    alpha = 1f
                    val easedT = FastOutSlowInEasing.transform(rawT)
                    val currentOrigin = lerp(flight.startOrigin, flight.targetOrigin, easedT)
                    val localOffset = currentOrigin - boardOriginHolder.value
                    translationX = localOffset.x
                    translationY = localOffset.y
                    val scale = when {
                        flight.fromDragDrop -> 1.03f - (0.03f * easedT)
                        flight.isDeal -> 1f + 0.04f * sin(easedT * PI).toFloat()
                        else -> 1f + 0.06f * sin(easedT * PI).toFloat()
                    }
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                flight.cards.forEachIndexed { index, card ->
                    key(card.id) {
                        val bitmap = imageResource(getCardDrawableRes(card))
                        Canvas(
                            modifier = Modifier
                                .offset(y = stepDp * index)
                                .size(cardWidth, cardHeight)
                                .graphicsLayer {
                                    shadowElevation = 6.dp.toPx()
                                    shape = FlightCardShape
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
                                    color = colors.cardBorder,
                                    strokeWidthDp = CardBorderWidth,
                                    cornerDp = CardCornerRadius
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
