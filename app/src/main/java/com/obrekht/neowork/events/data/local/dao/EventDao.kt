package com.obrekht.neowork.events.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.obrekht.neowork.events.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Transaction
    @Query("SELECT * FROM event ORDER BY eventId DESC")
    fun pagingSource(): PagingSource<Int, EventEntity>

    @Transaction
    @Query("SELECT * FROM event ORDER BY eventId DESC LIMIT :count")
    fun observeLatest(count: Long = 1): Flow<List<EventEntity>>

    @Transaction
    @Query("SELECT * FROM event WHERE eventId = :id")
    fun observeById(id: Long): Flow<EventEntity?>

    @Transaction
    @Query("SELECT * FROM event ORDER BY eventId DESC LIMIT :count")
    suspend fun getLatest(count: Long = 1): List<EventEntity>

    @Transaction
    @Query("SELECT * FROM event WHERE eventId = :id")
    suspend fun getById(id: Long): EventEntity?

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Upsert
    suspend fun upsert(eventList: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM event")
    suspend fun deleteAll()

    @Query("DELETE FROM event WHERE authorId = :authorId")
    suspend fun deleteAllByAuthorId(authorId: Long)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM event WHERE eventId = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun addParticipant(eventId: Long, userId: Long) {
        getById(eventId)?.let {
            val participantIds = it.participantIds.toMutableSet()
            participantIds.add(userId)
            upsert(it.copy(participantIds = participantIds))
        }
    }

    @Transaction
    suspend fun removeParticipant(eventId: Long, userId: Long) {
        getById(eventId)?.let {
            val participantIds = it.participantIds.toMutableSet()
            participantIds.remove(userId)
            upsert(it.copy(participantIds = participantIds))
        }
    }

    @Transaction
    suspend fun likeById(eventId: Long, userId: Long) {
        getById(eventId)?.let {
            val likeOwnerIds = it.likeOwnerIds.toMutableSet()
            likeOwnerIds.add(userId)
            upsert(it.copy(likeOwnerIds = likeOwnerIds))
        }
    }

    @Transaction
    suspend fun unlikeById(eventId: Long, userId: Long) {
        getById(eventId)?.let {
            val likeOwnerIds = it.likeOwnerIds.toMutableSet()
            likeOwnerIds.remove(userId)
            upsert(it.copy(likeOwnerIds = likeOwnerIds))
        }
    }
}