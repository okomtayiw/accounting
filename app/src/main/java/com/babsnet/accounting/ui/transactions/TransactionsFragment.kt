package com.babsnet.accounting.ui.transactions

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.babsnet.accounting.databinding.FragmentTransactionsBinding
import com.babsnet.accounting.data.entity.TransactionData
import android.view.Gravity
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.JournalDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.GenericViewModelFactory

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionsViewModel: TransactionsViewModel
    private lateinit var tableLayout: TableLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val repository = setupRepository()
        transactionsViewModel = setupViewModel(repository)
        transactionsViewModel = ViewModelProvider(this)[TransactionsViewModel::class.java]
        tableLayout = binding.tableLayout
        progressBar = binding.progressBar


        loadTransactionData()

        return root
    }

    private fun setupViewModel(repository: JournalRepository): TransactionsViewModel {
        val transactionsViewModelFactory = GenericViewModelFactory(
            TransactionsViewModel::class.java
        ) { TransactionsViewModel(repository) }
        return ViewModelProvider(this, transactionsViewModelFactory)[TransactionsViewModel::class.java]
    }

    private fun loadTransactionData() {
        progressBar.visibility = View.VISIBLE

        transactionsViewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            progressBar.visibility = View.GONE
            populateTransactionTable(transactions ?: emptyList())
        }
    }

    @SuppressLint("DefaultLocale")
    private fun populateTransactionTable(transactions: List<TransactionData>) {
        tableLayout.removeViews(1, tableLayout.childCount - 1)
        var totalDebit = 0.0
        var totalCredit = 0.0
        for (transaction in transactions) {
            val row = TableRow(requireContext())
            row.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            row.addView(createTextView(DateUtil.dateToString(transaction.journalDate)))
            row.addView(createTextView(transaction.accountName))
            row.addView(createTextView(transaction.description))
            row.addView(createTextView(transaction.debit.toString()))
            row.addView(createTextView(transaction.credit.toString()))

            tableLayout.addView(row)

            totalDebit += transaction.debit
            totalCredit += transaction.credit
        }

        binding.totalDebit.text = String.format("%.2f", totalDebit)
        binding.totalCredit.text = String.format("%.2f", totalCredit)
    }

    private fun createTextView(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            this.textSize = 14f
            this.setPadding(8, 8, 8, 8)
            this.gravity = Gravity.CENTER
            this.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun setupRepository(): JournalRepository {
        val dao: JournalDao = AppDatabase.getDatabase(requireContext()).journalDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(requireContext()).ledgerDao()
        return JournalRepository(dao, ledgerDao)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
