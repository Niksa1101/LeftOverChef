package com.leftoverchef.app.ui.pantry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.leftoverchef.app.R
import com.leftoverchef.app.databinding.ActivityPantryBinding
import com.leftoverchef.app.viewmodel.MainViewModel

class PantryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPantryBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPantryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, IngredientListFragment())
                .commit()
        }
    }

    fun updateIngredientCount(count: Int) { /* count shown in stats on home screen */ }
}
