package com.filantrop.pvnclient.auth.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val token: Flow<String?>

    suspend fun loginWithToken(token: String)

    suspend fun logout()
}
