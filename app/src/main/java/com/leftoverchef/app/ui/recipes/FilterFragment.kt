package com.leftoverchef.app.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.leftoverchef.app.R
import com.leftoverchef.app.databinding.FragmentFilterBinding
import com.leftoverchef.app.viewmodel.MainViewModel

class FilterFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    companion object {
        const val TAG = "FilterFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val current: RecipeFilters = viewModel.currentFilters.value ?: RecipeFilters()

        when (current.maxMissingIngredients) {
            0 -> binding.chipMissing0.isChecked = true
            1 -> binding.chipMissing1.isChecked = true
            2 -> binding.chipMissing2.isChecked = true
            else -> binding.chipMissingAny.isChecked = true
        }
        when (current.maxPrepTime) {
            15 -> binding.chipTime15.isChecked = true
            30 -> binding.chipTime30.isChecked = true
            60 -> binding.chipTime60.isChecked = true
            else -> binding.chipTimeAny.isChecked = true
        }
        when (current.maxCalories) {
            300 -> binding.chipCal300.isChecked = true
            500 -> binding.chipCal500.isChecked = true
            800 -> binding.chipCal800.isChecked = true
            else -> binding.chipCalAny.isChecked = true
        }

        binding.btnApply.setOnClickListener {
            val maxMissing = when (binding.chipGroupMissing.checkedChipId) {
                R.id.chipMissing0 -> 0
                R.id.chipMissing1 -> 1
                R.id.chipMissing2 -> 2
                else -> Int.MAX_VALUE
            }
            val maxTime = when (binding.chipGroupTime.checkedChipId) {
                R.id.chipTime15 -> 15
                R.id.chipTime30 -> 30
                R.id.chipTime60 -> 60
                else -> Int.MAX_VALUE
            }
            val maxCal = when (binding.chipGroupCalories.checkedChipId) {
                R.id.chipCal300 -> 300
                R.id.chipCal500 -> 500
                R.id.chipCal800 -> 800
                else -> Int.MAX_VALUE
            }
            viewModel.applyFilters(RecipeFilters(maxMissing, maxTime, maxCal))
            dismiss()
        }

        binding.btnReset.setOnClickListener {
            viewModel.applyFilters(RecipeFilters())
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
