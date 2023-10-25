package com.obrekht.neowork.users.data.repository

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.obrekht.neowork.auth.data.local.AppAuth
import com.obrekht.neowork.posts.data.local.PostDatabase
import com.obrekht.neowork.users.data.local.dao.UserDao
import com.obrekht.neowork.users.data.local.entity.UserEntity
import com.obrekht.neowork.users.data.local.entity.toEntity
import com.obrekht.neowork.users.data.local.entity.toModel
import com.obrekht.neowork.users.data.remote.UserApiService
import com.obrekht.neowork.users.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachedUserRepository @Inject constructor(
    private val auth: AppAuth,
    private val db: PostDatabase,
    private val userDao: UserDao,
    private val userApi: UserApiService
) : UserRepository {

    private val pagingSourceFactory = InvalidatingPagingSourceFactory {
        userDao.pagingSource()
    }

    override fun getPagingData(config: PagingConfig): Flow<PagingData<User>> = Pager(
        config = config,
        pagingSourceFactory = pagingSourceFactory
    ).flow.map {
        it.map(UserEntity::toModel)
    }

    override fun invalidatePagingSource() = pagingSourceFactory.invalidate()

    override suspend fun refreshAll() = try {
        userDao.deleteAll()

        val response = userApi.getAll()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        userDao.upsert(body.toEntity())
    } catch (e: Exception) {
        throw e
    }

    override suspend fun refreshUser(userId: Long) = try {
        val response = userApi.getById(userId)
        if (!response.isSuccessful) {
            if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                userDao.deleteById(userId)
            }
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        userDao.upsert(body.toEntity())
    } catch (e: Exception) {
        throw e
    }

    override suspend fun getUser(userId: Long): User? = userDao.getById(userId)?.toModel()

    override fun getUserStream(userId: Long): Flow<User?> =
        userDao.observeById(userId).map { it?.toModel() }
}