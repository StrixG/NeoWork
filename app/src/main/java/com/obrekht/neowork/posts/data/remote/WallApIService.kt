package com.obrekht.neowork.posts.data.remote

import com.obrekht.neowork.posts.model.Post
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WallApiService {

    @GET("{userId}/wall/{id}/newer")
    suspend fun getNewer(
        @Path("userId") userId: Long,
        @Path("id") postId: Long
    ): Response<List<Post>>

    @GET("{userId}/wall/{id}/before")
    suspend fun getBefore(
        @Path("userId") userId: Long,
        @Path("id") postId: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("{userId}/wall/{id}/after")
    suspend fun getAfter(
        @Path("userId") userId: Long,
        @Path("id") postId: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("{userId}/wall/latest")
    suspend fun getLatest(
        @Path("userId") userId: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @POST("{userId}/wall/{id}/likes")
    suspend fun likeById(
        @Path("userId") userId: Long,
        @Path("id") postId: Long
    ): Response<Post>

    @DELETE("{userId}/wall/{id}/likes")
    suspend fun unlikeById(
        @Path("userId") userId: Long,
        @Path("id") postId: Long
    ): Response<Post>
}