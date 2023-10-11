package com.obrekht.neowork.posts.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.obrekht.neowork.posts.model.Post
import java.time.Instant

data class PostData(
    @Embedded
    val post: PostEntity,
    @Relation(
        entity = LikeOwnerEntity::class,
        parentColumn = "postId",
        entityColumn = "postId",
        projection = ["userId"]
    )
    val likeOwnerIds: List<Long>,
    @Relation(
        entity = MentionEntity::class,
        parentColumn = "postId",
        entityColumn = "postId",
        projection = ["userId"]
    )
    val mentionIds: List<Long>
)

fun PostData.toModel() = Post(
    post.postId, post.authorId, post.author, post.authorJob, post.authorAvatar, post.content,
    Instant.ofEpochSecond(post.published), post.coords, post.link,
    mentionIds, post.mentionedMe, likeOwnerIds, post.likedByMe,
    post.attachment, emptyMap()
)

fun Post.toEntityData(isShown: Boolean = true) = PostData(
    post = PostEntity(id, authorId, author, authorJob, authorAvatar, content,
        published?.epochSecond ?: 0, coords, link,
        mentionedMe, likedByMe, attachment, isShown),
    likeOwnerIds = likeOwnerIds,
    mentionIds = mentionIds
)

fun List<PostData>.toModel() = map(PostData::toModel)
fun List<Post>.toEntityData(isShown: Boolean = true) = map { it.toEntityData(isShown) }