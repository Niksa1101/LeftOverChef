package com.leftoverchef.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.leftoverchef.app.R
import com.leftoverchef.app.auth.AuthManager
import com.leftoverchef.app.auth.JwtManager
import com.leftoverchef.app.data.db.AppDatabase
import com.leftoverchef.app.databinding.ActivityLoginBinding
import com.leftoverchef.app.ui.home.MainActivity
import com.leftoverchef.app.ui.home.OnboardingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        val clearError = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etEmail.addTextChangedListener(clearError)
        binding.etPassword.addTextChangedListener(clearError)

        if (intent.getBooleanExtra("password_reset_success", false)) {
            Toast.makeText(this, getString(R.string.password_reset_success), Toast.LENGTH_LONG).show()
        }
    }

    // Proveravamo kredencijale i prijavljujemo korisnika ako su tacni
    private fun attemptLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        // Radimo osnovnu validaciju pre pristupa bazi
        if (email.isEmpty()) { showError(getString(R.string.error_email_required)); return }
        if (password.isEmpty()) { showError("Please enter your password"); return }

        setLoading(true)
        Log.d(TAG, "Login attempt for email=$email")

        lifecycleScope.launch {
            // Citamo korisnika iz baze na IO thread-u da UI ostane responzivan
            val user = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).userDao().getUserByEmail(email)
            }
            setLoading(false)

            when {
                user == null -> {
                    Log.w(TAG, "Login failed: no account for $email")
                    showError("No account found with that email")
                }
                user.passwordHash != JwtManager.hashPassword(password) -> {
                    Log.w(TAG, "Login failed: wrong password for $email")
                    showError("Incorrect password")
                }
                else -> {
                    Log.d(TAG, "Login successful for userId=${user.id}")
                    // Cuvamo aktivnu sesiju kao JWT token
                    val token = JwtManager.createToken(user.id, user.username, user.email)
                    AuthManager.saveToken(applicationContext, token)
                    goToMain()
                }
            }
        }
    }

    // Vodimo korisnika na onboarding ili glavni ekran u zavisnosti od stanja naloga
    private fun goToMain() {
        val userId = AuthManager.getLoggedInUserId(this) ?: 0
        val prefs = getSharedPreferences("leftover_chef_prefs", android.content.Context.MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_complete_$userId", false)
        val intent = if (onboardingDone) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, OnboardingActivity::class.java).apply {
                putExtra(OnboardingActivity.EXTRA_USER_ID, userId)
            }
        }
        startActivity(intent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }


    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Signing in..." else "Sign In"
        binding.tvError.visibility = View.GONE
    }
}
