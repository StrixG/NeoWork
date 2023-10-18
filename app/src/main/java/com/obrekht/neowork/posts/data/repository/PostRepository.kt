package com.obrekht.neowork.posts.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.obrekht.neowork.media.model.MediaUpload
import com.obrekht.neowork.posts.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPagingData(config: PagingConfig): Flow<PagingData<Post>>
    fun invalidatePagingSource()

    fun getNewerCount(): Flow<Int>
    suspend fun showNewPosts()
    suspend fun getPost(postId: Long): Post?
    fun getPostStream(postId: Long): Flow<Post?>
    suspend fun likeById(postId: Long): Post
    suspend fun unlikeById(postId: Long): Post
    suspend fun deleteById(postId: Long)
    suspend fun remove(post: Post)
    suspend fun save(post: Post, mediaUpload: MediaUpload? = null): Post
    suspend fun refreshPost(postId: Long)
}