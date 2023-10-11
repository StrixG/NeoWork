package com.obrekht.neowork.posts.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.obrekht.neowork.posts.data.local.dao.PostDao
import com.obrekht.neowork.posts.data.local.entity.LikeOwnerEntity
import com.obrekht.neowork.posts.data.local.entity.MentionEntity
import com.obrekht.neowork.posts.data.local.entity.PostEntity

@Database(
    entities = [
        PostEntity::class,
        LikeOwnerEntity::class,
        MentionEntity::class
    ], version = 1
)
abstract class PostDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
