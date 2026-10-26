package com.leftoverchef.app.ui.saved

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.leftoverchef.app.R
import com.leftoverchef.app.databinding.ActivitySavedRecipesBinding

class SavedRecipesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedRecipesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedRecipesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SavedRecipesFragment())
                .commit()
        }
    }
}
