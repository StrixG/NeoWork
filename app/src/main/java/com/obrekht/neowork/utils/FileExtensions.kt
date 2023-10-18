package com.obrekht.neowork.utils

import android.webkit.MimeTypeMap
import java.io.File

fun File.getMimeType(fallback: String = "*/*"): String {
    return MimeTypeMap.getFileExtensionFromUrl(path)
        ?.run { MimeTypeMap.getSingleton().getMimeTypeFromExtension(lowercase()) }
        ?: fallback
}