package com.obrekht.neowork.userchooser.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.users.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import javax.inject.Inject

private const val USERS_PER_PAGE = 10

@HiltViewModel
class UserChooserViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val repository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = UserChooserFragmentArgs.fromSavedStateHandle(savedStateHandle)

    val data: Flow<PagingData<UserItem>> = repository
        .getPagingData(
            PagingConfig(
                pageSize = USERS_PER_PAGE,
                initialLoadSize = USERS_PER_PAGE * 2
            )
        )
        .map {
            it.map { user ->
                UserItem(
                    user = user,
                    isSelected = _selectedUserIds.contains(user.id)
                )
            }
        }
        .cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(UserChooserUiState())
    val uiState: StateFlow<UserChooserUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean
        get() = uiState.value.isLoggedIn

    private var _selectedUserIds: MutableSet<Long> = mutableSetOf()
    val selectedUserIds: Set<Long>
        get() = _selectedUserIds.toSet()

    init {
        refresh()

        viewModelScope.launch {
            appAuth.state.onEach { authState ->
                repository.invalidatePagingSource()
                _uiState.update { it.copy(isLoggedIn = authState.id > 0L) }
            }.launchIn(this)
        }

        _selectedUserIds = args.selectedUserIds.toMutableSet()
        Timber.d(_selectedUserIds.toString())
        repository.invalidatePagingSource()
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

    fun setUserSelected(userId: Long, isChecked: Boolean) {
        if (isChecked == _selectedUserIds.contains(userId)) return
        _selectedUserIds.apply {
            if (isChecked) {
                add(userId)
            } else {
                remove(userId)
            }
        }
        repository.invalidatePagingSource()
    }
}

data class UserChooserUiState(
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