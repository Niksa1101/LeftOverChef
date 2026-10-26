package com.leftoverchef.app.ui.pantry

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leftoverchef.app.R
import com.leftoverchef.app.data.model.Ingredient
import com.leftoverchef.app.databinding.ItemIngredientBinding
import com.leftoverchef.app.databinding.ItemIngredientHeaderBinding

class IngredientAdapter(
    private val onEdit: (Ingredient) -> Unit,
    private val onDelete: (Ingredient) -> Unit
) : ListAdapter<IngredientListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_INGREDIENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is IngredientListItem.Header -> VIEW_TYPE_HEADER
            is IngredientListItem.IngredientItem -> VIEW_TYPE_INGREDIENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemIngredientHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemIngredientBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                IngredientViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is IngredientListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is IngredientListItem.IngredientItem -> (holder as IngredientViewHolder).bind(item.ingredient)
        }
    }

    fun getIngredientAt(position: Int): Ingredient? {
        return when (val item = getItem(position)) {
            is IngredientListItem.IngredientItem -> item.ingredient
            else -> null
        }
    }

    inner class HeaderViewHolder(
        private val binding: ItemIngredientHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: IngredientListItem.Header) {
            val emoji = when (header.category.uppercase()) {
                "DAIRY"    -> "🥛"
                "PRODUCE"  -> "🥦"
                "MEAT"     -> "🥩"
                "BAKERY"   -> "🍞"
                "SPICES"   -> "🌶️"
                "FROZEN"   -> "🧊"
                "CANNED"   -> "🥫"
                "DRINKS"   -> "🥤"
                "SNACKS"   -> "🍿"
                "SEAFOOD"  -> "🐟"
                else       -> "🍽️"
            }
            binding.tvCategoryHeader.text = "$emoji  ${header.category.uppercase()}"
        }
    }

    inner class IngredientViewHolder(
        private val binding: ItemIngredientBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ingredient: Ingredient) {
            binding.tvIngredientName.text = ingredient.name
            val amountText = when {
                ingredient.amount.isNotEmpty() && ingredient.unit.isNotEmpty() ->
                    "${ingredient.amount} ${ingredient.unit}"
                ingredient.amount.isNotEmpty() -> ingredient.amount
                else -> ""
            }
            binding.tvIngredientAmount.text = amountText

            val dotColor = when (ingredient.category.uppercase()) {
                "DAIRY"   -> Color.parseColor("#4A90E2")
                "PRODUCE" -> Color.parseColor("#1E6B3C")
                "MEAT"    -> Color.parseColor("#E85D04")
                "BAKERY"  -> Color.parseColor("#C8882A")
                "SPICES"  -> Color.parseColor("#D4380D")
                "FROZEN"  -> Color.parseColor("#4AADE2")
                "CANNED"  -> Color.parseColor("#8B6F47")
                "DRINKS"  -> Color.parseColor("#2A9D8F")
                "SNACKS"  -> Color.parseColor("#E8A04A")
                else      -> Color.parseColor("#70615C")
            }
            binding.vCategoryDot.backgroundTintList = ColorStateList.valueOf(dotColor)

            binding.btnEdit.setOnClickListener { onEdit(ingredient) }
            binding.btnDelete.setOnClickListener { onDelete(ingredient) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<IngredientListItem>() {
        override fun areItemsTheSame(
            oldItem: IngredientListItem,
            newItem: IngredientListItem
        ): Boolean {
            return when {
                oldItem is IngredientListItem.Header && newItem is IngredientListItem.Header ->
                    oldItem.category == newItem.category
                oldItem is IngredientListItem.IngredientItem && newItem is IngredientListItem.IngredientItem ->
                    oldItem.ingredient.id == newItem.ingredient.id
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: IngredientListItem,
            newItem: IngredientListItem
        ): Boolean = oldItem == newItem
    }
}
