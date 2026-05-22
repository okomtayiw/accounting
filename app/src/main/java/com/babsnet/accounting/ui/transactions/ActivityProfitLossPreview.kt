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
import com.babsnet.accounting.databinding.ActivityProfitLossPreviewBinding
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.AccountLocalizationUtil
import com.babsnet.accounting.utils.CurrencyFormatUtil
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.LanguagePreference
import com.babsnet.accounting.utils.SystemBarsHelper
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.TransactionsViewModel

class ActivityProfitLossPreview : AppCompatActivity() {

    private lateinit var binding: ActivityProfitLossPreviewBinding
    private lateinit var viewModel: TransactionsViewModel
    private var currentTx: List<TransactionData> = emptyList()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguagePreference.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfitLossPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarsHelper.applySystemBarsPadding(this, binding.root)

        val formatNumber: (Double) -> String = { value ->
            CurrencyFormatUtil.formatCurrency(this, value)
        }

        // RecyclerView
        binding.rvLines.layoutManager = LinearLayoutManager(this)
        val adapter = BalanceLineAdapter(formatNumber)
        binding.rvLines.adapter = adapter

        binding.btnClose.setOnClickListener { finish() }

        binding.btnGeneratePdf.setOnClickListener {
            if (currentTx.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_empty_report_data), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Utils.createPdfProfitAndLoss(this, currentTx)
        }

        // ViewModel (PENTING: jangan buat ViewModelProvider di property class)
        val repository = setupRepository()
        viewModel = setupViewModel(repository)

        // Ambil parameter dari Intent
        val startDate = intent.getLongExtra("startDate", 0L)
        val endDate = intent.getLongExtra("endDate", 0L)

        // Kalau accountId negatif (-2/-3), anggap null (ambil semua)
        val rawAccountId = if (intent.hasExtra("accountId")) intent.getIntExtra("accountId", -1) else -1
        val accountId: Int? = rawAccountId.takeIf { it >= 0 }

        // Load & observe data
        viewModel.getTransactionData(startDate, endDate, accountId).observe(this) { tx ->
            currentTx = tx

            // sesuai PDF
            val revenue = tx.filter { it.accountType == "Income" }.sumOf { it.credit - it.debit }
            val expenses = tx.filter { it.accountType == "Expenses" }.sumOf { it.debit - it.credit }
            val net = revenue - expenses

            binding.tvRevenue.text = getString(
                R.string.label_total_type,
                AccountLocalizationUtil.localizeAccountType(this, "Income"),
                formatNumber(revenue)
            )
            binding.tvExpenses.text = getString(
                R.string.label_total_type,
                AccountLocalizationUtil.localizeAccountType(this, "Expenses"),
                formatNumber(expenses)
            )
            binding.tvNet.text = getString(
                R.string.label_net_profit_loss,
                CurrencyFormatUtil.formatSignedCurrency(this, net)
            )

            // Breakdown per accountName (Income & Expenses)
            val lines = tx
                .filter { it.accountType == "Income" || it.accountType == "Expenses" }
                .groupBy { it.accountType to it.accountName }
                .map { (key, list) ->
                    val (type, name) = key
                    val amount = when (type) {
                        "Income" -> list.sumOf { it.credit - it.debit }
                        "Expenses" -> list.sumOf { it.debit - it.credit }
                        else -> 0.0
                    }
                    BalanceLine(type, name, amount)
                }
                // urutkan Income dulu baru Expenses
                .sortedWith(compareBy<BalanceLine>(
                    { if (it.accountType == "Income") 0 else 1 },
                    { it.title }
                ))

            adapter.submit(lines)
        }
    }

    private fun setupViewModel(repository: JournalRepository): TransactionsViewModel {
        val factory = GenericViewModelFactory(TransactionsViewModel::class.java) {
            TransactionsViewModel(repository)
        }
        return ViewModelProvider(this, factory)[TransactionsViewModel::class.java]
    }

    private fun setupRepository(): JournalRepository {
        val dao: JournalDao = AppDatabase.getDatabase(this).journalDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(this).ledgerDao()
        return JournalRepository(dao, ledgerDao)
    }
}
