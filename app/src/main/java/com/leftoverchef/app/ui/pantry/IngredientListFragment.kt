package com.leftoverchef.app.ui.pantry


import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.leftoverchef.app.R
import com.leftoverchef.app.data.model.Ingredient
import com.leftoverchef.app.databinding.FragmentIngredientListBinding
import com.leftoverchef.app.ui.recipes.RecipeResultsActivity
import com.leftoverchef.app.viewmodel.MainViewModel

class IngredientListFragment : Fragment() {

    private var _binding: FragmentIngredientListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: IngredientAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIngredientListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Fragment spaja prikaz liste, pretragu i akcije nad pantry-jem
        setupRecyclerView()
        setupSwipeToDelete()
        observeIngredients()
        setupSearch()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = IngredientAdapter(
            onEdit = { ingredient -> showEditBottomSheet(ingredient) },
            onDelete = { ingredient -> viewModel.deleteIngredient(ingredient) }
        )
        binding.rvIngredients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvIngredients.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val ingredient = adapter.getIngredientAt(viewHolder.adapterPosition) ?: return
                viewModel.deleteIngredient(ingredient)

                Snackbar.make(binding.root, "${ingredient.name} removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        viewModel.addIngredient(ingredient)
                    }
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.on_background))
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.surface))
                    .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    .show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint().apply { color = ContextCompat.getColor(requireContext(), R.color.error) }
                val background = RectF(
                    itemView.right + dX, itemView.top.toFloat(),
                    itemView.right.toFloat(), itemView.bottom.toFloat()
                )
                c.drawRoundRect(background, 12f, 12f, paint)

                val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)!!
                deleteIcon.setTint(Color.WHITE)
                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + deleteIcon.intrinsicHeight
                val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                deleteIcon.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvIngredients)
    }

    private fun observeIngredients() {
        viewModel.displayIngredients.observe(viewLifecycleOwner) { ingredients ->
            // Adapter dobija listu sa header stavkama i stvarnim sastojcima
            val groupedList = buildGroupedList(ingredients)
            adapter.submitList(groupedList)
            binding.tvEmpty.visibility =
                if (ingredients.isEmpty()) View.VISIBLE else View.GONE
            (activity as? PantryActivity)?.updateIngredientCount(ingredients.size)
        }
    }

    private fun buildGroupedList(ingredients: List<Ingredient>): List<IngredientListItem> {
        val result = mutableListOf<IngredientListItem>()
        val grouped = ingredients.groupBy { it.category }
        val sortedCategories = grouped.keys.sorted()
        for (category in sortedCategories) {
            result.add(IngredientListItem.Header(category))
            grouped[category]?.forEach { ingredient ->
                result.add(IngredientListItem.IngredientItem(ingredient))
            }
        }
        return result
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtons() {
        binding.fabAddIngredient.setOnClickListener {
            showAddBottomSheet()
        }
        binding.fabFindRecipes.setOnClickListener {
            // Prelazimo na API pretragu koristeci trenutno stanje ostave
            startActivity(Intent(requireContext(), RecipeResultsActivity::class.java))
        }
    }

    private fun showAddBottomSheet() {
        val sheet = AddIngredientFragment.newInstance(null)
        sheet.show(parentFragmentManager, AddIngredientFragment.TAG)
    }

    private fun showEditBottomSheet(ingredient: Ingredient) {
        val sheet = AddIngredientFragment.newInstance(ingredient)
        sheet.show(parentFragmentManager, AddIngredientFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
