package com.nowlsantos.nowlsolitaire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nowlsantos.nowlsolitaire.ui.components.GameBottomBar
import com.nowlsantos.nowlsolitaire.ui.components.GameTopBar
import com.nowlsantos.nowlsolitaire.ui.components.pyramid.PyramidBoard
import com.nowlsantos.nowlsolitaire.ui.components.pyramid.PyramidBoardState
import com.nowlsantos.nowlsolitaire.ui.components.pyramid.PyramidSettingsBottomSheet
import com.nowlsantos.nowlsolitaire.ui.state.PyramidIntent
import com.nowlsantos.nowlsolitaire.ui.state.PyramidUiState
import com.nowlsantos.nowlsolitaire.ui.state.PyramidViewModel
import com.nowlsantos.nowlsolitaire.ui.theme.toColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PyramidRoute(
    onNavigateBack: () -> Unit = {},
    viewModel: PyramidViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onIntent(PyramidIntent.SetTimerPaused(true))
                Lifecycle.Event.ON_RESUME -> viewModel.onIntent(PyramidIntent.SetTimerPaused(false))
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.onIntent(PyramidIntent.SetTimerPaused(false))
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onIntent(PyramidIntent.SetTimerPaused(true))
        }
    }

    PyramidScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = onNavigateBack
    )
}

@Composable
fun PyramidScreen(
    uiState: PyramidUiState,
    onIntent: (PyramidIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val colors = uiState.settings.tableTheme.toColors()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    var showNewGameDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    var isBoardSettled by remember { mutableStateOf(true) }
    var shouldShowWinOverlay by remember { mutableStateOf(false) }
    var shouldShowNoMovesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isWon, isBoardSettled) {
        if (uiState.isWon) {
            shouldShowNoMovesDialog = false
            if (isBoardSettled) {
                delay(400.milliseconds)
                if (uiState.isWon && isBoardSettled) {
                    shouldShowWinOverlay = true
                }
            } else {
                shouldShowWinOverlay = false
            }
        } else {
            shouldShowWinOverlay = false
        }
    }

    LaunchedEffect(uiState.showNoMovesDialog, isBoardSettled, uiState.isWon) {
        if (uiState.showNoMovesDialog && !uiState.isWon) {
            if (isBoardSettled) {
                delay(450.milliseconds)
                if (uiState.showNoMovesDialog && !uiState.isWon && isBoardSettled) {
                    shouldShowNoMovesDialog = true
                }
            } else {
                shouldShowNoMovesDialog = false
            }
        } else {
            shouldShowNoMovesDialog = false
        }
    }

    val anyModalOpen = showNewGameDialog || showSettingsSheet || showStatsDialog || shouldShowNoMovesDialog
    LaunchedEffect(anyModalOpen) {
        onIntent(PyramidIntent.SetTimerPaused(anyModalOpen))
    }

    LaunchedEffect(shouldShowWinOverlay) {
        if (shouldShowWinOverlay) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(uiState.hintMessage) {
        val msg = uiState.hintMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onIntent(PyramidIntent.ClearHintMessage)
        }
    }

    val backgroundBrush = remember(colors.feltPrimary, colors.feltSecondary) {
        Brush.verticalGradient(
            colors = listOf(colors.feltPrimary, colors.feltSecondary)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            GameTopBar(
                score = uiState.score,
                elapsedSeconds = uiState.elapsedSeconds,
                moveCount = uiState.moveCount,
                colors = colors,
                onOpenStats = { showStatsDialog = true },
                onOpenSettings = { showSettingsSheet = true },
                onBack = onBack,
                showTimer = uiState.settings.showTimer,
                roundNumber = uiState.runSession.currentRound,
                lives = uiState.runSession.lives,
                maxLives = uiState.runSession.maxLives,
                streak = uiState.runSession.consecutiveWins
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    val boardState = remember(
                        uiState.gameState,
                        uiState.activeHint,
                        uiState.activeMatch,
                        uiState.activeDrawCard,
                        uiState.settings.tableTheme,
                        uiState.settings.cardBackTheme,
                        uiState.dealId
                    ) {
                        PyramidBoardState(
                            gameState = uiState.gameState,
                            tableTheme = uiState.settings.tableTheme,
                            cardBackTheme = uiState.settings.cardBackTheme,
                            activeHint = uiState.activeHint,
                            activeMatch = uiState.activeMatch,
                            activeDrawCard = uiState.activeDrawCard,
                            dealId = uiState.dealId
                        )
                    }
                    PyramidBoard(
                        boardState = boardState,
                        onIntent = onIntent,
                        onBoardSettledChanged = { settled -> isBoardSettled = settled },
                        onWinFlightsSettled = { isBoardSettled = true }
                    )
                }
            }

            GameBottomBar(
                canUndo = uiState.canUndo,
                isAutoCompleteAvailable = false,
                colors = colors,
                onNewDeal = { showNewGameDialog = true },
                onUndo = { onIntent(PyramidIntent.Undo) },
                onHint = { onIntent(PyramidIntent.RequestHint) },
                onAutoComplete = {}
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        if (shouldShowWinOverlay) {
            PyramidRoundWinOverlay(
                round = uiState.runSession.currentRound,
                roundScore = uiState.score,
                totalRunScore = uiState.runSession.totalRunScore,
                streak = uiState.runSession.consecutiveWins,
                lives = uiState.runSession.lives,
                maxLives = uiState.runSession.maxLives,
                elapsedSeconds = uiState.elapsedSeconds,
                moveCount = uiState.moveCount,
                onNextRound = { onIntent(PyramidIntent.NextRound) },
                onReplay = { onIntent(PyramidIntent.ReplayDeal) },
                showTimer = uiState.settings.showTimer
            )
        }

        if (showNewGameDialog) {
            NewGameDialog(
                onNewDeal = {
                    showNewGameDialog = false
                    onIntent(PyramidIntent.NewGame)
                },
                onReplayDeal = {
                    showNewGameDialog = false
                    onIntent(PyramidIntent.ReplayDeal)
                },
                onDismiss = { showNewGameDialog = false }
            )
        }

        if (shouldShowNoMovesDialog) {
            if (uiState.runSession.isGameOver || uiState.runSession.lives <= 1) {
                PyramidGameOverDialog(
                    finalRound = uiState.runSession.currentRound,
                    totalRunScore = uiState.runSession.totalRunScore,
                    bestStreak = maxOf(uiState.runSession.bestStreakRecord, uiState.runSession.consecutiveWins),
                    canUndo = uiState.canUndo,
                    onUndo = {
                        onIntent(PyramidIntent.DismissNoMovesDialog)
                        onIntent(PyramidIntent.Undo)
                    },
                    onStartNewRun = {
                        onIntent(PyramidIntent.DismissNoMovesDialog)
                        onIntent(PyramidIntent.StartNewRun)
                    },
                    onDismiss = { onIntent(PyramidIntent.DismissNoMovesDialog) }
                )
            } else {
                PyramidLifeLostDialog(
                    remainingLives = uiState.runSession.lives - 1,
                    canUndo = uiState.canUndo,
                    onUndo = {
                        onIntent(PyramidIntent.DismissNoMovesDialog)
                        onIntent(PyramidIntent.Undo)
                    },
                    onRetryDeal = {
                        onIntent(PyramidIntent.DismissNoMovesDialog)
                        onIntent(PyramidIntent.ReplayDeal)
                    },
                    onContinueNextDeal = {
                        onIntent(PyramidIntent.DismissNoMovesDialog)
                        onIntent(PyramidIntent.ContinueRunWithLostLife)
                    },
                    onDismiss = { onIntent(PyramidIntent.DismissNoMovesDialog) }
                )
            }
        }

        if (showSettingsSheet) {
            PyramidSettingsBottomSheet(
                settings = uiState.settings,
                onIntent = onIntent,
                onDismiss = { showSettingsSheet = false }
            )
        }

        if (showStatsDialog) {
            StatsDialog(
                stats = uiState.stats,
                onResetStats = { onIntent(PyramidIntent.ResetStats) },
                onDismiss = { showStatsDialog = false },
                pyramidTopRuns = uiState.runSession.topRuns
            )
        }
    }
}

/*@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun PyramidScreenPreview() {
    NowlSolitaireTheme {
        PyramidScreen(
            uiState = PyramidUiState(
                isLoading = false,
                gameState = PyramidEngine.dealNewGame(seed = 42L),
                elapsedSeconds = 62
            ),
            onIntent = {}
        )
    }
}*/
