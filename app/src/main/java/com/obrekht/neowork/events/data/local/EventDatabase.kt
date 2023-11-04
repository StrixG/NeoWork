package com.obrekht.neowork.events.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.obrekht.neowork.core.data.typeconverter.InstantTypeConverter
import com.obrekht.neowork.core.data.typeconverter.ListOfLongTypeConverter
import com.obrekht.neowork.events.data.local.dao.EventDao
import com.obrekht.neowork.events.data.local.entity.EventEntity

@Database(
    entities = [
        EventEntity::class,
    ], version = 1
)
@TypeConverters(InstantTypeConverter::class, ListOfLongTypeConverter::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
