package com.obrekht.neowork.events.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.events.data.repository.EventRepository
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.events.ui.common.DateSeparatorItem
import com.obrekht.neowork.events.ui.common.EventItem
import com.obrekht.neowork.events.ui.common.EventListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private const val EVENTS_PER_PAGE = 10

@HiltViewModel
class EventFeedViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val repository: EventRepository
) : ViewModel() {

    val data: Flow<PagingData<EventListItem>> = repository
        .getPagingData(
            PagingConfig(
                pageSize = EVENTS_PER_PAGE,
                initialLoadSize = EVENTS_PER_PAGE * 2
            )
        )
        .map {
            it.map(::EventItem).insertDateSeparators()
        }
        .cachedIn(viewModelScope)
        .flowOn(Dispatchers.Default)

    private val _uiState = MutableStateFlow(EventFeedUiState())
    val uiState: StateFlow<EventFeedUiState> = _uiState

    val isLoggedIn: Boolean
        get() = uiState.value.isLoggedIn

    private val _event = Channel<UiEvent>()
    val event: Flow<UiEvent> = _event.receiveAsFlow()

    private val likeJobs: HashMap<Long, Job> = hashMapOf()
    private val participateJobs: HashMap<Long, Job> = hashMapOf()

    init {
        viewModelScope.launch {
            appAuth.state.onEach { authState ->
                repository.invalidatePagingSource()
                _uiState.update { it.copy(isLoggedIn = authState.id > 0L) }
            }.launchIn(this)
        }
    }

    fun updateDataState(state: DataState) = _uiState.update { it.copy(dataState = state) }

    fun toggleLike(event: Event) {
        likeJobs[event.id]?.cancel()
        likeJobs[event.id] = viewModelScope.launch {
            try {
                if (event.likedByMe) {
                    repository.unlikeById(event.id)
                } else {
                    repository.likeById(event.id)
                }
            } catch (e: HttpException) {
                _event.send(UiEvent.ErrorLikingEvent(event.id))
            }

            likeJobs.remove(event.id)
        }
    }

    fun toggleParticipation(event: Event) {
        participateJobs[event.id]?.cancel()
        participateJobs[event.id] = viewModelScope.launch {
            try {
                if (event.participatedByMe) {
                    repository.refuseToParticipate(event.id)
                } else {
                    repository.participate(event.id)
                }
            } catch (e: HttpException) {
                _event.send(UiEvent.ErrorParticipatingEvent(event.id))
            }

            participateJobs.remove(event.id)
        }
    }

    fun toggleParticipationById(eventId: Long) = viewModelScope.launch {
        val event = repository.getEvent(eventId)
        event?.let(::toggleParticipation) ?: _event.send(UiEvent.ErrorParticipatingEvent(eventId))
    }

    fun toggleLikeById(eventId: Long) = viewModelScope.launch {
        val event = repository.getEvent(eventId)
        event?.let(::toggleLike) ?: _event.send(UiEvent.ErrorLikingEvent(eventId))
    }

    fun deleteById(eventId: Long) = viewModelScope.launch {
        try {
            repository.deleteById(eventId)
        } catch (e: Exception) {
            _event.send(UiEvent.ErrorDeleting(eventId))
        }
    }

    fun delete(event: Event) = deleteById(event.id)
}

private fun PagingData<EventItem>.insertDateSeparators() = insertSeparators { before, after ->
    val beforeEventDate = before?.let {
        LocalDate.ofInstant(before.event.published, ZoneId.systemDefault())
    }

    val afterEventDate = after?.let {
        LocalDate.ofInstant(after.event.published, ZoneId.systemDefault())
    }

    if (afterEventDate != null && beforeEventDate != afterEventDate) {
        DateSeparatorItem(afterEventDate)
    } else {
        null
    }
}

sealed interface DataState {
    data object Loading : DataState
    data object Success : DataState
    data object Error : DataState
}

data class EventFeedUiState(
    val isLoggedIn: Boolean = false,
    val dataState: DataState = DataState.Success
)

sealed interface UiEvent {
    class ErrorLikingEvent(val eventId: Long) : UiEvent
    class ErrorParticipatingEvent(val eventId: Long) : UiEvent
    class ErrorDeleting(val eventId: Long) : UiEvent
}
