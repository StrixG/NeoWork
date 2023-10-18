package com.obrekht.neowork.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.auth.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginResult {
    data object Success : LoginResult
    data class Error(val error: Exception) : LoginResult
}

data class LoginUiState(
    val formState: LoginFormState = LoginFormState(),
    val result: LoginResult? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean
        get() = authRepository.isLoggedIn

    fun logIn(username: String, password: String) = viewModelScope.launch {
        if (uiState.value.isLoading) return@launch
        if (!uiState.value.formState.isDataValid) return@launch

        _uiState.update { it.copy(isLoading = true, result = null) }

        try {
            authRepository.logIn(username, password)
            _uiState.update { it.copy(isLoading = false, result = LoginResult.Success) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, result = LoginResult.Error(e)) }
        }
    }

    fun validateFormData(username: String, password: String) {
        if (username.isEmpty()) {
            _uiState.update {
                it.copy(formState = LoginFormState(usernameError = UsernameError.Empty))
            }
        } else if (password.isEmpty()) {
            _uiState.update {
                it.copy(formState = LoginFormState(passwordError = PasswordError.Empty))
            }
        } else {
            _uiState.update {
                it.copy(formState = LoginFormState(isDataValid = true))
            }
        }
    }

    fun resultHandled() {
        _uiState.update { it.copy(result = null) }
    }
}
