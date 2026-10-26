package com.leftoverchef.app.repository

import com.leftoverchef.app.data.api.RecipeDetail
import com.leftoverchef.app.data.api.RecipeSearchResult
import com.leftoverchef.app.data.api.RetrofitInstance
import com.leftoverchef.app.data.db.IngredientDao
import com.leftoverchef.app.data.db.SavedRecipeDao
import com.leftoverchef.app.data.model.Ingredient
import com.leftoverchef.app.data.model.SavedRecipe

// Repository je tanki sloj izmedju ViewModela i izvora podataka.
class RecipeRepository(
    private val ingredientDao: IngredientDao,
    private val savedRecipeDao: SavedRecipeDao
) {

    fun getAllIngredients(userId: Int) = ingredientDao.getAllIngredients(userId)
    fun searchIngredients(query: String, userId: Int) = ingredientDao.searchIngredients(query, userId)
    suspend fun insertIngredient(ingredient: Ingredient) = ingredientDao.insertIngredient(ingredient)
    suspend fun updateIngredient(ingredient: Ingredient) = ingredientDao.updateIngredient(ingredient)
    suspend fun deleteIngredient(ingredient: Ingredient) = ingredientDao.deleteIngredient(ingredient)
    suspend fun getAllIngredientNames(userId: Int) = ingredientDao.getAllIngredientNames(userId)

    fun getAllSavedRecipes(userId: Int) = savedRecipeDao.getAllSavedRecipes(userId)
    fun searchSavedRecipes(query: String, userId: Int) = savedRecipeDao.searchSavedRecipes(query, userId)
    suspend fun saveRecipe(recipe: SavedRecipe) = savedRecipeDao.insertRecipe(recipe)
    suspend fun deleteSavedRecipe(recipe: SavedRecipe) = savedRecipeDao.deleteRecipe(recipe)
    suspend fun isRecipeSaved(recipeId: Int, userId: Int) = savedRecipeDao.isRecipeSaved(recipeId, userId)


    suspend fun findRecipesByIngredients(ingredients: String): List<RecipeSearchResult> {
        return RetrofitInstance.api.findRecipesByIngredients(ingredients)
    }

    suspend fun getRecipeDetail(id: Int): RecipeDetail {
        return RetrofitInstance.api.getRecipeInformation(id)
    }
}
