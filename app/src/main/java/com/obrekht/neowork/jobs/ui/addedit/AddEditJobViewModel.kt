package com.obrekht.neowork.jobs.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.jobs.data.repository.JobRepository
import com.obrekht.neowork.jobs.model.Job
import com.obrekht.neowork.utils.isValidWebUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AddEditJobViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = AddEditJobFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _uiState = MutableStateFlow(AddEditJobUiState())
    val uiState: StateFlow<AddEditJobUiState> = _uiState.asStateFlow()

    private val _event = Channel<Event>()
    val event: Flow<Event> = _event.receiveAsFlow()

    private var initialJob: Job = Job()

    init {
        if (args.jobId == NEW_JOB_ID) {
            _uiState.value = AddEditJobUiState(initialized = true)
        } else {
            viewModelScope.launch {
                initialJob = jobRepository.getJob(args.jobId)?.also { job ->
                    _uiState.update {
                        it.copy(
                            shouldInitialize = true,
                            formState = AddEditJobFormState(
                                name = job.name,
                                position = job.position,
                                link = job.link ?: "",
                                isDataValid = true
                            ),
                            startDate = job.start,
                            endDate = job.finish
                        )
                    }
                } ?: initialJob
            }
        }
    }

    fun onInitialized() {
        _uiState.update { it.copy(shouldInitialize = false, initialized = true) }
    }

    fun save() = viewModelScope.launch {
        viewModelScope.launch {
            if (!uiState.value.formState.isDataValid) {
                _event.send(Event.ErrorInvalidData)
            } else {
                try {
                    val formState = uiState.value.formState

                    val job = initialJob.copy(
                        name = formState.name.trim(),
                        position = formState.position.trim(),
                        link = formState.link.trim().ifEmpty { null },
                        start = uiState.value.startDate,
                        finish = uiState.value.endDate,
                    )
                    jobRepository.save(job)

                    _event.send(Event.JobSaved)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _event.send(Event.ErrorSaving)
                }
            }
        }
    }

    fun updateForm(name: String, position: String, link: String) {
        val nameError = if (name.isBlank()) {
            NameError.Empty
        } else null

        val positionError = if (position.isBlank()) {
            PositionError.Empty
        } else null

        val linkError = if (link.isNotBlank() && !link.isValidWebUrl()) {
            LinkError.InvalidLink
        } else null

        val isValidData = (nameError == null && positionError == null && linkError == null)

        _uiState.update {
            it.copy(
                formState = AddEditJobFormState(
                    name, position, link,
                    nameError, positionError, linkError,
                    isValidData
                )
            )
        }
    }

    fun setDates(start: Instant, end: Instant? = null) {
        _uiState.update {
            it.copy(
                startDate = start,
                endDate = end
            )
        }
    }

    companion object {
        const val NEW_JOB_ID = 0L
    }
}

data class AddEditJobUiState(
    val shouldInitialize: Boolean = false,
    val initialized: Boolean = false,
    val isLoading: Boolean = false,
    val formState: AddEditJobFormState = AddEditJobFormState(),
    val startDate: Instant = Instant.now(),
    val endDate: Instant? = null
)

sealed interface Event {
    data object ErrorInvalidData : Event
    data object ErrorSaving : Event
    data object JobSaved : Event
}