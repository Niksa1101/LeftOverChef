package com.leftoverchef.app.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.leftoverchef.app.R
import com.leftoverchef.app.auth.AuthManager
import com.leftoverchef.app.data.db.AppDatabase
import com.leftoverchef.app.databinding.ActivityMainBinding
import com.leftoverchef.app.ui.auth.LoginActivity
import com.leftoverchef.app.ui.calories.CalorieLogActivity
import com.leftoverchef.app.ui.pantry.PantryActivity
import com.leftoverchef.app.ui.saved.SavedRecipesActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGreeting()
        loadStats()

        binding.btnPantry.setOnClickListener {
            startActivity(Intent(this, PantryActivity::class.java))
        }

        binding.btnSaved.setOnClickListener {
            startActivity(Intent(this, SavedRecipesActivity::class.java))
        }

        binding.btnCalorieLog.setOnClickListener {
            startActivity(Intent(this, CalorieLogActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    AuthManager.clearToken(this)
                    startActivity(
                        Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun setupGreeting() {
        val username = AuthManager.getLoggedInUsername(this) ?: ""

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning,"
            hour < 17 -> "Good afternoon,"
            else -> "Good evening,"
        }

        val displayName = username.trimEnd { it.isDigit() }.ifEmpty { username }

        binding.tvGreeting.text = greeting
        binding.tvSubtitle.text = "$displayName 👋"

        val initials = username.take(2).uppercase()
        binding.tvAvatar.text = initials
    }

    private fun loadStats() {
        val db = AppDatabase.getDatabase(applicationContext)
        val userId = com.leftoverchef.app.auth.AuthManager.getLoggedInUserId(this) ?: 0

        // Danasnje kalorije se racunaju samo unutar granica tekuceg dana
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000L

        lifecycleScope.launch {
            val ingredientCount = withContext(Dispatchers.IO) {
                db.ingredientDao().getCount(userId)
            }
            val savedCount = withContext(Dispatchers.IO) {
                db.savedRecipeDao().getCount(userId)
            }
            val todayKcal = withContext(Dispatchers.IO) {
                db.calorieLogDao().getTodayCaloriesLogged(startOfDay, endOfDay, userId)
            }

            val kcalInt = todayKcal.toInt()

            val ingSuffix  = if (ingredientCount != 1) "s" else ""
            val recSuffix  = if (savedCount != 1) "s" else ""

            binding.tvStatIngredients.text = ingredientCount.toString()
            binding.tvStatSaved.text       = savedCount.toString()
            binding.tvStatCalories.text    = String.format(java.util.Locale.getDefault(), "%,d", kcalInt)

            binding.tvHeroSubtitle.text = getString(R.string.hero_subtitle, ingredientCount, ingSuffix)
            binding.tvPantryCount.text  = getString(R.string.pantry_count, ingredientCount, ingSuffix)
            binding.tvSavedCount.text   = getString(R.string.saved_count, savedCount, recSuffix)
            binding.tvCalorieCount.text = getString(R.string.calorie_count_today, kcalInt)
        }
    }
}
