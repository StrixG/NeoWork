package com.obrekht.neowork.userpreview.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.obrekht.neowork.userpreview.model.UserPreview

@Entity("user_preview")
data class UserPreviewEntity(
    @PrimaryKey
    val userId: Long,
    val name: String,
    val avatar: String? = null
)

fun UserPreviewEntity.toModel() = UserPreview(name, avatar)
fun List<UserPreviewEntity>.toModelMap(): Map<Long, UserPreview> = associate {
    it.userId to it.toModel()
}

fun Map<Long, UserPreview>.toEntity(): List<UserPreviewEntity> = map { (userId, preview) ->
    UserPreviewEntity(userId, preview.name, preview.avatar)
}
