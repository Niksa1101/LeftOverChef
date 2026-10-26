package com.leftoverchef.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.leftoverchef.app.data.model.SavedRecipe

@Dao
interface SavedRecipeDao {

    @Query("SELECT * FROM saved_recipes WHERE userId = :userId ORDER BY title ASC")
    fun getAllSavedRecipes(userId: Int): LiveData<List<SavedRecipe>>

    @Query("SELECT * FROM saved_recipes WHERE userId = :userId AND title LIKE '%' || :query || '%'")
    fun searchSavedRecipes(query: String, userId: Int): LiveData<List<SavedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: SavedRecipe)

    @Delete
    suspend fun deleteRecipe(recipe: SavedRecipe)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE id = :recipeId AND userId = :userId)")
    suspend fun isRecipeSaved(recipeId: Int, userId: Int): Boolean

    @Query("SELECT COUNT(*) FROM saved_recipes WHERE userId = :userId")
    suspend fun getCount(userId: Int): Int
}
