package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.utils.AccountLocalizationUtil

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
        holder.name.text = AccountLocalizationUtil.localizeAccountName(holder.itemView.context, item)

        val context = holder.itemView.context
        val isSelected = position == selectedPosition
        holder.itemView.setBackgroundColor(
            if (isSelected) ContextCompat.getColor(context, R.color.soft_accent_surface) else Color.TRANSPARENT
        )
        holder.name.setTextColor(
            if (isSelected) ContextCompat.getColor(context, R.color.nav_active_text)
            else ContextCompat.getColor(context, R.color.text_primary)
        )

        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onClick(item)
        }
    }

    override fun getItemCount() = accounts.size
}
