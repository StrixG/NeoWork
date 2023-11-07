package com.obrekht.neowork.events.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.core.model.Attachment
import com.obrekht.neowork.events.data.local.dao.EventDao
import com.obrekht.neowork.events.data.local.entity.EventEntity
import com.obrekht.neowork.events.data.local.entity.toEntity
import com.obrekht.neowork.events.data.local.entity.toModel
import com.obrekht.neowork.events.data.remote.EventApiService
import com.obrekht.neowork.events.data.remote.EventFeedRemoteMediator
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.media.data.MediaApiService
import com.obrekht.neowork.media.model.Media
import com.obrekht.neowork.media.model.MediaUpload
import com.obrekht.neowork.userpreview.data.local.dao.UserPreviewDao
import com.obrekht.neowork.userpreview.data.local.entity.toEntity
import com.obrekht.neowork.userpreview.data.local.entity.toModelMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.net.HttpURLConnection
import javax.inject.Inject

class CachedEventRepository @Inject constructor(
    private val auth: AppAuth,
    private val eventFeedRemoteMediator: EventFeedRemoteMediator,
    private val eventDao: EventDao,
    private val userPreviewDao: UserPreviewDao,
    private val eventApi: EventApiService,
    private val mediaApi: MediaApiService
) : EventRepository {

    private val loggedInUserId: Long
        get() = auth.state.value.id

    private val pagingSourceFactory = InvalidatingPagingSourceFactory {
        eventDao.pagingSource()
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagingData(config: PagingConfig): Flow<PagingData<Event>> = Pager(
        config = config,
        remoteMediator = eventFeedRemoteMediator,
        pagingSourceFactory = pagingSourceFactory
    ).flow.map {
        it.map(EventEntity::toModel)
    }.combine(auth.state) { pagingData, authState ->
        pagingData.map {
            it.copy(ownedByMe = (it.authorId == authState.id))
        }
    }

    override fun invalidatePagingSource() = pagingSourceFactory.invalidate()

    override suspend fun refreshEvent(eventId: Long) {
        val response = eventApi.getById(eventId)
        if (!response.isSuccessful) {
            if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                eventDao.deleteById(eventId)
            }
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        eventDao.upsert(body.toEntity())
        userPreviewDao.upsert(body.users.toEntity())
    }

    override suspend fun getEvent(eventId: Long): Event? = eventDao.getById(eventId)?.run {
        val userIds = getUserIds()
        val users = userPreviewDao.getByIds(userIds)
        toModel(users.toModelMap(), authorId == auth.state.value.id)
    }

    override fun getEventStream(eventId: Long): Flow<Event?> =
        eventDao.observeById(eventId).flatMapLatest { event ->
            event?.let {
                val userIds = event.getUserIds()
                userPreviewDao.observeByIds(userIds).map { previewList ->
                    event.toModel(users = previewList.toModelMap())
                }
            } ?: emptyFlow()
        }.combine(auth.state) { event, authState ->
            event.copy(ownedByMe = (event.authorId == authState.id))
        }

    override suspend fun participate(eventId: Long): Event = runCatching {
        eventDao.addParticipant(eventId, loggedInUserId)

        val response = eventApi.participate(eventId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val event = response.body() ?: throw HttpException(response)
        eventDao.upsert(event.toEntity())

        event
    }.getOrElse { exception ->
        eventDao.removeParticipant(eventId, loggedInUserId)
        throw exception
    }

    override suspend fun refuseToParticipate(eventId: Long): Event = runCatching {
        eventDao.removeParticipant(eventId, loggedInUserId)

        val response = eventApi.refuseToParticipate(eventId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val event = response.body() ?: throw HttpException(response)
        eventDao.upsert(event.toEntity())

        event
    }.getOrElse { exception ->
        eventDao.addParticipant(eventId, loggedInUserId)
        throw exception
    }

    override suspend fun likeById(eventId: Long): Event = runCatching {
        eventDao.likeById(eventId, loggedInUserId)

        val response = eventApi.likeById(eventId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val event = response.body() ?: throw HttpException(response)
        eventDao.upsert(event.toEntity())

        event
    }.getOrElse { exception ->
        eventDao.unlikeById(eventId, loggedInUserId)
        throw exception
    }

    override suspend fun unlikeById(eventId: Long): Event = runCatching {
        eventDao.unlikeById(eventId, loggedInUserId)

        val response = eventApi.unlikeById(eventId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val event = response.body() ?: throw HttpException(response)
        eventDao.upsert(event.toEntity())

        event
    }.getOrElse { exception ->
        eventDao.likeById(eventId, loggedInUserId)
        throw exception
    }

    override suspend fun save(event: Event, mediaUpload: MediaUpload?): Event {
        val savingEvent = mediaUpload?.let {
            val media = uploadMedia(mediaUpload.file)
            event.copy(attachment = Attachment(media.url, mediaUpload.type))
        } ?: event

        val response = eventApi.save(savingEvent)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val savedEvent = response.body() ?: throw HttpException(response)
        eventDao.upsert(savedEvent.toEntity())

        return savedEvent
    }

    override suspend fun deleteById(eventId: Long) {
        var event: EventEntity? = null
        runCatching {
            event = eventDao.getById(eventId)
            eventDao.deleteById(eventId)

            val response = eventApi.deleteById(eventId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
        }.onFailure { exception ->
            event?.let { eventDao.upsert(it) }
            throw exception
        }
    }

    override suspend fun delete(event: Event) = deleteById(event.id)

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
