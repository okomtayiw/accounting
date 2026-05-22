package com.babsnet.accounting.ui.transactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.babsnet.accounting.R
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.JournalDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.data.entity.TransactionData
import com.babsnet.accounting.databinding.FragmentTransactionsBinding
import com.babsnet.accounting.databinding.ItemReportTopTransactionBinding
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.AccountLocalizationUtil
import com.babsnet.accounting.utils.CurrencyFormatUtil
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.AccountViewModel
import com.babsnet.accounting.viewModel.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionsViewModel: TransactionsViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var accountViewModel: AccountViewModel
    private var accountId: Int? = null
    private var selectedPeriod: String? = null
    private var reportObserver: Observer<List<TransactionData>>? = null
    private var reportSource: LiveData<List<TransactionData>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val repository = setupRepository()
        transactionsViewModel = setupViewModel(repository)

        val accountDao = AppDatabase.getDatabase(requireContext()).accountDao()
        val ledgerDao = AppDatabase.getDatabase(requireContext()).ledgerDao()
        val repositoryAccount = AccountRepository(accountDao, ledgerDao)
        accountViewModel = ViewModelProvider(
            this,
            GenericViewModelFactory(AccountViewModel::class.java) {
                AccountViewModel(repositoryAccount)
            }
        )[AccountViewModel::class.java]

        progressBar = binding.progressBar
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        binding.btnMenu.setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        binding.cardCategory.setOnClickListener {
            lifecycleScope.launch {
                val originalAccounts = withContext(Dispatchers.IO) {
                    accountViewModel.getListAccount("Expenses")
                }

                val allOptions = listOf(
                    Account(accountId = -1, accountName = "Ledger", accountType = "Report"),
                    Account(accountId = -2, accountName = "Laba Rugi", accountType = "Report"),
                    Account(accountId = -3, accountName = "Neraca", accountType = "Report")
                )

                val modifiedAccounts = allOptions + originalAccounts

                Utils.showAccountSelectionDialog(
                    context = requireContext(),
                    lifecycleScope = viewLifecycleOwner.lifecycleScope,
                    accountViewModel = accountViewModel,
                    accountType = "Expenses",
                    preloadedAccounts = modifiedAccounts
                ) { selectedAccount ->
                    binding.tvCategory.text =
                        AccountLocalizationUtil.localizeAccountName(requireContext(), selectedAccount)
                    accountId = selectedAccount.accountId
                }
            }
        }

        setupPeriodTabs()
        binding.tvCategory.text = getString(R.string.all_categories)
        accountId = null
        loadDefaultDashboard()

        binding.btnShow.setOnClickListener {
            Utils.showLoading(binding.progressBar)
            val period = selectedPeriod
            if (period == null) {
                Utils.hideLoading(binding.progressBar)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_select_period),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val (startDate, endDate) = when (period) {
                getString(R.string.mingguan) -> Utils.getStartAndEndOfCurrentWeekMillis()
                getString(R.string.bulanan) -> Utils.getStartAndEndOfCurrentMonth()
                getString(R.string.tahunan) -> Utils.getStartAndEndOfCurrentYear()
                else -> {
                    Utils.hideLoading(binding.progressBar)
                    return@setOnClickListener
                }
            }

            when (accountId) {
                -2 -> {
                    val intent = Intent(requireContext(), ActivityProfitLossPreview::class.java)
                    intent.putExtra("startDate", startDate)
                    intent.putExtra("endDate", endDate)
                    intent.putExtra("accountId", accountId)
                    startActivity(intent)
                    Utils.hideLoading(binding.progressBar)
                }

                -3 -> {
                    val intent = Intent(requireContext(), ActivityBalanceSheetPreview::class.java)
                    intent.putExtra("startDate", startDate)
                    intent.putExtra("endDate", endDate)
                    intent.putExtra("accountId", accountId)
                    startActivity(intent)
                    Utils.hideLoading(binding.progressBar)
                }

                -1 -> {
                    val intent = Intent(requireContext(), ActivityTransactionLedgerPreview::class.java)
                    intent.putExtra("startDate", startDate)
                    intent.putExtra("endDate", endDate)
                    intent.putExtra("accountId", accountId)
                    startActivity(intent)
                    Utils.hideLoading(binding.progressBar)
                }

                else -> observeDashboard(startDate, endDate, accountId)
            }
        }
        return root
    }

    private fun setupPeriodTabs() {
        selectedPeriod = getString(R.string.bulanan)
        updateSelectedTab(binding.tabMonthly)

        binding.tabWeekly.setOnClickListener {
            selectedPeriod = getString(R.string.mingguan)
            updateSelectedTab(binding.tabWeekly)
        }

        binding.tabMonthly.setOnClickListener {
            selectedPeriod = getString(R.string.bulanan)
            updateSelectedTab(binding.tabMonthly)
        }

        binding.tabYearly.setOnClickListener {
            selectedPeriod = getString(R.string.tahunan)
            updateSelectedTab(binding.tabYearly)
        }
    }

    private fun loadDefaultDashboard() {
        Utils.showLoading(binding.progressBar)
        val (startDate, endDate) = Utils.getStartAndEndOfCurrentMonth()
        observeDashboard(startDate, endDate, accountId)
    }

    private fun updateSelectedTab(selectedTab: TextView) {
        val tabs = listOf(binding.tabWeekly, binding.tabMonthly, binding.tabYearly)
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            tab.setBackgroundResource(
                if (isSelected) R.drawable.report_tab_active_bg
                else R.drawable.report_tab_inactive_bg
            )
            tab.setTextColor(
                resources.getColor(
                    if (isSelected) R.color.nav_active_text else R.color.text_secondary,
                    null
                )
            )
        }
    }

    private fun observeDashboard(startDate: Long, endDate: Long, selectedAccountId: Int?) {
        reportObserver?.let { observer ->
            reportSource?.removeObserver(observer)
        }

        val liveData = transactionsViewModel.getTransactionData(startDate, endDate, selectedAccountId)
        val observer = Observer<List<TransactionData>> { transactions ->
            renderDashboard(transactions)
            Utils.hideLoading(binding.progressBar)
        }

        reportSource = liveData
        reportObserver = observer
        liveData.observe(viewLifecycleOwner, observer)
    }

    private fun renderDashboard(transactions: List<TransactionData>) {
        binding.dashboardScroll.visibility = View.VISIBLE
        binding.tvOverviewPeriod.text = when (selectedPeriod) {
            getString(R.string.mingguan) -> getString(R.string.weekly)
            getString(R.string.bulanan) -> getString(R.string.monthly)
            getString(R.string.tahunan) -> getString(R.string.yearly)
            else -> getString(R.string.monthly)
        }
        binding.tvSelectedCategorySummary.text = binding.tvCategory.text

        if (transactions.isEmpty()) {
            binding.progressOverview.progress = 0
            binding.tvOverviewNet.text = CurrencyFormatUtil.zeroCurrency(requireContext())
            binding.tvOverviewHeadline.text = getString(R.string.report_spotlight_default_title)
            binding.tvOverviewSubheadline.text = getString(R.string.report_spotlight_default_subtitle)
            binding.tvIncomeMetric.text = getString(
                R.string.label_type_amount,
                AccountLocalizationUtil.localizeAccountType(requireContext(), "Income"),
                CurrencyFormatUtil.zeroCurrency(requireContext())
            )
            binding.tvExpenseMetric.text = getString(
                R.string.label_type_amount,
                AccountLocalizationUtil.localizeAccountType(requireContext(), "Expenses"),
                CurrencyFormatUtil.zeroCurrency(requireContext())
            )
            binding.tvTotalEntries.text = "0"
            binding.tvSpotlightTitle.text = getString(R.string.report_spotlight_default_title)
            binding.tvSpotlightSubtitle.text = getString(R.string.report_spotlight_default_subtitle)
            binding.tvSpotlightAmount.text = CurrencyFormatUtil.zeroCurrency(requireContext())
            binding.topTransactionsContainer.removeAllViews()
            binding.tvTopTransactionsEmpty.visibility = View.VISIBLE
            return
        }

        val income = transactions
            .filter { it.accountType == "Income" }
            .sumOf { it.credit - it.debit }
        val expenses = transactions
            .filter { it.accountType == "Expenses" }
            .sumOf { it.debit - it.credit }
        val net = income - expenses
        val totalFlow = income + expenses
        val expenseRatio =
            if (totalFlow > 0) ((expenses / totalFlow) * 100).toInt().coerceIn(0, 100) else 0

        binding.progressOverview.progress = expenseRatio
        binding.tvOverviewNet.text = formatSignedAmount(net)
        binding.tvOverviewHeadline.text = if (net >= 0) {
            getString(R.string.report_cashflow_positive)
        } else {
            getString(R.string.report_cashflow_negative)
        }
        binding.tvOverviewSubheadline.text = if (net >= 0) {
            getString(R.string.report_cashflow_positive_sub)
        } else {
            getString(R.string.report_cashflow_negative_sub)
        }
        binding.tvIncomeMetric.text = getString(
            R.string.label_type_amount,
            AccountLocalizationUtil.localizeAccountType(requireContext(), "Income"),
            formatSignedAmount(income)
        )
        binding.tvExpenseMetric.text = getString(
            R.string.label_type_amount,
            AccountLocalizationUtil.localizeAccountType(requireContext(), "Expenses"),
            formatSignedAmount(expenses)
        )
        binding.tvTotalEntries.text = transactions.size.toString()

        val grouped = transactions
            .groupBy { it.accountName to it.accountType }
            .map { (key, items) ->
                val (name, type) = key
                val amount = when (type) {
                    "Income" -> items.sumOf { it.credit - it.debit }
                    else -> items.sumOf { it.debit - it.credit }
                }
                ReportRow(
                    accountName = name,
                    accountType = type,
                    amount = amount,
                    entries = items.size
                )
            }
            .sortedByDescending { abs(it.amount) }

        val topRow = grouped.first()
        binding.tvSpotlightTitle.text =
            AccountLocalizationUtil.localizeAccountName(requireContext(), topRow.accountName)
        binding.tvSpotlightSubtitle.text = getString(
            R.string.report_spotlight_meta_clean,
            topRow.entries,
            AccountLocalizationUtil.localizeAccountType(requireContext(), topRow.accountType)
        )
        binding.tvSpotlightAmount.text = formatSignedAmount(topRow.amount)

        binding.topTransactionsContainer.removeAllViews()
        binding.tvTopTransactionsEmpty.visibility = if (grouped.isEmpty()) View.VISIBLE else View.GONE
        grouped.take(3).forEach { row ->
            val itemBinding = ItemReportTopTransactionBinding.inflate(
                layoutInflater,
                binding.topTransactionsContainer,
                false
            )
            val localizedName =
                AccountLocalizationUtil.localizeAccountName(requireContext(), row.accountName)
            itemBinding.tvTransactionBadge.text = localizedName.take(1).uppercase()
            itemBinding.tvTransactionTitle.text = localizedName
            itemBinding.tvTransactionMeta.text = getString(
                R.string.report_top_transaction_meta,
                row.entries,
                AccountLocalizationUtil.localizeAccountType(requireContext(), row.accountType)
            )
            itemBinding.tvTransactionAmount.text = formatSignedAmount(row.amount)
            if (row.accountType == "Income") {
                itemBinding.tvTransactionBadge.setTextColor(
                    resources.getColor(R.color.journal_income_text, null)
                )
                itemBinding.tvTransactionBadge.setBackgroundResource(
                    R.drawable.journal_form_tab_unselected
                )
            } else if (row.accountType == "Expenses") {
                itemBinding.tvTransactionBadge.setTextColor(
                    resources.getColor(R.color.journal_expense_text, null)
                )
                itemBinding.tvTransactionBadge.setBackgroundResource(
                    R.drawable.category_selected_icon_badge_bg
                )
            }
            binding.topTransactionsContainer.addView(itemBinding.root)
        }
    }

    private fun formatSignedAmount(value: Double): String {
        return CurrencyFormatUtil.formatSignedCurrency(requireContext(), value)
    }

    private data class ReportRow(
        val accountName: String,
        val accountType: String,
        val amount: Double,
        val entries: Int
    )

    private fun setupViewModel(repository: JournalRepository): TransactionsViewModel {
        val transactionsViewModelFactory = GenericViewModelFactory(
            TransactionsViewModel::class.java
        ) { TransactionsViewModel(repository) }
        return ViewModelProvider(this, transactionsViewModelFactory)[TransactionsViewModel::class.java]
    }

    private fun setupRepository(): JournalRepository {
        val dao: JournalDao = AppDatabase.getDatabase(requireContext()).journalDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(requireContext()).ledgerDao()
        return JournalRepository(dao, ledgerDao)
    }

    override fun onDestroyView() {
        reportObserver?.let { observer ->
            reportSource?.removeObserver(observer)
        }
        reportObserver = null
        reportSource = null
        super.onDestroyView()
        _binding = null
    }
}
