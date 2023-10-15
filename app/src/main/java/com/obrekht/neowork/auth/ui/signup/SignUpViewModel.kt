package com.obrekht.neowork.auth.ui.signup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.auth.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AvatarModel(val uri: Uri? = null, val file: File? = null)

sealed interface SignUpResult {
    data object Success : SignUpResult
    data class Error(val error: Exception) : SignUpResult
}

data class SignUpUiState(
    val avatarModel: AvatarModel = AvatarModel(),
    val formState: SignUpFormState = SignUpFormState(),
    val result: SignUpResult? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean
        get() = authRepository.isLoggedIn

    fun signUp(username: String, password: String, name: String) =
        viewModelScope.launch {
            if (uiState.value.isLoading) return@launch
            if (!uiState.value.formState.isDataValid) return@launch

            _uiState.update { it.copy(isLoading = true, result = null) }

            try {
                authRepository.signUp(username, password, name, _uiState.value.avatarModel.file)
                _uiState.update { it.copy(isLoading = false, result = SignUpResult.Success) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, result = SignUpResult.Error(e)) }
            }
        }

    fun validateFormData(
        name: String,
        username: String,
        password: String
    ) {

        if (name.isBlank()) {
            _uiState.update {
                it.copy(formState = SignUpFormState(nameError = NameError.Empty))
            }
        } else if (username.isBlank()) {
            _uiState.update {
                it.copy(formState = SignUpFormState(usernameError = UsernameError.Empty))
            }
        } else if (password.isBlank()) {
            _uiState.update {
                it.copy(formState = SignUpFormState(passwordError = PasswordError.Empty))
            }
        } else {
            _uiState.update {
                it.copy(formState = SignUpFormState(isDataValid = true))
            }
        }
    }

    fun changeAvatar(uri: Uri?, toFile: File?) {
        _uiState.update { it.copy(avatarModel = AvatarModel(uri, toFile)) }
    }
}
