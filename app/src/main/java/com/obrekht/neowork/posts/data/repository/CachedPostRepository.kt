package com.obrekht.neowork.posts.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.core.model.Attachment
import com.obrekht.neowork.media.data.remote.MediaApiService
import com.obrekht.neowork.media.model.Media
import com.obrekht.neowork.media.model.MediaUpload
import com.obrekht.neowork.posts.data.local.dao.PostDao
import com.obrekht.neowork.posts.data.local.entity.PostEntity
import com.obrekht.neowork.posts.data.local.entity.toEntity
import com.obrekht.neowork.posts.data.local.entity.toModel
import com.obrekht.neowork.posts.data.remote.PostApiService
import com.obrekht.neowork.posts.data.remote.PostFeedRemoteMediator
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.userpreview.data.local.dao.UserPreviewDao
import com.obrekht.neowork.userpreview.data.local.entity.toEntity
import com.obrekht.neowork.userpreview.data.local.entity.toModelMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val postFeedRemoteMediator: PostFeedRemoteMediator,
    private val postDao: PostDao,
    private val userPreviewDao: UserPreviewDao,
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
        remoteMediator = postFeedRemoteMediator,
        pagingSourceFactory = pagingSourceFactory
    ).flow.map {
        it.map(PostEntity::toModel)
    }.combine(auth.state) { pagingData, authState ->
        pagingData.map {
            it.copy(ownedByMe = (it.authorId == authState.id))
        }
    }

    override fun invalidatePagingSource() = pagingSourceFactory.invalidate()

    override fun getNewerCount(): Flow<Int> = flow {
        while (currentCoroutineContext().isActive) {
            delay(GET_NEW_POSTS_INTERVAL)
            val postId = postDao.getLatest().firstOrNull()?.postId ?: 0

            val response = postApi.getNewer(postId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            val body = response.body() ?: throw HttpException(response)
            if (body.isNotEmpty()) {
                postDao.upsert(body.toEntity(false))
            }
            emit(postDao.getNewerCount())
        }
    }
        .flowOn(Dispatchers.Default)

    override suspend fun showNewPosts() {
        postDao.showNewPosts()
    }

    override suspend fun refreshPost(postId: Long) {
        val response = postApi.getById(postId)
        if (!response.isSuccessful) {
            if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                postDao.deleteById(postId)
            }
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        postDao.upsert(body.toEntity())
        userPreviewDao.upsert(body.users.toEntity())
    }

    override suspend fun getPost(postId: Long): Post? = postDao.getById(postId)?.run {
        val userIds = likeOwnerIds union mentionIds
        val users = userPreviewDao.getByIds(userIds)
        toModel(users.toModelMap(), authorId == auth.state.value.id)
    }

    override fun getPostStream(postId: Long): Flow<Post?> =
        postDao.observeById(postId).map { it?.toModel() }.flatMapLatest { post ->
            post?.let {
                val userIds = post.likeOwnerIds union post.mentionIds
                userPreviewDao.observeByIds(userIds).map { previewList ->
                    post.copy(users = previewList.toModelMap())
                }
            } ?: flowOf(null)
        }.combine(auth.state) { post, authState ->
            post?.let {
                post.copy(ownedByMe = (post.authorId == authState.id))
            }
        }

    override suspend fun likeById(postId: Long): Post = runCatching {
        postDao.likeById(postId, loggedInUserId)

        val response = postApi.likeById(postId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val post = response.body() ?: throw HttpException(response)
        postDao.upsert(post.toEntity())

        post
    }.getOrElse { exception ->
        postDao.unlikeById(postId, loggedInUserId)
        throw exception
    }

    override suspend fun unlikeById(postId: Long): Post = runCatching {
        postDao.unlikeById(postId, loggedInUserId)

        val response = postApi.unlikeById(postId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val post = response.body() ?: throw HttpException(response)
        postDao.upsert(post.toEntity())

        post
    }.getOrElse { exception ->
        postDao.likeById(postId, loggedInUserId)
        throw exception
    }

    override suspend fun save(post: Post, mediaUpload: MediaUpload?): Post {
        val savingPost = mediaUpload?.let {
            val media = uploadMedia(mediaUpload.file)
            post.copy(attachment = Attachment(media.url, mediaUpload.type))
        } ?: post

        val response = postApi.save(savingPost)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        postDao.upsert(body.toEntity())

        return body
    }

    override suspend fun deleteById(postId: Long) {
        var post: PostEntity? = null
        runCatching {
            post = postDao.getById(postId)
            postDao.deleteById(postId)

            val response = postApi.deleteById(postId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
        }.onFailure { exception ->
            post?.let { postDao.upsert(it) }
            throw exception
        }
    }

    override suspend fun delete(post: Post) = deleteById(post.id)

    private suspend fun uploadMedia(upload: File): Media {
        val media = MultipartBody.Part.createFormData(
            "file", upload.name, upload.asRequestBody()
        )

        val response = mediaApi.upload(media)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        return response.body() ?: throw HttpException(response)
    }
}
