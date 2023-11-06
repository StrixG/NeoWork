package com.obrekht.neowork.events.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.obrekht.neowork.core.data.typeconverter.InstantTypeConverter
import com.obrekht.neowork.core.data.typeconverter.SetOfLongTypeConverter
import com.obrekht.neowork.events.data.local.dao.EventDao
import com.obrekht.neowork.events.data.local.entity.EventEntity

@Database(
    entities = [
        EventEntity::class,
    ], version = 2
)
@TypeConverters(InstantTypeConverter::class, SetOfLongTypeConverter::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
