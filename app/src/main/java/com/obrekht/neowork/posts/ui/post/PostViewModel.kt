package com.obrekht.neowork.posts.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.posts.data.repository.CommentRepository
import com.obrekht.neowork.posts.data.repository.PostRepository
import com.obrekht.neowork.posts.model.Comment
import com.obrekht.neowork.posts.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: Long = PostFragmentArgs.fromSavedStateHandle(savedStateHandle).postId

    private val _uiState = MutableStateFlow(PostUiState())
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _event = Channel<UiEvent>()
    val event: Flow<UiEvent> = _event.receiveAsFlow()

    val isLoggedIn: Boolean
        get() = uiState.value.isLoggedIn

    private var likeJob: Job? = null

    init {
        viewModelScope.launch {
            refreshComments()

            appAuth.loggedInState.onEach { isLoggedIn ->
                _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
            }.launchIn(this)

            postRepository.getPostStream(postId).onEach {
                if (it == null) _event.send(UiEvent.PostDeleted)
            }.filterNotNull().onEach { post ->
                _uiState.update { it.copy(post = post, state = State.Success) }
            }.launchIn(this)

            commentRepository.getCommentListStream(postId).onEach { comments ->
                _uiState.update {
                    it.copy(
                        comments = comments,
                        commentsState = if (comments.isNotEmpty()) State.Success else it.commentsState
                    )
                }
            }.launchIn(this)
        }
    }

    fun refresh() {
        refreshPost()
        refreshComments(false)
    }

    fun refreshPost() = viewModelScope.launch {
        _uiState.update { it.copy(state = State.Loading) }
        runCatching {
            postRepository.refreshPost(postId)
        }.onFailure { exception ->
            when (exception) {
                is HttpException -> {
                    _event.send(UiEvent.PostDeleted)
                }

                else -> {
                    _uiState.update { it.copy(state = State.Error) }
                }
            }
        }
    }

    fun refreshComments(showLoadingState: Boolean = true) = viewModelScope.launch {
        if (showLoadingState) _uiState.update { it.copy(commentsState = State.Loading) }

        runCatching {
            commentRepository.refreshComments(postId)
            _uiState.update { it.copy(commentsState = State.Success) }
        }.onFailure {
            _uiState.update { it.copy(commentsState = State.Error) }
        }
    }

    fun toggleLike() {
        likeJob?.cancel()
        likeJob = viewModelScope.launch {
            uiState.value.post?.runCatching {
                if (likedByMe) {
                    postRepository.unlikeById(postId)
                } else {
                    postRepository.likeById(postId)
                }
            }?.onFailure {
                _event.send(UiEvent.ErrorLikingPost)
            }

            likeJob = null
        }
    }

    fun delete() = viewModelScope.launch {
        runCatching {
            postRepository.deleteById(postId)
            _event.send(UiEvent.PostDeleted)
        }.onFailure {
            _event.send(UiEvent.ErrorRemovingPost)
        }
    }

    fun getComment(commentId: Long): Flow<Comment?> = commentRepository.getCommentStream(commentId)

    fun toggleCommentLike(comment: Comment) = viewModelScope.launch {
        runCatching {
            if (comment.likedByMe) {
                commentRepository.unlikeCommentById(comment.id)
            } else {
                commentRepository.likeCommentById(comment.id)
            }
        }.onFailure {
            _event.send(UiEvent.ErrorLikingComment(comment.id))
        }
    }

    fun toggleCommentLikeById(commentId: Long) {
        val commentList = _uiState.value.comments
        val comment = commentList.find { it.id == commentId }

        comment?.let(::toggleCommentLike) ?: viewModelScope.launch {
            _event.send(UiEvent.ErrorLikingComment(commentId))
        }
    }

    fun deleteCommentById(commentId: Long) = viewModelScope.launch {
        runCatching {
            commentRepository.deleteCommentById(commentId)
        }.onFailure {
            _event.send(UiEvent.ErrorDeletingComment(commentId))
        }
    }

    fun sendComment(commentContent: String) = viewModelScope.launch {
        if (_uiState.value.isCommentSending || commentContent.isEmpty()) return@launch

        _uiState.update { it.copy(isCommentSending = true) }

        runCatching {
            commentRepository.save(
                Comment(
                    id = 0,
                    postId = postId,
                    content = commentContent.trim()
                )
            )
        }.also {
            _uiState.update { it.copy(isCommentSending = false) }
        }.onSuccess {
            _event.send(UiEvent.CommentSent)
        }.onFailure {
            it.printStackTrace()
            _event.send(UiEvent.ErrorSendingComment)
        }
    }
}

sealed interface State {
    data object Loading : State
    data object Success : State
    data object Error : State
}

data class PostUiState(
    val isLoggedIn: Boolean = false,
    val post: Post? = null,
    val state: State = State.Success,
    val comments: List<Comment> = emptyList(),
    val commentsState: State = State.Success,
    val isCommentSending: Boolean = false
)

sealed interface UiEvent {
    data object PostDeleted : UiEvent
    data object ErrorLikingPost : UiEvent
    data object ErrorRemovingPost : UiEvent

    data object CommentSent : UiEvent
    class ErrorLikingComment(val commentId: Long) : UiEvent
    data object ErrorSendingComment : UiEvent
    class ErrorDeletingComment(val commentId: Long) : UiEvent
}
