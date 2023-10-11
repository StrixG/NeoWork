package com.obrekht.neowork.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreview(
    val name: String,
    val avatar: String? = null
)