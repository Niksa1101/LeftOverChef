package com.leftoverchef.app.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.leftoverchef.app.R
import com.leftoverchef.app.auth.AuthManager
import com.leftoverchef.app.databinding.ActivityOnboardingBinding
import com.leftoverchef.app.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"

        const val EXTRA_IS_EDIT = "extra_is_edit"

        fun editIntent(context: Context, userId: Int): Intent =
            Intent(context, OnboardingActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_IS_EDIT, true)
            }
    }

    private lateinit var binding: ActivityOnboardingBinding
    private var weightKg: Int = 0
    private var selectedGoal: Int = -1
    private var userId: Int = -1
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Onboarding racuna personalizovani kalorijski cilj za prijavljenog korisnika.
        userId = intent.getIntExtra(EXTRA_USER_ID, -1).takeIf { it >= 0 }
            ?: AuthManager.getLoggedInUserId(this)
            ?: run {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
                return
            }

        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT, false)

        if (isEditMode) {
            prefillExistingValues()
            binding.btnGetStarted.text = "Save Changes"
        }

        setupStep1()
        setupStep2()
    }

    private fun prefillExistingValues() {
        // U edit modu citamo prethodno sacuvanu tezinu i cilj iz SharedPreferences.
        val prefs = getSharedPreferences("leftover_chef_prefs", Context.MODE_PRIVATE)
        val savedWeight = prefs.getInt("user_weight_kg_$userId", 0)
        val savedGoal  = prefs.getInt("user_goal_$userId", -1)

        if (savedWeight > 0) binding.etWeight.setText(savedWeight.toString())
        if (savedGoal in 0..2) {
            selectedGoal = savedGoal
        }
    }

    private fun setupStep1() {
        binding.btnNext.setOnClickListener {
            val kg = binding.etWeight.text?.toString()?.trim()?.toIntOrNull()
            // Ovdje ogranicavamo unos na razuman raspon za tezinu.
            if (kg == null || kg < 20 || kg > 300) {
                Toast.makeText(this, "Please enter a valid weight (20–300 kg)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            weightKg = kg
            showStep2()
        }
    }

    private fun showStep2() {
        val fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        val fadeIn  = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

        binding.stepWeight.startAnimation(fadeOut)
        binding.stepWeight.visibility = View.GONE
        binding.stepGoal.visibility = View.VISIBLE
        binding.stepGoal.startAnimation(fadeIn)

        binding.dot1.layoutParams.width = resources.getDimensionPixelSize(R.dimen.dot_inactive)
        binding.dot1.setBackgroundResource(R.drawable.bg_dot_inactive)
        binding.dot2.layoutParams.width = resources.getDimensionPixelSize(R.dimen.dot_active)
        binding.dot2.setBackgroundResource(R.drawable.bg_dot_active)
        binding.dot1.requestLayout()
        binding.dot2.requestLayout()

        if (selectedGoal in 0..2) selectGoal(selectedGoal)
    }

    private fun setupStep2() {
        binding.cardLose.setOnClickListener     { selectGoal(0) }
        binding.cardMaintain.setOnClickListener { selectGoal(1) }
        binding.cardGain.setOnClickListener     { selectGoal(2) }

        binding.btnGetStarted.setOnClickListener {
            if (selectedGoal == -1) {
                Toast.makeText(this, "Please select a goal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveAndProceed()
        }
    }

    private fun selectGoal(goal: Int) {
        // selectedGoal je 0 lose, 1 maintain, 2 gain.
        selectedGoal = goal
        val cards  = listOf(binding.cardLose, binding.cardMaintain, binding.cardGain)
        val checks = listOf(binding.checkLose, binding.checkMaintain, binding.checkGain)
        val primaryColor = getColor(R.color.primary)
        val dividerColor = getColor(R.color.divider)

        cards.forEachIndexed { i, card ->
            val selected = i == goal
            card.strokeColor = if (selected) primaryColor else dividerColor
            card.strokeWidth = if (selected) 3 else resources.getDimensionPixelSize(R.dimen.card_stroke_width)
            checks[i].setBackgroundResource(
                if (selected) R.drawable.bg_check_selected else R.drawable.bg_check_unselected
            )
        }
    }

    private fun saveAndProceed() {
        // Kalorijski cilj zavisi od tezine i izabranog plana
        val multiplier  = when (selectedGoal) { 0 -> 24; 1 -> 30; 2 -> 36; else -> 30 }
        val calorieGoal = weightKg * multiplier

        // Onboarding podatke cuvamo po userId-u da svaki korisnik ima svoj cilj
        getSharedPreferences("leftover_chef_prefs", Context.MODE_PRIVATE).edit()
            .putInt("calorie_goal_$userId",      calorieGoal)
            .putInt("user_weight_kg_$userId",    weightKg)
            .putInt("user_goal_$userId",         selectedGoal)
            .putBoolean("onboarding_complete_$userId", true)
            .commit()

        if (isEditMode) {
            Toast.makeText(this, getString(R.string.goal_updated, calorieGoal), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}
