package com.obrekht.neowork.posts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    "like_owner",
    foreignKeys = [ForeignKey(
        entity = PostEntity::class,
        parentColumns = ["postId"],
        childColumns = ["postId"],
        onDelete = ForeignKey.CASCADE
    )],
    primaryKeys = ["postId", "userId"]
)
data class LikeOwnerEntity(
    val postId: Long,
    val userId: Long
)
