package com.leftoverchef.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.leftoverchef.app.R
import com.leftoverchef.app.auth.JwtManager
import com.leftoverchef.app.data.db.AppDatabase
import com.leftoverchef.app.databinding.ActivityResetPasswordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class ResetPasswordActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL     = "extra_reset_email"
        const val EXTRA_USER_ID   = "extra_reset_user_id"
        const val EXTRA_DEMO_CODE = "extra_reset_demo_code"
    }

    private lateinit var binding: ActivityResetPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email    = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        val userId   = intent.getIntExtra(EXTRA_USER_ID, -1)
        val demoCode = intent.getStringExtra(EXTRA_DEMO_CODE)

        if (demoCode != null) {
            binding.tvEmailHint.text        = getString(R.string.reset_email_hint_screen)
            binding.tvDemoBanner.text       = demoCode
            binding.cardDemoCode.visibility = View.VISIBLE
        } else {
            binding.tvEmailHint.text = getString(R.string.reset_email_hint_sent, email)
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnResetPassword.setOnClickListener { resetPassword(userId) }
    }

    private fun resetPassword(userId: Int) {
        val code     = binding.etCode.text?.toString()?.trim().orEmpty()
        val password = binding.etNewPassword.text?.toString().orEmpty()
        val confirm  = binding.etConfirmPassword.text?.toString().orEmpty()

        // Bez ispravnog koda i nove sifre ne pokusavamo update u bazi
        when {
            code.isEmpty()      -> { showError(getString(R.string.error_code_required));      return }
            password.isEmpty()  -> { showError(getString(R.string.error_password_required));  return }
            password.length < 6 -> { showError(getString(R.string.error_password_length));    return }
            password != confirm -> { showError(getString(R.string.error_passwords_no_match)); return }
        }

        setLoading(true)

        lifecycleScope.launch {
            val db     = AppDatabase.getDatabase(applicationContext)
            val stored = withContext(Dispatchers.IO) { db.passwordResetDao().getLatestCode(userId) }
            val now    = System.currentTimeMillis()

            // Kod mora da postoji, da nije istekao i da hash odgovara unesenom broju
            when {
                stored == null ->
                    { setLoading(false); showError(getString(R.string.error_code_invalid)); return@launch }
                stored.expiryMillis < now ->
                    { setLoading(false); showError(getString(R.string.error_code_expired)); return@launch }
                stored.codeHash != sha256(code) ->
                    { setLoading(false); showError(getString(R.string.error_code_invalid)); return@launch }
            }

            val newHash = JwtManager.hashPassword(password)
            withContext(Dispatchers.IO) {
                // Posle promene sifre brisemo reset kodove da se ne mogu ponovo koristiti
                db.userDao().updatePassword(userId, newHash)
                db.passwordResetDao().deleteCodesForUser(userId)
            }

            setLoading(false)

            startActivity(
                Intent(this@ResetPasswordActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("password_reset_success", true)
                }
            )
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun showError(message: String) {
        binding.tvError.text       = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnResetPassword.isEnabled = !loading
        binding.btnResetPassword.text = if (loading) getString(R.string.resetting)
                                        else          getString(R.string.btn_reset_password)
        binding.tvError.visibility = View.GONE
    }
}
