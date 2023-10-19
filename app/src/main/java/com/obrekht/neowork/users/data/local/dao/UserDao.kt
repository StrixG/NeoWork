package com.obrekht.neowork.users.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.obrekht.neowork.users.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Transaction
    @Query("SELECT * FROM user ORDER BY userId DESC")
    fun pagingSource(): PagingSource<Int, UserEntity>

    @Transaction
    @Query("SELECT * FROM user ORDER BY userId DESC")
    fun observeAll(): Flow<List<UserEntity>>

    @Transaction
    @Query("SELECT * FROM user WHERE userId = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Transaction
    @Query("SELECT * FROM user WHERE userId = :id")
    suspend fun getById(id: Long): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsert(userList: List<UserEntity>)

    @Query("DELETE FROM user WHERE userId = :id")
    suspend fun deleteById(id: Long)
}