package com.obrekht.neowork.events.ui.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.events.data.repository.EventRepository
import com.obrekht.neowork.events.model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: Long = EventFragmentArgs.fromSavedStateHandle(savedStateHandle).eventId

    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    private val _event = Channel<UiEvent>()
    val event: Flow<UiEvent> = _event.receiveAsFlow()

    val isLoggedIn: Boolean
        get() = uiState.value.isLoggedIn

    private var likeJob: Job? = null
    private var participateJob: Job? = null

    init {
        viewModelScope.launch {
            appAuth.loggedInState.onEach { isLoggedIn ->
                _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
            }.launchIn(this)

            eventRepository.getEventStream(eventId).onEach {
                if (it == null) _event.send(UiEvent.EventDeleted)
            }.filterNotNull().combine(appAuth.state) { event, authState ->
                event.copy(ownedByMe = event.authorId == authState.id)
            }.onEach { event ->
                _uiState.update { it.copy(event = event, state = State.Success) }
            }.launchIn(this)
        }
    }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(state = State.Loading) }
        runCatching {
            eventRepository.refreshEvent(eventId)
        }.onFailure { exception ->
            when (exception) {
                is HttpException -> {
                    _event.send(UiEvent.EventDeleted)
                }

                else -> {
                    _uiState.update { it.copy(state = State.Error) }
                }
            }
        }
    }

    fun toggleLike() {
        likeJob?.cancel()
        likeJob = viewModelScope.launch {
            uiState.value.event?.runCatching {
                if (likedByMe) {
                    eventRepository.unlikeById(eventId)
                } else {
                    eventRepository.likeById(eventId)
                }
            }?.onFailure {
                _event.send(UiEvent.ErrorLikingEvent)
            }

            likeJob = null
        }
    }

    fun toggleParticipation() {
        participateJob?.cancel()
        participateJob = viewModelScope.launch {
            uiState.value.event?.runCatching {
                if (participatedByMe) {
                    eventRepository.refuseToParticipate(eventId)
                } else {
                    eventRepository.participate(eventId)
                }
            }?.onFailure {
                _event.send(UiEvent.ErrorParticipatingEvent)
            }

            participateJob = null
        }
    }

    fun delete() = viewModelScope.launch {
        runCatching {
            eventRepository.deleteById(eventId)
            _event.send(UiEvent.EventDeleted)
        }.onFailure {
            _event.send(UiEvent.ErrorRemovingEvent)
        }
    }
}

sealed interface State {
    data object Loading : State
    data object Success : State
    data object Error : State
}

data class EventUiState(
    val isLoggedIn: Boolean = false,
    val event: Event? = null,
    val state: State = State.Success
)

sealed interface UiEvent {
    data object EventDeleted : UiEvent

    data object ErrorLikingEvent : UiEvent
    data object ErrorParticipatingEvent : UiEvent
    data object ErrorRemovingEvent : UiEvent
}
