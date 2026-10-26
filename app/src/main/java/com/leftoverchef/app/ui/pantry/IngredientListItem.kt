package com.leftoverchef.app.ui.pantry

import com.leftoverchef.app.data.model.Ingredient

sealed class IngredientListItem {
    data class Header(val category: String) : IngredientListItem()
    data class IngredientItem(val ingredient: Ingredient) : IngredientListItem()
}
