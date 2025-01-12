package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account
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
        private val deleteImageView: ImageView = view.findViewById(R.id.imageDelete)

        @SuppressLint("SetTextI18n")
        fun bind(account: Account) {
            nameTextView.text = account.accountName
            nameTextAccountType.text = account.accountType
            balanceTextView.text = "$${account.balance}"
            itemView.setOnClickListener { onEditClick(account) }

            deleteImageView.setOnClickListener {
                Utils.showDeleteConfirmationDialog(itemView.context){
                    onDeleteClick(account) }
                }

        }

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
