package com.obrekht.neowork.jobs.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.obrekht.neowork.jobs.model.Job
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun getPagingData(userId: Long, config: PagingConfig): Flow<PagingData<Job>>
    fun invalidatePagingSource()

    suspend fun getJob(jobId: Long): Job?

    suspend fun refreshUserJobs(userId: Long)
    suspend fun save(job: Job): Job
    suspend fun deleteById(jobId: Long)
    suspend fun delete(job: Job)
}