package com.obrekht.neowork.posts.data.local.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.obrekht.neowork.posts.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comment WHERE postId = :postId ORDER BY commentId")
    fun observeAllFromPost(postId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comment WHERE commentId = :commentId")
    fun observeById(commentId: Long): Flow<CommentEntity?>

    @Query("SELECT * FROM comment WHERE commentId = :commentId")
    suspend fun getById(commentId: Long): CommentEntity?

    @Upsert
    suspend fun upsert(comment: CommentEntity)

    @Upsert
    suspend fun upsert(commentList: List<CommentEntity>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(comment: CommentEntity)

    @Query("DELETE FROM comment")
    suspend fun deleteAll()

    @Query("DELETE FROM comment WHERE postId = :postId")
    suspend fun deleteAllFromPost(postId: Long)

    @Query("DELETE FROM comment WHERE commentId = :commentId")
    suspend fun deleteById(commentId: Long)

    @Transaction
    suspend fun likeById(commentId: Long, userId: Long) {
        getById(commentId)?.let {
            val likeOwnerIds = it.likeOwnerIds.toMutableSet().apply {
                add(userId)
            }
            upsert(it.copy(
                likeOwnerIds = likeOwnerIds.toList()
            ))
        }
    }

    @Transaction
    suspend fun unlikeById(commentId: Long, userId: Long) {
        getById(commentId)?.let {
            val likeOwnerIds = it.likeOwnerIds.toMutableSet().apply {
                remove(userId)
            }
            upsert(it.copy(
                likeOwnerIds = likeOwnerIds.toList()
            ))
        }
    }
}