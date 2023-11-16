package com.obrekht.neowork.events.ui.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.events.data.repository.EventRepository
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.media.data.local.AudioPlaybackManager
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
    private val audioPlaybackManager: AudioPlaybackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: Long = EventFragmentArgs.fromSavedStateHandle(savedStateHandle).eventId

    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent: Flow<UiEvent> = _uiEvent.receiveAsFlow()

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
                if (it == null) _uiEvent.send(UiEvent.EventDeleted)
            }.filterNotNull().onEach { event ->
                _uiState.update { it.copy(event = event, state = State.Success) }
            }.launchIn(this)

            combine(
                eventRepository.getEventStream(eventId),
                audioPlaybackManager.playbackState,
                audioPlaybackManager.nowPlaying
            ) { event, playbackState, nowPlaying ->
                val attachment = event?.attachment
                if (attachment != null && attachment.type == AttachmentType.AUDIO) {
                    _uiState.update {
                        it.copy(
                            isAudioPlaying = nowPlaying.mediaId == attachment.url
                                    && playbackState.isPlaying
                        )
                    }
                }
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
                    _uiEvent.send(UiEvent.EventDeleted)
                }

                else -> {
                    _uiState.update { it.copy(state = State.Error) }
                }
            }
        }
    }

    fun playAudio(url: String) = audioPlaybackManager.playUrl(url)

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
                _uiEvent.send(UiEvent.ErrorLikingEvent)
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
                _uiEvent.send(UiEvent.ErrorParticipatingEvent)
            }

            participateJob = null
        }
    }

    fun delete() = viewModelScope.launch {
        runCatching {
            eventRepository.deleteById(eventId)
            _uiEvent.send(UiEvent.EventDeleted)
        }.onFailure {
            _uiEvent.send(UiEvent.ErrorRemovingEvent)
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
    val isAudioPlaying: Boolean = false,
    val state: State = State.Success
)

sealed interface UiEvent {
    data object EventDeleted : UiEvent

    data object ErrorLikingEvent : UiEvent
    data object ErrorParticipatingEvent : UiEvent
    data object ErrorRemovingEvent : UiEvent
}
