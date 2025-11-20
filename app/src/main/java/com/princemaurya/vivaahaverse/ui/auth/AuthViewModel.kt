package com.princemaurya.vivaahaverse.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.princemaurya.vivaahaverse.data.local.AuthPreferences
import com.princemaurya.vivaahaverse.data.local.UserPreferences
import com.princemaurya.vivaahaverse.data.local.UserSession
import com.princemaurya.vivaahaverse.data.model.AuthResponse
import com.princemaurya.vivaahaverse.data.model.LoginRequest
import com.princemaurya.vivaahaverse.data.model.SignupRequest
import com.princemaurya.vivaahaverse.data.remote.AuthTokenProvider
import com.princemaurya.vivaahaverse.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val session: UserSession = UserSession(),
    val isAuthenticated: Boolean = false
)

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPreferences: UserPreferences = UserPreferences(application)
    private val repository: AuthRepository =
        AuthRepository(authPreferences = AuthPreferences.getInstance(application))

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.sessionFlow.collect { session ->
                AuthTokenProvider.token = session.token
                _uiState.update {
                    it.copy(
                        session = session,
                        isAuthenticated = !session.token.isNullOrBlank(),
                        errorMessage = null,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isLoginMode = !it.isLoginMode,
                errorMessage = null
            )
        }
    }

    fun authenticate() {
        if (_uiState.value.isLoginMode) {
            login()
        } else {
            signup()
        }
    }

    fun logout() {
        viewModelScope.launch {
            AuthTokenProvider.token = null
            userPreferences.clearSession()
            _uiState.update {
                AuthUiState()
            }
        }
    }

    private fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        if (!validateCredentials(email, password)) return

        viewModelScope.launch {
            setLoading()
            val result = repository.login(LoginRequest(email = email, password = password))
            handleAuthResult(result)
        }
    }

    private fun signup() {
        val name = _uiState.value.name.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name is required") }
            return
        }

        if (!validateCredentials(email, password)) return

        viewModelScope.launch {
            setLoading()
            val result = repository.signup(SignupRequest(name = name, email = email, password = password))
            handleAuthResult(result)
        }
    }

    private fun validateCredentials(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email is required") }
            return false
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return false
        }
        return true
    }

    private suspend fun handleAuthResult(result: Result<AuthResponse>) {
        result.fold(
            onSuccess = { response ->
                userPreferences.saveSession(
                    token = response.token,
                    name = response.user.name,
                    email = response.user.email
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        password = ""
                    )
                }
            },
            onFailure = { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        )
    }

    private fun setLoading() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }
}
