package com.obrekht.neowork.userpreview.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.obrekht.neowork.userpreview.data.local.entity.UserPreviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreviewDao {

    @Query("SELECT * FROM user_preview WHERE userId IN (:userIds)")
    fun observeByIds(userIds: Collection<Long>): Flow<List<UserPreviewEntity>>

    @Query("SELECT * FROM user_preview WHERE userId IN (:userIds)")
    suspend fun getByIds(userIds: Collection<Long>): List<UserPreviewEntity>

    @Upsert
    suspend fun upsert(previewList: List<UserPreviewEntity>)

    @Query("DELETE FROM user_preview WHERE userId IN(:userIds)")
    suspend fun deleteByIds(userIds: Collection<Long>)
}
