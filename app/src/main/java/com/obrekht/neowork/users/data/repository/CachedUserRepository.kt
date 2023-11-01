package com.obrekht.neowork.users.data.repository

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
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
    private val db: PostDatabase,
    private val userDao: UserDao,
    private val userApi: UserApiService
) : UserRepository {

    private var pagingSourceFactory: InvalidatingPagingSourceFactory<Int, UserEntity>? = null

    override fun getPagingData(
        userIds: Collection<Long>?,
        config: PagingConfig
    ): Flow<PagingData<User>> {
        val factory = InvalidatingPagingSourceFactory {
            userIds?.let(userDao::pagingSource) ?: userDao.pagingSource()
        }
        pagingSourceFactory = factory

        return Pager(
            config = config,
            pagingSourceFactory = factory
        ).flow.map {
            it.map(UserEntity::toModel)
        }
    }

    override fun invalidatePagingSource() {
        pagingSourceFactory?.invalidate()
    }

    override suspend fun refreshAll() {
        val response = userApi.getAll()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)

        db.withTransaction {
            userDao.deleteAll()
            userDao.upsert(body.toEntity())
        }
    }

    override suspend fun refreshUsers(userIds: Collection<Long>) {
        val userList = mutableListOf<User>()
        for (userId in userIds) {
            val response = userApi.getById(userId)
            if (response.isSuccessful) {
                val user = response.body() ?: throw HttpException(response)
                userList.add(user)
            }
        }

        db.withTransaction {
            for (userId in userIds) {
                userDao.deleteById(userId)
            }
            userDao.upsert(userList.toEntity())
        }
    }

    override suspend fun refreshUser(userId: Long) {
        val response = userApi.getById(userId)
        if (!response.isSuccessful) {
            if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                userDao.deleteById(userId)
            }
            throw HttpException(response)
        }

        val body = response.body() ?: throw HttpException(response)
        userDao.upsert(body.toEntity())
    }

    override suspend fun getUser(userId: Long): User? = userDao.getById(userId)?.toModel()

    override fun getUserStream(userId: Long): Flow<User?> =
        userDao.observeById(userId).map { it?.toModel() }
}
