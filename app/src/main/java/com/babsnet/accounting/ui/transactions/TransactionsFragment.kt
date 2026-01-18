package com.babsnet.accounting.ui.transactions

//noinspection SuspiciousImport
import android.R
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.babsnet.accounting.databinding.FragmentTransactionsBinding
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.JournalDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.AccountViewModel
import com.babsnet.accounting.viewModel.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionsViewModel: TransactionsViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var accountViewModel: AccountViewModel
    private var accountId: Int? = null
    private var selectedPeriod: String? = null


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

        val accountDao = AppDatabase.getDatabase(requireContext()).accountDao()
        val ledgerDao = AppDatabase.getDatabase(requireContext()).ledgerDao()
        val repositoryAccount = AccountRepository(accountDao, ledgerDao)
        accountViewModel = ViewModelProvider(
            this, GenericViewModelFactory(AccountViewModel::class.java) {
                AccountViewModel(repositoryAccount)
            }
        )[AccountViewModel::class.java]

        progressBar = binding.progressBar
        binding.icCategory.setOnClickListener {
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
                    binding.tvCategory.text = selectedAccount.accountName
                    accountId = selectedAccount.accountId
                }
            }
        }

        val items = listOf("Tahunan", "Bulanan", "Mingguan")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter

        binding.spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedPeriod = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedPeriod = null
            }
        }

        binding.btnShow.setOnClickListener {
            Utils.showLoading(binding.progressBar)
            if (selectedPeriod == null) {
                Toast.makeText(requireContext(), "Please select data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val (startDate, endDate) = when (selectedPeriod) {
                getString(com.babsnet.accounting.R.string.mingguan) -> Utils.getStartAndEndOfCurrentWeekMillis()
                getString(com.babsnet.accounting.R.string.bulanan) -> Utils.getStartAndEndOfCurrentMonth()
                getString(com.babsnet.accounting.R.string.tahunan) -> Utils.getStartAndEndOfCurrentYear()
                else -> return@setOnClickListener
            }


            if (accountId != null) {
                if(accountId ==  -2) {
                    val intent = Intent(requireContext(), ActivityProfitLossPreview::class.java)
                    intent.putExtra("startDate", startDate)
                    intent.putExtra("endDate", endDate)
                    intent.putExtra("accountId", accountId)
                    startActivity(intent)
                } else if(accountId == -3) {
                    val intent = Intent(requireContext(), ActivityBalanceSheetPreview::class.java)
                    intent.putExtra("startDate", startDate)
                    intent.putExtra("endDate", endDate)
                    intent.putExtra("accountId", accountId)
                    startActivity(intent)
                }else if(accountId == -1){
                    val intent = Intent(requireContext(), ActivityTransactionLedgerPreview::class.java)
                    intent.putExtra("startDate", startDate)
                    intent.putExtra("endDate", endDate)
                    intent.putExtra("accountId", accountId)
                    startActivity(intent)
                } else {
                    val intent = Intent(requireContext(), ActivityCategoryPreview::class.java)
                    intent.putExtra("startDate", startDate)
                    intent.putExtra("endDate", endDate)
                    intent.putExtra("accountId", accountId)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(requireContext(), "Please select category", Toast.LENGTH_SHORT).show()
            }

            Utils.hideLoading(binding.progressBar)
        }
        return root
    }

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
        super.onDestroyView()
        _binding = null
    }
}
