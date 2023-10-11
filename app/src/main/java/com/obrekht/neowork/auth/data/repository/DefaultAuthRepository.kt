package com.obrekht.neowork.auth.data.repository

import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.auth.data.remote.AuthApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val appAuth: AppAuth,
    private val authService: AuthApiService
) : AuthRepository {

    override val isLoggedIn: Boolean
        get() = appAuth.state.value.id > 0

    override suspend fun logIn(username: String, password: String) = try {
        val response = authService.logIn(username, password)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val authState = response.body() ?: throw HttpException(response)
        authState.token?.let {
            appAuth.setAuth(authState.id, authState.token)
        }

        authState
    } catch (e: Exception) {
        throw e
    }

    override suspend fun signUp(
        username: String,
        password: String,
        name: String,
        avatar: File?
    ) = try {
        val media = if (avatar != null) MultipartBody.Part.createFormData(
            "file", avatar.name, avatar.asRequestBody()
        ) else null

        val response = authService.signUp(
            username.toRequestBody("text/plain".toMediaType()),
            password.toRequestBody("text/plain".toMediaType()),
            name.toRequestBody("text/plain".toMediaType()),
            media
        )
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val authState = response.body() ?: throw HttpException(response)
        authState.token?.let {
            appAuth.setAuth(authState.id, authState.token)
        }

        authState
    } catch (e: Exception) {
        throw e
    }
}
