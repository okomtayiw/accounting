package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.data.entity.JournalWithDetails
import com.babsnet.accounting.databinding.ItemJournalBinding
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.Utils.showDeleteConfirmationDialog

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

            binding.textAccountNameOne.text = "${journalWithDetails.ledgers[0].accountName} (${valueAmountOne})"
            binding.textAccountNameTwo.text =  "${journalWithDetails.ledgers[1].accountName} (${valueAmountTwo})"

            val context = binding.root.context

            // Handle delete action
            binding.btnDeleteJournal.setOnClickListener {
                showDeleteConfirmationDialog(context) {
                    onDeleteJournal(journalWithDetails)
                }
            }

            // Handle edit action (item click or specific button click)
            binding.btnEditJournal.setOnClickListener {
                onEditJournal(journalWithDetails) // Trigger the onEditJournal callback
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
