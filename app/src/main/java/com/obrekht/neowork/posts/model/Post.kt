@file:UseSerializers(InstantSerializer::class)

package com.obrekht.neowork.posts.model

import com.obrekht.neowork.core.model.Attachment
import com.obrekht.neowork.core.model.UserPreview
import com.obrekht.neowork.utils.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class Post(
    val id: Long = 0,
    val authorId: Long = 0,
    val author: String = "",
    val authorJob: String? = null,
    val authorAvatar: String? = null,
    val content: String = "",
    val published: Instant? = null,
    val coords: Coordinates? = null,
    val link: String? = null,
    val mentionIds: List<Long> = emptyList(),
    val mentionedMe: Boolean = false,
    val likeOwnerIds: List<Long> = emptyList(),
    val likedByMe: Boolean = false,
    val attachment: Attachment? = null,
    val users: Map<Long, UserPreview> = emptyMap(),
    val ownedByMe: Boolean = false
)
