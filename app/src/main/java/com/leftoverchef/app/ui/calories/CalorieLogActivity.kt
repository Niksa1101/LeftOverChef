package com.leftoverchef.app.ui.calories

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.leftoverchef.app.R
import com.leftoverchef.app.auth.AuthManager
import com.leftoverchef.app.data.model.CalorieLogEntry
import com.leftoverchef.app.data.model.SavedRecipe
import com.leftoverchef.app.databinding.ActivityCalorieLogBinding
import com.leftoverchef.app.ui.home.OnboardingActivity
import com.leftoverchef.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class CalorieLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalorieLogBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: CalorieLogAdapter

    private var calorieGoal: Int = 2000
    private var userId: Int = 0
    private var cachedSavedRecipes: List<SavedRecipe> = emptyList()

    override fun onResume() {
        super.onResume()
        val newGoal = prefs.getInt("calorie_goal_$userId", calorieGoal)
        if (newGoal != calorieGoal) {
            calorieGoal = newGoal
            viewModel.todayLogEntries.value?.let { entries ->
                updateProgressUI(entries.sumOf { it.calories * it.servingsEaten })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalorieLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Calorie log koristi onboarding cilj kao referencu za dnevni napredak
        userId = AuthManager.getLoggedInUserId(this) ?: 0
        prefs = getSharedPreferences("leftover_chef_prefs", Context.MODE_PRIVATE)
        calorieGoal = prefs.getInt("calorie_goal_$userId", 2000)

        binding.tvDate.text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
        setupButtons()
        updateProgressUI(0.0)
    }

    private fun setupRecyclerView() {
        adapter = CalorieLogAdapter { entry -> viewModel.deleteLogEntry(entry) }
        binding.rvLog.layoutManager = LinearLayoutManager(this)
        binding.rvLog.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.todayLogEntries.observe(this) { entries ->
            // Zbir kalorija se racuna iz svih danasnjih unosa i njihovih porcija
            adapter.submitList(entries)
            binding.tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            updateProgressUI(entries.sumOf { it.calories * it.servingsEaten })
        }

        viewModel.savedRecipes.observe(this) { recipes ->
            // Add Meal bira jedan od prethodno sacuvanih recepata
            cachedSavedRecipes = recipes ?: emptyList()
            Log.d("CalorieLog", "Sačuvani recepti: ${cachedSavedRecipes.size}")
        }
    }

    private fun updateProgressUI(totalCalories: Double) {
        val total = totalCalories.toInt()
        val remaining = calorieGoal - total
        val progress = ((totalCalories / calorieGoal) * 100).toInt().coerceIn(0, 100)

        binding.tvGoal.text = getString(R.string.calorie_progress, total, calorieGoal)
        binding.tvCaloriesRemaining.text = if (remaining >= 0)
            getString(R.string.calories_remaining, remaining)
        else
            getString(R.string.calories_over, -remaining)
        binding.tvProgressPercent.text = getString(R.string.progress_percent, progress)
        binding.progressCalories.progress = progress
    }

    private fun setupButtons() {
        binding.btnSetGoal.setOnClickListener {
            startActivity(OnboardingActivity.editIntent(this, userId))
        }
        binding.btnAddMeal.setOnClickListener { showAddMealDialog() }
    }

    private fun showAddMealDialog() {
        if (cachedSavedRecipes.isEmpty()) {
            Log.d("CalorieLog", "Add meal tapped but no saved recipes available")
            Toast.makeText(
                this,
                "No saved recipes yet. Save some recipes first!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        showRecipePickerDialog(cachedSavedRecipes)
    }

    private fun showRecipePickerDialog(savedRecipes: List<SavedRecipe>) {
        val recipeNames = savedRecipes.map { it.title }.toTypedArray()
        var selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Select a Recipe")
            .setSingleChoiceItems(recipeNames, 0) { _, which -> selectedIndex = which }
            .setPositiveButton("Next") { _, _ -> showServingsDialog(savedRecipes[selectedIndex]) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showServingsDialog(recipe: SavedRecipe) {
        // Jedan saved recipe moze da se prijavi vise puta kroz broj pojedenih porcija
        val picker = NumberPicker(this).apply {
            minValue = 1; maxValue = 10; value = 1; wrapSelectorWheel = false
        }

        AlertDialog.Builder(this)
            .setTitle("How many servings of\n${recipe.title}?")
            .setView(picker)
            .setPositiveButton("Log") { _, _ ->
                val servings = picker.value
                Log.d("CalorieLog", "Logging ${recipe.title} ×$servings servings " +
                        "(${(recipe.calories * servings).toInt()} kcal)")
                // U log cuvamo nutritivne vrijednosti po receptu i broj pojedenih porcija
                viewModel.addLogEntry(CalorieLogEntry(
                    recipeId     = recipe.id,
                    recipeTitle  = recipe.title,
                    calories     = recipe.calories,
                    protein      = recipe.protein,
                    carbs        = recipe.carbs,
                    fat          = recipe.fat,
                    servingsEaten = servings
                ))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
