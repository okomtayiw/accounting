package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account
import androidx.core.graphics.toColorInt

class AccountAdapter(
    private var accounts: List<Account>,
    private val onEditClick: (Account) -> Unit,
    private val onDeleteClick: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        private val imgIcon: ImageView = view.findViewById(R.id.imgCategoryIcon)

        @SuppressLint("SetTextI18n")
        fun bind(account: Account) {

            // Set name
            tvName.text = account.accountName

            // --- SET ICON ---
            val context = itemView.context

            val iconId = context.resources.getIdentifier(
                account.iconResName ?: "",
                "drawable",
                context.packageName
            )

            if (iconId != 0) {
                imgIcon.setImageResource(iconId)
            } else {
                imgIcon.setImageResource(R.drawable.ic_category)
            }

            // --- SET COLOR ---
            try {
                imgIcon.setColorFilter((account.color ?: "#000000").toColorInt())
            } catch (e: Exception) {
                imgIcon.setColorFilter(Color.GRAY)
            }

            // Klik item → edit
            itemView.setOnClickListener { onEditClick(account) }

            // Long press popup
            tvName.setOnClickListener {
                showPopupMenu(it, itemView.context, account)
            }
        }
    }

    private fun showPopupMenu(
        view: View,
        context: Context,
        account: Account
    ) {
        val popup = android.widget.PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_edit -> {
                    onEditClick(account)
                    true
                }
                R.id.menu_delete -> {
                    onDeleteClick(account)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int = accounts.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newAccounts: List<Account>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }
}
