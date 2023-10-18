package com.obrekht.neowork.posts.data.repository

import com.obrekht.neowork.posts.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    suspend fun refreshComments(postId: Long)
    fun getCommentListStream(postId: Long): Flow<List<Comment>>
    fun getCommentStream(commentId: Long): Flow<Comment?>
    suspend fun getComment(commentId: Long): Comment?
    suspend fun likeCommentById(commentId: Long): Comment
    suspend fun unlikeCommentById(commentId: Long): Comment
    suspend fun save(comment: Comment): Comment
    suspend fun deleteCommentById(commentId: Long)
}