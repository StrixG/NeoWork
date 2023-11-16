package com.obrekht.neowork.posts.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.media.data.local.AudioPlaybackManager
import com.obrekht.neowork.posts.data.repository.PostRepository
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
    private val repository: PostRepository,
    private val audioPlaybackManager: AudioPlaybackManager
) : ViewModel() {

    val data: Flow<PagingData<PostListItem>> = repository
        .getPagingData(
            PagingConfig(
                pageSize = POSTS_PER_PAGE,
                initialLoadSize = POSTS_PER_PAGE * 2
            )
        )
        .map { pagingData ->
            val player = audioPlaybackManager.mediaController
            val currentMediaId = player?.currentMediaItem?.mediaId
            pagingData.map { post ->
                val attachment = post.attachment
                val isAudioPlaying = player != null
                        && attachment != null
                        && attachment.type == AttachmentType.AUDIO
                        && attachment.url == currentMediaId
                        && audioPlaybackManager.playbackState.value.isPlaying

                PostItem(post, isAudioPlaying)
            }
        }
        .map {
            it.insertDateSeparators()
        }
        .cachedIn(viewModelScope)
        .flowOn(Dispatchers.Default)

    private val _uiState = MutableStateFlow(PostFeedUiState())
    val uiState: StateFlow<PostFeedUiState> = _uiState

    val isLoggedIn: Boolean
        get() = uiState.value.isLoggedIn

    private val _event = Channel<UiEvent>()
    val event: Flow<UiEvent> = _event.receiveAsFlow()

    private val likeJobs: HashMap<Long, Job> = hashMapOf()

    init {
        viewModelScope.launch {
            appAuth.state.onEach { authState ->
                repository.invalidatePagingSource()
                _uiState.update { it.copy(isLoggedIn = authState.id > 0L) }
            }.launchIn(this)

            combine(
                audioPlaybackManager.playbackState,
                audioPlaybackManager.nowPlaying
            ) { _, _ ->
                repository.invalidatePagingSource()
            }.launchIn(this)
        }
    }

    fun updateDataState(state: DataState) = _uiState.update { it.copy(dataState = state) }

    fun playAudio(url: String) = audioPlaybackManager.playUrl(url)

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
                _event.send(UiEvent.ErrorLikingPost(post.id))
            }

            likeJobs.remove(post.id)
        }
    }

    fun toggleLikeById(postId: Long) = viewModelScope.launch {
        val post = repository.getPost(postId)
        post?.let(::toggleLike) ?: _event.send(UiEvent.ErrorLikingPost(postId))
    }

    fun deleteById(postId: Long) = viewModelScope.launch {
        try {
            repository.deleteById(postId)
        } catch (e: Exception) {
            _event.send(UiEvent.ErrorDeleting(postId))
        }
    }

    fun delete(post: Post) = deleteById(post.id)
}

private fun PagingData<PostItem>.insertDateSeparators() = insertSeparators { before, after ->
    val beforePostDate = before?.let {
        LocalDate.ofInstant(before.post.published, ZoneId.systemDefault())
    }

    val afterPostDate = after?.let {
        LocalDate.ofInstant(after.post.published, ZoneId.systemDefault())
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

data class PostFeedUiState(
    val isLoggedIn: Boolean = false,
    val dataState: DataState = DataState.Success,
    val newerCount: Int = 0
)

sealed interface UiEvent {
    class ErrorLikingPost(val postId: Long) : UiEvent
    class ErrorDeleting(val postId: Long) : UiEvent
}
