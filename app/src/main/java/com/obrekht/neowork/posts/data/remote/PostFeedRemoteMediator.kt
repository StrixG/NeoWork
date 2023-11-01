package com.obrekht.neowork.posts.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.obrekht.neowork.posts.data.local.PostDatabase
import com.obrekht.neowork.posts.data.local.dao.PostDao
import com.obrekht.neowork.posts.data.local.entity.PostData
import com.obrekht.neowork.posts.data.local.entity.toEntityData
import com.obrekht.neowork.userpreview.data.local.dao.UserPreviewDao
import com.obrekht.neowork.userpreview.data.local.entity.toEntity
import com.obrekht.neowork.userpreview.model.UserPreview
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class PostFeedRemoteMediator @Inject constructor(
    private val database: PostDatabase,
    private val postDao: PostDao,
    private val userPreviewDao: UserPreviewDao,
    private val postApi: PostApiService
) : RemoteMediator<Int, PostData>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostData>
    ): MediatorResult {
        return runCatching {
            state.firstItemOrNull()
            val response = when (loadType) {
                LoadType.REFRESH -> postApi.getLatest(state.config.initialLoadSize)

                LoadType.PREPEND -> {
                    return MediatorResult.Success(
                        endOfPaginationReached = true
                    )
                }

                LoadType.APPEND -> {
                    val id = state.lastItemOrNull()?.post?.postId ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    postApi.getBefore(id, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            val post = response.body() ?: throw HttpException(response)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    postDao.deleteAll()
                }
                postDao.upsertWithData(post.toEntityData())

                val previewMap = post.fold(mutableMapOf<Long, UserPreview>()) { acc, value ->
                    acc.apply { putAll(value.users) }
                }
                userPreviewDao.upsert(previewMap.toEntity())
            }
            MediatorResult.Success(endOfPaginationReached = post.isEmpty())
        }.getOrElse { exception ->
            when (exception) {
                is IOException, is HttpException -> MediatorResult.Error(exception)
                is CancellationException -> MediatorResult.Success(true)
                else -> throw exception
            }
        }
    }

    override suspend fun initialize(): InitializeAction {
        return super.initialize()
    }
}
