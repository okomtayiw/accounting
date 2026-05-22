package com.babsnet.accounting.ui.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.AccountAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.AccountDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.databinding.FragmentAccountBinding
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.utils.AccountLocalizationUtil
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.AccountViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var adapter: AccountAdapter
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)

        val repository = setupRepository()
        accountViewModel = setupViewModel(repository)

        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        binding.btnMenu.setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        setupRecyclerView()
        setupSearch()
        observeAccounts()

        binding.fabAddAccount.setOnClickListener {
            navigateToAccountInputFragment()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = AccountAdapter(
            accounts = emptyList(),
            onEditClick = { account ->
                val bundle = Bundle().apply {
                    putInt("accountId", account.accountId)
                }
                findNavController().navigate(R.id.action_to_accountInputFragment, bundle)
            },
            onDeleteClick = { account ->
                Utils.showLoading(binding.progressBar)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                    val ledgers = accountViewModel.checkLedgerByAccountId(account)
                    if (ledgers.isNotEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_account_in_use),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        accountViewModel.delete(account)
                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.toast_account_deleted,
                                AccountLocalizationUtil.localizeAccountName(requireContext(), account)
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Utils.hideLoading(binding.progressBar)
                }
            }
        )

        binding.recyclerViewAccount.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAccount.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchCategory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString().orEmpty())
                updateVisibleState()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observeAccounts() {
        accountViewModel.allAccount.observe(viewLifecycleOwner) { accounts ->
            adapter.updateData(accounts)
            adapter.filter(binding.etSearchCategory.text?.toString().orEmpty())
            updateVisibleState()
            loadEntryCounts(accounts)
        }
    }

    private fun loadEntryCounts(accounts: List<Account>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val counts = withContext(Dispatchers.IO) {
                accounts.associate { account ->
                    account.accountId to accountViewModel.checkLedgerByAccountId(account).size
                }
            }
            adapter.updateEntryCounts(counts)
        }
    }

    private fun updateVisibleState() {
        val isEmpty = adapter.currentItemCount() == 0
        binding.tvNoData.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewAccount.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun navigateToAccountInputFragment() {
        findNavController().navigate(R.id.accountInputFragment)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
