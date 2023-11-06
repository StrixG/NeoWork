package com.obrekht.neowork.core.data.typeconverter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SetOfLongTypeConverter {
    @TypeConverter
    fun jsonStringToSet(json: String): Set<Long> = Json.decodeFromString(json)

    @TypeConverter
    fun setToJsonString(set: Set<Long>): String = Json.encodeToString(set)
}
