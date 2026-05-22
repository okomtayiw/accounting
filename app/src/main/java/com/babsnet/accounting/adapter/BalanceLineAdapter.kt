package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.BalanceLine
import com.babsnet.accounting.utils.AccountLocalizationUtil

class BalanceLineAdapter(
    private val formatNumber: (Double) -> String
) : RecyclerView.Adapter<BalanceLineAdapter.VH>() {

    private val items = mutableListOf<BalanceLine>()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<BalanceLine>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvLeft: TextView = v.findViewById(R.id.tvLeft)
        val tvRight: TextView = v.findViewById(R.id.tvRight)
        val tvSub: TextView = v.findViewById(R.id.tvSub)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_balance_line, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvLeft.text = AccountLocalizationUtil.localizeAccountName(holder.itemView.context, item.title)
        holder.tvSub.text = AccountLocalizationUtil.localizeAccountType(holder.itemView.context, item.accountType)
        holder.tvRight.text = formatNumber(item.amount)
    }

    override fun getItemCount(): Int = items.size
}
