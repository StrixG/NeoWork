package com.obrekht.neowork.posts.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.core.model.Attachment
import com.obrekht.neowork.media.data.MediaApiService
import com.obrekht.neowork.media.model.Media
import com.obrekht.neowork.media.model.MediaUpload
import com.obrekht.neowork.posts.data.local.dao.PostDao
import com.obrekht.neowork.posts.data.local.entity.PostData
import com.obrekht.neowork.posts.data.local.entity.PostLikeOwnerEntity
import com.obrekht.neowork.posts.data.local.entity.toEntityData
import com.obrekht.neowork.posts.data.local.entity.toModel
import com.obrekht.neowork.posts.data.remote.PostApiService
import com.obrekht.neowork.posts.data.remote.PostRemoteMediator
import com.obrekht.neowork.posts.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

private const val GET_NEW_POSTS_INTERVAL = 10_000L

@Singleton
class CachedPostRepository @Inject constructor(
    private val auth: AppAuth,
    private val postRemoteMediator: PostRemoteMediator,
    private val postDao: PostDao,
    private val postApi: PostApiService,
    private val mediaApi: MediaApiService
) : PostRepository {

    private val loggedInUserId: Long
        get() = auth.state.value.id

    private val pagingSourceFactory = InvalidatingPagingSourceFactory {
        postDao.pagingSource()
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagingData(config: PagingConfig): Flow<PagingData<Post>> = Pager(
        config = config,
        remoteMediator = postRemoteMediator,
        pagingSourceFactory = pagingSourceFactory
    ).flow.map {
        it.map(PostData::toModel)
    }

    override fun invalidatePagingSource() = pagingSourceFactory.invalidate()

    override fun getNewerCount(): Flow<Int> = flow {
        while (currentCoroutineContext().isActive) {
            delay(GET_NEW_POSTS_INTERVAL)
            val postId = postDao.getLatest().firstOrNull()?.post?.postId ?: 0

            val response = postApi.getNewer(postId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            val body = response.body() ?: throw HttpException(response)
            if (body.isNotEmpty()) {
                postDao.upsertWithData(body.toEntityData(false))
            }
            emit(postDao.getNewerCount())
        }
    }
        .flowOn(Dispatchers.Default)

    override suspend fun showNewPosts() = try {
        postDao.showNewPosts()
    } catch (e: Exception) {
        throw e
    }

    override suspend fun refreshPost(postId: Long) = try {
        val response = postApi.getById(postId)
        if (!response.isSuccessful) {
            if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                postDao.deleteById(postId)
            }
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        postDao.upsertWithData(body.toEntityData())
    } catch (e: Exception) {
        throw e
    }

    override suspend fun getPost(postId: Long): Post? = postDao.getById(postId)?.toModel()

    override fun getPostStream(postId: Long): Flow<Post?> =
        postDao.observeById(postId).map { it?.toModel() }

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

    override suspend fun save(post: Post, mediaUpload: MediaUpload?): Post = try {
        val savingPost = mediaUpload?.let {
            val media = uploadMedia(mediaUpload.file)
            post.copy(attachment = Attachment(media.url, mediaUpload.type))
        } ?: post

        val response = postApi.save(savingPost)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        postDao.upsertWithData(body.toEntityData())

        body
    } catch (e: Exception) {
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

    private suspend fun uploadMedia(upload: File): Media = try {
        val media = MultipartBody.Part.createFormData(
            "file", upload.name, upload.asRequestBody()
        )

        val response = mediaApi.upload(media)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        response.body() ?: throw HttpException(response)
    } catch (e: Exception) {
        throw e
    }
}