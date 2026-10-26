package com.leftoverchef.app.ui.pantry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.leftoverchef.app.R
import com.leftoverchef.app.data.model.Ingredient
import com.leftoverchef.app.databinding.FragmentAddIngredientBinding
import com.leftoverchef.app.viewmodel.MainViewModel

class AddIngredientFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddIngredientBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var existingIngredient: Ingredient? = null

    companion object {
        const val TAG = "AddIngredientFragment"
        private const val ARG_INGREDIENT_ID = "ingredient_id"
        private const val ARG_INGREDIENT_NAME = "ingredient_name"
        private const val ARG_INGREDIENT_AMOUNT = "ingredient_amount"
        private const val ARG_INGREDIENT_UNIT = "ingredient_unit"
        private const val ARG_INGREDIENT_CATEGORY = "ingredient_category"

        // Pravimo fragment za dodavanje novog ili izmenu postojeceg sastojka.
        fun newInstance(ingredient: Ingredient?): AddIngredientFragment {
            val fragment = AddIngredientFragment()
            if (ingredient != null) {
                fragment.arguments = Bundle().apply {
                    putInt(ARG_INGREDIENT_ID, ingredient.id)
                    putString(ARG_INGREDIENT_NAME, ingredient.name)
                    putString(ARG_INGREDIENT_AMOUNT, ingredient.amount)
                    putString(ARG_INGREDIENT_UNIT, ingredient.unit)
                    putString(ARG_INGREDIENT_CATEGORY, ingredient.category)
                }
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddIngredientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nudimo cesce sastojke kao predloge tokom unosa naziva

        val suggestions = resources.getStringArray(R.array.ingredient_suggestions)
        val autocompleteAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            suggestions
        )
        binding.etIngredientName.setAdapter(autocompleteAdapter)
        binding.etIngredientName.setOnItemClickListener { _, _, _, _ ->
            binding.tilName.error = null
        }


        val units = resources.getStringArray(R.array.ingredient_units)
        val unitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            units
        )
        binding.etIngredientUnit.setAdapter(unitAdapter)

        arguments?.let { args ->
            // Popunjavamo polja ako fragment otvaramo za izmenu postojeceg sastojka
            if (args.containsKey(ARG_INGREDIENT_NAME)) {
                existingIngredient = Ingredient(
                    id = args.getInt(ARG_INGREDIENT_ID),
                    name = args.getString(ARG_INGREDIENT_NAME, ""),
                    amount = args.getString(ARG_INGREDIENT_AMOUNT, ""),
                    unit = args.getString(ARG_INGREDIENT_UNIT, ""),
                    category = args.getString(ARG_INGREDIENT_CATEGORY, "Other")
                )
                binding.tvTitle.text = "Edit Ingredient"
                binding.etIngredientName.setText(existingIngredient!!.name)
                binding.etIngredientAmount.setText(existingIngredient!!.amount)
                binding.etIngredientUnit.setText(existingIngredient!!.unit, false)
                binding.spinnerCategory.setSelection(
                    getCategoryIndex(existingIngredient!!.category)
                )
            }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etIngredientName.text.toString().trim()
            if (name.isEmpty()) {
                binding.tilName.error = "Name is required"
                return@setOnClickListener
            }
            val category = binding.spinnerCategory.selectedItem.toString()
            val ingredient = Ingredient(
                id = existingIngredient?.id ?: 0,
                name = name,
                amount = binding.etIngredientAmount.text.toString().trim(),
                unit = binding.etIngredientUnit.text.toString().trim(),
                category = category,
                userId = viewModel.userId
            )
            if (existingIngredient != null) {
                viewModel.updateIngredient(ingredient)
            } else {
                viewModel.addIngredient(ingredient)
            }
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    // Nalazimo poziciju kategorije da spinner moze da je selektuje
    private fun getCategoryIndex(category: String): Int {
        val categories = resources.getStringArray(R.array.ingredient_categories)
        return categories.indexOf(category).takeIf { it >= 0 } ?: 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
