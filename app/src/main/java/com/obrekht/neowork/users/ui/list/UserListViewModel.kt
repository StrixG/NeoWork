package com.obrekht.neowork.users.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.users.data.repository.UserRepository
import com.obrekht.neowork.users.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import javax.inject.Inject

private const val USERS_PER_PAGE = 10

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val repository: UserRepository
) : ViewModel() {

    val data: Flow<PagingData<User>> = repository
        .getPagingData(
            PagingConfig(
                pageSize = USERS_PER_PAGE,
                initialLoadSize = USERS_PER_PAGE * 2
            )
        )
        .cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(UserListUiState())
    val uiState: StateFlow<UserListUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean
        get() = uiState.value.isLoggedIn

    init {
        refresh()

        viewModelScope.launch {
            appAuth.state.onEach { authState ->
                repository.invalidatePagingSource()
                _uiState.update { it.copy(isLoggedIn = authState.id > 0L) }
            }.launchIn(this)
        }
    }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(dataState = DataState.Loading) }
        try {
            repository.refreshAll()
            _uiState.update { it.copy(dataState = DataState.Success) }
        } catch (e: Exception) {
            val errorType = when (e) {
                is HttpException -> ErrorType.FailedToLoad
                is ConnectException -> ErrorType.Connection
                else -> ErrorType.Unknown
            }
            _uiState.update { it.copy(dataState = DataState.Error(errorType)) }
        }
    }
}

data class UserListUiState(
    val isLoggedIn: Boolean = false,
    val dataState: DataState = DataState.Success
)

sealed interface DataState {
    data object Loading : DataState
    data object Success : DataState
    data class Error(val type: ErrorType = ErrorType.Unknown) : DataState
}

enum class ErrorType {
    Unknown,
    FailedToLoad,
    Connection
}