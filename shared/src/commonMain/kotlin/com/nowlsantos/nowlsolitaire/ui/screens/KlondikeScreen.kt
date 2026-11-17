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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nowlsantos.nowlsolitaire.domain.engine.DeckDealer
import com.nowlsantos.nowlsolitaire.domain.model.DrawMode
import com.nowlsantos.nowlsolitaire.ui.components.GameBottomBar
import com.nowlsantos.nowlsolitaire.ui.components.GameTopBar
import com.nowlsantos.nowlsolitaire.ui.components.klondike.KlondikeBoard
import com.nowlsantos.nowlsolitaire.ui.components.klondike.KlondikeBoardState
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeIntent
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeUiState
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeViewModel
import com.nowlsantos.nowlsolitaire.ui.theme.NowlSolitaireTheme
import com.nowlsantos.nowlsolitaire.ui.theme.toColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KlondikeRoute(
    onNavigateBack: () -> Unit = {},
    viewModel: KlondikeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onIntent(KlondikeIntent.SetTimerPaused(true))
                Lifecycle.Event.ON_RESUME -> viewModel.onIntent(KlondikeIntent.SetTimerPaused(false))
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.onIntent(KlondikeIntent.SetTimerPaused(false))
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onIntent(KlondikeIntent.SetTimerPaused(true))
        }
    }

    KlondikeScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = onNavigateBack
    )
}

@Composable
fun KlondikeScreen(
    uiState: KlondikeUiState,
    onIntent: (KlondikeIntent) -> Unit,
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
        onIntent(KlondikeIntent.SetTimerPaused(anyModalOpen))
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
            onIntent(KlondikeIntent.ClearHintMessage)
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
                        uiState.selection,
                        uiState.activeHint,
                        uiState.isAutoCompleting,
                        uiState.settings.tableTheme,
                        uiState.settings.cardBackTheme,
                        uiState.settings.leftHanded
                    ) {
                        KlondikeBoardState(
                            gameState = uiState.gameState,
                            selection = uiState.selection,
                            activeHint = uiState.activeHint,
                            isAutoCompleting = uiState.isAutoCompleting,
                            tableTheme = uiState.settings.tableTheme,
                            leftHanded = uiState.settings.leftHanded,
                            cardBackTheme = uiState.settings.cardBackTheme
                        )
                    }
                    KlondikeBoard(
                        boardState = boardState,
                        onIntent = onIntent,
                        onWinFlightsSettled = { isWaitingForWinFlight = false }
                    )
                }
            }

            GameBottomBar(
                canUndo = uiState.canUndo,
                isAutoCompleteAvailable = uiState.isAutoCompleteAvailable,
                colors = colors,
                onNewDeal = { showNewGameDialog = true },
                onUndo = { onIntent(KlondikeIntent.Undo) },
                onHint = { onIntent(KlondikeIntent.RequestHint) },
                onAutoComplete = { onIntent(KlondikeIntent.RunAutoComplete) }
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
                onReplay = { onIntent(KlondikeIntent.ReplayDeal) },
                onNewDeal = { onIntent(KlondikeIntent.NewGame) },
                showTimer = uiState.settings.showTimer
            )
        }

        if (showNewGameDialog) {
            NewGameDialog(
                onNewDeal = {
                    showNewGameDialog = false
                    onIntent(KlondikeIntent.NewGame)
                },
                onReplayDeal = {
                    showNewGameDialog = false
                    onIntent(KlondikeIntent.ReplayDeal)
                },
                onDismiss = { showNewGameDialog = false }
            )
        }

        if (uiState.showNoMovesDialog) {
            NoMovesDialog(
                canUndo = uiState.canUndo,
                onUndo = {
                    onIntent(KlondikeIntent.DismissNoMovesDialog)
                    onIntent(KlondikeIntent.Undo)
                },
                onReplayDeal = {
                    onIntent(KlondikeIntent.DismissNoMovesDialog)
                    onIntent(KlondikeIntent.ReplayDeal)
                },
                onNewDeal = {
                    onIntent(KlondikeIntent.DismissNoMovesDialog)
                    onIntent(KlondikeIntent.NewGame)
                },
                onDismiss = { onIntent(KlondikeIntent.DismissNoMovesDialog) }
            )
        }

        if (showSettingsSheet) {
            SettingsBottomSheet(
                settings = uiState.settings,
                onIntent = onIntent,
                onDismiss = { showSettingsSheet = false }
            )
        }

        if (showStatsDialog) {
            StatsDialog(
                stats = uiState.stats,
                onResetStats = { onIntent(KlondikeIntent.ResetStats) },
                onDismiss = { showStatsDialog = false }
            )
        }
    }
}

/*@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun KlondikeScreenPreview() {
    NowlSolitaireTheme {
        KlondikeScreen(
            uiState = KlondikeUiState(
                isLoading = false,
                gameState = DeckDealer.dealNewGame(drawMode = DrawMode.DRAW_ONE, seed = 42L)
                    .copy(score = 35),
                elapsedSeconds = 84
            ),
            onIntent = {}
        )
    }
}*/
