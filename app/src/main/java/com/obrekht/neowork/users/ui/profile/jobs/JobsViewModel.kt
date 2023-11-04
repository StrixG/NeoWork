package com.obrekht.neowork.users.ui.profile.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.obrekht.neowork.jobs.data.repository.JobRepository
import com.obrekht.neowork.jobs.model.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val JOBS_PER_PAGE = 5

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Long = savedStateHandle.get<Long>(JobsArguments.USER_ID) ?: 0L

    private val _uiState = MutableStateFlow(JobsUiState())
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    private val _event = Channel<UiEvent>()
    val event: Flow<UiEvent> = _event.receiveAsFlow()

    init {
        refresh()
    }

    val data: Flow<PagingData<Job>> = jobRepository
        .getPagingData(
            userId,
            PagingConfig(
                pageSize = JOBS_PER_PAGE,
                initialLoadSize = JOBS_PER_PAGE * 2
            )
        )
        .cachedIn(viewModelScope)
        .flowOn(Dispatchers.Default)

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(dataState = DataState.Loading) }

        try {
            jobRepository.refreshUserJobs(userId)
            _uiState.update { it.copy(dataState = DataState.Success) }
        } catch (e: Exception) {
            _uiState.update { it.copy(dataState = DataState.Error) }
        }
    }

    fun deleteJobById(jobId: Long) = viewModelScope.launch {
        try {
            jobRepository.deleteById(jobId)
        } catch (e: Exception) {
            _event.send(UiEvent.ErrorDeleting(jobId))
        }
    }

    fun deleteJob(job: Job) = deleteJobById(job.id)
}

data class JobsUiState(
    val dataState: DataState = DataState.Success
)

sealed interface DataState {
    data object Loading : DataState
    data object Success : DataState
    data object Error : DataState
}

sealed interface UiEvent {
    data class ErrorDeleting(val jobId: Long) : UiEvent
}
