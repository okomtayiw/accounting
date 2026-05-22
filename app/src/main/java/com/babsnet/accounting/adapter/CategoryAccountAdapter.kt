package com.babsnet.accounting.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.utils.AccountLocalizationUtil

class CategoryAccountAdapter(
    private val accounts: List<Account>,
    private val onCategorySelected: (Account) -> Unit
) : RecyclerView.Adapter<CategoryAccountAdapter.ViewHolder>() {

    private var selectedPosition = -1
    private var selectedAccountId: Int? = null


    // ================================
    //  VIEW HOLDER
    // ================================
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layout: LinearLayout = itemView.findViewById(R.id.layoutCategory)
        val img: ImageView = itemView.findViewById(R.id.imgCategory)
        val name: TextView = itemView.findViewById(R.id.tvCategoryName)
    }


    // ================================
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }


    // ================================
    //  FAST UPDATE WITH PAYLOAD
    // ================================
    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        updateUI(holder, position)

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) updateUI(holder, position)
        else super.onBindViewHolder(holder, position, payloads)
    }


    // ================================
    //  MAIN UI LOGIC (HIGHLIGHT)
    // ================================
    private fun updateUI(holder: ViewHolder, position: Int) {
        val account = accounts[position]
        holder.name.text = AccountLocalizationUtil.localizeAccountName(holder.itemView.context, account)

        val iconResId = holder.itemView.context.resources.getIdentifier(
            account.iconResName, "drawable", holder.itemView.context.packageName
        )
        holder.img.setImageResource(if (iconResId != 0) iconResId else R.drawable.ic_home)


        val selectedText = ContextCompat.getColor(holder.itemView.context, R.color.nav_active_text)
        val defaultText = ContextCompat.getColor(holder.itemView.context, R.color.text_primary)

        if (position == selectedPosition) {
            holder.layout.setBackgroundResource(R.drawable.journal_category_card_selected_bg)
            holder.name.setTextColor(selectedText)
            holder.img.setColorFilter(selectedText)
        } else {
            holder.layout.setBackgroundResource(R.drawable.journal_category_card_bg)
            holder.name.setTextColor(defaultText)
            holder.img.setColorFilter(defaultText)
        }


        holder.itemView.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = position

            if (prev != -1) notifyItemChanged(prev, "unselect")
            notifyItemChanged(selectedPosition, "select")

            onCategorySelected(account)
        }
    }

    fun setSelectedAccountId(id: Int?) {
        if (id == null) return

        var index = -1
        for (i in accounts.indices) {
            if (accounts[i].accountId.toString() == id.toString()) {
                index = i
                break
            }
        }

        if (index != -1) {
            val prev = selectedPosition
            selectedPosition = index
            notifyItemChanged(prev)
            notifyItemChanged(index)
        } else {
            Log.w("Adapter", "ID $id tidak ditemukan — list belum update")
        }
    }

    override fun getItemCount() = accounts.size
}
