package com.ccard.tracker.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCardCompany(value: CardCompany): String = value.name

    @TypeConverter
    fun toCardCompany(value: String): CardCompany =
        runCatching { CardCompany.valueOf(value) }.getOrDefault(CardCompany.UNKNOWN)
}
