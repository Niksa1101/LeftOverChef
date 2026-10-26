package com.leftoverchef.app.ui.recipes

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.leftoverchef.app.R
import com.leftoverchef.app.data.api.Nutrition
import com.leftoverchef.app.data.api.RecipeDetail
import com.leftoverchef.app.data.model.SavedRecipe
import com.leftoverchef.app.databinding.ActivityRecipeDetailBinding
import com.leftoverchef.app.viewmodel.MainViewModel
import kotlin.math.roundToInt

class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RecipeDetail"
    }

    private lateinit var binding: ActivityRecipeDetailBinding
    private lateinit var viewModel: MainViewModel
    private var recipeId: Int = -1
    private var recipeTitle: String = ""
    private var baseServings: Int = 1
    private var currentServings: Int = 1
    private var currentRecipe: RecipeDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Detail ekran dobija osnovne podatke iz liste, a pune detalje ucitava sa API-ja
        binding.btnBack.setOnClickListener { finish() }

        recipeId    = intent.getIntExtra("RECIPE_ID", -1)
        recipeTitle = intent.getStringExtra("RECIPE_TITLE") ?: ""

        val missedIngredients = intent.getStringArrayListExtra("MISSED_INGREDIENTS") ?: arrayListOf()
        showMissingIngredients(missedIngredients)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupServingButtons()
        setupFabListeners()
        observeViewModel()

        if (recipeId != -1) {
            Log.d(TAG, "Loading recipe id=$recipeId title='$recipeTitle'")
            viewModel.loadRecipeDetail(recipeId)
        } else {
            Log.e(TAG, "RecipeDetailActivity launched with recipeId=-1")
        }
    }

    private fun showMissingIngredients(missing: List<String>) {
        if (missing.isEmpty()) return
        binding.llMissingIngredients.visibility = View.VISIBLE
        // Missing ingredients su oni koje korisnik trenutno nema u pantriju
        missing.forEach { name ->
            val tv = TextView(this).apply {
                text = "• $name"
                textSize = 14f
                setTextColor(getColor(R.color.error))
                typeface = resources.getFont(R.font.poppins_regular)
                setPadding(0, 4, 0, 4)
            }
            binding.llMissingChips.addView(tv)
        }
    }

    private fun setupServingButtons() {
        // Prikaz kalorija i makroa se menja zajedno sa brojem porcija
        binding.btnIncreaseServings.setOnClickListener {
            if (currentServings < 20) {
                currentServings++
                updateNutritionForServings()
            }
        }
        binding.btnDecreaseServings.setOnClickListener {
            if (currentServings > 1) {
                currentServings--
                updateNutritionForServings()
            }
        }
    }

    private fun cookMultiplier(): Float = currentServings.toFloat() / baseServings.toFloat()

    private fun setupFabListeners() {
        binding.fabSave.setOnClickListener {
            val recipe = currentRecipe ?: return@setOnClickListener
            val n        = recipe.nutrition
            // U saved recept cuvamo i nutritivne podatke da kasnije mogu u calorie log
            val kcal     = n?.getCalories() ?: 0.0
            val proteinG = n?.getProtein()  ?: 0.0
            val carbsG   = n?.getCarbs()    ?: 0.0
            val fatG     = n?.getFat()      ?: 0.0

            val savedRecipe = SavedRecipe(
                id             = recipe.id,
                title          = recipe.title,
                image          = recipe.image,
                calories       = kcal,
                protein        = proteinG,
                carbs          = carbsG,
                fat            = fatG,
                readyInMinutes = recipe.readyInMinutes,
                servings       = recipe.servings,
                sourceUrl      = recipe.sourceUrl
            )
            viewModel.toggleSaveRecipe(savedRecipe)
        }

        binding.fabCookMode.setOnClickListener {
            val recipe = currentRecipe ?: return@setOnClickListener
            Log.d(TAG, "Launching CookMode for '${recipe.title}' " +
                    "(${currentServings}/${baseServings} servings, ×${cookMultiplier()})")
            val multiplier = cookMultiplier()
            // Sastojci se salju kroz Intent da Cook Mode moze kasnije da uradi deduction.
            val names   = ArrayList(recipe.extendedIngredients.map { it.name })
            val amounts = recipe.extendedIngredients.map { it.amount.toFloat() }.toFloatArray()
            val units   = ArrayList(recipe.extendedIngredients.map { it.unit })
            startActivity(Intent(this, CookModeActivity::class.java).apply {
                putExtra("INSTRUCTIONS",        recipe.instructions ?: "")
                putExtra("RECIPE_TITLE",        recipe.title)
                putExtra("SERVINGS_MULTIPLIER", multiplier)
                putStringArrayListExtra("ING_NAMES",  names)
                putExtra("ING_AMOUNTS",               amounts)
                putStringArrayListExtra("ING_UNITS",  units)
            })
        }
    }

    private fun updateNutritionForServings() {
        val recipe = currentRecipe ?: return
        val multiplier = currentServings.toDouble()
        binding.tvServings.text = getString(R.string.servings, currentServings)
        recipe.nutrition?.let { bindNutrition(it, multiplier) }
    }

    private fun bindNutrition(nutrition: Nutrition, multiplier: Double = 1.0) {
        val kcal     = (nutrition.getCalories() * multiplier).roundToInt()
        val proteinG = (nutrition.getProtein()  * multiplier).roundToInt()
        val carbsG   = (nutrition.getCarbs()    * multiplier).roundToInt()
        val fatG     = (nutrition.getFat()       * multiplier).roundToInt()

        binding.tvCalories.text = getString(R.string.kcal_value, kcal)
        binding.tvProtein.text  = getString(R.string.macro_grams, proteinG)
        binding.tvCarbs.text    = getString(R.string.macro_grams, carbsG)
        binding.tvFat.text      = getString(R.string.macro_grams, fatG)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading: Boolean ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.selectedRecipe.observe(this) { recipe: RecipeDetail? ->
            if (recipe == null) return@observe
            // Kad API vrati detalj, ovde vezujemo sve podatke na UI
            currentRecipe   = recipe
            baseServings    = maxOf(1, recipe.servings)
            currentServings = 1

            binding.tvTitle.text    = recipe.title
            binding.tvTime.text     = getString(R.string.ready_in, recipe.readyInMinutes)
            binding.tvServings.text = getString(R.string.servings, currentServings)

            Glide.with(this)
                .load(recipe.image)
                .centerCrop()
                .into(binding.ivRecipeImage)

            recipe.nutrition?.let { nutrition ->
                binding.cardNutrition.visibility = View.VISIBLE
                bindNutrition(nutrition, 1.0)
            }

            val instructions = recipe.instructions
            binding.tvInstructions.text = if (!instructions.isNullOrEmpty()) {
                // Spoonacular vraca HTML, pa ga Android renderuje kao formatiran tekst
                Html.fromHtml(instructions, Html.FROM_HTML_MODE_COMPACT)
            } else {
                getString(R.string.no_instructions)
            }
        }

        viewModel.isRecipeSaved.observe(this) { isSaved: Boolean ->
            binding.fabSave.text = getString(
                if (isSaved) R.string.recipe_saved else R.string.recipe_save
            )
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Log.e(TAG, "Error loading recipe: $error")
            }
        }
    }
}
