package com.obrekht.neowork.media.model

import com.obrekht.neowork.core.model.AttachmentType
import java.io.File

data class MediaUpload(
    val file: File,
    val type: AttachmentType
)