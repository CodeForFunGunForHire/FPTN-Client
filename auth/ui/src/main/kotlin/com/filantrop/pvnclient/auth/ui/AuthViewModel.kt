package com.filantrop.pvnclient.auth.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthState(
    val token: String,
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthState(""))
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun login(token: String) {
        System.out.println("moggot login: $token")
    }
}
