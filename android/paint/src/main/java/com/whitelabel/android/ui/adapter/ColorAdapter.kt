package com.whitelabel.android.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.whitelabel.android.data.model.ColorProperty
import com.whitelabel.android.databinding.ColorItemBinding
import com.whitelabel.android.utils.Utils.toTextColor

class ColorAdapter(
    private val colors: List<ColorProperty>,
    private val onColorSelected: (ColorProperty) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    inner class ColorViewHolder(private val binding: ColorItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(color: ColorProperty) {
            Log.d("ColorAdapter", "bind: $color ${color.colorValue}")
            binding.colorCodeTextView.setBackgroundColor(color.colorValue)
            binding.colorCodeTextView.text = color.colorName
            binding.colorCodeTextView.setTextColor(color.colorValue.toTextColor())
            binding.root.setOnClickListener {
                onColorSelected(color)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ColorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size
}
