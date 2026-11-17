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
    var isWaitingForWinFlight by remember(uiState.isWon) { mutableStateOf(uiState.isWon) }
    val showWinOverlay = uiState.isWon && !isWaitingForWinFlight

    val anyModalOpen = showNewGameDialog || showSettingsSheet || showStatsDialog || uiState.showNoMovesDialog
    LaunchedEffect(anyModalOpen) {
        onIntent(PyramidIntent.SetTimerPaused(anyModalOpen))
    }

    LaunchedEffect(showWinOverlay) {
        if (showWinOverlay) {
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
                showTimer = uiState.settings.showTimer
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
                        onWinFlightsSettled = { isWaitingForWinFlight = false }
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

        if (showWinOverlay) {
            WinOverlay(
                score = uiState.score,
                elapsedSeconds = uiState.elapsedSeconds,
                moveCount = uiState.moveCount,
                onReplay = { onIntent(PyramidIntent.ReplayDeal) },
                onNewDeal = { onIntent(PyramidIntent.NewGame) },
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

        if (uiState.showNoMovesDialog) {
            NoMovesDialog(
                canUndo = uiState.canUndo,
                onUndo = {
                    onIntent(PyramidIntent.DismissNoMovesDialog)
                    onIntent(PyramidIntent.Undo)
                },
                onReplayDeal = {
                    onIntent(PyramidIntent.DismissNoMovesDialog)
                    onIntent(PyramidIntent.ReplayDeal)
                },
                onNewDeal = {
                    onIntent(PyramidIntent.DismissNoMovesDialog)
                    onIntent(PyramidIntent.NewGame)
                },
                onDismiss = { onIntent(PyramidIntent.DismissNoMovesDialog) }
            )
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
                onDismiss = { showStatsDialog = false }
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
