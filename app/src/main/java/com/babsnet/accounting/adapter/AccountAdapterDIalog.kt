package com.babsnet.accounting.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account

class AccountAdapterDialog(
    private val accounts: List<Account>,
    private val onAccountSelected: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapterDialog.AccountViewHolder>() {

    // ViewHolder class untuk item account
    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAccountName: TextView = view.findViewById(R.id.tvAccountName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_dialog, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accounts[position]
        holder.tvAccountName.text = account.accountName
        holder.itemView.setOnClickListener { onAccountSelected(account) }
    }

    override fun getItemCount(): Int = accounts.size
}
