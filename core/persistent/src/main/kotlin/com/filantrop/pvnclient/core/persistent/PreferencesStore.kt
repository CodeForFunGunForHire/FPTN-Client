package com.filantrop.pvnclient.core.persistent

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATA_STORE_NAME = "PvnDataPreferencesStore"
private const val BASE_NAMESPACE = "com.filantrop.pvnclient"
const val PREFERENCE_CLIENT_UID = BASE_NAMESPACE + "current.client.uid"
private val PREFERENCE_CLIENT_TOKEN_KEY = stringPreferencesKey(PREFERENCE_CLIENT_UID)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(DATA_STORE_NAME)

interface PreferenceStore {
    val token: Flow<String?>

    suspend fun updateToken(token: String)

    suspend fun clearAllData()
}

class PreferencesStoreImpl(
    context: Context,
) : PreferenceStore {
    private val dataStore: DataStore<Preferences> = context.dataStore

    override val token: Flow<String?> = dataStore.data.map { data -> data[PREFERENCE_CLIENT_TOKEN_KEY] }

    override suspend fun updateToken(token: String) {
        dataStore.edit { preferences -> preferences[PREFERENCE_CLIENT_TOKEN_KEY] = token }
    }

    override suspend fun clearAllData() {
        dataStore.edit { it.clear() }
    }
}
