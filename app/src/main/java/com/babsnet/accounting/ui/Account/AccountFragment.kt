package com.babsnet.accounting.ui.Account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.AccountAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.AccountDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.databinding.FragmentAccountBinding
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.AccountViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private lateinit var accountViewModel: AccountViewModel
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup repository and ViewModel
        val repository = setupRepository()
        accountViewModel = setupViewModel(repository)
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        binding.btnMenu.setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        // Setup RecyclerView
        val adapter = AccountAdapter(
            accounts = emptyList(),
            onEditClick = { account ->
                // Navigate to AddEditJournalFragment with the journal ID
                val bundle = Bundle().apply {
                    putInt("accountId", account.accountId)
                }
                findNavController().navigate(R.id.action_to_accountInputFragment, bundle)
            },
            onDeleteClick = { account ->
                Utils.showLoading(binding.progressBar)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                    // Delete the account
                    val ledgers = accountViewModel.checkLedgerByAccountId(account)
                    if(ledgers.isNotEmpty()) {
                        Toast.makeText(requireContext(), "The account cannot be deleted because it is already used for transaction data", Toast.LENGTH_SHORT).show()
                    } else {
                        accountViewModel.delete(account) // Delete from ViewModel
                        Toast.makeText(requireContext(), "Account deleted: ${account.accountName}", Toast.LENGTH_SHORT).show()
                    }
                    Utils.hideLoading(binding.progressBar)
                }

            }
        )
        binding.recyclerViewAccount.adapter = adapter
        binding.recyclerViewAccount.layoutManager = LinearLayoutManager(requireContext())

        // Observe data from ViewModel
        accountViewModel.allAccount.observe(viewLifecycleOwner) { accounts ->
            adapter.updateData(accounts) // Update RecyclerView data
            showEmptyState(accounts.isEmpty())
        }
        // Handle Floating Action Button click
        binding.fabAddAccount.setOnClickListener {
            navigateToAccountInputFragment()
        }

        return root
    }

    private fun navigateToAccountInputFragment() {
        val navController = findNavController()
        navController.navigate(R.id.accountInputFragment)
    }


    private fun setupRepository(): AccountRepository {
        val dao: AccountDao = AppDatabase.getDatabase(requireContext()).accountDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(requireContext()).ledgerDao()
        return AccountRepository(dao, ledgerDao)
    }

    private fun setupViewModel(repository: AccountRepository): AccountViewModel {
        val accountViewModelFactory = GenericViewModelFactory(
            AccountViewModel::class.java
        ) { AccountViewModel(repository) }
        return ViewModelProvider(this, accountViewModelFactory)[AccountViewModel::class.java]
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.tvNoData.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewAccount.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
