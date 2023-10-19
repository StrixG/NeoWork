package com.obrekht.neowork.users.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long = 0,
    val login: String = "",
    val name: String = "",
    val avatar: String? = null
)
