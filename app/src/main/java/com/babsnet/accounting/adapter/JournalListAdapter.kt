package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.JournalWithDetails
import com.babsnet.accounting.databinding.ItemJournalBinding
import com.babsnet.accounting.utils.CurrencyFormatUtil
import com.babsnet.accounting.utils.DateUtil


class JournalWithDetailsAdapter(
    private val onDeleteJournal: (JournalWithDetails) -> Unit,
    private val onEditJournal: (JournalWithDetails) -> Unit // Add callback for editing
) : ListAdapter<JournalWithDetails, JournalWithDetailsAdapter.JournalViewHolder>(JournalWithDetailsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JournalViewHolder(onDeleteJournal, onEditJournal, binding) // Pass onEditJournal to the ViewHolder
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val current = getItem(position)
        if (current != null) {
            holder.bind(current)
        }
    }

    class JournalViewHolder(
        private val onDeleteJournal: (JournalWithDetails) -> Unit,
        private val onEditJournal: (JournalWithDetails) -> Unit, // Add onEditJournal as a parameter
        private val binding: ItemJournalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(journalWithDetails: JournalWithDetails) {
            val journal = journalWithDetails.journal
            val dateString = DateUtil.formatDateFromDateObject(journal.date!!)
            binding.textDate.text = DateUtil.formatDate(dateString)
            binding.textDescription.text = journal.description
            binding.textCreatedAt.text = DateUtil.formatDate(journal.createdAt.toString())
            val context = binding.root.context
            if(journalWithDetails.ledgers.isNotEmpty()) {
                val firstLedger = journalWithDetails.ledgers[0]
                val valueAmountOne = if (firstLedger.ledgerCredit == 0.0) {
                    firstLedger.ledgerDebit
                } else {
                    firstLedger.ledgerCredit
                }

                val firstLedgerTwo = journalWithDetails.ledgers[1]
                val valueAmountTwo = if (firstLedgerTwo.ledgerDebit == 0.0) {
                    firstLedgerTwo.ledgerCredit
                } else {
                    firstLedgerTwo.ledgerDebit
                }
                val df = CurrencyFormatUtil.decimalDownFormatter()
                binding.textAccountNameOne.text = "${journalWithDetails.ledgers[0].accountName} (${df.format(valueAmountOne)})"
                binding.textAccountNameTwo.text =  "${journalWithDetails.ledgers[1].accountName} (${df.format(valueAmountTwo)})"

                val defaultBg = context.themeColor(R.attr.colorSurface)
                val incomeBg  = ContextCompat.getColor(context, R.color.green)

                binding.cardListJournal.setBackgroundColor(defaultBg)

                val l1 = journalWithDetails.ledgers.getOrNull(0)
                val l2 = journalWithDetails.ledgers.getOrNull(1)

                val hasIncome = listOfNotNull(l1, l2).any { it.accountType.equals("Income", true) }

                if (hasIncome) {
                    binding.labelAccountNameTwo.text = "Income"
                    binding.cardListJournal.setBackgroundColor(incomeBg)
                    binding.textCreatedAt.setTextColor(ContextCompat.getColor(context, R.color.white))
                } else {
                    binding.labelAccountNameTwo.text = l2?.accountType.orEmpty()
                    binding.cardListJournal.setBackgroundColor(defaultBg)
                }

            }




            binding.menuButton.setOnClickListener {
                showPopupMenu(it, context, journalWithDetails)
            }
        }

        private fun showPopupMenu(
            view: View,
            context: Context,
            journalWithDetails: JournalWithDetails
        ) {
            val popup = android.widget.PopupMenu(context, view)
            popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_edit -> {
                        onEditJournal(journalWithDetails)
                        true
                    }
                    R.id.menu_delete -> {
                        onDeleteJournal(journalWithDetails)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        fun Context.themeColor(@AttrRes attr: Int): Int {
            val tv = TypedValue()
            theme.resolveAttribute(attr, tv, true)
            return if (tv.resourceId != 0) {
                ContextCompat.getColor(this, tv.resourceId)
            } else {
                tv.data
            }
        }
    }




    class JournalWithDetailsComparator : DiffUtil.ItemCallback<JournalWithDetails>() {
        override fun areItemsTheSame(oldItem: JournalWithDetails, newItem: JournalWithDetails): Boolean {
            return oldItem.journal.journalId == newItem.journal.journalId
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: JournalWithDetails, newItem: JournalWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}
