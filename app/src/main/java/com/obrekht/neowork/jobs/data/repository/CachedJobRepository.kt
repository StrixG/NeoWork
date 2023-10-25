package com.obrekht.neowork.jobs.data.repository

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.jobs.data.local.dao.JobDao
import com.obrekht.neowork.jobs.data.local.entity.JobEntity
import com.obrekht.neowork.jobs.data.local.entity.toEntity
import com.obrekht.neowork.jobs.data.local.entity.toModel
import com.obrekht.neowork.jobs.data.remote.JobApiService
import com.obrekht.neowork.jobs.model.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject

class CachedJobRepository @Inject constructor(
    private val auth: AppAuth,
    private val jobDao: JobDao,
    private val jobApi: JobApiService
) : JobRepository {

    private val loggedInUserId: Long
        get() = auth.state.value.id

    private var pagingSourceFactory: InvalidatingPagingSourceFactory<Int, JobEntity>? = null

    override fun getPagingData(userId: Long, config: PagingConfig): Flow<PagingData<Job>> {
        val factory = InvalidatingPagingSourceFactory {
            jobDao.pagingSource(userId)
        }
        pagingSourceFactory = factory

        return Pager(
            config = config,
            pagingSourceFactory = factory
        ).flow.map {
            it.map(JobEntity::toModel)
        }
    }

    override fun invalidatePagingSource() {
        pagingSourceFactory?.invalidate()
    }

    override suspend fun refreshUserJobs(userId: Long) = try {
        jobDao.deleteAllByUserId(userId)

        val response = jobApi.getByUserId(userId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        jobDao.upsert(body.toEntity(userId))
    } catch (e: Exception) {
        throw e
    }

    override suspend fun getJob(jobId: Long): Job? = try {
        jobDao.getById(jobId)?.toModel()
    } catch (e: Exception) {
        throw e
    }

    override suspend fun save(job: Job): Job = try {
        val response = jobApi.save(job)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        jobDao.upsert(body.toEntity(loggedInUserId))

        body
    } catch (e: Exception) {
        throw e
    }

    override suspend fun deleteById(jobId: Long) {
        var job: JobEntity? = null
        try {
            job = jobDao.getById(jobId)
            jobDao.deleteById(jobId)

            val response = jobApi.deleteById(jobId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            job?.let { jobDao.upsert(it) }
            throw e
        }
    }

    override suspend fun delete(job: Job) = deleteById(job.id)
}