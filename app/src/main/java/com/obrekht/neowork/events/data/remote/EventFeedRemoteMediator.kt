package com.obrekht.neowork.events.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.obrekht.neowork.events.data.local.EventDatabase
import com.obrekht.neowork.events.data.local.dao.EventDao
import com.obrekht.neowork.events.data.local.entity.EventEntity
import com.obrekht.neowork.events.data.local.entity.toEntity
import com.obrekht.neowork.userpreview.data.local.dao.UserPreviewDao
import com.obrekht.neowork.userpreview.data.local.entity.toEntity
import com.obrekht.neowork.userpreview.model.UserPreview
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class EventFeedRemoteMediator @Inject constructor(
    private val database: EventDatabase,
    private val eventDao: EventDao,
    private val userPreviewDao: UserPreviewDao,
    private val eventApi: EventApiService
) : RemoteMediator<Int, EventEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, EventEntity>
    ): MediatorResult {
        return runCatching {
            state.firstItemOrNull()
            val response = when (loadType) {
                LoadType.REFRESH -> eventApi.getLatest(state.config.initialLoadSize)

                LoadType.PREPEND -> {
                    return MediatorResult.Success(
                        endOfPaginationReached = true
                    )
                }

                LoadType.APPEND -> {
                    val id = state.lastItemOrNull()?.eventId ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    eventApi.getBefore(id, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            val events = response.body() ?: throw HttpException(response)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    eventDao.deleteAll()
                }
                eventDao.upsert(events.toEntity())

                val previewMap = events.fold(mutableMapOf<Long, UserPreview>()) { acc, value ->
                    acc.apply { putAll(value.users) }
                }
                userPreviewDao.upsert(previewMap.toEntity())
            }
            MediatorResult.Success(endOfPaginationReached = events.isEmpty())
        }.getOrElse { exception ->
            when (exception) {
                is IOException, is HttpException -> MediatorResult.Error(exception)
                is CancellationException -> MediatorResult.Success(true)
                else -> throw exception
            }
        }
    }
}
