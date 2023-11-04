package com.obrekht.neowork.editor.ui.editor

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.core.model.Coordinates
import com.obrekht.neowork.map.model.LocationPoint
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

    private val _edited = MutableStateFlow(getEmptyEditable())
    val edited: StateFlow<Any> = _edited.asStateFlow()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _attachment = MutableStateFlow<AttachmentModel?>(null)
    val attachment: StateFlow<AttachmentModel?> = _attachment.asStateFlow()

    private val _event = Channel<UiEvent>()
    val event: Flow<UiEvent> = _event.receiveAsFlow()

    init {
        if (args.id == 0L) {
            _uiState.update { it.copy(shouldInitialize = true) }
        } else {
            viewModelScope.launch {
                when (args.editableType) {
                    EditableType.POST -> postRepository.getPost(args.id)
                        ?.let { post ->
                            _edited.value = post
                            _uiState.update {
                                it.copy(shouldInitialize = true, initialContent = post.content)
                            }
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
                            _edited.value = comment
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

    fun save(content: String) = viewModelScope.launch {
        if (content.isBlank()) {
            _event.send(UiEvent.ErrorEmptyContent)
        } else {
            runCatching {
                when (args.editableType) {
                    EditableType.POST -> {
                        val post = (edited.value as Post).copy(
                            content = content.trim()
                        )
                        val mediaUpload = _attachment.value?.let {
                            it.file?.let { file ->
                                MediaUpload(file, it.type)
                            }
                        }
                        postRepository.save(post, mediaUpload)
                    }

                    EditableType.COMMENT -> {
                        val comment = (edited.value as Comment).copy(
                            content = content.trim()
                        )
                        commentRepository.save(comment)
                    }
                }

                clearEdited()
                removeAttachment()

                _event.send(UiEvent.Saved)
            }.onFailure {
                it.printStackTrace()
                _event.send(UiEvent.ErrorSaving)
            }
        }
    }

    fun setAttachment(uri: Uri) {
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

    fun setMentionedUserIds(userIds: Set<Long>) {
        _edited.update {
            when (it) {
                is Post -> {
                    it.copy(mentionIds = userIds)
                }
                else -> it
            }
        }
    }

    fun setLocationCoordinates(coordinates: Coordinates?) {
        _edited.update {
            when (it) {
                is Post -> {
                    it.copy(coords = coordinates)
                }
                else -> it
            }
        }
    }

    private fun clearEdited() {
        _edited.value = getEmptyEditable()
    }

    private fun getEmptyEditable(): Any = when (args.editableType) {
        EditableType.POST -> Post()
        EditableType.COMMENT -> Comment()
    }

    fun navigateToMentionedUsersChooser() = viewModelScope.launch {
        when (val editable = edited.value) {
            is Post -> _event.send(
                UiEvent.NavigateToMentionedUsersChooser(editable.mentionIds.toLongArray())
            )
        }
    }

    fun navigateToLocationPicker() = viewModelScope.launch {
        when (val editable = edited.value) {
            is Post -> {
                val locationPoint = editable.coords?.let {
                    LocationPoint(it.lat, it.long)
                }

                _event.send(UiEvent.NavigateToLocationPicker(locationPoint))
            }
        }
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

sealed interface UiEvent {
    class NavigateToMentionedUsersChooser(val userIds: LongArray) : UiEvent
    class NavigateToLocationPicker(val locationPoint: LocationPoint? = null) : UiEvent
    data object Saved : UiEvent
    data object ErrorEmptyContent : UiEvent
    data object ErrorSaving : UiEvent
}
