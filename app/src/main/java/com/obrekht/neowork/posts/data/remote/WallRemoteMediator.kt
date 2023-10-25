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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class WallRemoteMediator @AssistedInject constructor(
    @Assisted private val userId: Long,
    private val database: PostDatabase,
    private val postDao: PostDao,
    private val wallApi: WallApiService
) : RemoteMediator<Int, PostData>() {

    @AssistedFactory
    interface Factory {
        fun create(userId: Long): WallRemoteMediator
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostData>
    ): MediatorResult {
        return try {
            state.firstItemOrNull()
            val response = when (loadType) {
                LoadType.REFRESH -> wallApi.getLatest(userId, state.config.initialLoadSize)

                LoadType.PREPEND -> {
                    val id = state.firstItemOrNull()?.post?.postId ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )

                    wallApi.getAfter(userId, id, state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val id = state.lastItemOrNull()?.post?.postId ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    wallApi.getBefore(userId, id, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            val body = response.body() ?: throw HttpException(response)

            database.withTransaction {
                postDao.upsertWithData(body.toEntityData())
            }
            MediatorResult.Success(endOfPaginationReached = body.isEmpty())
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> MediatorResult.Error(e)
                is CancellationException -> MediatorResult.Success(true)
                else -> throw e
            }
        }
    }
}