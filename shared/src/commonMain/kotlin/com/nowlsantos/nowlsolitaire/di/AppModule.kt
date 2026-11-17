package com.nowlsantos.nowlsolitaire.di

import com.nowlsantos.nowlsolitaire.data.DataStoreKlondikeRepository
import com.nowlsantos.nowlsolitaire.data.DataStorePyramidRepository
import com.nowlsantos.nowlsolitaire.data.KlondikeRepository
import com.nowlsantos.nowlsolitaire.data.PyramidRepository
import com.nowlsantos.nowlsolitaire.data.createPreferencesDataStore
import com.nowlsantos.nowlsolitaire.domain.engine.DeckDealer
import com.nowlsantos.nowlsolitaire.domain.engine.KlondikeEngine
import com.nowlsantos.nowlsolitaire.domain.engine.KlondikeRules
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidEngine
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidRules
import com.nowlsantos.nowlsolitaire.domain.engine.PyramidScoring
import com.nowlsantos.nowlsolitaire.domain.engine.ScoringRules
import com.nowlsantos.nowlsolitaire.ui.state.KlondikeViewModel
import com.nowlsantos.nowlsolitaire.ui.state.PyramidViewModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val domainModule = module {
    single { DeckDealer }
    single { KlondikeRules }
    single { KlondikeEngine }
    single { ScoringRules }
    single { PyramidRules }
    single { PyramidEngine }
    single { PyramidScoring }
}

val dataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<KlondikeRepository> {
        DataStoreKlondikeRepository(
            dataStore = createPreferencesDataStore("nowls_klondike_prefs"),
            json = get()
        )
    }
    single<PyramidRepository> {
        DataStorePyramidRepository(
            dataStore = createPreferencesDataStore("nowls_pyramid_prefs"),
            json = get()
        )
    }
}

val viewModelModule = module {
    viewModelOf(::KlondikeViewModel)
    viewModelOf(::PyramidViewModel)
}

val appModules = listOf(
    domainModule,
    dataModule,
    viewModelModule
)
