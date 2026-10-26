package com.leftoverchef.app.ui.calories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leftoverchef.app.R
import com.leftoverchef.app.data.model.CalorieLogEntry
import com.leftoverchef.app.databinding.ItemCalorieLogBinding
import java.text.SimpleDateFormat
import java.util.*

class CalorieLogAdapter(
    private val onDelete: (CalorieLogEntry) -> Unit
) : ListAdapter<CalorieLogEntry, CalorieLogAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemCalorieLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: CalorieLogEntry) {
            val s = entry.servingsEaten
            binding.tvMealName.text = if (s > 1) "${entry.recipeTitle} ×$s" else entry.recipeTitle
            binding.tvMealCalories.text = "${(entry.calories * s).toInt()} kcal"
            binding.tvMealProtein.text = "P: ${(entry.protein * s).toInt()}g"
            binding.tvMealCarbs.text = "C: ${(entry.carbs * s).toInt()}g"
            binding.tvMealFat.text = "F: ${(entry.fat * s).toInt()}g"
            binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.dateMillis))
            binding.btnDelete.setOnClickListener { onDelete(entry) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCalorieLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<CalorieLogEntry>() {
        override fun areItemsTheSame(a: CalorieLogEntry, b: CalorieLogEntry) = a.id == b.id
        override fun areContentsTheSame(a: CalorieLogEntry, b: CalorieLogEntry) = a == b
    }
}
