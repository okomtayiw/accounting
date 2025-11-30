package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R

class IconAdapter(
    private var iconList: List<IconItem>,
    private val onClick: (IconItem) -> Unit
) : RecyclerView.Adapter<IconAdapter.IconViewHolder>() {

    private var filteredList = iconList
    private var selectedKey: String? = null

    inner class IconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgIcon)
        val container: FrameLayout = view.findViewById(R.id.iconContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val item = filteredList[position]

        holder.icon.setImageResource(item.resId)

        val ctx: Context = holder.itemView.context

        if (item.key == selectedKey) {
            holder.container.background =
                ContextCompat.getDrawable(ctx, R.drawable.icon_selected_bg)
        } else {
            holder.container.background =
                ContextCompat.getDrawable(ctx, R.drawable.icon_unselected_bg)
        }

        holder.container.setOnClickListener {
            selectedKey = item.key
            notifyDataSetChanged()
            onClick(item)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            iconList
        } else {
            iconList.filter {
                it.displayName.lowercase().contains(query.lowercase()) ||
                        it.category.lowercase().contains(query.lowercase())
            }
        }
        notifyDataSetChanged()
    }

    fun setSelectedIcon(key: String?) {
        selectedKey = key
        notifyDataSetChanged()
    }

    fun getSelectedIcon(): String {
        return selectedKey ?: "ic_home"
    }
}
