package com.filantrop.pvnclient.auth.data

import com.filantrop.pvnclient.auth.domain.AuthRepository
import com.filantrop.pvnclient.core.persistent.PreferenceStore
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val preferenceStore: PreferenceStore,
) : AuthRepository {
    override val token: Flow<String?> = preferenceStore.token

    override suspend fun loginWithToken(token: String) = preferenceStore.updateToken(token)

    override suspend fun logout() = preferenceStore.clearAllData()
}
