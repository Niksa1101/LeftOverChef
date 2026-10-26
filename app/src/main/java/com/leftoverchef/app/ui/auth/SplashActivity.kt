package com.leftoverchef.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.leftoverchef.app.auth.AuthManager
import com.leftoverchef.app.ui.home.MainActivity
import com.leftoverchef.app.ui.home.OnboardingActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val navigateRunnable = Runnable { navigate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler.postDelayed(navigateRunnable, 1500)
    }

    private fun navigate() {
        when {
            !AuthManager.isLoggedIn(this) -> {
                Log.d("SplashActivity", "No active session → LoginActivity")
                startActivity(Intent(this, LoginActivity::class.java))
            }
            !isOnboardingComplete() -> {
                val userId = AuthManager.getLoggedInUserId(this) ?: 0
                Log.d("SplashActivity", "Onboarding incomplete for userId=$userId → OnboardingActivity")
                startActivity(Intent(this, OnboardingActivity::class.java).apply {
                    putExtra(OnboardingActivity.EXTRA_USER_ID, userId)
                })
            }
            else -> {
                Log.d("SplashActivity", "Session valid, onboarding complete → MainActivity")
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
        finish()
    }

    private fun isOnboardingComplete(): Boolean {
        val userId = AuthManager.getLoggedInUserId(this) ?: return false
        return getSharedPreferences("leftover_chef_prefs", MODE_PRIVATE)
            .getBoolean("onboarding_complete_$userId", false)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(navigateRunnable)
    }
}
