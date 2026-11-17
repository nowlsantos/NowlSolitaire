package com.nowlsantos.nowlsolitaire.ui.components.pyramid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import kotlinx.coroutines.isActive

@Composable
internal fun PyramidFlightOverlay(
    activeFlightsState: State<List<PyramidFlight>>,
    cardWidth: Dp,
    cardHeight: Dp,
    cardBackTheme: CardBackTheme,
    boardOriginHolder: PyramidOffsetHolder,
    onFlyingCardsUpdated: (List<PyramidFlight>) -> Unit,
    onAllFlightsFinished: () -> Unit
) {
    val flights = activeFlightsState.value
    if (flights.isEmpty()) return

    LaunchedEffect(true) {
        var prevFrameNanos = -1L
        var tickCount = 0
        while (isActive) {
            val hasActive = withFrameNanos { frameNanos ->
                val currentFlights = activeFlightsState.value
                if (currentFlights.isEmpty()) return@withFrameNanos false

                val gapNanos = if (prevFrameNanos > 0L) frameNanos - prevFrameNanos else 0L
                val shouldRebaseDealStart = tickCount == 1 && gapNanos > 50_000_000L
                prevFrameNanos = frameNanos
                tickCount++

                var anyNewlyLanded = false
                var allFinished = true
                for (i in currentFlights.indices) {
                    val flight = currentFlights[i]
                    if (flight.progressState.floatValue >= 1f) continue

                    if (flight.startTimeNanos < 0L) {
                        flight.startTimeNanos = frameNanos - 16_666_667L
                    } else if (shouldRebaseDealStart && flight.isDeal) {
                        flight.startTimeNanos += gapNanos - 16_666_667L
                    }
                    val elapsedMs = ((frameNanos - flight.startTimeNanos) / 1_000_000f) - flight.delayMs
                    val progress = if (elapsedMs <= 0f) 0f else (elapsedMs / flight.durationMs).coerceIn(0f, 1f)
                    flight.progressState.floatValue = progress

                    if (progress >= 1f) {
                        anyNewlyLanded = true
                    } else {
                        allFinished = false
                    }
                }

                if (anyNewlyLanded) {
                    onFlyingCardsUpdated(currentFlights)
                }

                if (allFinished) {
                    onAllFlightsFinished()
                    false
                } else {
                    true
                }
            }
            if (!hasActive) break
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        for (i in flights.indices) {
            val flight = flights[i]
            key(flight.id) {
                PyramidFlightCard(
                    flight = flight,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    cardBackTheme = cardBackTheme,
                    boardOriginHolder = boardOriginHolder
                )
            }
        }
    }
}
