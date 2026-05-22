package com.babsnet.accounting.ui.transactions

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.TransactionCategoryAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.JournalDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.TransactionData
import com.babsnet.accounting.databinding.ActivityCategoryPreviewBinding
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.CurrencyFormatUtil
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.LanguagePreference
import com.babsnet.accounting.utils.SystemBarsHelper
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.TransactionsViewModel

class ActivityCategoryPreview : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryPreviewBinding
    private lateinit var viewModel: TransactionsViewModel
    private lateinit var adapter: TransactionCategoryAdapter
    private var currentTx: List<TransactionData> = emptyList()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguagePreference.wrapContext(newBase))
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityCategoryPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarsHelper.applySystemBarsPadding(this, binding.root)


        binding.hScroll.post { binding.hScroll.scrollTo(0, 0) }

        // --- RecyclerView setup ---
        binding.rvLedger.layoutManager = LinearLayoutManager(this)
        adapter = TransactionCategoryAdapter()
        binding.rvLedger.adapter = adapter

        binding.btnClose.setOnClickListener { finish() }

        binding.btnGeneratePdf.setOnClickListener {
            if (currentTx.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_empty_transactions), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Utils.createLedgerPdfOneCategory(this, currentTx)
        }


        // --- ViewModel setup ---
        val repository = setupRepository()
        viewModel = setupViewModel(repository)

        val startDate = intent.getLongExtra("startDate", 0L)
        val endDate = intent.getLongExtra("endDate", 0L)
        val accountId = if (intent.hasExtra("accountId")) intent.getIntExtra("accountId", 0) else null

        viewModel.getTransactionData(startDate, endDate, accountId).observe(this) { tx ->
            currentTx = tx
            adapter.submitData(tx)

            val totalDebit = tx.sumOf { it.debit }
            binding.tvTotalDebit.text = getString(
                R.string.label_total_amount,
                CurrencyFormatUtil.formatCurrency(this, totalDebit)
            )
        }
    }

    private fun setupRepository(): JournalRepository {
        val dao: JournalDao = AppDatabase.getDatabase(this).journalDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(this).ledgerDao()
        return JournalRepository(dao, ledgerDao)
    }

    private fun setupViewModel(repository: JournalRepository): TransactionsViewModel {
        val factory = GenericViewModelFactory(TransactionsViewModel::class.java) {
            TransactionsViewModel(repository)
        }
        return ViewModelProvider(this, factory)[TransactionsViewModel::class.java]
    }
}
