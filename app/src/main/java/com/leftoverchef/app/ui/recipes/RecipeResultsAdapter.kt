package com.leftoverchef.app.ui.recipes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.leftoverchef.app.R
import com.leftoverchef.app.data.api.RecipeSearchResult
import com.leftoverchef.app.databinding.ItemRecipeBinding

class RecipeResultsAdapter(
    private val onClick: (RecipeSearchResult) -> Unit
) : ListAdapter<RecipeSearchResult, RecipeResultsAdapter.RecipeViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecipeViewHolder(
        private val binding: ItemRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: RecipeSearchResult) {
            binding.tvRecipeTitle.text = recipe.title
            binding.tvUsedIngredients.text = "✅ Used: ${recipe.usedIngredientCount} ingredients"
            binding.tvMissedIngredients.text = "❌ Missing: ${recipe.missedIngredientCount} ingredients"

            Glide.with(binding.root.context)
                .load(recipe.image)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivRecipeImage)

            binding.root.setOnClickListener { onClick(recipe) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RecipeSearchResult>() {
        override fun areItemsTheSame(oldItem: RecipeSearchResult, newItem: RecipeSearchResult) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RecipeSearchResult, newItem: RecipeSearchResult) =
            oldItem == newItem
    }
}
