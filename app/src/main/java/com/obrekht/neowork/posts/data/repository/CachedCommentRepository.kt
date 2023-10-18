package com.obrekht.neowork.posts.data.repository

import androidx.room.withTransaction
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.posts.data.local.PostDatabase
import com.obrekht.neowork.posts.data.local.dao.CommentDao
import com.obrekht.neowork.posts.data.local.entity.CommentEntity
import com.obrekht.neowork.posts.data.local.entity.toEntity
import com.obrekht.neowork.posts.data.local.entity.toModel
import com.obrekht.neowork.posts.data.remote.CommentApiService
import com.obrekht.neowork.posts.model.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachedCommentRepository @Inject constructor(
    private val auth: AppAuth,
    private val db: PostDatabase,
    private val commentDao: CommentDao,
    private val commentApi: CommentApiService,
) : CommentRepository {

    private val loggedInUserId: Long
        get() = auth.state.value.id

    /**
     * Retrieve post comments from the network and update local database
     *
     * @param postId Post ID
     */
    override suspend fun refreshComments(postId: Long) = try {
        val response = commentApi.getPostComments(postId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val commentList = response.body() ?: throw HttpException(response)

        db.withTransaction {
            commentDao.deleteAllFromPost(postId)
            commentDao.upsert(commentList.toEntity())
        }
    } catch (e: Exception) {
        throw e
    }

    override fun getCommentListStream(postId: Long) =
        commentDao.observeAllFromPost(postId).map { it.toModel() }

    override fun getCommentStream(commentId: Long): Flow<Comment?> =
        commentDao.observeById(commentId).map { it?.toModel() }

    override suspend fun getComment(commentId: Long): Comment? =
        commentDao.getById(commentId)?.toModel()

    override suspend fun likeCommentById(commentId: Long): Comment = try {
        commentDao.likeById(commentId, loggedInUserId)

        val response = commentApi.likeById(commentId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val comment = response.body() ?: throw HttpException(response)
        commentDao.update(comment.toEntity())

        comment
    } catch (e: Exception) {
        commentDao.unlikeById(commentId, loggedInUserId)
        throw e
    }

    override suspend fun unlikeCommentById(commentId: Long): Comment = try {
        commentDao.unlikeById(commentId, loggedInUserId)

        val response = commentApi.unlikeById(commentId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val comment = response.body() ?: throw HttpException(response)
        commentDao.update(comment.toEntity())

        comment
    } catch (e: Exception) {
        commentDao.likeById(commentId, loggedInUserId)
        throw e
    }

    override suspend fun save(comment: Comment) = try {
        val response = commentApi.save(comment.postId, comment)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        commentDao.upsert(body.toEntity())

        body
    } catch (e: Exception) {
        throw e
    }

    override suspend fun deleteCommentById(commentId: Long) {
        var comment: CommentEntity? = null
        try {
            comment = commentDao.getById(commentId) ?: return
            commentDao.deleteById(commentId)

            val response = commentApi.deleteById(commentId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            comment?.let { commentDao.upsert(it) }
            throw e
        }
    }
}