package com.obrekht.neowork.users.ui.profile.wall

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.posts.data.repository.WallRepository
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.posts.ui.common.DateSeparatorItem
import com.obrekht.neowork.posts.ui.common.PostItem
import com.obrekht.neowork.posts.ui.common.PostListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

private const val POSTS_PER_PAGE = 10

@HiltViewModel
class WallViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val wallRepository: WallRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Long = savedStateHandle.get<Long>(WallArguments.USER_ID) ?: 0L

    private val _uiState = MutableStateFlow(WallUiState())
    val uiState: StateFlow<WallUiState> = _uiState.asStateFlow()

    private val _event = Channel<Event>()
    val event: Flow<Event> = _event.receiveAsFlow()

    private val likeJobs: HashMap<Long, Job> = hashMapOf()

    val data: Flow<PagingData<PostListItem>> = wallRepository
        .getPagingData(
            userId,
            PagingConfig(
                pageSize = POSTS_PER_PAGE,
                initialLoadSize = POSTS_PER_PAGE * 2
            )
        )
        .cachedIn(viewModelScope)
        .combine(appAuth.state) { pagingData, authState ->
            pagingData.map {
                PostItem(it.copy(ownedByMe = it.authorId == authState.id))
            }
        }.map {
            it.insertDateSeparators()
        }.flowOn(Dispatchers.Default)

    init {
        viewModelScope.launch {
            appAuth.state.onEach {
                wallRepository.invalidatePagingSource()
            }.launchIn(this)
        }
    }

    fun togglePostLike(post: Post) {
        likeJobs[post.id]?.cancel()
        likeJobs[post.id] = viewModelScope.launch {
            try {
                if (post.likedByMe) {
                    wallRepository.unlikeById(post.id)
                } else {
                    wallRepository.likeById(post.id)
                }
            } catch (e: HttpException) {
                _event.send(Event.ErrorLiking)
            }

            likeJobs.remove(post.id)
        }
    }

    fun deletePostById(postId: Long) = viewModelScope.launch {
        try {
            wallRepository.deleteById(postId)
        } catch (e: Exception) {
            _event.send(Event.ErrorDeleting)
        }
    }

    fun deletePost(post: Post) = deletePostById(post.id)

    fun updateDataState(dataState: DataState) {
        _uiState.update {
            it.copy(dataState = dataState)
        }
    }
}

private fun PagingData<PostItem>.insertDateSeparators() = insertSeparators { before, after ->
    val beforePostDate = before?.let {
        LocalDate.ofInstant(
            before.post.published,
            ZoneId.systemDefault()
        )
    }

    val afterPostDate = after?.let {
        LocalDate.ofInstant(
            after.post.published,
            ZoneId.systemDefault()
        )
    }

    if (afterPostDate != null && beforePostDate != afterPostDate) {
        DateSeparatorItem(afterPostDate)
    } else {
        null
    }
}

data class WallUiState(
    val dataState: DataState = DataState.Success
)

sealed interface DataState {
    data object Loading : DataState
    data object Success : DataState
    data object Error : DataState
}

sealed interface Event {
    data object ErrorDeleting : Event
    data object ErrorLiking: Event
}