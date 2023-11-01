package com.obrekht.neowork.userpreview.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.obrekht.neowork.userpreview.data.local.dao.UserPreviewDao
import com.obrekht.neowork.userpreview.data.local.entity.UserPreviewEntity

@Database(
    entities = [
        UserPreviewEntity::class,
    ], version = 1
)
abstract class UserPreviewDatabase : RoomDatabase() {
    abstract fun userPreviewDao(): UserPreviewDao
}
