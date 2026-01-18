package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.TransactionData
import com.babsnet.accounting.utils.DateUtil
import java.text.DecimalFormat

class TransactionCategoryAdapter : RecyclerView.Adapter<TransactionCategoryAdapter.ViewHolder>() {

    private val items = mutableListOf<TransactionData>()
    private val decimalFormat = DecimalFormat("#,###.##")

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(list: List<TransactionData>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNo: TextView = v.findViewById(R.id.tvNo)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvAccount: TextView = v.findViewById(R.id.tvAccount)
        val tvDesc: TextView = v.findViewById(R.id.tvDesc)
        val tvTotal: TextView = v.findViewById(R.id.tvTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_category, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNo.text = (position + 1).toString()
        holder.tvAccount.text = item.accountName
        val dateStr = DateUtil.dateToString(item.journalDate, "dd MMM yyyy")
        holder.tvDate.text = dateStr
        holder.tvDesc.text = item.description
        holder.tvTotal.text = if (item.debit != 0.0) decimalFormat.format(item.debit) else ""
    }

    override fun getItemCount(): Int = items.size
}