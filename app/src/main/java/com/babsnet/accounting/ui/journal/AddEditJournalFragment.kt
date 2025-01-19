package com.babsnet.accounting.ui.journal

import android.os.Bundle
import android.text.Editable
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.AccountAdapterDialog
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.data.entity.Journal
import com.babsnet.accounting.databinding.FragmentAddEditJournalBinding
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.DateUtil.dateToString
import com.babsnet.accounting.viewModel.JournalViewModel
import com.babsnet.accounting.utils.GenericViewModelFactory
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditJournalBinding.inflate(inflater, container, false)
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
                    binding.inputDescription.text = Editable.Factory.getInstance()
                        .newEditable(journalWithDetails.journal.description ?: "")

                    binding.accountNameOne.text = journalWithDetails.ledgers.getOrNull(0)?.accountName ?: ""
                    binding.accountNameTwo.text = journalWithDetails.ledgers.getOrNull(1)?.accountName ?: ""

                    val total = journalWithDetails.ledgers.getOrNull(0)?.ledgerCredit?.takeIf { it > 0.0 }
                        ?: journalWithDetails.ledgers.getOrNull(0)?.ledgerDebit ?: 0.0

                    binding.inputTotalAmount.text = Editable.Factory.getInstance().newEditable(total.toString())
                    accountIdOne = journalWithDetails.ledgers[0].accountId
                    accountIdTwo = journalWithDetails.ledgers[1].accountId
                    existAccountIdOne = journalWithDetails.ledgers[0].accountId
                    existAccountIdTwo = journalWithDetails.ledgers[1].accountId


                    binding.inputDate.text = Editable.Factory.getInstance().newEditable(
                        journalWithDetails.journal.date?.let { dateToString(it) }
                    )
                    journalExisting = journalWithDetails.journal
                }
            }
        }


        binding.inputDate.setOnClickListener {
            DateUtil.showDatePicker(requireActivity(), binding.inputDate)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.accountNameOne.setOnClickListener {
            showAccountSelectionDialog("Assets") { selectedAccount ->
                binding.accountNameOne.text = selectedAccount.accountName
                accountIdOne = selectedAccount.accountId
            }
        }

        binding.accountNameTwo.setOnClickListener {
            showAccountSelectionDialog("Expenses") { selectedAccount ->
                binding.accountNameTwo.text = selectedAccount.accountName
                accountIdTwo = selectedAccount.accountId
            }
        }

        saveButtonClick()
    }

    private fun saveButtonClick() {
        binding.btnSave.setOnClickListener {
            val description = binding.inputDescription.text.toString()
            val total = binding.inputTotalAmount.text.toString().toDoubleOrNull() ?: 0.0
            val date = binding.inputDate.text.toString()


            if (description.isEmpty() || date.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
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
                            var existingJournalUpdateSuccess: Journal? = null
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

    private fun showAccountSelectionDialog(accountType: String, onAccountSelected: (Account) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_account, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewAccounts)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                if (!isAdded || requireActivity().isFinishing) return@launch

                val accounts = withContext(Dispatchers.IO) {
                    accountViewModel.getListAccount(accountType)
                }

                withContext(Dispatchers.Main.immediate) {
                    if (!isAdded || requireActivity().isFinishing) return@withContext
                    recyclerView.adapter = AccountAdapterDialog(accounts) { account ->
                        onAccountSelected(account)
                        dialog.dismiss()
                    }
                    dialog.show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewLifecycleOwner.lifecycleScope.coroutineContext.cancelChildren()
    }
}
