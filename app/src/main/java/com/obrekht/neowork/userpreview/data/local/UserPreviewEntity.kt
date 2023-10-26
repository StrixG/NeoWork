package com.obrekht.neowork.userpreview.data.local

import androidx.room.Entity
import com.obrekht.neowork.userpreview.model.UserPreview

@Entity("user_preview")
data class UserPreviewEntity(
    val name: String,
    val avatar: String? = null
)

fun UserPreviewEntity.toModel() = UserPreview(name, avatar)
fun UserPreview.toEntity() = UserPreviewEntity(name, avatar)

fun List<UserPreviewEntity>.toModel() = map(UserPreviewEntity::toModel)
fun List<UserPreview>.toEntity() = map(UserPreview::toEntity)