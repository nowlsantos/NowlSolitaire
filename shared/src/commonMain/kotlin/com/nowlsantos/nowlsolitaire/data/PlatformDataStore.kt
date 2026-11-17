package com.nowlsantos.nowlsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createPreferencesDataStore(name: String): DataStore<Preferences>
