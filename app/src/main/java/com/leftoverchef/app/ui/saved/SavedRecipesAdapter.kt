package com.leftoverchef.app.ui.saved

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.leftoverchef.app.R
import com.leftoverchef.app.data.model.SavedRecipe
import com.leftoverchef.app.databinding.ItemSavedRecipeBinding

class SavedRecipesAdapter(
    private val onClick: (SavedRecipe) -> Unit,
    private val onDelete: (SavedRecipe) -> Unit
) : ListAdapter<SavedRecipe, SavedRecipesAdapter.SavedRecipeViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedRecipeViewHolder {
        val binding = ItemSavedRecipeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SavedRecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedRecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedRecipeViewHolder(
        private val binding: ItemSavedRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: SavedRecipe) {
            binding.tvRecipeTitle.text = recipe.title
            binding.tvCalories.text = "🔥 ${recipe.calories.toInt()} kcal"
            binding.tvProtein.text = "P: ${recipe.protein.toInt()}g"
            binding.tvCarbs.text = "C: ${recipe.carbs.toInt()}g"
            binding.tvFat.text = "F: ${recipe.fat.toInt()}g"

            Glide.with(binding.root.context)
                .load(recipe.image)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivRecipeImage)

            binding.root.setOnClickListener { onClick(recipe) }
            binding.btnDelete.setOnClickListener { onDelete(recipe) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SavedRecipe>() {
        override fun areItemsTheSame(oldItem: SavedRecipe, newItem: SavedRecipe) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SavedRecipe, newItem: SavedRecipe) =
            oldItem == newItem
    }
}
