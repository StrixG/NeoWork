package com.obrekht.neowork.editor.ui.editor

import androidx.annotation.StringRes
import com.obrekht.neowork.R

data class EditableConfig(
    val doesSupportAttachments: Boolean = false,
    @StringRes val hintInputField: Int = R.string.hint_edit,
    @StringRes val messageErrorEmpty: Int = R.string.error_empty_content,
    @StringRes val messageErrorSaving: Int = R.string.error_saving,
    @StringRes val messageErrorLoading: Int = R.string.error_loading,
) {
    companion object {
        private val DEFAULT_CONFIG = EditableConfig()

        val typeMap: Map<EditableType, EditableConfig> = mapOf(
            EditableType.POST to EditableConfig(
                doesSupportAttachments = true,
                hintInputField = R.string.hint_post_edit,
                messageErrorSaving = R.string.error_saving_post
            ),
            EditableType.COMMENT to EditableConfig(
                doesSupportAttachments = false,
                hintInputField = R.string.hint_comment_edit,
                messageErrorSaving = R.string.error_saving_comment
            ),
            EditableType.EVENT to EditableConfig(
                doesSupportAttachments = true,
                hintInputField = R.string.hint_event_edit,
                messageErrorSaving = R.string.error_saving_event
            )
        )

        fun getByType(editableType: EditableType) = typeMap[editableType] ?: DEFAULT_CONFIG
    }
}
