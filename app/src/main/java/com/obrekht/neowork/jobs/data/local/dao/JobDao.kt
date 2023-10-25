package com.obrekht.neowork.jobs.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.obrekht.neowork.jobs.data.local.entity.JobEntity

@Dao
interface JobDao {

    @Transaction
    @Query("SELECT * FROM job WHERE userId = :userId ORDER BY start DESC")
    fun pagingSource(userId: Long): PagingSource<Int, JobEntity>

    @Query("SELECT * FROM job WHERE jobId = :id")
    suspend fun getById(id: Long): JobEntity?

    @Upsert
    suspend fun upsert(user: JobEntity)

    @Upsert
    suspend fun upsert(userList: List<JobEntity>)

    @Query("DELETE FROM job WHERE jobId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM job WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: Long)
}