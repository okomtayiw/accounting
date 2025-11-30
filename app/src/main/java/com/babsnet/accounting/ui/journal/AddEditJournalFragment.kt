package com.babsnet.accounting.ui.journal

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.CategoryAccountAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.entity.Journal
import com.babsnet.accounting.databinding.FragmentAddEditJournalBinding
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.DateUtil.dateToString
import com.babsnet.accounting.viewModel.JournalViewModel
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.AccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class AddEditJournalFragment : Fragment() {

    private var _binding: FragmentAddEditJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalViewModel: JournalViewModel
    private lateinit var accountViewModel: AccountViewModel
    private var journalId: Int? = null
    private var accountIdOne: Int? = null
    private var accountIdTwo: Int? = null
    private var journalExisting : Journal? = null
    private var  existAccountIdOne : Int? =null
    private var  existAccountIdTwo : Int? =null
    private var tabExpenditure: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditJournalBinding.inflate(inflater, container, false)
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val application = requireActivity().application
        val journalDao = AppDatabase.getDatabase(application).journalDao()
        val accountDao = AppDatabase.getDatabase(application).accountDao()
        val ledgerDao = AppDatabase.getDatabase(application).ledgerDao()
        val repository = JournalRepository(journalDao, ledgerDao)
        val repositoryAccount = AccountRepository(accountDao, ledgerDao)

        binding.tvTime.text = dateToString(Date())
        journalViewModel = ViewModelProvider(
            this, GenericViewModelFactory(JournalViewModel::class.java) {
                JournalViewModel(repository)
            }
        )[JournalViewModel::class.java]

        accountViewModel = ViewModelProvider(
            this, GenericViewModelFactory(AccountViewModel::class.java) {
                AccountViewModel(repositoryAccount)
            }
        )[AccountViewModel::class.java]

        journalId = arguments?.getInt("journalId", -1)?.takeIf { it != -1 }
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        journalId?.let { it ->
            journalViewModel.getJournalWithDetails(it).observe(viewLifecycleOwner) { journalWithDetails ->
                if (journalWithDetails != null) {
                    binding.etNote.text = Editable.Factory.getInstance()
                        .newEditable(journalWithDetails.journal.description ?: "")


                    val total = (journalWithDetails.ledgers.getOrNull(0)?.ledgerCredit?.takeIf { it > 0.0 }
                        ?: journalWithDetails.ledgers.getOrNull(0)?.ledgerDebit ?: 0.0).toLong()

                    binding.etAmount.text = Editable.Factory.getInstance().newEditable(total.toString())
                    journalWithDetails.ledgers.forEach { ledger ->
                        if (ledger.accountType == "Assets") {
                            accountIdOne = ledger.accountId
                            existAccountIdOne = ledger.accountId
                        } else {
                            accountIdTwo = ledger.accountId
                            existAccountIdTwo = ledger.accountId
                        }
                    }

                    var isIncomeFound = false

                    for (ledger in journalWithDetails.ledgers) {
                        if (ledger.accountType == "Income") {
                            isIncomeFound = true
                            break
                        }
                    }

                    if (isIncomeFound) {
                        tabExpenditure = false
                        selectTab(false)
                        loadAccountCategories(
                            accountViewModel = accountViewModel,
                            accountType = "Income"
                        )
                    }

                    binding.accountNameOne.text = journalWithDetails.ledgers[0].accountName

                    binding.tvTime.text = Editable.Factory.getInstance().newEditable(
                        journalWithDetails.journal.date?.let { dateToString(it) }
                    )
                    journalExisting = journalWithDetails.journal
                }
            }
        }


        binding.inputDate.setOnClickListener {
            DateUtil.showDatePicker(requireActivity(), binding.tvTime)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.accountNameOne.setOnClickListener {
            Utils.showAccountSelectionDialog(
                context = requireContext(),
                lifecycleScope = viewLifecycleOwner.lifecycleScope,
                accountViewModel = accountViewModel,
                accountType = "Assets"
            ) { selectedAccount ->
                binding.accountNameOne.text = selectedAccount.accountName
                accountIdOne = selectedAccount.accountId
            }
        }



        loadAccountCategories(
            accountViewModel = accountViewModel,
            accountType = "Expenses"
        )

        saveButtonClick()
        selectTab(tabExpenditure)
        binding.tabExpenditure.setOnClickListener {
            selectTab(true)
            tabExpenditure = true
            loadAccountCategories(
                accountViewModel = accountViewModel,
                accountType = "Expenses"
            )
        }
        binding.tabRevenue.setOnClickListener {
            tabExpenditure = false
            selectTab(false)
            loadAccountCategories(
                accountViewModel = accountViewModel,
                accountType = "Income"
            )
        }
    }


    private fun selectTab(isExpenditure: Boolean) {

        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.tab_text_selected)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.tab_text_unselected)

        if (isExpenditure) {

            binding.tabExpenditure.setBackgroundResource(R.drawable.tab_selected_bg)
            binding.tabExpenditure.setTextColor(selectedTextColor)

            binding.tabRevenue.setBackgroundResource(R.drawable.tab_unselected_bg)
            binding.tabRevenue.setTextColor(unselectedTextColor)

        } else {

            binding.tabRevenue.setBackgroundResource(R.drawable.tab_selected_bg)
            binding.tabRevenue.setTextColor(selectedTextColor)

            binding.tabExpenditure.setBackgroundResource(R.drawable.tab_unselected_bg)
            binding.tabExpenditure.setTextColor(unselectedTextColor)
        }
    }



    private fun saveButtonClick() {
        binding.btnSave.setOnClickListener {
            val description = binding.etNote.text.toString()
            val total = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val date = binding.tvTime.text.toString()


            if (description.isEmpty() || date.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Utils.showLoading(binding.progressBar)
            lifecycleScope.launch {
                if (accountIdTwo == null) {
                    Toast.makeText(requireContext(), "Category must be selected", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    withContext(Dispatchers.IO) {
                        val debit = 0.0
                        val credit = 0.0
                        var newJournal: Journal? = null
                        var existJournal: Journal? = null
                        val accountOne = accountViewModel.getAccountByIdAsync(accountIdOne!!)
                        val accountTwo = accountViewModel.getAccountByIdAsync(accountIdTwo!!)
                        if (journalId == null) {
                            newJournal = Journal(
                                date = DateUtil.stringToDate(date),
                                description = description,
                                createdAt = Date(),
                                createdBy = "User"
                            )
                            if(tabExpenditure) {
                                if (accountOne?.accountType  == "Assets") {
                                    existJournal = journalViewModel.saveJournalLedger(
                                        newJournal,
                                        debit,
                                        total,
                                        accountOne)
                                }

                                if (accountTwo?.accountType == "Expenses" && existJournal != null ) {
                                    journalViewModel.saveJournalLedger(
                                        existJournal,
                                        total,
                                        credit,
                                        accountTwo)
                                }
                            } else {
                                if (accountOne?.accountType  == "Assets") {
                                    existJournal = journalViewModel.saveJournalLedger(
                                        newJournal,
                                        total,
                                        credit,
                                        accountOne)
                                }

                                if (accountTwo?.accountType == "Income" && existJournal != null ) {
                                    journalViewModel.saveJournalLedger(
                                        existJournal,
                                        debit,
                                        total,
                                        accountTwo)
                                }
                            }

                        } else {
                            var existingJournalUpdateSuccess: Journal? = null
                            if(tabExpenditure){
                                if (accountOne?.accountType  == "Assets") {
                                    journalExisting?.updatedBy = "User"
                                    journalExisting?.description = description
                                    journalExisting?.updatedAt = Date()
                                    journalExisting?.date = DateUtil.stringToDate(date)
                                    existingJournalUpdateSuccess = journalViewModel.updateJournalLedger(
                                        journalExisting!!,
                                        debit,
                                        total,
                                        accountOne, existAccountIdOne)
                                }
                                if (accountTwo?.accountType == "Expenses" && existingJournalUpdateSuccess != null ) {
                                    journalViewModel.updateJournalLedger(
                                        existingJournalUpdateSuccess,
                                        total,
                                        credit,
                                        accountTwo,
                                        existAccountIdTwo)
                                }
                            } else {
                                if (accountOne?.accountType  == "Assets") {
                                    journalExisting?.updatedBy = "User"
                                    journalExisting?.description = description
                                    journalExisting?.updatedAt = Date()
                                    journalExisting?.date = DateUtil.stringToDate(date)
                                    existingJournalUpdateSuccess = journalViewModel.updateJournalLedger(
                                        journalExisting!!,
                                        total,
                                        credit,
                                        accountOne, existAccountIdOne)
                                }
                                if (accountTwo?.accountType == "Income" && existingJournalUpdateSuccess != null ) {
                                    journalViewModel.updateJournalLedger(
                                        existingJournalUpdateSuccess,
                                        debit,
                                        total,
                                        accountTwo,
                                        existAccountIdTwo)
                                }
                            }

                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Journal saved successfully", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                Utils.hideLoading(binding.progressBar)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadAccountCategories(
        accountViewModel: AccountViewModel,
        accountType: String
    ) {
        val rv = requireView().findViewById<RecyclerView>(R.id.rvCategory)
        Log.d("CATEGORY", "rv = $rv")
        rv.layoutManager = GridLayoutManager(requireContext(), 3)

        viewLifecycleOwner.lifecycleScope.launch {
            val accounts = withContext(Dispatchers.IO) {
                accountViewModel.getListAccount(accountType)
            }


            val adapter = CategoryAccountAdapter(accounts) {
                accountIdTwo = it.accountId
            }
            rv.adapter = adapter

            if (journalId != null) {
                adapter.setSelectedAccountId(existAccountIdTwo)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
        _binding = null
        viewLifecycleOwner.lifecycleScope.coroutineContext.cancelChildren()
    }
}
