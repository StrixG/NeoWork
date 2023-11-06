package com.obrekht.neowork.posts.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.obrekht.neowork.posts.model.Comment
import java.time.Instant

@Entity(
    "comment", foreignKeys = [ForeignKey(
        entity = PostEntity::class,
        parentColumns = ["postId"],
        childColumns = ["postId"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )]
)
data class CommentEntity(
    @PrimaryKey
    val commentId: Long,
    @ColumnInfo(index = true)
    val postId: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: Instant?,
    val likeOwnerIds: Set<Long>
)

fun CommentEntity.toModel() = Comment(
    commentId, postId, authorId, author, authorAvatar, content,
    published, likeOwnerIds
)

fun Comment.toEntity() = CommentEntity(
    id, postId, authorId, author, authorAvatar, content,
    published, likeOwnerIds
)

fun List<CommentEntity>.toModel() = map(CommentEntity::toModel)
fun List<Comment>.toEntity() = map(Comment::toEntity)
