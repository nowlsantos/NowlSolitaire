package com.nowlsantos.nowlsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private val iosDataStores = mutableMapOf<String, DataStore<Preferences>>()

@OptIn(ExperimentalForeignApi::class)
actual fun createPreferencesDataStore(name: String): DataStore<Preferences> {
    iosDataStores[name]?.let { return it }
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val path = "${requireNotNull(documentDirectory?.path)}/$name.preferences_pb"
    val store = PreferenceDataStoreFactory.createWithPath(
        produceFile = { path.toPath() }
    )
    iosDataStores[name] = store
    return store
}
