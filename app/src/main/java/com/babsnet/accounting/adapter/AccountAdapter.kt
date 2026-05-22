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
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.utils.AccountLocalizationUtil

class AccountAdapter(
    private var accounts: List<Account>,
    private val onEditClick: (Account) -> Unit,
    private val onDeleteClick: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private var allAccounts: List<Account> = accounts
    private var entryCounts: Map<Int, Int> = emptyMap()
    private var attachedContext: Context? = null

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        private val tvCount: TextView = view.findViewById(R.id.tvCategoryCount)
        private val imgIcon: ImageView = view.findViewById(R.id.imgCategoryIcon)

        @SuppressLint("SetTextI18n")
        fun bind(account: Account) {
            tvName.text = AccountLocalizationUtil.localizeAccountName(itemView.context, account)
            tvCount.text = itemView.context.getString(
                R.string.entries_count,
                entryCounts[account.accountId] ?: 0
            )

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

            try {
                imgIcon.setColorFilter((account.color ?: "#000000").toColorInt())
            } catch (e: Exception) {
                imgIcon.setColorFilter(Color.GRAY)
            }

            itemView.setOnClickListener {
                showPopupMenu(it, itemView.context, account)
            }
            itemView.setOnLongClickListener {
                showPopupMenu(it, itemView.context, account)
                true
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
        allAccounts = newAccounts
        accounts = newAccounts
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateEntryCounts(counts: Map<Int, Int>) {
        entryCounts = counts
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        val keyword = query.trim()
        val context = attachedContext
        accounts = if (keyword.isEmpty()) {
            allAccounts
        } else if (context == null) {
            allAccounts.filter {
                it.accountName.contains(keyword, ignoreCase = true)
            }
        } else {
            allAccounts.filter {
                AccountLocalizationUtil.matchesAccountQuery(context, it, keyword)
            }
        }
        notifyDataSetChanged()
    }

    fun currentItemCount(): Int = accounts.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedContext = recyclerView.context
    }
}
