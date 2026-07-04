package com.ccard.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardConditionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(condition: CardCondition): Long

    @Delete
    suspend fun delete(condition: CardCondition)

    @Query("SELECT * FROM card_conditions ORDER BY id")
    fun observeAll(): Flow<List<CardCondition>>
}
