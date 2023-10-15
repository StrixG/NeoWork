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
import kotlinx.coroutines.flow.combine
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

    private val _event = Channel<Event>()
    val event: Flow<Event> = _event.receiveAsFlow()

    val isLoggedIn: Boolean
        get() = uiState.value.isLoggedIn

    private var likeJob: Job? = null

    init {
        viewModelScope.launch {
            refreshComments()

            appAuth.state.onEach { authState ->
                _uiState.update { it.copy(isLoggedIn = authState.id > 0L) }
            }.launchIn(this)

            postRepository.getPostStream(postId).combine(appAuth.state) { post, authState ->
                post?.copy(ownedByMe = post.authorId == authState.id)
            }.onEach { post ->
                if (post != null) {
                    _uiState.update { it.copy(post = post, state = State.Success) }
                } else {
                    _event.send(Event.PostDeleted)
                }
            }.launchIn(this)

            commentRepository.getCommentListStream(postId).combine(appAuth.state) { comments, authState ->
                comments.map { it.copy(
                    likedByMe = it.likeOwnerIds.contains(authState.id),
                    ownedByMe = it.authorId == authState.id
                ) }
            }.onEach { comments ->
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
        try {
            postRepository.refreshPost(postId)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    _event.send(Event.PostDeleted)
                }

                else -> {
                    _uiState.update { it.copy(state = State.Error) }
                }
            }
        }
    }

    fun refreshComments(showLoadingState: Boolean = true) = viewModelScope.launch {
        if (showLoadingState) _uiState.update { it.copy(commentsState = State.Loading) }

        try {
            commentRepository.refreshComments(postId)
            _uiState.update { it.copy(commentsState = State.Success) }
        } catch (e: Exception) {
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
                _event.send(Event.ErrorLikingPost)
            }

            likeJob = null
        }
    }

    fun remove() = viewModelScope.launch {
        try {
            postRepository.removeById(postId)
            _event.send(Event.PostDeleted)
        } catch (e: Exception) {
            _event.send(Event.ErrorRemovingPost)
        }
    }

    fun getComment(commentId: Long): Flow<Comment?> = commentRepository.getCommentStream(commentId)

    fun toggleCommentLike(comment: Comment) = viewModelScope.launch {
        try {
            if (comment.likedByMe) {
                commentRepository.unlikeCommentById(comment.id)
            } else {
                commentRepository.likeCommentById(comment.id)
            }
        } catch (e: Exception) {
            _event.send(Event.ErrorLikingComment(comment.id))
        }
    }

    fun toggleCommentLikeById(commentId: Long) {
        val commentList = _uiState.value.comments
        val comment = commentList.find { it.id == commentId }

        comment?.let(::toggleCommentLike) ?: viewModelScope.launch {
            _event.send(Event.ErrorLikingComment(commentId))
        }
    }

    fun deleteCommentById(commentId: Long) = viewModelScope.launch {
        try {
            commentRepository.deleteCommentById(commentId)
        } catch (e: Exception) {
            _event.send(Event.ErrorDeletingComment(commentId))
        }
    }

    fun sendComment(commentContent: String) = viewModelScope.launch {
        if (_uiState.value.isCommentSending || commentContent.isEmpty()) return@launch

        _uiState.update { it.copy(isCommentSending = true) }

        runCatching {
            commentRepository.saveComment(
                Comment(
                    id = 0,
                    postId = postId,
                    content = commentContent.trim()
                )
            )
        }.also {
            _uiState.update { it.copy(isCommentSending = false) }
        }.onSuccess {
            _event.send(Event.CommentSent)
        }.onFailure {
            it.printStackTrace()
            _event.send(Event.ErrorSendingComment)
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

sealed interface Event {
    data object PostDeleted : Event
    data object ErrorLikingPost : Event
    data object ErrorRemovingPost : Event

    data object CommentSent : Event
    class ErrorLikingComment(val commentId: Long) : Event
    data object ErrorSendingComment : Event
    class ErrorDeletingComment(val commentId: Long) : Event
}