package com.obrekht.neowork.editor.ui.editor

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.obrekht.neowork.core.model.AttachmentType

class GetMediaContract : ActivityResultContracts.GetContent() {
    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input)
            .putExtra(
                Intent.EXTRA_MIME_TYPES,
                AttachmentType.getAllSupportedMimeTypes().toTypedArray()
            )
    }
}