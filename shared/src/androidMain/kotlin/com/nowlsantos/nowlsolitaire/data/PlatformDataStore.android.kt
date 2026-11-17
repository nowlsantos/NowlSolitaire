package com.nowlsantos.nowlsolitaire.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

object AndroidDataStoreContext {
    private var appContext: Context? = null
    private val lock = Any()
    private val stores = mutableMapOf<String, DataStore<Preferences>>()

    fun init(context: Context) {
        synchronized(lock) {
            if (appContext == null) {
                appContext = context.applicationContext
            }
        }
    }

    internal fun getOrCreate(name: String): DataStore<Preferences> {
        synchronized(lock) {
            stores[name]?.let { return it }
            val context = requireNotNull(appContext) {
                "AndroidDataStoreContext.init(context) must be called before creating DataStore."
            }
            val store = PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile(name) }
            )
            stores[name] = store
            return store
        }
    }
}

actual fun createPreferencesDataStore(name: String): DataStore<Preferences> {
    return AndroidDataStoreContext.getOrCreate(name)
}
