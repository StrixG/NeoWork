package com.obrekht.neowork.auth.data.repository

import java.io.File

interface AuthRepository {

    val isLoggedIn: Boolean

    suspend fun logIn(username: String, password: String)
    suspend fun signUp(username: String, password: String, name: String, avatar: File?)
}
