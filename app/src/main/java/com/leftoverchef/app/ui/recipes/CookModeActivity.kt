package com.leftoverchef.app.ui.recipes

import android.Manifest
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.leftoverchef.app.R
import com.leftoverchef.app.data.api.ExtendedIngredient
import com.leftoverchef.app.databinding.ActivityCookModeBinding
import com.leftoverchef.app.viewmodel.MainViewModel
import java.util.Locale

class CookModeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CookMode"
    }

    private lateinit var binding: ActivityCookModeBinding
    private lateinit var viewModel: MainViewModel
    private var steps: List<String> = emptyList()
    private var currentStep = 0
    private var timer: CountDownTimer? = null
    private var timerRunning = false
    private var wasTimerRunning = false
    private var timeLeftMillis: Long = 0
    private var ingredients: List<ExtendedIngredient> = emptyList()
    private var servingsMultiplier: Double = 1.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCookModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cook Mode vodi korisnika korak po korak kroz izabrani recept.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val instructions = intent.getStringExtra("INSTRUCTIONS") ?: ""
        val title        = intent.getStringExtra("RECIPE_TITLE") ?: ""
        servingsMultiplier = intent.getFloatExtra("SERVINGS_MULTIPLIER", 1f).toDouble()

        val ingNames   = intent.getStringArrayListExtra("ING_NAMES")   ?: arrayListOf()
        val ingAmounts = intent.getFloatArrayExtra("ING_AMOUNTS")      ?: floatArrayOf()
        val ingUnits   = intent.getStringArrayListExtra("ING_UNITS")   ?: arrayListOf()
        ingredients = ingNames.indices.map { i ->
            ExtendedIngredient(
                name   = ingNames.getOrElse(i) { "" },
                amount = ingAmounts.getOrElse(i) { 0f }.toDouble(),
                unit   = ingUnits.getOrElse(i) { "" }
            )
        }

        binding.tvRecipeTitle.text = title
        steps = parseSteps(instructions)

        if (steps.isEmpty()) {
            steps = listOf(getString(R.string.no_instructions))
        }

        Log.d(TAG, "CookMode started: '$title', ${steps.size} steps, " +
                "${ingredients.size} ingredients, ×$servingsMultiplier")

        updateStep()
        setupButtons()
    }

    override fun onPause() {
        super.onPause()
        wasTimerRunning = timerRunning
        if (timerRunning) {
            cancelTimer()
            Log.d(TAG, "Timer paused (app backgrounded), ${timeLeftMillis}ms remaining")
        }
    }

    override fun onResume() {
        super.onResume()
        if (wasTimerRunning && timeLeftMillis > 0) {
            startTimer()
            Log.d(TAG, "Timer resumed, ${timeLeftMillis}ms remaining")
        }
    }

    private fun parseSteps(instructions: String): List<String> {
        // Spoonacular uputstvo stize kao HTML i ovde se pretvara u korake
        val plain = android.text.Html
            .fromHtml(instructions, android.text.Html.FROM_HTML_MODE_COMPACT)
            .toString().trim()
        val byNewline = plain.split(Regex("\\n+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (byNewline.size > 1) return byNewline
        val byNumber = plain.split(Regex("(?<=^|\\s)(?=\\d+\\.\\s)")).map { it.trim() }.filter { it.isNotEmpty() }
        if (byNumber.size > 1) return byNumber
        return plain.split(Regex("(?<=[.!?])\\s+(?=[A-Z])")).map { it.trim() }.filter { it.length > 15 }
    }

    private fun updateStep() {
        cancelTimer()
        val step    = steps[currentStep]
        val stepNum = currentStep + 1

        // Svaki prelazak na korak resetuje stanje tajmera za taj konkretan korak
        binding.tvStepNumber.text      = getString(R.string.step_counter, stepNum, steps.size)
        binding.tvStepNumberLarge.text = String.format(Locale.ROOT, "%02d", stepNum)
        binding.tvStepText.text        = step
        binding.progressBar.max        = steps.size
        binding.progressBar.progress   = stepNum

        binding.btnPrevious.isEnabled = currentStep > 0
        val isLastStep = currentStep == steps.size - 1
        binding.btnNext.text = getString(if (isLastStep) R.string.btn_finish else R.string.btn_next)

        val minutes = detectMinutes(step)
        if (minutes > 0) {
            timeLeftMillis = minutes * 60 * 1000L
            binding.timerCard.visibility = android.view.View.VISIBLE
            updateTimerDisplay()
        } else {
            binding.timerCard.visibility = android.view.View.GONE
        }
    }

    private fun detectMinutes(text: String): Int {
        val match = Regex("(\\d+)\\s*minute").find(text.lowercase())
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            if (currentStep < steps.size - 1) {
                currentStep++
                updateStep()
            } else {
                showFinishDialog()
            }
        }
        binding.btnPrevious.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                updateStep()
            }
        }
        binding.btnStartTimer.setOnClickListener {
            if (timerRunning) {
                cancelTimer()
                binding.btnStartTimer.text = getString(R.string.btn_timer_start)
            } else {
                startTimer()
                binding.btnStartTimer.text = getString(R.string.btn_timer_pause)
            }
        }
        binding.btnClose.setOnClickListener { finish() }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(timeLeftMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                updateTimerDisplay()
            }
            @RequiresPermission(Manifest.permission.VIBRATE)
            override fun onFinish() {
                timeLeftMillis = 0
                updateTimerDisplay()
                binding.btnStartTimer.text = getString(R.string.btn_timer_start)
                timerRunning = false
                Log.d(TAG, "Step ${currentStep + 1} timer finished — vibrating")
                vibrate()
            }
        }.start()
        timerRunning = true
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
        timerRunning = false
        binding.btnStartTimer.text = getString(R.string.btn_timer_start)
    }

    private fun updateTimerDisplay() {
        val minutes = (timeLeftMillis / 1000) / 60
        val seconds = (timeLeftMillis / 1000) % 60
        binding.tvTimer.text = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        val pattern = longArrayOf(0, 500, 200, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as? Vibrator)
                ?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun showFinishDialog() {
        if (ingredients.isEmpty()) {
            Log.d(TAG, "Cook mode finished with no ingredients to deduct")
            finish()
            return
        }

        // Korisnik na kraju bira da li stvarno zeli skidanje sastojaka iz pantry-ja.
        val lines = ingredients.take(5).map { ing: ExtendedIngredient ->
            val scaled = ing.amount * servingsMultiplier
            val amtStr = if (scaled % 1.0 == 0.0)
                scaled.toLong().toString()
            else
                String.format(Locale.ROOT, "%.1f", scaled)
            "• $amtStr ${ing.unit} ${ing.name}".trim()
        }
        val suffix = if (ingredients.size > 5)
            "\n  " + getString(R.string.ingredients_more, ingredients.size - 5)
        else ""
        val preview: CharSequence = lines.joinToString("\n") + suffix

        Log.d(TAG, "Showing finish dialog for ${ingredients.size} ingredients")

        AlertDialog.Builder(this)
            .setTitle(R.string.deduct_dialog_title)
            .setMessage(preview)
            .setPositiveButton(R.string.deduct_confirm) { _: DialogInterface, _: Int ->
                Log.d(TAG, "User confirmed ingredient deduction")
                viewModel.consumeIngredients(ingredients, servingsMultiplier)
                finish()
            }
            .setNegativeButton(R.string.deduct_skip) { _: DialogInterface, _: Int ->
                Log.d(TAG, "User skipped ingredient deduction")
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelTimer()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
