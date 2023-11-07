package com.obrekht.neowork.posts.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.obrekht.neowork.core.model.Attachment
import com.obrekht.neowork.core.model.Coordinates
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.userpreview.model.UserPreview
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
    val mentionIds: Set<Long> = emptySet(),
    val mentionedMe: Boolean = false,
    val likeOwnerIds: Set<Long> = emptySet(),
    val likedByMe: Boolean = false,
    @Embedded("attachment")
    val attachment: Attachment? = null,
    val isShown: Boolean = true
)

fun PostEntity.toModel(users: Map<Long, UserPreview> = emptyMap(), ownedByMe: Boolean = false) =
    Post(
        postId, authorId, author, authorJob, authorAvatar, content,
        published, coords, link,
        mentionIds, mentionedMe, likeOwnerIds, likedByMe,
        attachment, users, ownedByMe
    )

fun Post.toEntity(isShown: Boolean = true) = PostEntity(
    id, authorId, author, authorJob, authorAvatar, content,
    published, coords, link,
    mentionIds, mentionedMe, likeOwnerIds, likedByMe,
    attachment, isShown
)

fun List<PostEntity>.toModel() = map(PostEntity::toModel)
fun List<Post>.toEntity(isShown: Boolean = true) = map { it.toEntity(isShown) }
