package com.leftoverchef.app.ui.saved

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.leftoverchef.app.R
import com.leftoverchef.app.databinding.FragmentSavedRecipesBinding
import com.leftoverchef.app.ui.recipes.RecipeDetailActivity
import com.leftoverchef.app.viewmodel.MainViewModel

class SavedRecipesFragment : Fragment() {

    private var _binding: FragmentSavedRecipesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: SavedRecipesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeToDelete()
        observeSavedRecipes()
        setupSearch()
    }

    // Podesavamo listu sacuvanih recepata i akcije nad svakom stavkom
    private fun setupRecyclerView() {
        adapter = SavedRecipesAdapter(
            onClick = { recipe ->
                val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
                intent.putExtra("RECIPE_ID", recipe.id)
                intent.putExtra("RECIPE_TITLE", recipe.title)
                startActivity(intent)
            },
            onDelete = { recipe -> viewModel.deleteSavedRecipe(recipe) }
        )
        binding.rvSavedRecipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSavedRecipes.adapter = adapter
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
                val recipe = adapter.currentList[viewHolder.adapterPosition]
                viewModel.deleteSavedRecipe(recipe)

                Snackbar.make(binding.root, "${recipe.title} removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        viewModel.saveRecipe(recipe)
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
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvSavedRecipes)
    }

    // Posmatramo promene sacuvanih recepata i osvezavamo prikaz liste
    private fun observeSavedRecipes() {
        viewModel.savedRecipes.observe(viewLifecycleOwner) { recipes ->
            adapter.submitList(recipes)
            binding.tvEmpty.visibility =
                if (recipes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // Filtriramo sacuvane recepte dok korisnik unosi tekst u pretragu
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    observeSavedRecipes()
                } else {
                    viewModel.searchSavedRecipes(query).observe(viewLifecycleOwner) { recipes ->
                        adapter.submitList(recipes)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
