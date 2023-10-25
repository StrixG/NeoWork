package com.obrekht.neowork.jobs.data.remote

import com.obrekht.neowork.jobs.model.Job
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface JobApiService {

    @GET("{userId}/jobs")
    suspend fun getByUserId(@Path("userId") userId: Long): Response<List<Job>>

    @POST("my/jobs")
    suspend fun save(@Body job: Job): Response<Job>

    @DELETE("my/jobs/{id}")
    suspend fun deleteById(@Path("id") postId: Long): Response<Unit>
}