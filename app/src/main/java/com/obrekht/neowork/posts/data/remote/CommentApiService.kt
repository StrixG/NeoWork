package com.obrekht.neowork.posts.data.remote

import com.obrekht.neowork.posts.model.Comment
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CommentApiService {
    @GET("posts/{id}/comments")
    suspend fun getPostComments(@Path("id") postId: Long): Response<List<Comment>>

    @POST("posts/{id}/comments")
    suspend fun save(@Path("id") postId: Long, @Body comment: Comment): Response<Comment>

    @DELETE("posts/0/comments/{id}")
    suspend fun deleteById(@Path("id") commentId: Long): Response<Unit>

    @POST("posts/0/comments/{id}/likes")
    suspend fun likeById(@Path("id") commentId: Long): Response<Comment>

    @DELETE("posts/0/comments/{id}/likes")
    suspend fun unlikeById(@Path("id") commentId: Long): Response<Comment>
}