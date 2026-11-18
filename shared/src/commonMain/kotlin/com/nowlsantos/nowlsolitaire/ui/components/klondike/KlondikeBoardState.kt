package com.nowlsantos.nowlsolitaire.ui.components.klondike

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.GameState
import com.nowlsantos.nowlsolitaire.domain.model.HintMove
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme
import com.nowlsantos.nowlsolitaire.ui.state.CardSelection
import kotlinx.coroutines.Job

@Immutable
data class KlondikeBoardState(
    val gameState: GameState,
    val selection: CardSelection?,
    val activeHint: HintMove?,
    val isAutoCompleting: Boolean,
    val tableTheme: TableTheme,
    val leftHanded: Boolean,
    val cardBackTheme: CardBackTheme = CardBackTheme.CLASSIC,
    val dealId: Long = 0L
) {
    val isWon: Boolean
        get() = gameState.isWon
}

internal class OffsetHolder(var value: Offset = Offset.Zero)
internal class NullableOffsetHolder(var value: Offset? = null)
internal class BooleanHolder(var value: Boolean = false)
internal class JobHolder(var value: Job? = null)
internal class IntHolder(var value: Int = 0)
internal class LongHolder(var value: Long = -1L)
internal class GameStateHolder(var value: GameState? = null)
internal class DragDropHolder(var value: DragDropSnapshot? = null)

internal class ColumnOriginRegistrationHolder {
    var colTopLeft: Offset = Offset.Unspecified
    var cards: List<Card>? = null
    var offsets: List<Dp>? = null
}

internal val DealPositionEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

internal data class DragDropSnapshot(
    val headCardId: String,
    val releaseOrigin: Offset,
    val stackStepPx: Float
)

internal data class CardSlotInfo(
    val location: PileLocation,
    val card: Card
)

internal class CardFlight(
    val id: String,
    val cards: List<Card>,
    var startOrigin: Offset,
    var targetOrigin: Offset,
    val stackStepPx: Float,
    val durationMs: Float,
    val fromDragDrop: Boolean,
    val delayMs: Float = 0f,
    val flipFromBack: Boolean = false,
    val flipToBack: Boolean = false,
    val startFromStock: Boolean = false,
    val endAtStock: Boolean = false,
    val isDeal: Boolean = false,
    var startTimeNanos: Long = -1L,
    val progressState: MutableFloatState = mutableFloatStateOf(0f)
)

internal data class ActiveDragState(
    val from: PileLocation,
    val cardIndex: Int,
    val cards: List<Card>,
    val originInRoot: Offset,
    val cardWidthPx: Float,
    val cardHeightPx: Float,
    val stackStepPx: Float
)
