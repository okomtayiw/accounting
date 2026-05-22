package com.babsnet.accounting.ui.transactions

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.BalanceLineAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.BalanceLine
import com.babsnet.accounting.data.dao.JournalDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.TransactionData
import com.babsnet.accounting.databinding.ActivityBalanceSheetPreviewBinding
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.AccountLocalizationUtil
import com.babsnet.accounting.utils.CurrencyFormatUtil
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.LanguagePreference
import com.babsnet.accounting.utils.SystemBarsHelper
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.TransactionsViewModel

class ActivityBalanceSheetPreview : AppCompatActivity() {

    private lateinit var binding: ActivityBalanceSheetPreviewBinding
    private lateinit var viewModel: TransactionsViewModel
    private var currentTx: List<TransactionData> = emptyList()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguagePreference.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBalanceSheetPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarsHelper.applySystemBarsPadding(this, binding.root)

        binding.rvLines.layoutManager = LinearLayoutManager(this)
        val formatNumber: (Double) -> String = { value ->
            CurrencyFormatUtil.formatCurrency(this, value)
        }
        val signedAmount: (Double) -> String = { value ->
            CurrencyFormatUtil.formatSignedCurrency(this, value)
        }
        val adapter = BalanceLineAdapter(formatNumber)
        binding.rvLines.adapter = adapter


        binding.btnClose.setOnClickListener { finish() }
        binding.btnGeneratePdf.setOnClickListener {
            if (currentTx.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_empty_report_data), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Utils.createPdfBalanceSheet(this, currentTx)
        }


        val repository = setupRepository()
        viewModel = setupViewModel(repository)

        val startDate = intent.getLongExtra("startDate", 0L)
        val endDate = intent.getLongExtra("endDate", 0L)
        val accountId = if (intent.hasExtra("accountId")) {
            intent.getIntExtra("accountId", 0)
        } else null


        viewModel.getTransactionData(startDate, endDate, accountId).observe(this) { tx ->
            currentTx = tx

            val totalAssets = tx.filter { it.accountType == "Assets" }.sumOf { it.debit - it.credit }
            val totalIncome = tx.filter { it.accountType == "Income" }.sumOf { it.credit - it.debit }
            val totalExpenses = tx.filter { it.accountType == "Expenses" }.sumOf { it.debit - it.credit }
            val equity = totalIncome - totalExpenses

            binding.tvAssets.text = getString(
                R.string.label_type_amount,
                AccountLocalizationUtil.localizeAccountType(this, "Assets"),
                signedAmount(totalAssets)
            )
            binding.tvNetIncome.text = getString(R.string.label_net_income_loss, signedAmount(equity))
            binding.tvExpenses.text = getString(
                R.string.label_total_type,
                AccountLocalizationUtil.localizeAccountType(this, "Expenses"),
                signedAmount(totalExpenses)
            )
            binding.tvTotalBalance.text = getString(
                R.string.label_total_balance,
                AccountLocalizationUtil.localizeAccountType(this, "Assets"),
                signedAmount(totalAssets)
            )

            val lines = tx.groupBy { it.accountType to it.accountName }
                .map { (key, list) ->
                    val (type, name) = key
                    val amount = when (type) {
                        "Assets" -> list.sumOf { it.debit - it.credit }
                        "Income" -> list.sumOf { it.credit - it.debit }
                        "Expenses" -> list.sumOf { it.debit - it.credit }
                        else -> 0.0
                    }
                    BalanceLine(type, name, amount)
                }
                .sortedWith(compareBy({ it.accountType }, { it.title }))
            adapter.submit(lines)
        }
    }

    private fun setupViewModel(repository: JournalRepository): TransactionsViewModel {
        val factory = GenericViewModelFactory(
            TransactionsViewModel::class.java
        ) { TransactionsViewModel(repository) }
        return ViewModelProvider(this, factory)[TransactionsViewModel::class.java]
    }

    private fun setupRepository(): JournalRepository {
        val dao: JournalDao = AppDatabase.getDatabase(this).journalDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(this).ledgerDao()
        return JournalRepository(dao, ledgerDao)
    }
}
