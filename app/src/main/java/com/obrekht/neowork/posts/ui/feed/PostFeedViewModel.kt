package com.obrekht.neowork.posts.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.posts.data.repository.PostRepository
import com.obrekht.neowork.posts.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
class PostFeedViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val repository: PostRepository
) : ViewModel() {

    val data: Flow<PagingData<FeedItem>> = repository
        .getPagingData(
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

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState

    val isLoggedIn: Boolean
        get() = uiState.value.isLoggedIn

    private val _event = Channel<Event>()
    val event: Flow<Event> = _event.receiveAsFlow()

    private val likeJobs: HashMap<Long, Job> = hashMapOf()

    init {
        viewModelScope.launch {
            appAuth.state.onEach { authState ->
                repository.invalidatePagingSource()
                _uiState.update { it.copy(isLoggedIn = authState.id > 0L) }
            }.launchIn(this)

//            repository.getNewerCount().onEach { newerCount ->
//                _uiState.update { it.copy(newerCount = newerCount) }
//            }
//                .retry()
//                .launchIn(this)
        }
    }

    fun updateDataState(state: DataState) = _uiState.update { it.copy(dataState = state) }

    fun showNewPosts() = viewModelScope.launch {
        repository.showNewPosts()
        _uiState.update { it.copy(newerCount = 0) }
    }

    fun toggleLike(post: Post) {
        likeJobs[post.id]?.cancel()
        likeJobs[post.id] = viewModelScope.launch {
            try {
                if (post.likedByMe) {
                    repository.unlikeById(post.id)
                } else {
                    repository.likeById(post.id)
                }
            } catch (e: HttpException) {
                _event.send(Event.ErrorLikingPost(post.id))
            }

            likeJobs.remove(post.id)
        }
    }

    fun toggleLikeById(postId: Long) = viewModelScope.launch {
        val post = repository.getPost(postId)
        post?.let(::toggleLike) ?: _event.send(Event.ErrorLikingPost(postId))
    }

    fun deleteById(postId: Long) = viewModelScope.launch {
        try {
            repository.deleteById(postId)
        } catch (e: Exception) {
            _event.send(Event.ErrorRemovingPost(postId))
        }
    }

    fun remove(post: Post) = deleteById(post.id)

    fun scrollDone() {
        _uiState.update { it.copy(scrollDone = true) }
    }

    fun logOut() {
        appAuth.removeAuth()
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

sealed interface DataState {
    data object Loading : DataState
    data object Success : DataState
    data object Error : DataState
}

data class FeedUiState(
    val isLoggedIn: Boolean = false,
    val dataState: DataState = DataState.Success,
    val newerCount: Int = 0,
    val scrollDone: Boolean = false
)

sealed interface Event {
    data object ErrorLoadPosts : Event
    class ErrorLikingPost(val postId: Long) : Event
    class ErrorRemovingPost(val postId: Long) : Event
}