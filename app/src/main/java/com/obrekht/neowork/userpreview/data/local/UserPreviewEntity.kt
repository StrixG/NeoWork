package com.obrekht.neowork.userpreview.data.local

import androidx.room.Entity

@Entity("user_preview")
data class UserPreviewEntity(
    val name: String,
    val avatar: String? = null
)
