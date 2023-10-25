package com.obrekht.neowork.jobs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.obrekht.neowork.core.data.typeconverter.InstantTypeConverter
import com.obrekht.neowork.jobs.data.local.dao.JobDao
import com.obrekht.neowork.jobs.data.local.entity.JobEntity

@Database(
    entities = [
        JobEntity::class,
    ], version = 1
)
@TypeConverters(InstantTypeConverter::class)
abstract class JobDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
}