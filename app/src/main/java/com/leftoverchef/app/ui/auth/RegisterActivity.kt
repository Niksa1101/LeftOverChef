package com.leftoverchef.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.leftoverchef.app.R
import com.leftoverchef.app.auth.AuthManager
import com.leftoverchef.app.auth.JwtManager
import com.leftoverchef.app.data.db.AppDatabase
import com.leftoverchef.app.data.model.User
import com.leftoverchef.app.databinding.ActivityRegisterBinding
import com.leftoverchef.app.ui.home.OnboardingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Registrujemo novog korisnika i pravimo Onboarding odmah
        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvLoginLink.setOnClickListener { finish() }

        val clearError = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etUsername.addTextChangedListener(clearError)
        binding.etEmail.addTextChangedListener(clearError)
        binding.etPassword.addTextChangedListener(clearError)
        binding.etConfirmPassword.addTextChangedListener(clearError)
    }

    private fun attemptRegister() {
        val username        = binding.etUsername.text?.toString()?.trim().orEmpty()
        val email           = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password        = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        // Prvo se proverava ispravnost unosa pre rada sa bazom
        if (username.isEmpty())       { showError("Please enter a username"); return }
        if (username.length < 3)      { showError("Username must be at least 3 characters"); return }
        if (email.isEmpty())          { showError(getString(R.string.error_email_required)); return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_email_invalid)); return
        }
        if (password.isEmpty())       { showError("Please enter a password"); return }
        if (password.length < 6)      { showError("Password must be at least 6 characters"); return }
        if (password != confirmPassword) { showError(getString(R.string.error_passwords_no_match)); return }

        setLoading(true)
        Log.d(TAG, "Registration attempt: username=$username, email=$email")

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)

            // Email mora biti jedinstven
            val emailTaken = withContext(Dispatchers.IO) {
                db.userDao().emailExists(email) > 0
            }

            if (emailTaken) {
                setLoading(false)
                Log.w(TAG, "Registration failed: email already in use — $email")
                showError("An account with this email already exists")
                return@launch
            }

            // Lozinka se cuva samo kao hash
            val newUser = User(
                username     = username,
                email        = email,
                passwordHash = JwtManager.hashPassword(password)
            )

            val userId = withContext(Dispatchers.IO) {
                db.userDao().insertUser(newUser).toInt()
            }

            Log.d(TAG, "Registration successful: userId=$userId, username=$username")
            setLoading(false)

            // Novi korisnik dobija aktivnu sesiju odmah nakon registracije
            val token = JwtManager.createToken(userId, username, email)
            AuthManager.saveToken(applicationContext, token)
            goToOnboarding(userId)
        }
    }

    private fun goToOnboarding(userId: Int) {
        startActivity(Intent(this, OnboardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(OnboardingActivity.EXTRA_USER_ID, userId)
        })
    }

    private fun showError(message: String) {
        binding.tvError.text       = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text      = if (loading) "Creating account…" else "Create Account"
        binding.tvError.visibility    = View.GONE
    }
}
