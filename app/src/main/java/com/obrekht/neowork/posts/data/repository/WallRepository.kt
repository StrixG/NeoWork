package com.obrekht.neowork.posts.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.obrekht.neowork.posts.model.Post
import kotlinx.coroutines.flow.Flow

interface WallRepository {
    fun getPagingData(userId: Long, config: PagingConfig): Flow<PagingData<Post>>
    fun invalidatePagingSource()

    suspend fun refreshAll(userId: Long)

    suspend fun likeById(postId: Long): Post
    suspend fun unlikeById(postId: Long): Post
    suspend fun deleteById(postId: Long)
    suspend fun delete(post: Post)
}