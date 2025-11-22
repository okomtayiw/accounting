package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.data.entity.JournalWithDetails
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.Utils

class AccountAdapter(
    private var accounts: List<Account>,
    private val onEditClick: (Account) -> Unit,
    private val onDeleteClick: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    // ViewHolder class
    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.findViewById(R.id.textViewName)
        private val nameTextAccountType: TextView = view.findViewById(R.id.textViewAccountType)
        private val balanceTextView: TextView = view.findViewById(R.id.textViewBalance)
        private val textViewCreatedDate:TextView = view.findViewById(R.id.textViewCreatedDate)
        private val openMenu: ImageView = view.findViewById(R.id.arrowIcon)

        @SuppressLint("SetTextI18n")
        fun bind(account: Account) {
            nameTextView.text = account.accountName
            nameTextAccountType.text = account.accountType
            if(account.createdAt.toString().isEmpty() || account.createdAt == null) {
                textViewCreatedDate.text = ""
            } else {
                textViewCreatedDate.text = DateUtil.dateToString(account.createdAt!!)
            }

            itemView.setOnClickListener { onEditClick(account) }

            openMenu.setOnClickListener {
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
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
