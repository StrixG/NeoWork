package com.obrekht.neowork.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val url: String,
    val type: AttachmentType
)

enum class AttachmentType(val supportedTypes: List<String>) {
    IMAGE(listOf("image/jpeg", "image/png")),
    VIDEO(listOf("video/mp4")),
    AUDIO(listOf("audio/mp3"));

    companion object {
        fun getFromMimeType(mimeType: String): AttachmentType? =
            values().find {
                it.supportedTypes.contains(mimeType)
            }

        fun getAllSupportedMimeTypes(): List<String> {
            return values().fold(emptySequence<String>()) { acc, next ->
                acc + next.supportedTypes
            }.toList()
        }
    }
}
