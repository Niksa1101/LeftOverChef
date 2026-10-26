package com.leftoverchef.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calorie_log")
data class CalorieLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = 0,
    val recipeId: Int,
    val recipeTitle: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val servingsEaten: Int,
    val dateMillis: Long = System.currentTimeMillis()
)
