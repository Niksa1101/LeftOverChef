package com.leftoverchef.app.ui.recipes

data class RecipeFilters(
    val maxMissingIngredients: Int = Int.MAX_VALUE,
    val maxPrepTime: Int = Int.MAX_VALUE,
    val maxCalories: Int = Int.MAX_VALUE
)
