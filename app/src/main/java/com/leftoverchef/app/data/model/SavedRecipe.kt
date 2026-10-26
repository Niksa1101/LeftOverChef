package com.leftoverchef.app.data.model

import androidx.room.Entity

@Entity(tableName = "saved_recipes", primaryKeys = ["id", "userId"])
data class SavedRecipe(
    val id: Int,
    val userId: Int = 0,
    val title: String,
    val image: String,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val readyInMinutes: Int = 0,
    val servings: Int = 1,
    val sourceUrl: String = ""
)
