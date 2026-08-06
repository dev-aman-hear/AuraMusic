package com.aman.auramusic.data.local

import androidx.room.TypeConverter
import com.aman.auramusic.data.model.OnlineSong
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromOnlineSongList(value: List<OnlineSong>?): String {
        return gson.toJson(value ?: emptyList<OnlineSong>())
    }

    @TypeConverter
    fun toOnlineSongList(value: String?): List<OnlineSong> {
        val listType = object : TypeToken<List<OnlineSong>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }
}
