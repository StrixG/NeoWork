package com.obrekht.neowork.media.data.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.obrekht.neowork.utils.copyToFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaCache @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val mimeTypeMap = MimeTypeMap.getSingleton()
    private val mediaCacheDir = context.cacheDir.resolve(DIRECTORY_NAME)

    init {
        clear()
        mediaCacheDir.mkdirs()
    }

    fun createFile(uri: Uri): File? {
        context.contentResolver.run {
            val extension = when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> mimeTypeMap.getExtensionFromMimeType(getType(uri))
                ContentResolver.SCHEME_FILE -> MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                else -> return null
            }

            return File.createTempFile(
                FILE_PREFIX,
                ".$extension",
                mediaCacheDir
            ).apply {
                deleteOnExit()
                copyToFile(uri, this)
            }
        }
    }

    fun clear() {
        mediaCacheDir.deleteRecursively()
    }

    companion object {
        const val FILE_PREFIX = "media"
        const val DIRECTORY_NAME = "media"
    }
}
