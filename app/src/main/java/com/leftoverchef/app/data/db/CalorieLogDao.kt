package com.leftoverchef.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.leftoverchef.app.data.model.CalorieLogEntry

@Dao
interface CalorieLogDao {

    @Insert
    suspend fun insert(entry: CalorieLogEntry)

    @Delete
    suspend fun delete(entry: CalorieLogEntry)

    @Query("SELECT * FROM calorie_log WHERE userId = :userId ORDER BY dateMillis DESC")
    fun getAllEntries(userId: Int): LiveData<List<CalorieLogEntry>>

    @Query("SELECT * FROM calorie_log WHERE userId = :userId AND dateMillis >= :startOfDay AND dateMillis < :endOfDay ORDER BY dateMillis DESC")
    fun getEntriesForDay(startOfDay: Long, endOfDay: Long, userId: Int): LiveData<List<CalorieLogEntry>>

    @Query("SELECT COALESCE(SUM(calories * servingsEaten), 0) FROM calorie_log WHERE userId = :userId AND dateMillis >= :startOfDay AND dateMillis < :endOfDay")
    suspend fun getTodayCaloriesLogged(startOfDay: Long, endOfDay: Long, userId: Int): Double
}
