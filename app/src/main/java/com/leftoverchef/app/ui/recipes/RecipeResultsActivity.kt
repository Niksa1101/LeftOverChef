package com.leftoverchef.app.ui.recipes

import com.leftoverchef.app.data.api.RecipeSearchResult
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.leftoverchef.app.R
import com.leftoverchef.app.data.db.AppDatabase
import com.leftoverchef.app.databinding.ActivityRecipeResultsBinding
import com.leftoverchef.app.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeResultsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RecipeResults"
    }

    private lateinit var binding: ActivityRecipeResultsBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ovaj ekran prikazuje recepte koje API vrati na osnovu pantry sastojaka
        binding.btnBack.setOnClickListener { finish() }
        binding.btnFilter.setOnClickListener {
            FilterFragment().show(supportFragmentManager, FilterFragment.TAG)
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
        loadIngredientCountForTitle()

        viewModel.searchRecipesByIngredients()
    }

    private fun loadIngredientCountForTitle() {
        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext)
                    .ingredientDao().getCount(viewModel.userId)
            }
            val suffix = if (count != 1) "s" else ""
            binding.tvToolbarTitle.text = getString(R.string.recipes_for_count, count, suffix)
            Log.d(TAG, "Searching recipes for $count ingredient(s)")
        }
    }

    private fun setupRecyclerView() {
        val adapter = RecipeResultsAdapter { recipe ->
            Log.d(TAG, "Recipe tapped: '${recipe.title}' " +
                    "(used=${recipe.usedIngredientCount}, missed=${recipe.missedIngredientCount})")
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID",    recipe.id)
            intent.putExtra("RECIPE_TITLE", recipe.title)
            intent.putExtra("MISSED_COUNT", recipe.missedIngredientCount)
            intent.putExtra("USED_COUNT",   recipe.usedIngredientCount)
            val missedNames = ArrayList(recipe.missedIngredients.map {
                it.original.ifEmpty { it.name }
            })
            intent.putStringArrayListExtra("MISSED_INGREDIENTS", missedNames)
            startActivity(intent)
        }
        binding.rvRecipes.layoutManager = LinearLayoutManager(this)
        binding.rvRecipes.adapter = adapter

        viewModel.filteredResults.observe(this) { recipes: List<RecipeSearchResult> ->
            adapter.submitList(recipes)
            binding.tvEmpty.visibility =
                if (recipes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            Log.d(TAG, "Recipe list updated: ${recipes.size} results displayed")
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading: Boolean ->
            binding.progressBar.visibility =
                if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.errorMessage.observe(this) { error: String? ->
            // Greska uglavnom znaci da nema interneta ili je API poziv pao.
            if (!error.isNullOrEmpty()) {
                Log.w(TAG, "Error: $error")
                binding.tvError.text = error
                binding.tvError.visibility = android.view.View.VISIBLE
            } else {
                binding.tvError.visibility = android.view.View.GONE
            }
        }
    }
}
