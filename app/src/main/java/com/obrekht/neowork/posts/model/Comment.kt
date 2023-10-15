@file:UseSerializers(InstantSerializer::class)

package com.obrekht.neowork.posts.model

import com.obrekht.neowork.utils.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class Comment(
    val id: Long = 0,
    val postId: Long = 0,
    val authorId: Long = 0,
    val author: String = "",
    val authorAvatar: String? = null,
    val content: String = "",
    val published: Instant? = null,
    val likeOwnerIds: List<Long> = emptyList(),
    val likedByMe: Boolean = false,
    val ownedByMe: Boolean = false
)
