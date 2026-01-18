package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account
import androidx.core.graphics.toColorInt

class AccountAdapterDialog(
    private val accounts: List<Account>,
    private val onClick: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapterDialog.ViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.tvAccountName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_dialog, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = accounts[position]
        holder.name.text = item.accountName

        holder.itemView.setBackgroundColor(
            if (position == selectedPosition) "#E3F2FD".toColorInt() else Color.TRANSPARENT
        )

        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onClick(item)
        }
    }

    override fun getItemCount() = accounts.size
}
