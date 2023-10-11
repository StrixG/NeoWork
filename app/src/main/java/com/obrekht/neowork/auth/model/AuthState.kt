package com.obrekht.neowork.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthState(val id: Long = 0, val token: String? = null)
