package com.obrekht.neowork.posts.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.posts.data.local.dao.PostDao
import com.obrekht.neowork.posts.data.local.entity.PostData
import com.obrekht.neowork.posts.data.local.entity.PostLikeOwnerEntity
import com.obrekht.neowork.posts.data.local.entity.toEntityData
import com.obrekht.neowork.posts.data.local.entity.toModel
import com.obrekht.neowork.posts.data.remote.PostApiService
import com.obrekht.neowork.posts.data.remote.WallApiService
import com.obrekht.neowork.posts.data.remote.WallRemoteMediator
import com.obrekht.neowork.posts.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject

class CachedWallRepository @Inject constructor(
    private val auth: AppAuth,
    private val postDao: PostDao,
    private val postApi: PostApiService,
    private val wallApi: WallApiService
) : WallRepository {

    private val loggedInUserId: Long
        get() = auth.state.value.id

    @Inject
    lateinit var wallRemoteMediatorFactory: WallRemoteMediator.Factory

    private var pagingSourceFactory: InvalidatingPagingSourceFactory<Int, PostData>? = null

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagingData(userId: Long, config: PagingConfig): Flow<PagingData<Post>> {
        val wallRemoteMediator = wallRemoteMediatorFactory.create(userId)
        val factory = InvalidatingPagingSourceFactory {
            postDao.userWallPagingSource(userId)
        }
        pagingSourceFactory = factory

        return Pager(
            config = config,
            remoteMediator = wallRemoteMediator,
            pagingSourceFactory = factory
        ).flow.map {
            it.map(PostData::toModel)
        }
    }

    override fun invalidatePagingSource() {
        pagingSourceFactory?.invalidate()
    }

    override suspend fun likeById(postId: Long): Post = try {
        postDao.like(PostLikeOwnerEntity(postId, loggedInUserId))

        val response = postApi.likeById(postId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val post = response.body() ?: throw HttpException(response)
        postDao.upsertWithData(post.toEntityData())

        post
    } catch (e: Exception) {
        postDao.unlike(PostLikeOwnerEntity(postId, loggedInUserId))
        throw e
    }

    override suspend fun unlikeById(postId: Long): Post = try {
        postDao.unlike(PostLikeOwnerEntity(postId, loggedInUserId))

        val response = postApi.unlikeById(postId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val post = response.body() ?: throw HttpException(response)
        postDao.upsertWithData(post.toEntityData())

        post
    } catch (e: Exception) {
        postDao.like(PostLikeOwnerEntity(postId, loggedInUserId))
        throw e
    }

    override suspend fun deleteById(postId: Long) {
        var post: PostData? = null
        try {
            post = postDao.getById(postId)
            postDao.deleteById(postId)

            val response = postApi.deleteById(postId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            post?.let { postDao.upsertWithData(it) }
            throw e
        }
    }

    override suspend fun delete(post: Post) = deleteById(post.id)
}