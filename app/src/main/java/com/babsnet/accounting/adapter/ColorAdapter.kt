package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import androidx.core.graphics.toColorInt

class ColorAdapter(
    private var colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private var selectedColor: Int? = null

    inner class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: FrameLayout = view.findViewById(R.id.colorContainer)
        val viewColor: View = view.findViewById(R.id.viewColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]

        // Set warna circle
        holder.viewColor.background.setTint(color)

        // Highlight jika dipilih
        if (selectedColor == color) {
            holder.container.background = ContextCompat.getDrawable(
                holder.itemView.context,
                R.drawable.color_circle_selected
            )
        } else {
            holder.container.background = null
        }

        holder.container.setOnClickListener {
            selectedColor = color
            notifyDataSetChanged()
            onColorSelected(color)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedColor(colorHex: String?) {
        selectedColor = if (colorHex != null) {
            try {
                colorHex.toColorInt() } catch (e: Exception) { null }
        } else null
        notifyDataSetChanged()
    }



    override fun getItemCount(): Int = colors.size
}
