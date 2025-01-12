package com.babsnet.accounting.ui.Account

import android.os.Bundle
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
        val repository = AccountRepository(dao)
        val factory = GenericViewModelFactory(AccountViewModel::class.java) {
            AccountViewModel(repository)
        }
        accountViewModel = ViewModelProvider(this, factory)[AccountViewModel::class.java]

        // Setup Menu Provider for the back button
        setupMenu()

        // Setup UI
        setupUI()
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
        // Populate spinner
        val accountTypes = resources.getStringArray(R.array.account_types)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, accountTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAccountType.adapter = spinnerAdapter

        // Set default value for Balance
        binding.editTextBalance.setText("0.0") // Set default value here

        // Check if editing an account
        val accountId = arguments?.getInt("accountId")
        if (accountId != null && accountId != -1) { // Ensure valid accountId
            accountViewModel.getAccountById(accountId).observe(viewLifecycleOwner) { account ->
                currentAccount = account
                binding.editTextAccountName.setText(account?.accountName ?: "")
                binding.editTextBalance.setText(account?.balance?.toString() ?: "0.0")
                binding.spinnerAccountType.setSelection(accountTypes.indexOf(account?.accountType ?: ""))
            }
        }

        // Save button click listener
        binding.buttonSave.setOnClickListener {
            saveAccount()
        }
    }

    private fun saveAccount() {
        val name = binding.editTextAccountName.text.toString()
        val type = binding.spinnerAccountType.selectedItem.toString()
        val balance = binding.editTextBalance.text.toString().toDoubleOrNull() ?: 0.0

        // Validate account name
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Account Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        Utils.showLoading(binding.progressBar)
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)

            val account = currentAccount?.copy(
                accountName = name,
                accountType = type,
                balance = balance
            ) ?: Account(0, name, type, balance)

            if (currentAccount == null) {
                accountViewModel.insert(account)
                Toast.makeText(requireContext(), "Account added successfully", Toast.LENGTH_SHORT).show()
            } else {
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
        _binding = null
    }
}
