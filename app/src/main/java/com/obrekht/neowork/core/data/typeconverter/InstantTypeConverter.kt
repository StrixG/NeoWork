package com.obrekht.neowork.core.data.typeconverter

import androidx.room.TypeConverter
import java.time.Instant

class InstantTypeConverter {
    @TypeConverter
    fun epochToInstant(epochMilli: Long?): Instant? =
        epochMilli?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun instantToEpoch(instant: Instant?): Long? =
        instant?.toEpochMilli()
}