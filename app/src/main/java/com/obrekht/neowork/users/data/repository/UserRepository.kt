package com.obrekht.neowork.users.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.obrekht.neowork.users.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getPagingData(config: PagingConfig): Flow<PagingData<User>>
    fun invalidatePagingSource()

    suspend fun refreshAll()

    suspend fun getUser(userId: Long): User?
    fun getUserStream(userId: Long): Flow<User?>
    suspend fun refreshUser(userId: Long)
}