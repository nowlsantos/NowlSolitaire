package com.nowlsantos.nowlsolitaire.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowlsantos.nowlsolitaire.data.KlondikeRepository
import com.nowlsantos.nowlsolitaire.domain.engine.DeckDealer
import com.nowlsantos.nowlsolitaire.domain.engine.KlondikeEngine
import com.nowlsantos.nowlsolitaire.domain.engine.KlondikeRules
import com.nowlsantos.nowlsolitaire.domain.engine.KlondikeSession
import com.nowlsantos.nowlsolitaire.domain.model.DealMode
import com.nowlsantos.nowlsolitaire.domain.model.DeckPasses
import com.nowlsantos.nowlsolitaire.domain.model.DrawMode
import com.nowlsantos.nowlsolitaire.domain.model.GameSettings
import com.nowlsantos.nowlsolitaire.domain.model.PileLocation
import com.nowlsantos.nowlsolitaire.domain.model.SavedGameSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class KlondikeViewModel(
    private val repository: KlondikeRepository,
    private val deckDealer: DeckDealer,
    private val engine: KlondikeEngine,
    private val rules: KlondikeRules
) : ViewModel() {

    internal var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val _uiState = MutableStateFlow(KlondikeUiState())
    val uiState: StateFlow<KlondikeUiState> = _uiState.asStateFlow()

    private var session: KlondikeSession = KlondikeSession.newGame()
    private var hasStartedMoving = false
    private var recordedAsPlayed = false
    private var recordedAsWon = false
    private var isTimerPaused = false

    private var autoCompleteJob: Job? = null
    private var deadlockJob: Job? = null
    private val persistChannel = Channel<SavedGameSnapshot?>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            for (snapshot in persistChannel) {
                repository.saveGame(snapshot)
            }
        }
        observePreferencesAndStats()
        initializeGame()
        startTimerLoop()
    }

    fun onIntent(intent: KlondikeIntent) {
        when (intent) {
            KlondikeIntent.NewGame -> startNewGame()
            KlondikeIntent.ReplayDeal -> replayCurrentDeal()
            KlondikeIntent.DrawOrRecycle -> drawOrRecycle()
            KlondikeIntent.Undo -> undoMove()
            KlondikeIntent.RequestHint -> requestHint()
            KlondikeIntent.ClearHint -> clearHint()
            KlondikeIntent.DismissNoMovesDialog -> _uiState.update { it.copy(showNoMovesDialog = false) }
            KlondikeIntent.ClearHintMessage -> _uiState.update { it.copy(hintMessage = null) }
            KlondikeIntent.RunAutoComplete -> runAutoComplete()
            is KlondikeIntent.TapPileOrCard -> handleTap(intent.location, intent.cardIndex)
            is KlondikeIntent.MoveCards -> moveCards(intent.from, intent.cardIndex, intent.to)
            is KlondikeIntent.SetTimerPaused -> {
                isTimerPaused = intent.paused
                if (intent.paused && hasStartedMoving && !_uiState.value.isWon) {
                    persistCurrentGame()
                }
            }
            is KlondikeIntent.UpdateDrawMode -> updateDrawMode(intent.drawMode)
            is KlondikeIntent.UpdateDeckPasses -> updateDeckPasses(intent.deckPasses)
            is KlondikeIntent.UpdateDealMode -> updateDealMode(intent.dealMode)
            is KlondikeIntent.UpdateTableTheme -> updateSettings { it.copy(tableTheme = intent.theme) }
            is KlondikeIntent.UpdateCardBackTheme -> updateSettings { it.copy(cardBackTheme = intent.cardBackTheme) }
            is KlondikeIntent.UpdateLeftHanded -> updateSettings { it.copy(leftHanded = intent.leftHanded) }
            is KlondikeIntent.UpdateTapToAutoMove -> updateSettings { it.copy(tapToAutoMove = intent.enabled) }
            is KlondikeIntent.UpdateShowTimer -> updateSettings { it.copy(showTimer = intent.enabled) }
            KlondikeIntent.ResetStats -> viewModelScope.launch { repository.resetStats() }
        }
    }

    private fun observePreferencesAndStats() {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            repository.statsFlow.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    private fun initializeGame() {
        viewModelScope.launch {
            val saved = repository.loadSavedGame()
            val settings = repository.settingsFlow.first()

            if (saved != null && !saved.gameState.isWon) {
                session = KlondikeSession(
                    currentState = saved.gameState,
                    undoStack = saved.undoStack
                )
                hasStartedMoving = saved.hasStartedMoving
                recordedAsPlayed = saved.recordedAsPlayed
                recordedAsWon = saved.gameState.isWon
                val snapshot = session.currentState
                val isDeadlocked = hasStartedMoving && withContext(defaultDispatcher) {
                    !engine.hasAvailableMoves(snapshot)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameState = session.currentState,
                        canUndo = session.canUndo,
                        elapsedSeconds = saved.elapsedSeconds,
                        settings = settings,
                        showNoMovesDialog = isDeadlocked
                    )
                }
            } else {
                val fresh = deckDealer.dealNewGame(
                    drawMode = settings.drawMode,
                    deckPasses = settings.deckPasses,
                    dealMode = settings.dealMode
                )
                session = KlondikeSession(currentState = fresh)
                hasStartedMoving = false
                recordedAsPlayed = false
                recordedAsWon = false
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameState = session.currentState,
                        canUndo = false,
                        elapsedSeconds = 0,
                        settings = settings
                    )
                }
                persistCurrentGame()
            }
        }
    }

    private fun startTimerLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1.seconds)
                val state = _uiState.value
                if (!state.isLoading &&
                    hasStartedMoving &&
                    !isTimerPaused &&
                    !state.isWon &&
                    !state.isAutoCompleting
                ) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    private fun startNewGame(
        drawModeOverride: DrawMode? = null,
        deckPassesOverride: DeckPasses? = null,
        dealModeOverride: DealMode? = null
    ) {
        autoCompleteJob?.cancel()
        deadlockJob?.cancel()
        val drawMode = drawModeOverride ?: _uiState.value.settings.drawMode
        val deckPasses = deckPassesOverride ?: _uiState.value.settings.deckPasses
        val dealMode = dealModeOverride ?: _uiState.value.settings.dealMode
        session = KlondikeSession(
            currentState = deckDealer.dealNewGame(
                drawMode = drawMode,
                deckPasses = deckPasses,
                dealMode = dealMode
            )
        )
        hasStartedMoving = false
        recordedAsPlayed = false
        recordedAsWon = false
        _uiState.update {
            it.copy(
                gameState = session.currentState,
                canUndo = false,
                elapsedSeconds = 0,
                selection = null,
                activeHint = null,
                isAutoCompleting = false,
                showNoMovesDialog = false,
                hintMessage = null
            )
        }
        persistCurrentGame()
    }

    private fun replayCurrentDeal() {
        autoCompleteJob?.cancel()
        deadlockJob?.cancel()
        session = KlondikeSession(
            currentState = deckDealer.dealNewGame(
                drawMode = session.currentState.drawMode,
                deckPasses = session.currentState.deckPasses,
                seed = session.currentState.seed
            )
        )
        hasStartedMoving = false
        recordedAsPlayed = false
        recordedAsWon = false
        _uiState.update {
            it.copy(
                gameState = session.currentState,
                canUndo = false,
                elapsedSeconds = 0,
                selection = null,
                activeHint = null,
                isAutoCompleting = false,
                showNoMovesDialog = false,
                hintMessage = null
            )
        }
        persistCurrentGame()
    }

    private fun drawOrRecycle() {
        if (_uiState.value.isAutoCompleting || _uiState.value.isWon) return
        val updated = session.drawOrRecycle()
        if (updated === session) {
            scheduleDeadlockCheck()
            return
        }
        applyNewSession(updated)
    }

    private fun undoMove() {
        if (_uiState.value.isAutoCompleting || !session.canUndo) return
        deadlockJob?.cancel()
        session = session.undo()
        _uiState.update {
            it.copy(
                gameState = session.currentState,
                canUndo = session.canUndo,
                selection = null,
                activeHint = null,
                showNoMovesDialog = false,
                hintMessage = null
            )
        }
        persistCurrentGame()
    }

    private fun requestHint() {
        if (_uiState.value.isAutoCompleting || _uiState.value.isWon) return
        val snapshot = session.currentState
        viewModelScope.launch {
            val hint = withContext(defaultDispatcher) {
                engine.findHint(snapshot)
            }
            if (!isActive ||
                session.currentState !== snapshot ||
                _uiState.value.isAutoCompleting ||
                snapshot.isWon
            ) {
                return@launch
            }
            if (hint != null) {
                val message = if (hint.from == PileLocation.Stock) {
                    if (snapshot.stock.isNotEmpty()) {
                        "Draw from the stock pile to reach a playable card"
                    } else {
                        "Recycle the waste pile to reach a playable card"
                    }
                } else null
                _uiState.update {
                    it.copy(
                        activeHint = hint,
                        selection = null,
                        showNoMovesDialog = false,
                        hintMessage = message
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        activeHint = null,
                        selection = null,
                        showNoMovesDialog = true
                    )
                }
            }
        }
    }

    private fun clearHint() {
        _uiState.update { it.copy(activeHint = null) }
    }

    private fun handleTap(location: PileLocation, cardIndex: Int?) {
        if (_uiState.value.isAutoCompleting || _uiState.value.isWon) return

        if (location == PileLocation.Stock) {
            drawOrRecycle()
            return
        }

        val currentSelection = _uiState.value.selection
        if (currentSelection != null) {
            val isSameCard = currentSelection.location == location &&
                currentSelection.cardIndex == cardIndex
            if (isSameCard) {
                // Second tap on the selected card tries auto-move, or deselects if none.
                val autoMoved = session.autoMove(currentSelection.location, currentSelection.cardIndex)
                if (autoMoved !== session) {
                    applyNewSession(autoMoved)
                } else {
                    _uiState.update { it.copy(selection = null) }
                }
                return
            }

            // Attempt move from selected source to tapped destination pile.
            val moved = session.moveCards(currentSelection.location, currentSelection.cardIndex, location)
            if (moved !== session) {
                applyNewSession(moved)
                return
            }

            // If invalid move from previous selection to this destination:
            // Check if the newly tapped card itself can be auto-moved!
            val newSelectable = resolveSelectableIndex(location, cardIndex)
            if (newSelectable != null && _uiState.value.settings.tapToAutoMove) {
                val autoMoved = session.autoMove(location, newSelectable)
                if (autoMoved !== session) {
                    applyNewSession(autoMoved)
                    return
                }
            }

            // If it cannot auto-move, either switch selection to tapped playable card or clear selection.
            _uiState.update {
                it.copy(
                    selection = newSelectable?.let { idx -> CardSelection(location, idx) },
                    activeHint = null
                )
            }
            return
        }

        val selectableIndex = resolveSelectableIndex(location, cardIndex) ?: return

        if (_uiState.value.settings.tapToAutoMove) {
            val autoMoved = session.autoMove(location, selectableIndex)
            if (autoMoved !== session) {
                applyNewSession(autoMoved)
                return
            }
        }

        _uiState.update {
            it.copy(
                selection = CardSelection(location, selectableIndex),
                activeHint = null
            )
        }
    }

    private fun moveCards(from: PileLocation, cardIndex: Int, to: PileLocation) {
        if (_uiState.value.isAutoCompleting || _uiState.value.isWon) return
        val updated = session.moveCards(from, cardIndex, to)
        if (updated === session) return
        applyNewSession(updated)
    }

    private fun resolveSelectableIndex(location: PileLocation, requestedIndex: Int?): Int? {
        val state = session.currentState
        return when (location) {
            PileLocation.Stock -> null
            PileLocation.Waste -> state.waste.lastIndex.takeIf { it >= 0 }
            is PileLocation.Foundation -> {
                val pile = state.foundations.getOrNull(location.index) ?: return null
                pile.lastIndex.takeIf { it >= 0 }
            }
            is PileLocation.Tableau -> {
                val column = state.tableau.getOrNull(location.column) ?: return null
                if (column.isEmpty()) return null
                val index = requestedIndex ?: column.lastIndex
                if (rules.extractMovingCards(state, location, index) != null) index else null
            }
        }
    }

    private fun runAutoComplete() {
        if (!_uiState.value.isAutoCompleteAvailable || _uiState.value.isAutoCompleting) return
        autoCompleteJob?.cancel()
        _uiState.update {
            it.copy(
                isAutoCompleting = true,
                selection = null,
                activeHint = null
            )
        }

        autoCompleteJob = viewModelScope.launch {
            try {
                while (isActive && !session.currentState.isWon) {
                    val next = session.stepAutoComplete()
                    if (next === session) break
                    session = next
                    _uiState.update {
                        it.copy(
                            gameState = session.currentState,
                            canUndo = session.canUndo
                        )
                    }
                    delay(110.milliseconds)
                }
                if (session.currentState.isWon) {
                    delay(300.milliseconds)
                }
            } finally {
                _uiState.update { it.copy(isAutoCompleting = false) }
                onStateChangedCheckWin()
                persistCurrentGame()
            }
        }
    }

    private fun applyNewSession(updated: KlondikeSession) {
        session = updated
        if (!hasStartedMoving) {
            hasStartedMoving = true
        }
        if (!recordedAsPlayed) {
            recordedAsPlayed = true
            viewModelScope.launch { repository.recordGamePlayed() }
        }

        _uiState.update {
            it.copy(
                gameState = session.currentState,
                canUndo = session.canUndo,
                selection = null,
                activeHint = null,
                showNoMovesDialog = false
            )
        }
        onStateChangedCheckWin()
        persistCurrentGame()

        // Check deadlock in the background without blocking the UI thread!
        scheduleDeadlockCheck()
    }

    private fun scheduleDeadlockCheck() {
        deadlockJob?.cancel()
        val snapshot = session.currentState
        if (snapshot.isWon || _uiState.value.isAutoCompleting) return
        deadlockJob = viewModelScope.launch {
            val hasMoves = withContext(defaultDispatcher) {
                engine.hasAvailableMoves(snapshot)
            }
            if (!hasMoves &&
                isActive &&
                session.currentState === snapshot &&
                !_uiState.value.isAutoCompleting &&
                !snapshot.isWon
            ) {
                _uiState.update { it.copy(showNoMovesDialog = true) }
            }
        }
    }

    private fun onStateChangedCheckWin() {
        if (session.currentState.isWon && !recordedAsWon) {
            recordedAsWon = true
            val score = session.currentState.score
            val elapsed = _uiState.value.elapsedSeconds
            viewModelScope.launch {
                repository.recordGameWon(score = score, elapsedSeconds = elapsed)
            }
            persistChannel.trySend(null)
        }
    }

    private fun updateDrawMode(drawMode: DrawMode) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(drawMode = drawMode) }
            if (!hasStartedMoving) {
                startNewGame(drawModeOverride = drawMode)
            }
        }
    }

    private fun updateDeckPasses(deckPasses: DeckPasses) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(deckPasses = deckPasses) }
            if (!hasStartedMoving) {
                startNewGame(deckPassesOverride = deckPasses)
            } else {
                val updatedState = session.currentState.copy(deckPasses = deckPasses)
                session = session.copy(currentState = updatedState)
                _uiState.update { it.copy(gameState = updatedState, showNoMovesDialog = false) }
                persistCurrentGame()
                scheduleDeadlockCheck()
            }
        }
    }

    private fun updateDealMode(dealMode: DealMode) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(dealMode = dealMode) }
            if (!hasStartedMoving) {
                startNewGame(dealModeOverride = dealMode)
            }
        }
    }

    private fun updateSettings(transform: (GameSettings) -> GameSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
        }
    }

    private fun persistCurrentGame() {
        if (session.currentState.isWon) {
            persistChannel.trySend(null)
            return
        }
        val snapshot = SavedGameSnapshot(
            gameState = session.currentState,
            undoStack = session.undoStack,
            elapsedSeconds = _uiState.value.elapsedSeconds,
            hasStartedMoving = hasStartedMoving,
            recordedAsPlayed = recordedAsPlayed
        )
        persistChannel.trySend(snapshot)
    }

    override fun onCleared() {
        autoCompleteJob?.cancel()
        deadlockJob?.cancel()
        persistChannel.close()
    }
}
