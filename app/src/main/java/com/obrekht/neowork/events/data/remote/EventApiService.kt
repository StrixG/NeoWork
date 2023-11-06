package com.obrekht.neowork.events.data.remote

import com.obrekht.neowork.events.model.Event
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EventApiService {

    @GET("events/{id}/newer")
    suspend fun getNewer(@Path("id") eventId: Long): Response<List<Event>>

    @GET("events/{id}/before")
    suspend fun getBefore(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Event>>

    @GET("events/{id}/after")
    suspend fun getAfter(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Event>>

    @GET("events/latest")
    suspend fun getLatest(@Query("count") count: Int): Response<List<Event>>

    @GET("events/{id}")
    suspend fun getById(@Path("id") eventId: Long): Response<Event>

    @POST("events")
    suspend fun save(@Body event: Event): Response<Event>

    @DELETE("events/{id}")
    suspend fun deleteById(@Path("id") eventId: Long): Response<Unit>

    @POST("events/{id}/likes")
    suspend fun likeById(@Path("id") eventId: Long): Response<Event>

    @DELETE("events/{id}/likes")
    suspend fun unlikeById(@Path("id") eventId: Long): Response<Event>

    @POST("events/{id}/participants")
    suspend fun participate(@Path("id") eventId: Long): Response<Event>

    @DELETE("events/{id}/participants")
    suspend fun refuseToParticipate(@Path("id") eventId: Long): Response<Event>
}
