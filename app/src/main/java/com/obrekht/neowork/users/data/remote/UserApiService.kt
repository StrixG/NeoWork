package com.obrekht.neowork.users.data.remote

import com.obrekht.neowork.users.model.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface UserApiService {

    @GET("users")
    suspend fun getAll(): Response<List<User>>

    @GET("users/{id}")
    suspend fun getById(@Path("id") userId: Long): Response<User>
}