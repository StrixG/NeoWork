package com.obrekht.neowork.editor.ui.editor

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.media.data.MediaCache
import com.obrekht.neowork.media.model.MediaUpload
import com.obrekht.neowork.posts.data.repository.CommentRepository
import com.obrekht.neowork.posts.data.repository.PostRepository
import com.obrekht.neowork.posts.model.Comment
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.utils.getMimeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val mediaCache: MediaCache,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = EditorFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private var edited: Any = getEmptyEditable()
    private var mentionedUserIds: Set<Long> = emptySet()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _attachment = MutableStateFlow<AttachmentModel?>(null)
    val attachment: StateFlow<AttachmentModel?> = _attachment.asStateFlow()

    private val _event = Channel<Event>()
    val event: Flow<Event> = _event.receiveAsFlow()

    init {
        if (args.id == 0L) {
            _uiState.update { it.copy(shouldInitialize = true) }
        } else {
            viewModelScope.launch {
                when (args.editableType) {
                    EditableType.POST -> postRepository.getPost(args.id)
                        ?.let { post ->
                            edited = post
                            _uiState.update {
                                it.copy(shouldInitialize = true, initialContent = post.content)
                            }
                            mentionedUserIds = post.mentionIds
                            post.attachment?.let {
                                _attachment.value = AttachmentModel(
                                    uri = it.url.toUri(),
                                    type = it.type
                                )
                            }
                        }

                    EditableType.COMMENT -> commentRepository.getCommentStream(args.id)
                        .firstOrNull()
                        ?.let { comment ->
                            edited = comment
                            _uiState.update {
                                it.copy(shouldInitialize = true, initialContent = comment.content)
                            }
                        }
                }
            }
        }
    }

    fun onInitialized() {
        _uiState.update { it.copy(shouldInitialize = false, initialized = true) }
    }

    fun save(content: String) {
        viewModelScope.launch {
            if (content.isBlank()) {
                _event.send(Event.ErrorEmptyContent)
            } else {
                try {
                    when (args.editableType) {
                        EditableType.POST -> {
                            val post = (edited as Post).copy(
                                content = content.trim(),
                                mentionIds = mentionedUserIds
                            )
                            val mediaUpload = _attachment.value?.let {
                                it.file?.let { file ->
                                    MediaUpload(file, it.type)
                                }
                            }
                            postRepository.save(post, mediaUpload)
                        }

                        EditableType.COMMENT -> {
                            val comment = (edited as Comment).copy(
                                content = content.trim()
                            )
                            commentRepository.save(comment)
                        }
                    }

                    clearEdited()
                    removeAttachment()

                    _event.send(Event.Saved)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _event.send(Event.ErrorSaving)
                }
            }
        }
    }

    fun changeAttachment(uri: Uri) {
        _attachment.update {
            it?.file?.delete()

            mediaCache.createFile(uri)?.let { file ->
                val mimeType = file.getMimeType()
                AttachmentType.getFromMimeType(mimeType)?.let { type ->
                    AttachmentModel(
                        file = file,
                        type = type
                    )
                }
            }
        }
    }

    fun removeAttachment() {
        _attachment.update {
            it?.file?.delete()

            null
        }
    }

    private fun clearEdited() {
        edited = getEmptyEditable()
    }

    private fun getEmptyEditable(): Any = when (args.editableType) {
        EditableType.POST -> Post()
        EditableType.COMMENT -> Comment()
    }

    fun setMentionedUserIds(userIds: Set<Long>) {
        mentionedUserIds = userIds
    }

    fun navigateToUserChooser() = viewModelScope.launch {
        _event.send(
            Event.NavigateToMentionedUsersChooser(mentionedUserIds.toLongArray())
        )
    }
}

data class EditorUiState(
    val initialContent: String = "",
    val shouldInitialize: Boolean = false,
    val initialized: Boolean = false
)

data class AttachmentModel(
    val uri: Uri? = null,
    val file: File? = null,
    val type: AttachmentType
)

sealed interface Event {
    data class NavigateToMentionedUsersChooser(val userIds: LongArray) : Event
    data object Saved : Event
    data object ErrorEmptyContent : Event
    data object ErrorSaving : Event
}
