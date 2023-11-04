package com.obrekht.neowork.events.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.media.model.MediaUpload
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getPagingData(config: PagingConfig): Flow<PagingData<Event>>
    fun invalidatePagingSource()

    suspend fun getEvent(eventId: Long): Event?
    fun getEventStream(eventId: Long): Flow<Event?>
    suspend fun participate(eventId: Long): Event
    suspend fun refuseToParticipate(eventId: Long): Event
    suspend fun likeById(eventId: Long): Event
    suspend fun unlikeById(eventId: Long): Event
    suspend fun save(event: Event, mediaUpload: MediaUpload? = null): Event
    suspend fun deleteById(eventId: Long)
    suspend fun delete(event: Event)
    suspend fun refreshEvent(eventId: Long)
}
