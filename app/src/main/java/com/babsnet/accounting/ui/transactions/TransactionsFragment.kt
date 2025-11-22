package com.babsnet.accounting.ui.transactions


//noinspection SuspiciousImport
import android.R
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
import com.babsnet.accounting.utils.observeOnce
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
        binding.inputAccountName.setOnClickListener {
            lifecycleScope.launch {
                val originalAccounts = withContext(Dispatchers.IO) {
                    accountViewModel.getListAccount("Expenses")
                }

                val allAccount = Account(
                    accountId = -1,
                    accountName = "ALL",
                    accountType = "Expenses"
                )
                val modifiedAccounts = listOf(allAccount) + originalAccounts

                Utils.showAccountSelectionDialog(
                    context = requireContext(),
                    lifecycleScope = viewLifecycleOwner.lifecycleScope,
                    accountViewModel = accountViewModel,
                    accountType = "Expenses",
                    preloadedAccounts = modifiedAccounts // lewatkan list dari sini
                ) { selectedAccount ->
                    binding.inputAccountName.setText(selectedAccount.accountName)
                    accountId = selectedAccount.accountId
                }
            }
        }

        val items = listOf("Tahunan", "Bulanan", "Mingguan")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriod.adapter = adapter

        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedPeriod = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedPeriod = null
            }
        }

        binding.buttonDownloadReport.setOnClickListener {
            Utils.showLoading(binding.progressBar)
            if (selectedPeriod == null) {
                Toast.makeText(requireContext(), "Please select data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val (startDate, endDate) = when (selectedPeriod) {
                "Mingguan" -> Utils.getStartAndEndOfCurrentWeekMillis()
                "Bulanan" -> Utils.getStartAndEndOfCurrentMonth()
                "Tahunan" -> Utils.getStartAndEndOfCurrentYear()
                else -> return@setOnClickListener
            }

            transactionsViewModel.getTransactionData(startDate, endDate, accountId)
                .observeOnce(viewLifecycleOwner) { transactions ->
                    if (transactions.isEmpty()) {
                        Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
                    } else {
                        Utils.createPdf(requireContext(), transactions)
                    }
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
