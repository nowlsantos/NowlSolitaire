package com.nowlsantos.nowlsolitaire.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowlsantos.nowlsolitaire.data.PyramidRepository
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidEngine
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidInteractionResult
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidRules
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidSession
import com.nowlsantos.nowlsolitaire.domain.model.DealMode
import com.nowlsantos.nowlsolitaire.domain.model.PyramidLocation
import com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession
import com.nowlsantos.nowlsolitaire.domain.model.PyramidSettings
import com.nowlsantos.nowlsolitaire.domain.model.SavedPyramidGameSnapshot
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

class PyramidViewModel(
    private val repository: PyramidRepository,
    private val engine: PyramidEngine,
    private val rules: PyramidRules
) : ViewModel() {

    internal var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val _uiState = MutableStateFlow(PyramidUiState())
    val uiState: StateFlow<PyramidUiState> = _uiState.asStateFlow()

    private var session: PyramidSession = PyramidSession.newGame()
    private var runSession: PyramidRunSession = PyramidRunSession()
    private var hasStartedMoving = false
    private var recordedAsPlayed = false
    private var recordedAsWon = false
    private var isTimerPaused = false
    private var internalElapsedSeconds = 0
    private var nextDealId = 1L

    private var deadlockJob: Job? = null
    private var persistDebounceJob: Job? = null
    private val persistChannel = Channel<SavedPyramidGameSnapshot?>(Channel.CONFLATED)

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

    fun onIntent(intent: PyramidIntent) {
        when (intent) {
            PyramidIntent.NewGame -> startNewGame()
            PyramidIntent.ReplayDeal -> replayCurrentDeal()
            PyramidIntent.NextRound -> nextRound()
            PyramidIntent.ContinueRunWithLostLife -> continueRunWithLostLife()
            PyramidIntent.StartNewRun -> startNewRun()
            PyramidIntent.DrawOrRecycle -> drawOrRecycle()
            PyramidIntent.Undo -> undoMove()
            PyramidIntent.RequestHint -> requestHint()
            PyramidIntent.ClearHint -> clearHint()
            PyramidIntent.DismissNoMovesDialog -> _uiState.update { it.copy(showNoMovesDialog = false) }
            PyramidIntent.ClearHintMessage -> _uiState.update { it.copy(hintMessage = null) }
            PyramidIntent.ClearActiveMatch -> _uiState.update { it.copy(activeMatch = null) }
            PyramidIntent.ClearActiveDrawCard -> _uiState.update { it.copy(activeDrawCard = null) }
            is PyramidIntent.TapCard -> handleTapCard(intent.location)
            is PyramidIntent.SetTimerPaused -> {
                isTimerPaused = intent.paused
                if (intent.paused && hasStartedMoving && !_uiState.value.isWon) {
                    persistCurrentGame(immediate = true)
                }
            }
            is PyramidIntent.UpdateTableTheme -> updateSettings { it.copy(tableTheme = intent.theme) }
            is PyramidIntent.UpdateCardBackTheme -> updateSettings { it.copy(cardBackTheme = intent.cardBackTheme) }
            is PyramidIntent.UpdateMaxRecycles -> updateMaxRecycles(intent.maxRecycles)
            is PyramidIntent.UpdateDealMode -> updateDealMode(intent.dealMode)
            is PyramidIntent.UpdateShowTimer -> {
                if (intent.enabled) {
                    _uiState.update { it.copy(elapsedSeconds = internalElapsedSeconds) }
                }
                updateSettings { it.copy(showTimer = intent.enabled) }
            }
            is PyramidIntent.UpdateLeftHanded -> updateSettings { it.copy(leftHanded = intent.leftHanded) }
            PyramidIntent.ResetStats -> viewModelScope.launch { repository.resetStats() }
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
            val loadedRun = repository.loadRunSession()
            val settings = repository.settingsFlow.first()

            runSession = if (saved != null && !saved.gameState.isWon) {
                saved.runSession
            } else {
                loadedRun
            }

            if (saved != null && !saved.gameState.isWon) {
                val normalizedState = if (saved.gameState.stock.any { !it.isFaceUp }) {
                    saved.gameState.copy(stock = saved.gameState.stock.map { it.copy(isFaceUp = true) })
                } else {
                    saved.gameState
                }
                val normalizedUndo = saved.undoStack.map { undoState ->
                    if (undoState.stock.any { !it.isFaceUp }) {
                        undoState.copy(stock = undoState.stock.map { it.copy(isFaceUp = true) })
                    } else {
                        undoState
                    }
                }
                session = PyramidSession(
                    currentState = normalizedState,
                    undoStack = normalizedUndo
                )
                hasStartedMoving = saved.hasStartedMoving
                recordedAsPlayed = saved.recordedAsPlayed
                recordedAsWon = saved.gameState.isWon
                internalElapsedSeconds = saved.elapsedSeconds
                val currentSnapshot = session.currentState
                val isDeadlocked = hasStartedMoving && withContext(defaultDispatcher) {
                    !rules.hasAnyMovesLeft(currentSnapshot) { !coroutineContext.isActive }
                }
                val isOngoing = saved.hasStartedMoving || saved.gameState.moveCount > 0
                val dealId = if (isOngoing) 0L else nextDealId++
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameState = session.currentState,
                        canUndo = session.canUndo,
                        elapsedSeconds = internalElapsedSeconds,
                        settings = settings,
                        runSession = runSession,
                        showNoMovesDialog = isDeadlocked,
                        dealId = dealId
                    )
                }
            } else {
                startFreshDeal(settings)
            }
        }
    }

    private fun startFreshDeal(settings: PyramidSettings) {
        deadlockJob?.cancel()
        val dealId = nextDealId++
        val effectiveRecycles = PyramidRunSession.getEffectiveMaxRecycles(runSession.currentRound, settings.maxRecycles)
        session = PyramidSession.newGame(
            maxRecycles = effectiveRecycles,
            dealMode = settings.dealMode
        )
        hasStartedMoving = false
        recordedAsPlayed = false
        recordedAsWon = false
        internalElapsedSeconds = 0
        _uiState.update {
            it.copy(
                isLoading = false,
                gameState = session.currentState,
                canUndo = false,
                elapsedSeconds = 0,
                activeHint = null,
                showNoMovesDialog = false,
                hintMessage = null,
                settings = settings,
                runSession = runSession,
                lastMatchedResult = null,
                activeMatch = null,
                activeDrawCard = null,
                dealId = dealId
            )
        }
        persistCurrentGame(immediate = true)
    }

    private fun startNewGame() {
        startFreshDeal(_uiState.value.settings)
    }

    private fun replayCurrentDeal() {
        deadlockJob?.cancel()
        val dealId = nextDealId++
        session = PyramidSession.replayDeal(session.currentState)
        hasStartedMoving = false
        recordedAsPlayed = false
        recordedAsWon = false
        internalElapsedSeconds = 0
        _uiState.update {
            it.copy(
                gameState = session.currentState,
                canUndo = false,
                elapsedSeconds = 0,
                activeHint = null,
                showNoMovesDialog = false,
                hintMessage = null,
                runSession = runSession,
                lastMatchedResult = null,
                activeMatch = null,
                activeDrawCard = null,
                dealId = dealId
            )
        }
        persistCurrentGame(immediate = true)
    }

    private fun nextRound() {
        val nextRoundNum = runSession.currentRound + 1
        runSession = runSession.copy(
            currentRound = nextRoundNum,
            bestRoundRecord = maxOf(runSession.bestRoundRecord, nextRoundNum)
        )
        viewModelScope.launch {
            repository.saveRunSession(runSession)
        }
        startFreshDeal(_uiState.value.settings)
    }

    private fun continueRunWithLostLife() {
        val remainingLives = (runSession.lives - 1).coerceAtLeast(0)
        val isGameOver = remainingLives <= 0
        val sessionWithLoss = runSession.copy(
            lives = remainingLives,
            consecutiveWins = 0,
            isGameOver = isGameOver
        )
        runSession = if (isGameOver) {
            sessionWithLoss.withRunRecorded()
        } else {
            sessionWithLoss
        }
        viewModelScope.launch {
            repository.saveRunSession(runSession)
        }
        if (!isGameOver) {
            startFreshDeal(_uiState.value.settings)
        } else {
            _uiState.update { it.copy(runSession = runSession, showNoMovesDialog = false) }
        }
    }

    private fun startNewRun() {
        val sessionWithRecord = runSession.withRunRecorded()
        val bestRound = maxOf(sessionWithRecord.bestRoundRecord, sessionWithRecord.currentRound)
        val bestStreak = maxOf(sessionWithRecord.bestStreakRecord, sessionWithRecord.consecutiveWins)
        runSession = PyramidRunSession(
            bestRoundRecord = bestRound,
            bestStreakRecord = bestStreak,
            topRuns = sessionWithRecord.topRuns
        )
        viewModelScope.launch {
            repository.saveRunSession(runSession)
        }
        startFreshDeal(_uiState.value.settings)
    }

    private fun drawOrRecycle() {
        if (_uiState.value.isWon) return
        clearHint()

        val oldState = session.currentState
        val updated = session.drawOrRecycle()
        if (updated === session) {
            scheduleDeadlockCheck()
            return
        }

        ensureGameStarted()
        session = updated
        val drawnCard = if (oldState.stock.isNotEmpty()) {
            oldState.stock.last().copy(isFaceUp = true)
        } else {
            null
        }
        _uiState.update {
            it.copy(
                gameState = session.currentState,
                canUndo = session.canUndo,
                showNoMovesDialog = false,
                activeDrawCard = drawnCard,
                activeMatch = null
            )
        }
        persistCurrentGame(immediate = false)
        scheduleDeadlockCheck()
    }

    private fun handleTapCard(location: PyramidLocation) {
        if (_uiState.value.isWon) return
        clearHint()

        val (newSession, result) = session.interactCard(location)
        session = newSession

        when (result) {
            is PyramidInteractionResult.Matched -> {
                ensureGameStarted()
                val isWon = result.isVictory
                val elapsed = internalElapsedSeconds
                if (isWon && !recordedAsWon) {
                    recordedAsWon = true
                    val roundScore = session.currentState.score
                    val newStreak = runSession.consecutiveWins + 1
                    val bonusLives = when {
                        newStreak == 3 -> 1
                        newStreak == 6 -> 2
                        newStreak > 6 && newStreak % 3 == 0 -> 1
                        else -> 0
                    }
                    val newLives = minOf(runSession.maxLives, runSession.lives + bonusLives)
                    val streakBonus = newStreak * 50
                    val newTotalScore = runSession.totalRunScore + roundScore + streakBonus
                    val newBestRound = maxOf(runSession.bestRoundRecord, runSession.currentRound)
                    val newBestStreak = maxOf(runSession.bestStreakRecord, newStreak)

                    runSession = runSession.copy(
                        lives = newLives,
                        consecutiveWins = newStreak,
                        totalRunScore = newTotalScore,
                        bestRoundRecord = newBestRound,
                        bestStreakRecord = newBestStreak,
                        isGameOver = false
                    )
                    viewModelScope.launch {
                        repository.saveRunSession(runSession)
                        repository.recordGameWon(
                            score = roundScore,
                            elapsedSeconds = elapsed
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        gameState = session.currentState,
                        canUndo = session.canUndo,
                        elapsedSeconds = if (isWon) elapsed else it.elapsedSeconds,
                        showNoMovesDialog = false,
                        runSession = runSession,
                        lastMatchedResult = "+${result.pointsEarned} pts",
                        activeMatch = result.match,
                        activeDrawCard = null
                    )
                }
                persistCurrentGame(immediate = isWon)
                if (!isWon) {
                    scheduleDeadlockCheck()
                }
            }
            is PyramidInteractionResult.Selected,
            is PyramidInteractionResult.Deselected,
            is PyramidInteractionResult.InvalidPair -> {
                _uiState.update {
                    it.copy(
                        gameState = session.currentState,
                        canUndo = session.canUndo
                    )
                }
            }
            null -> Unit
        }
    }

    private fun undoMove() {
        if (!session.canUndo) return
        deadlockJob?.cancel()
        clearHint()

        session = session.undo()

        _uiState.update {
            it.copy(
                gameState = session.currentState,
                canUndo = session.canUndo,
                showNoMovesDialog = false,
                activeMatch = null,
                activeDrawCard = null
            )
        }
        persistCurrentGame(immediate = false)
        scheduleDeadlockCheck()
    }

    private fun requestHint() {
        if (_uiState.value.isWon) return
        val snapshot = session.currentState
        viewModelScope.launch {
            val hint = withContext(defaultDispatcher) {
                rules.findBestHint(snapshot)
            }
            if (!isActive || session.currentState !== snapshot || snapshot.isWon) return@launch
            if (hint != null) {
                _uiState.update {
                    it.copy(
                        activeHint = hint,
                        showNoMovesDialog = false,
                        hintMessage = "Match found!"
                    )
                }
                return@launch
            }
            val hasMoves = withContext(defaultDispatcher) {
                rules.hasAnyMovesLeft(snapshot) { !coroutineContext.isActive }
            }
            if (!isActive || session.currentState !== snapshot || snapshot.isWon) return@launch
            if (hasMoves) {
                val message = if (snapshot.stock.isNotEmpty()) {
                    "No open matches. Draw from the stock."
                } else {
                    "No open matches. Recycle the waste pile."
                }
                _uiState.update {
                    it.copy(
                        activeHint = null,
                        showNoMovesDialog = false,
                        hintMessage = message
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        activeHint = null,
                        hintMessage = null,
                        showNoMovesDialog = true
                    )
                }
            }
        }
    }

    private fun updateMaxRecycles(maxRecycles: Int) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(maxRecycles = maxRecycles) }
            val updatedState = session.currentState.copy(maxRecycles = maxRecycles)
            session = session.copy(currentState = updatedState)
            _uiState.update {
                it.copy(
                    gameState = updatedState,
                    showNoMovesDialog = false
                )
            }
            persistCurrentGame(immediate = true)
            if (hasStartedMoving && !updatedState.isWon) {
                scheduleDeadlockCheck()
            }
        }
    }

    private fun updateDealMode(dealMode: DealMode) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(dealMode = dealMode) }
            if (!hasStartedMoving) {
                startFreshDeal(_uiState.value.settings.copy(dealMode = dealMode))
            }
        }
    }

    private fun clearHint() {
        if (_uiState.value.activeHint != null || _uiState.value.hintMessage != null) {
            _uiState.update {
                it.copy(
                    activeHint = null,
                    hintMessage = null
                )
            }
        }
    }

    private fun ensureGameStarted() {
        if (!hasStartedMoving) {
            hasStartedMoving = true
        }
        if (!recordedAsPlayed) {
            recordedAsPlayed = true
            viewModelScope.launch {
                repository.recordGamePlayed()
            }
        }
    }

    private fun updateSettings(transform: (PyramidSettings) -> PyramidSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
        }
    }

    private fun scheduleDeadlockCheck() {
        deadlockJob?.cancel()
        val snapshot = session.currentState
        if (snapshot.isWon) return
        deadlockJob = viewModelScope.launch {
            delay(250.milliseconds)
            val hasMoves = withContext(defaultDispatcher) {
                rules.hasAnyMovesLeft(snapshot) { !coroutineContext.isActive }
            }
            if (!hasMoves && isActive && session.currentState === snapshot && !snapshot.isWon) {
                _uiState.update { it.copy(showNoMovesDialog = true) }
            }
        }
    }

    private fun buildSnapshot(): SavedPyramidGameSnapshot? {
        if (session.currentState.isWon) return null
        return SavedPyramidGameSnapshot(
            gameState = session.currentState,
            undoStack = session.undoStack,
            elapsedSeconds = internalElapsedSeconds,
            hasStartedMoving = hasStartedMoving,
            recordedAsPlayed = recordedAsPlayed,
            runSession = runSession
        )
    }

    private fun persistCurrentGame(immediate: Boolean = false) {
        if (session.currentState.isWon) {
            persistDebounceJob?.cancel()
            persistChannel.trySend(null)
            return
        }
        if (immediate) {
            persistDebounceJob?.cancel()
            persistChannel.trySend(buildSnapshot())
        } else {
            persistDebounceJob?.cancel()
            persistDebounceJob = viewModelScope.launch {
                delay(350.milliseconds)
                persistChannel.trySend(buildSnapshot())
            }
        }
    }

    private fun startTimerLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1.seconds)
                val state = _uiState.value
                if (!isTimerPaused && hasStartedMoving && !state.isWon && !state.isLoading) {
                    internalElapsedSeconds += 1
                    if (state.settings.showTimer) {
                        _uiState.update { it.copy(elapsedSeconds = internalElapsedSeconds) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        deadlockJob?.cancel()
        persistDebounceJob?.cancel()
        persistChannel.close()
    }
}
