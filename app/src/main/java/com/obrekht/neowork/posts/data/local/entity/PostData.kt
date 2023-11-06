package com.obrekht.neowork.posts.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.userpreview.model.UserPreview

data class PostData(
    @Embedded
    val post: PostEntity,
    @Relation(
        entity = PostLikeOwnerEntity::class,
        parentColumn = "postId",
        entityColumn = "postId",
        projection = ["userId"]
    )
    val likeOwnerIds: Set<Long>,
    @Relation(
        entity = MentionEntity::class,
        parentColumn = "postId",
        entityColumn = "postId",
        projection = ["userId"]
    )
    val mentionIds: Set<Long>
)

fun PostData.toModel(users: Map<Long, UserPreview> = emptyMap()) = Post(
    post.postId, post.authorId, post.author, post.authorJob, post.authorAvatar, post.content,
    post.published, post.coords, post.link,
    mentionIds, post.mentionedMe, likeOwnerIds, post.likedByMe,
    post.attachment, users
)

fun Post.toEntityData(isShown: Boolean = true) = PostData(
    post = PostEntity(
        id, authorId, author, authorJob, authorAvatar, content,
        published, coords, link, mentionedMe, likedByMe, attachment, isShown
    ),
    likeOwnerIds = likeOwnerIds,
    mentionIds = mentionIds
)

fun List<PostData>.toModel() = map(PostData::toModel)
fun List<Post>.toEntityData(isShown: Boolean = true) = map { it.toEntityData(isShown) }
