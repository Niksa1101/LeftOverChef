package com.leftoverchef.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.leftoverchef.app.data.model.Ingredient

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients WHERE userId = :userId ORDER BY name ASC")
    fun getAllIngredients(userId: Int): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE userId = :userId AND name LIKE '%' || :query || '%'")
    fun searchIngredients(query: String, userId: Int): LiveData<List<Ingredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient)

    @Update
    suspend fun updateIngredient(ingredient: Ingredient)

    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)

    @Query("SELECT name FROM ingredients WHERE userId = :userId")
    suspend fun getAllIngredientNames(userId: Int): List<String>

    @Query("SELECT COUNT(*) FROM ingredients WHERE userId = :userId")
    suspend fun getCount(userId: Int): Int

    @Query("SELECT * FROM ingredients WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllIngredientsList(userId: Int): List<Ingredient>
}
