package com.obrekht.neowork.users.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.users.data.repository.UserRepository
import com.obrekht.neowork.users.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Long = UserProfileFragmentArgs.fromSavedStateHandle(savedStateHandle).userId

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean
        get() = uiState.value.isOwnProfile

    val isOwnProfile: Boolean
        get() = uiState.value.isOwnProfile

    private val _event = Channel<UiEvent>()
    val event: Flow<UiEvent> = _event.receiveAsFlow()

    init {
        refresh()

        viewModelScope.launch {
            appAuth.state.onEach { authState ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = (authState.id > 0L),
                        isOwnProfile = (authState.id == userId)
                    )
                }
            }.launchIn(this)

            userRepository.getUserStream(userId).onEach { user ->
                if (user != null) {
                    _uiState.update { it.copy(user = user, dataState = DataState.Success) }
                } else {
                    _uiState.update { it.copy(user = null, dataState = DataState.Error) }
                }
            }.launchIn(this)
        }
    }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(dataState = DataState.Loading) }
        try {
            userRepository.refreshUser(userId)
            _uiState.update { it.copy(dataState = DataState.Success) }
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    _event.send(UiEvent.ErrorLoadUser)
                }
                is ConnectException -> {
                    _event.send(UiEvent.ErrorConnection)
                }

                else -> {
                    _uiState.update { it.copy(dataState = DataState.Error) }
                }
            }
        }
    }

    fun logOut() {
        appAuth.removeAuth()
    }
}

data class UserProfileUiState(
    val isLoggedIn: Boolean = false,
    val isOwnProfile: Boolean = false,
    val user: User? = null,
    val dataState: DataState = DataState.Success
)

sealed interface DataState {
    data object Loading : DataState
    data object Success : DataState
    data object Error : DataState
}

sealed interface UiEvent {
    data object ErrorLoadUser : UiEvent
    data object ErrorConnection : UiEvent
}
