package com.obrekht.neowork.posts.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.obrekht.neowork.core.data.typeconverter.InstantTypeConverter
import com.obrekht.neowork.core.data.typeconverter.SetOfLongTypeConverter
import com.obrekht.neowork.posts.data.local.dao.CommentDao
import com.obrekht.neowork.posts.data.local.dao.PostDao
import com.obrekht.neowork.posts.data.local.entity.CommentEntity
import com.obrekht.neowork.posts.data.local.entity.PostEntity

@Database(
    entities = [
        PostEntity::class,
        CommentEntity::class
    ], version = 3
)
@TypeConverters(InstantTypeConverter::class, SetOfLongTypeConverter::class)
abstract class PostDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
}
