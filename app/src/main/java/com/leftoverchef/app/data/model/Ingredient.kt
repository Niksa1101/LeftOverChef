package com.leftoverchef.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: String = "",
    val unit: String = "",
    val category: String = "Other",
    val userId: Int = 0
)
