package com.babsnet.accounting.ui.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.babsnet.accounting.R
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.databinding.FragmentAccountInputBinding
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.AccountViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Date

class AccountInputFragment : Fragment() {

    private var _binding: FragmentAccountInputBinding? = null
    private val binding get() = _binding!!
    private lateinit var accountViewModel: AccountViewModel
    private var currentAccount: Account? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountInputBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        val dao = AppDatabase.getDatabase(requireContext()).accountDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(requireContext()).ledgerDao()
        val repository = AccountRepository(dao, ledgerDao)
        val factory = GenericViewModelFactory(AccountViewModel::class.java) {
            AccountViewModel(repository)
        }
        accountViewModel = ViewModelProvider(this, factory)[AccountViewModel::class.java]

        // Setup Menu Provider for the back button
        setupMenu()

        // Setup UI
        setupUI()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.GONE
        return root
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        findNavController().navigateUp()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupUI() {

        binding.editTextBalance.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.editTextBalance.text.toString() == "") {
                binding.editTextBalance.setText("")
            } else if (!hasFocus && binding.editTextBalance.text.isNullOrBlank()) {
                binding.editTextBalance.setText("")
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp() // Navigasi ke fragment sebelumnya
        }
        // Populate spinner
        val accountTypes = resources.getStringArray(R.array.account_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountTypes)

        binding.autoCompleteTypeAccount.setAdapter(adapter)
        binding.autoCompleteTypeAccount.setOnItemClickListener { _, _, _, _ ->
            binding.autoCompleteTypeAccountLayout.hint = null
        }

        binding.autoCompleteTypeAccount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    binding.autoCompleteTypeAccountLayout.hint = "Account Type"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Set default value for Balance
        binding.editTextBalance.setText("") // Set default value here

        // Check if editing an account
        val accountId = arguments?.getInt("accountId")
        if (accountId != null && accountId != -1) { // Ensure valid accountId
            binding.autoCompleteTypeAccountLayout.hint = null
            accountViewModel.getAccountById(accountId).observe(viewLifecycleOwner) { account ->
                currentAccount = account
                if (account?.accountName != null) {
                    binding.editTextAccountName.setText(account.accountName)
                } else {
                    binding.editTextAccountName.setText("")
                }

                if (account?.balance != null && account.balance != 0.0) {
                    binding.editTextBalance.setText(account.balance.toString())
                } else {
                    binding.editTextBalance.setText("");
                }

                binding.autoCompleteTypeAccount.setText(account?.accountType, false)
            }
        }

        // Save button click listener
        binding.buttonSave.setOnClickListener {
            saveAccount()
        }
    }

    private fun saveAccount() {
        val name = binding.editTextAccountName.text.toString()
        val type = binding.autoCompleteTypeAccount.text.toString()
        val balance = binding.editTextBalance.text.toString().toDoubleOrNull() ?: 0.0

        // Validate account name
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Account Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        Utils.showLoading(binding.progressBar)
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            val now = Date()
            val account = currentAccount?.copy(
                accountName = name,
                accountType = type,
                balance = balance
            ) ?: Account(0, name, type, balance)

            if (currentAccount == null) {
                account.createdAt = now
                accountViewModel.insert(account)
                Toast.makeText(requireContext(), "Account added successfully", Toast.LENGTH_SHORT).show()
            } else {
                account.updatedAt = now
                accountViewModel.update(account)
                Toast.makeText(requireContext(), "Account updated successfully", Toast.LENGTH_SHORT).show()
            }

            Utils.hideLoading(binding.progressBar)
            // Navigate back
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
        _binding = null
    }
}
