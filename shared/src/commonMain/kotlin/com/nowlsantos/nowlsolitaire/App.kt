package com.nowlsantos.nowlsolitaire

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.nowlsantos.nowlsolitaire.di.appModules
import com.nowlsantos.nowlsolitaire.ui.screens.KlondikeRoute
import com.nowlsantos.nowlsolitaire.ui.screens.LobbyScreen
import com.nowlsantos.nowlsolitaire.ui.screens.PyramidRoute
import com.nowlsantos.nowlsolitaire.ui.theme.NowlSolitaireTheme
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools

private enum class AppDestination {
    LOBBY,
    KLONDIKE,
    PYRAMID
}

private fun ensureKoinStarted() {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            modules(appModules)
        }
    }
}

@Composable
fun App() {
    remember { ensureKoinStarted() }

    NowlSolitaireTheme {
        var currentDestination by rememberSaveable { mutableStateOf(AppDestination.LOBBY) }

        when (currentDestination) {
            AppDestination.LOBBY -> LobbyScreen(
                onSelectKlondike = { currentDestination = AppDestination.KLONDIKE },
                onSelectPyramid = { currentDestination = AppDestination.PYRAMID }
            )
            AppDestination.KLONDIKE -> KlondikeRoute(
                onNavigateBack = { currentDestination = AppDestination.LOBBY }
            )
            AppDestination.PYRAMID -> PyramidRoute(
                onNavigateBack = { currentDestination = AppDestination.LOBBY }
            )
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    NowlSolitaireTheme {
        LobbyScreen(
            onSelectKlondike = {},
            onSelectPyramid = {}
        )
    }
}
