package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
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
import com.babsnet.accounting.utils.AccountLocalizationUtil
import com.babsnet.accounting.utils.CurrencyFormatUtil
import com.babsnet.accounting.utils.DateUtil
import java.util.Locale


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
            binding.textDate.text = DateUtil.formatDate(dateString).uppercase(Locale.getDefault())
            binding.textDescription.text = journal.description
            binding.textCreatedAt.text = DateUtil.formatDate(journal.createdAt.toString())
            binding.labelDate.text = journal.description
            val context = binding.root.context
            if (journalWithDetails.ledgers.isNotEmpty()) {
                val l1 = journalWithDetails.ledgers[0]
                val l2 = journalWithDetails.ledgers.getOrNull(1)
                val valueAmountOne = if (l1.ledgerCredit == 0.0) {
                    l1.ledgerDebit
                } else {
                    l1.ledgerCredit
                }

                val valueAmountTwo = if ((l2?.ledgerDebit ?: 0.0) == 0.0) {
                    l2?.ledgerCredit ?: 0.0
                } else {
                    l2?.ledgerDebit ?: 0.0
                }
                binding.textAccountNameOne.text =
                    "${AccountLocalizationUtil.localizeAccountName(context, l1.accountName)} (${CurrencyFormatUtil.formatCurrency(context, valueAmountOne)})"
                binding.textAccountNameTwo.text = if (l2 != null) {
                    "${AccountLocalizationUtil.localizeAccountName(context, l2.accountName)} (${CurrencyFormatUtil.formatCurrency(context, valueAmountTwo)})"
                } else {
                    "-"
                }

                val defaultBg = ContextCompat.getColor(context, R.color.journal_card_bg)
                val incomeBg = ContextCompat.getColor(context, R.color.journal_income_bg)
                val incomeText = ContextCompat.getColor(context, R.color.journal_income_text)
                val expenseBg = ContextCompat.getColor(context, R.color.journal_expense_bg)
                val expenseText = ContextCompat.getColor(context, R.color.journal_expense_text)
                val titleText = ContextCompat.getColor(context, R.color.journal_title_text)
                val subtleText = ContextCompat.getColor(context, R.color.journal_subtle_text)

                binding.cardListJournal.setCardBackgroundColor(defaultBg)
                binding.textDescription.setTextColor(titleText)
                binding.labelDate.setTextColor(subtleText)
                binding.textCreatedAt.setTextColor(subtleText)
                binding.labelCreatedAt.setTextColor(subtleText)

                val hasIncome = listOfNotNull(l1, l2).any {
                    it.accountType.equals("Income", true)
                }

                if (hasIncome) {
                    binding.labelAccountNameOne.text = AccountLocalizationUtil.localizeAccountType(context, "Expenses")
                    binding.labelAccountNameTwo.text = AccountLocalizationUtil.localizeAccountType(context, "Assets")
                    binding.labelAccountNameOne.backgroundTintList = ColorStateList.valueOf(expenseBg)
                    binding.labelAccountNameOne.setTextColor(expenseText)
                    binding.labelAccountNameTwo.backgroundTintList = ColorStateList.valueOf(incomeBg)
                    binding.labelAccountNameTwo.setTextColor(incomeText)
                } else {
                    binding.labelAccountNameOne.text = AccountLocalizationUtil.localizeAccountType(context, "Expenses")
                    binding.labelAccountNameTwo.text = AccountLocalizationUtil.localizeAccountType(context, "Assets")
                    binding.labelAccountNameOne.backgroundTintList = ColorStateList.valueOf(expenseBg)
                    binding.labelAccountNameOne.setTextColor(expenseText)
                    binding.labelAccountNameTwo.backgroundTintList = ColorStateList.valueOf(incomeBg)
                    binding.labelAccountNameTwo.setTextColor(incomeText)
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
