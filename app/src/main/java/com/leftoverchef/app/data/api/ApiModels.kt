package com.leftoverchef.app.data.api

import com.google.gson.annotations.SerializedName

data class RecipeIngredientInfo(
    val id: Int = 0,
    val name: String = "",
    val original: String = ""
)

data class RecipeSearchResult(
    val id: Int,
    val title: String,
    val image: String,
    val usedIngredientCount: Int,
    val missedIngredientCount: Int,
    val likes: Int,
    val missedIngredients: List<RecipeIngredientInfo> = emptyList(),
    val usedIngredients: List<RecipeIngredientInfo> = emptyList()
)

data class RecipeDetail(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val servings: Int,
    val sourceUrl: String,
    val summary: String,
    val instructions: String?,
    val nutrition: Nutrition?,
    val extendedIngredients: List<ExtendedIngredient> = emptyList()
)

data class ExtendedIngredient(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String = "",
    val original: String = ""
)

data class Nutrition(
    val nutrients: List<Nutrient>
) {
    fun getCalories() = nutrients.find { it.name == "Calories" }?.amount ?: 0.0
    fun getProtein() = nutrients.find { it.name == "Protein" }?.amount ?: 0.0
    fun getCarbs() = nutrients.find { it.name == "Carbohydrates" }?.amount ?: 0.0
    fun getFat() = nutrients.find { it.name == "Fat" }?.amount ?: 0.0
}

data class Nutrient(
    val name: String,
    val amount: Double,
    val unit: String
)