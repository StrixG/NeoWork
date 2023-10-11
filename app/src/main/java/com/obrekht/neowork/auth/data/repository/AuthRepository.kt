package com.obrekht.neowork.auth.data.repository

import com.obrekht.neowork.auth.model.AuthState
import java.io.File

interface AuthRepository {

    val isLoggedIn: Boolean

    suspend fun logIn(username: String, password: String): AuthState
    suspend fun signUp(username: String, password: String, name: String, avatar: File?): AuthState
}