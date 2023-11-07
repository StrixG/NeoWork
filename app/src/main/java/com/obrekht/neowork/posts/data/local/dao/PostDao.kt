package com.obrekht.neowork.posts.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.obrekht.neowork.posts.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM post WHERE isShown = 1 ORDER BY postId DESC")
    fun pagingSource(): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM post WHERE authorId = :userId ORDER BY postId DESC")
    fun userWallPagingSource(userId: Long): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM post ORDER BY postId DESC LIMIT :count")
    fun observeLatest(count: Long = 1): Flow<List<PostEntity>>

    @Query("SELECT * FROM post WHERE postId = :id")
    fun observeById(id: Long): Flow<PostEntity?>

    @Query("SELECT * FROM post ORDER BY postId DESC LIMIT :count")
    suspend fun getLatest(count: Long = 1): List<PostEntity>

    @Query("SELECT * FROM post WHERE postId = :id")
    suspend fun getById(id: Long): PostEntity?

    @Query("SELECT COUNT(*) FROM post WHERE isShown = 0 ")
    suspend fun getNewerCount(): Int

    @Query("UPDATE post SET isShown = 1 WHERE isShown = 0")
    suspend fun showNewPosts()

    @Upsert
    suspend fun upsert(post: PostEntity)

    @Upsert
    suspend fun upsert(postList: List<PostEntity>)

    @Update
    suspend fun update(post: PostEntity)

    @Query("DELETE FROM post")
    suspend fun deleteAll()

    @Query("DELETE FROM post WHERE authorId = :authorId")
    suspend fun deleteAllByAuthorId(authorId: Long)

    @Delete
    suspend fun delete(post: PostEntity)

    @Query("DELETE FROM post WHERE postId = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE post SET likedByMe = :isLiked WHERE postId = :postId")
    suspend fun setLikedByMe(postId: Long, isLiked: Boolean)

    @Transaction
    suspend fun likeById(postId: Long, userId: Long) {
        getById(postId)?.let {
            val likeOwnerIds = it.likeOwnerIds.toMutableSet()
            likeOwnerIds.add(userId)
            upsert(it.copy(likeOwnerIds = likeOwnerIds))
            setLikedByMe(postId, true)
        }
    }

    @Transaction
    suspend fun unlikeById(postId: Long, userId: Long) {
        getById(postId)?.let {
            val likeOwnerIds = it.likeOwnerIds.toMutableSet()
            likeOwnerIds.remove(userId)
            upsert(it.copy(likeOwnerIds = likeOwnerIds))
            setLikedByMe(postId, false)
        }
    }
}
