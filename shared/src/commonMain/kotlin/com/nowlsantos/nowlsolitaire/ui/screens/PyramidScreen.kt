package com.nowlsantos.nowlsolitaire.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
//import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidEngine
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.PyramidSettings
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme
import com.nowlsantos.nowlsolitaire.ui.components.GameBottomBar
import com.nowlsantos.nowlsolitaire.ui.components.GameTopBar
import com.nowlsantos.nowlsolitaire.ui.components.pyramid.PyramidBoard
import com.nowlsantos.nowlsolitaire.ui.components.pyramid.PyramidBoardState
import com.nowlsantos.nowlsolitaire.ui.components.getCardBackDrawableRes
import com.nowlsantos.nowlsolitaire.ui.state.PyramidIntent
import com.nowlsantos.nowlsolitaire.ui.state.PyramidUiState
import com.nowlsantos.nowlsolitaire.ui.state.PyramidViewModel
import com.nowlsantos.nowlsolitaire.ui.theme.NowlSolitaireTheme
import com.nowlsantos.nowlsolitaire.ui.theme.toColors
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.painterResource

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PyramidSettingsBottomSheet(
    settings: PyramidSettings,
    onIntent: (PyramidIntent) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pyramid Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Redeal limit
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Stock Redeals",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = settings.maxRecycles == 2,
                        onClick = { onIntent(PyramidIntent.UpdateMaxRecycles(2)) },
                        label = { Text("2 Redeals (Standard)") }
                    )
                    FilterChip(
                        selected = settings.maxRecycles == 1,
                        onClick = { onIntent(PyramidIntent.UpdateMaxRecycles(1)) },
                        label = { Text("1 Redeal") }
                    )
                    FilterChip(
                        selected = settings.maxRecycles < 0,
                        onClick = { onIntent(PyramidIntent.UpdateMaxRecycles(-1)) },
                        label = { Text("Unlimited") }
                    )
                }
            }

            // Table Felt
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Table Felt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TableTheme.entries.forEach { theme ->
                        val palette = theme.toColors()
                        val selected = settings.tableTheme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                onIntent(PyramidIntent.UpdateTableTheme(theme))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(palette.feltPrimary)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Gray.copy(alpha = 0.5f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = theme.displayName,
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Card Back Theme
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Card Back",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CardBackTheme.entries.forEach { theme ->
                        val selected = settings.cardBackTheme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                onIntent(PyramidIntent.UpdateCardBackTheme(theme))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 66.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Gray.copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(getCardBackDrawableRes(theme)),
                                    contentDescription = theme.displayName,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.28f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Timer Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Timer",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Display elapsed game time in the top bar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showTimer,
                    onCheckedChange = { onIntent(PyramidIntent.UpdateShowTimer(it)) }
                )
            }
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
