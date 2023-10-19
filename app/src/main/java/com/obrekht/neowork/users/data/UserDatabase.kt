package com.obrekht.neowork.users.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.obrekht.neowork.users.data.local.dao.UserDao
import com.obrekht.neowork.users.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
    ], version = 1
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
