package com.babsnet.accounting.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.data.entity.Journal
import com.babsnet.accounting.databinding.ItemJournalBinding
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.Utils.showDeleteConfirmationDialog

class JournalListAdapter(
    private val onDeleteJournal: (Journal) -> Unit,
    private val onEditJournal: (Journal) -> Unit // Add callback for editing
) : ListAdapter<Journal, JournalListAdapter.JournalViewHolder>(JournalComparator()) {

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
        private val onDeleteJournal: (Journal) -> Unit,
        private val onEditJournal: (Journal) -> Unit, // Add onEditJournal as a parameter
        private val binding: ItemJournalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(journal: Journal) {
            binding.textDate.text = DateUtil.formatDate(journal.date.toString())
            binding.textDescription.text = journal.description
            binding.textCreatedAt.text = DateUtil.formatDate(journal.createdAt.toString())
            val context = binding.root.context

            // Handle delete action
            binding.btnDeleteJournal.setOnClickListener {
                showDeleteConfirmationDialog(context) {
                    onDeleteJournal(journal)
                }
            }

            // Handle edit action (item click or specific button click)
            binding.btnEditJournal.setOnClickListener {
                onEditJournal(journal) // Trigger the onEditJournal callback
            }
        }
    }


    class JournalComparator : DiffUtil.ItemCallback<Journal>() {
        override fun areItemsTheSame(oldItem: Journal, newItem: Journal): Boolean {
            return oldItem.journalId == newItem.journalId
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Journal, newItem: Journal): Boolean {
            return oldItem == newItem
        }
    }
}