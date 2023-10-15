package com.obrekht.neowork.posts.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.obrekht.neowork.core.model.Attachment
import com.obrekht.neowork.posts.model.Coordinates
import java.time.Instant

@Entity("post")
data class PostEntity(
    @PrimaryKey
    val postId: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String? = null,
    val authorAvatar: String? = null,
    val content: String,
    val published: Instant?,
    @Embedded
    val coords: Coordinates? = null,
    val link: String? = null,
    val mentionedMe: Boolean = false,
    val likedByMe: Boolean = false,
    @Embedded("attachment")
    val attachment: Attachment? = null,
    val isShown: Boolean = true
)
