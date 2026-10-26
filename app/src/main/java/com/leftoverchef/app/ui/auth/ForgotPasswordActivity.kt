package com.leftoverchef.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.leftoverchef.app.R
import com.leftoverchef.app.data.db.AppDatabase
import com.leftoverchef.app.data.model.PasswordResetCode
import com.leftoverchef.app.databinding.ActivityForgotPasswordBinding
import com.leftoverchef.app.email.EmailSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.tvSignInLink.setOnClickListener { finish() }
        binding.btnSendCode.setOnClickListener { sendCode() }
    }

    // Proveravamo email, cuvamo reset kod i saljemo ga korisniku.
    private fun sendCode() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()

        // Proveravamo da li je unet validan email pre rada sa bazom.
        if (email.isEmpty()) {
            showError(getString(R.string.error_email_required))
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_email_invalid))
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val user = withContext(Dispatchers.IO) { db.userDao().getUserByEmail(email) }

            if (user == null) {
                setLoading(false)
                showError(getString(R.string.error_email_not_found))
                return@launch
            }

            val plainCode = (100_000..999_999).random().toString()
            val codeHash = sha256(plainCode)
            val expiry = System.currentTimeMillis() + 15L * 60 * 1_000

            withContext(Dispatchers.IO) {
                // Cuvamo samo poslednji aktivni reset kod za tog korisnika
                db.passwordResetDao().deleteCodesForUser(user.id)
                db.passwordResetDao().insertCode(
                    PasswordResetCode(
                        userId = user.id,
                        codeHash = codeHash,
                        expiryMillis = expiry
                    )
                )
            }

            // Nastavljamo na reset ekran i ako email ne ode, kroz demo kod fallback
            val emailSent = EmailSender.sendResetCode(email, user.username, plainCode)

            setLoading(false)

            val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java).apply {
                putExtra(ResetPasswordActivity.EXTRA_EMAIL, email)
                putExtra(ResetPasswordActivity.EXTRA_USER_ID, user.id)
                if (!emailSent) {
                    putExtra(ResetPasswordActivity.EXTRA_DEMO_CODE, plainCode)
                }
            }
            startActivity(intent)
        }
    }

    // Hesiramo reset kod pre cuvanja u bazi
    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSendCode.isEnabled = !loading
        binding.btnSendCode.text = if (loading) getString(R.string.sending)
        else getString(R.string.btn_send_code)
        binding.tvError.visibility = View.GONE
    }
}
