package com.obrekht.neowork.posts.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.obrekht.neowork.posts.data.local.entity.LikeOwnerEntity
import com.obrekht.neowork.posts.data.local.entity.MentionEntity
import com.obrekht.neowork.posts.data.local.entity.PostData
import com.obrekht.neowork.posts.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Transaction
    @Query("SELECT * FROM post WHERE isShown = 1 ORDER BY postId DESC")
    fun pagingSource(): PagingSource<Int, PostData>

    @Transaction
    @Query("SELECT * FROM post ORDER BY postId DESC")
    fun observeAll(): Flow<List<PostData>>

    @Transaction
    @Query("SELECT * FROM post ORDER BY postId DESC LIMIT :count")
    fun observeLatest(count: Long = 1): Flow<List<PostData>>

    @Transaction
    @Query("SELECT * FROM post WHERE isShown = 1 ORDER BY postId DESC")
    fun observeAllVisible(): Flow<List<PostData>>

    @Transaction
    @Query("SELECT * FROM post WHERE postId = :id")
    fun observeById(id: Long): Flow<PostData?>

    @Transaction
    @Query("SELECT * FROM post ORDER BY postId DESC LIMIT :count")
    suspend fun getLatest(count: Long = 1): List<PostData>

    @Transaction
    @Query("SELECT * FROM post WHERE postId = :id")
    suspend fun getById(id: Long): PostData?

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
    suspend fun removeAll()

    @Delete
    suspend fun remove(post: PostEntity)

    @Query("DELETE FROM post WHERE postId = :id")
    suspend fun removeById(id: Long)

    @Query("UPDATE post SET likedByMe = :isLiked WHERE postId = :postId")
    suspend fun setLikedByMe(postId: Long, isLiked: Boolean)

    @Transaction
    @Upsert
    suspend fun like(likeOwner: LikeOwnerEntity) {
        setLikedByMe(likeOwner.postId, true)
    }

    @Transaction
    @Delete
    suspend fun unlike(likeOwner: LikeOwnerEntity) {
        setLikedByMe(likeOwner.postId, false)
    }

    @Upsert
    suspend fun upsertLikeOwner(likeOwner: LikeOwnerEntity)

    @Upsert
    suspend fun upsertLikeOwner(likeOwnerList: List<LikeOwnerEntity>)

    @Upsert
    suspend fun upsertMention(mention: MentionEntity)

    @Upsert
    suspend fun upsertMention(mentionList: List<MentionEntity>)

    @Query("DELETE FROM like_owner WHERE postId = :postId")
    suspend fun deletePostLikeOwners(postId: Long)

    @Query("DELETE FROM mention WHERE postId = :postId")
    suspend fun deletePostMentions(postId: Long)

    @Transaction
    suspend fun upsertWithData(postData: PostData) {
        val postId = postData.post.postId

        deletePostLikeOwners(postId)
        deletePostMentions(postId)
        upsert(postData.post)
        upsertLikeOwner(postData.likeOwnerIds.map { LikeOwnerEntity(postId, it) })
        upsertMention(postData.mentionIds.map { MentionEntity(postId, it) })
    }

    @Transaction
    suspend fun upsertWithData(postDataList: List<PostData>) = postDataList.forEach {
        upsertWithData(it)
    }
}