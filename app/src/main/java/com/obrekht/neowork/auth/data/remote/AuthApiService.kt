package com.obrekht.neowork.auth.data.remote

import com.obrekht.neowork.auth.model.AuthState
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthApiService {

    @FormUrlEncoded
    @POST("users/authentication")
    suspend fun logIn(
        @Field("login") login: String,
        @Field("pass") pass: String
    ): Response<AuthState>

    @Multipart
    @POST("users/registration")
    suspend fun signUp(
        @Part login: RequestBody,
        @Part pass: RequestBody,
        @Part name: RequestBody,
        @Part file: MultipartBody.Part?
    ): Response<AuthState>
}
