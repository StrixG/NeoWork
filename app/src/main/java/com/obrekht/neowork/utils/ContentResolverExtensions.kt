package com.obrekht.neowork.utils

import android.content.ContentResolver
import android.net.Uri
import java.io.File

fun ContentResolver.copyToFile(uri: Uri, file: File) {
    openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}
