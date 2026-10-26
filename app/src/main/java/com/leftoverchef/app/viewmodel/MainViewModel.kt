package com.leftoverchef.app.viewmodel

import com.leftoverchef.app.ui.recipes.RecipeFilters
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.leftoverchef.app.auth.AuthManager
import com.leftoverchef.app.data.api.ExtendedIngredient
import com.leftoverchef.app.data.api.RecipeDetail
import com.leftoverchef.app.data.api.RecipeSearchResult
import com.leftoverchef.app.data.db.AppDatabase
import com.leftoverchef.app.data.model.CalorieLogEntry
import com.leftoverchef.app.data.model.Ingredient
import com.leftoverchef.app.data.model.SavedRecipe
import com.leftoverchef.app.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    val userId: Int = AuthManager.getLoggedInUserId(application) ?: 0

    private lateinit var repository: RecipeRepository
    private val db = AppDatabase.getDatabase(application)

    val allIngredients: LiveData<List<Ingredient>>

    private val _searchQuery = MutableLiveData("")

    lateinit var displayIngredients: LiveData<List<Ingredient>>
        private set

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _recipeResults = MutableLiveData<List<RecipeSearchResult>>()
    val recipeResults: LiveData<List<RecipeSearchResult>> = _recipeResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    val savedRecipes: LiveData<List<SavedRecipe>>

    private val _selectedRecipe = MutableLiveData<RecipeDetail?>()
    val selectedRecipe: LiveData<RecipeDetail?> = _selectedRecipe

    private val _isRecipeSaved = MutableLiveData<Boolean>()
    val isRecipeSaved: LiveData<Boolean> = _isRecipeSaved

    private val _currentFilters = MutableLiveData<RecipeFilters>(RecipeFilters())
    val currentFilters: LiveData<RecipeFilters> = _currentFilters

    private val _filteredResults = MutableLiveData<List<RecipeSearchResult>>()
    val filteredResults: LiveData<List<RecipeSearchResult>> = _filteredResults

    val allLogEntries: LiveData<List<CalorieLogEntry>>
    val todayLogEntries: LiveData<List<CalorieLogEntry>>

    init {
        repository = RecipeRepository(db.ingredientDao(), db.savedRecipeDao())
        allIngredients = repository.getAllIngredients(userId)
        savedRecipes = repository.getAllSavedRecipes(userId)

        // Menjamo izvor sastojaka u zavisnosti od unetog teksta za pretragu
        displayIngredients = _searchQuery.switchMap { query: String? ->
            if (query.isNullOrEmpty()) repository.getAllIngredients(userId)
            else repository.searchIngredients(query, userId)
        }

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000

        allLogEntries = db.calorieLogDao().getAllEntries(userId)
        todayLogEntries = db.calorieLogDao().getEntriesForDay(startOfDay, endOfDay, userId)

        Log.d(TAG, "ViewModel initialised for userId=$userId")
    }


    fun addIngredient(ingredient: Ingredient) = viewModelScope.launch {
        val toInsert = ingredient.copy(userId = userId)
        repository.insertIngredient(toInsert)
        Log.d(TAG, "Ingredient added: ${ingredient.name} (${ingredient.amount} ${ingredient.unit})")
    }

    fun updateIngredient(ingredient: Ingredient) = viewModelScope.launch {
        repository.updateIngredient(ingredient)
        Log.d(TAG, "Ingredient updated: ${ingredient.name}")
    }


    fun deleteIngredient(ingredient: Ingredient) = viewModelScope.launch {
        repository.deleteIngredient(ingredient)
        Log.d(TAG, "Ingredient deleted: ${ingredient.name}")
    }

    fun searchIngredients(query: String) = repository.searchIngredients(query, userId)

    // Trazimo recepte na osnovu svih sastojaka koje korisnik trenutno ima u ostavi.
    fun searchRecipesByIngredients() = viewModelScope.launch {
        try {
            _isLoading.value = true
            _errorMessage.value = null
            val ingredientNames = repository.getAllIngredientNames(userId)
            if (ingredientNames.isEmpty()) {
                Log.w(TAG, "Recipe search aborted: no ingredients in pantry")
                _errorMessage.value = "Please add some ingredients first!"
                _isLoading.value = false
                return@launch
            }
            Log.d(TAG, "Searching recipes for ${ingredientNames.size} ingredients: $ingredientNames")
            // Spajamo nazive sastojaka u jedan string zbog Spoonacular API-ja.
            val ingredientsString = ingredientNames.joinToString(",")
            val results = repository.findRecipesByIngredients(ingredientsString)
            Log.d(TAG, "Recipe search returned ${results.size} results")
            _recipeResults.value = results
            val filters = _currentFilters.value ?: RecipeFilters()
            val filtered = applyFiltersToList(results, filters)
            _filteredResults.value = filtered
            Log.d(TAG, "After filter: ${filtered.size} results visible")
        } catch (e: Exception) {
            Log.e(TAG, "Recipe search failed", e)
            _errorMessage.value = "Failed to fetch recipes. Check your internet connection."
        } finally {
            _isLoading.value = false
        }
    }

    // Ucitavamo detalje recepta i proveravamo da li je recept vec sacuvan
    fun loadRecipeDetail(id: Int) = viewModelScope.launch {
        try {
            _isLoading.value = true
            _errorMessage.value = null
            Log.d(TAG, "Loading recipe detail for id=$id")
            val detail = repository.getRecipeDetail(id)
            _selectedRecipe.value = detail
            _isRecipeSaved.value = repository.isRecipeSaved(id, userId)
            Log.d(TAG, "Recipe detail loaded: '${detail.title}', saved=${_isRecipeSaved.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recipe detail for id=$id", e)
            _errorMessage.value = "Failed to load recipe details."
        } finally {
            _isLoading.value = false
        }
    }

    // Cuvamo ili uklanjamo recept iz sacuvanih u zavisnosti od trenutnog stanja
    fun toggleSaveRecipe(recipe: SavedRecipe) = viewModelScope.launch {
        if (_isRecipeSaved.value == true) {
            repository.deleteSavedRecipe(recipe.copy(userId = userId))
            _isRecipeSaved.value = false
            Log.d(TAG, "Recipe unsaved: '${recipe.title}'")
        } else {
            repository.saveRecipe(recipe.copy(userId = userId))
            _isRecipeSaved.value = true
            Log.d(TAG, "Recipe saved: '${recipe.title}'")
        }
    }

    fun deleteSavedRecipe(recipe: SavedRecipe) = viewModelScope.launch {
        repository.deleteSavedRecipe(recipe)
        Log.d(TAG, "Saved recipe deleted: '${recipe.title}'")
    }

    fun saveRecipe(recipe: SavedRecipe) = viewModelScope.launch {
        repository.saveRecipe(recipe.copy(userId = userId))
        Log.d(TAG, "Recipe saved: '${recipe.title}'")
    }

    fun searchSavedRecipes(query: String) = repository.searchSavedRecipes(query, userId)

    // Primenjujemo filtere nad vec ucitanim rezultatima bez novog API poziva
    fun applyFilters(filters: RecipeFilters) {
        _currentFilters.value = filters
        val current = _recipeResults.value ?: return
        val filtered = applyFiltersToList(current, filters)
        _filteredResults.value = filtered
        Log.d(
            TAG,
            "Filters applied - missing<=${filters.maxMissingIngredients} -> " +
                    "${filtered.size}/${current.size} results shown"
        )
    }

    fun clearFilters() {
        _currentFilters.value = RecipeFilters()
        _filteredResults.value = _recipeResults.value ?: emptyList()
        Log.d(TAG, "Filters cleared")
    }

    private fun applyFiltersToList(
        results: List<RecipeSearchResult>,
        filters: RecipeFilters
    ): List<RecipeSearchResult> = results.filter { r ->
        r.missedIngredientCount <= filters.maxMissingIngredients
    }

    // Smanjujemo kolicine sastojaka iz ostave na osnovu sastojaka iskoriscenih u receptu
    fun consumeIngredients(ingredients: List<ExtendedIngredient>, servingsMultiplier: Double) =
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Consuming ${ingredients.size} ingredients (x$servingsMultiplier servings)")
            val pantry = db.ingredientDao().getAllIngredientsList(userId)

            ingredients.forEach { recipeIng ->
                val recipeAmount = recipeIng.amount * servingsMultiplier
                val recipeUnit = recipeIng.unit.trim().lowercase()
                val recipeName = recipeIng.name.trim().lowercase()

                // Traximo najslicniji sastojak iz ostave po nazivu
                val match = pantry.firstOrNull { pantryIng ->
                    val pName = pantryIng.name.trim().lowercase()
                    pName.contains(recipeName) || recipeName.contains(pName)
                }
                if (match == null) {
                    Log.d(TAG, "No pantry match for recipe ingredient: '${recipeIng.name}'")
                    return@forEach
                }

                val pantryAmount = match.amount.toDoubleOrNull()
                val pantryUnit = match.unit.trim().lowercase()

                if (pantryAmount == null || pantryAmount <= 0.0) {
                    db.ingredientDao().deleteIngredient(match)
                    Log.d(TAG, "Deleted zero-quantity pantry item: '${match.name}'")
                    return@forEach
                }

                val pantryFamily = unitFamily(pantryUnit)
                val recipeFamily = unitFamily(recipeUnit)

                if (pantryFamily != null && pantryFamily == recipeFamily) {
                    // Pretvaramo obe kolicine u istu baznu jedinicu pre oduzimanja
                    val remaining =
                        toBaseUnit(pantryAmount, pantryUnit) - toBaseUnit(recipeAmount, recipeUnit)
                    if (remaining <= 0.0) {
                        db.ingredientDao().deleteIngredient(match)
                        Log.d(TAG, "Pantry item fully consumed: '${match.name}'")
                    } else {
                        val remainingDisplay = fromBaseUnit(remaining, pantryUnit)
                        val formatted = if (remainingDisplay % 1.0 == 0.0)
                            remainingDisplay.toLong().toString()
                        else
                            "%.1f".format(remainingDisplay)
                        db.ingredientDao().updateIngredient(match.copy(amount = formatted))
                        Log.d(TAG, "Pantry item updated: '${match.name}' -> $formatted $pantryUnit")
                    }
                } else {
                    Log.w(
                        TAG,
                        "Unit family mismatch for '${match.name}': " +
                                "pantry='$pantryUnit' (${pantryFamily ?: "unknown"}) vs " +
                                "recipe='$recipeUnit' (${recipeFamily ?: "unknown"}) - skipping deduction"
                    )
                }
            }
        }

    // Odredjujemo kojoj grupi jedinica pripada uneta mera
    private fun unitFamily(unit: String): String? = when (unit.trim().lowercase()) {
        "g", "gram", "grams",
        "kg", "kilogram", "kilograms",
        "oz", "ounce", "ounces",
        "lb", "lbs", "pound", "pounds" -> "weight"

        "ml", "milliliter", "milliliters", "millilitre", "millilitres",
        "l", "liter", "liters", "litre", "litres",
        "tsp", "teaspoon", "teaspoons",
        "tbsp", "tablespoon", "tablespoons",
        "cup", "cups",
        "fl oz", "fluid ounce", "fluid ounces" -> "volume"

        "", "piece", "pieces", "pc", "pcs",
        "item", "items", "whole",
        "clove", "cloves",
        "slice", "slices",
        "head", "heads",
        "can", "cans",
        "bunch", "bunches",
        "serving", "servings",
        "large", "medium", "small",
        "stalk", "stalks",
        "sprig", "sprigs",
        "leaf", "leaves",
        "fillet", "fillets",
        "breast", "breasts",
        "thigh", "thighs",
        "link", "links",
        "package", "packages", "pack", "packs",
        "container", "containers",
        "bag", "bags",
        "box", "boxes",
        "jar", "jars",
        "bottle", "bottles",
        "drop", "drops",
        "pinch", "pinches",
        "dash", "dashes" -> "count"

        else -> null
    }


    private fun toBaseUnit(amount: Double, unit: String): Double = when (unit) {
        "kg", "kilogram", "kilograms" -> amount * 1000.0
        "oz", "ounce", "ounces" -> amount * 28.3495
        "lb", "lbs", "pound", "pounds" -> amount * 453.592
        "l", "liter", "liters", "litre", "litres" -> amount * 1000.0
        "tsp", "teaspoon", "teaspoons" -> amount * 4.929
        "tbsp", "tablespoon", "tablespoons" -> amount * 14.787
        "cup", "cups" -> amount * 236.588
        "fl oz" -> amount * 29.5735
        else -> amount
    }


    private fun fromBaseUnit(amount: Double, targetUnit: String): Double = when (targetUnit) {
        "kg", "kilogram", "kilograms" -> amount / 1000.0
        "oz", "ounce", "ounces" -> amount / 28.3495
        "lb", "lbs", "pound", "pounds" -> amount / 453.592
        "l", "liter", "liters", "litre", "litres" -> amount / 1000.0
        "tsp", "teaspoon", "teaspoons" -> amount / 4.929
        "tbsp", "tablespoon", "tablespoons" -> amount / 14.787
        "cup", "cups" -> amount / 236.588
        "fl oz" -> amount / 29.5735
        else -> amount
    }

    // Dodajemo unos u kalorijski dnevnik za trenutno prijavljenog korisnika
    fun addLogEntry(entry: CalorieLogEntry) = viewModelScope.launch {
        db.calorieLogDao().insert(entry.copy(userId = userId))
        Log.d(
            TAG,
            "Calorie log entry added: '${entry.recipeTitle}' " +
                    "(${(entry.calories * entry.servingsEaten).toInt()} kcal, " +
                    "${entry.servingsEaten} serving(s))"
        )
    }

    // Brisemo unos iz kalorijskog dnevnika
    fun deleteLogEntry(entry: CalorieLogEntry) = viewModelScope.launch {
        db.calorieLogDao().delete(entry)
        Log.d(TAG, "Calorie log entry deleted: '${entry.recipeTitle}'")
    }
}
