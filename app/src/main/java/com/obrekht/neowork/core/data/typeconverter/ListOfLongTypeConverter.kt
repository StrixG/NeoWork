package com.obrekht.neowork.core.data.typeconverter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ListOfLongTypeConverter {
    @TypeConverter
    fun jsonStringToList(json: String): List<Long> = Json.decodeFromString(json)

    @TypeConverter
    fun listToJsonString(list: List<Long>): String = Json.encodeToString(list)
}