package com.babsnet.accounting.ui.account

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.ColorAdapter
import com.babsnet.accounting.adapter.IconAdapter
import com.babsnet.accounting.adapter.IconItem
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.databinding.FragmentAccountInputBinding
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.GridSpacingItemDecoration
import com.babsnet.accounting.utils.IconProvider
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.AccountViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class AccountInputFragment : Fragment() {

    private var _binding: FragmentAccountInputBinding? = null
    private val binding get() = _binding!!
    private lateinit var accountViewModel: AccountViewModel
    private var currentAccount: Account? = null
    lateinit var iconAdapter: IconAdapter
    lateinit var colorAdapter: ColorAdapter
    private lateinit var allIcons: List<IconItem>
    private var selectedIcon: String = ""
    private var selectedColor: Int? = null


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
        setupColorAdapter()

        allIcons = IconProvider.getIcons(requireContext())
        iconAdapter = IconAdapter(allIcons) { selected ->
            selectedIcon = selected.key
            binding.tvSelectedIconName.text = selected.displayName
        }
        binding.rvIcons.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvIcons.adapter = iconAdapter

        val spacing = Utils.dpToPx(requireContext(), 10)
        binding.rvIcons.addItemDecoration(GridSpacingItemDecoration(4, spacing))

        binding.searchIcon.addTextChangedListener {
            iconAdapter.filter(it.toString())
        }

        val accountTypes = resources.getStringArray(R.array.account_types)
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            accountTypes
        )
        binding.autoCompleteTypeAccount.adapter = spinnerAdapter
        binding.autoCompleteTypeAccount.setSelection(1)


        val accountId = arguments?.getInt("accountId")
        if (accountId != null && accountId != -1) {
            binding.toolbarTitle.text = getString(R.string.edit_account)

            accountViewModel.getAccountById(accountId).observe(viewLifecycleOwner) { account ->
                currentAccount = account ?: return@observe

                // nama
                binding.editTextAccountName.setText(account.accountName)

                // spinner
                val index = accountTypes.indexOf(account.accountType)
                if (index != -1) binding.autoCompleteTypeAccount.setSelection(index)

                // ICON SELECTED
                selectedIcon = account.iconResName.toString()
                iconAdapter.setSelectedIcon(selectedIcon)
                binding.tvSelectedIconName.text =
                    allIcons.firstOrNull { it.key == selectedIcon }?.displayName
                        ?: getString(R.string.selected_icon)

                account.color.let { hex ->
                    colorAdapter.setSelectedColor(hex)
                    selectedColor = try {
                        Color.parseColor(hex)
                    } catch (e: Exception) {
                        null
                    }
                }

            }
        }

        if (selectedIcon.isBlank()) {
            selectedIcon = allIcons.firstOrNull()?.key.orEmpty()
            iconAdapter.setSelectedIcon(selectedIcon)
            binding.tvSelectedIconName.text =
                allIcons.firstOrNull { it.key == selectedIcon }?.displayName
                    ?: getString(R.string.selected_icon)
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonSave.setOnClickListener { saveAccount() }
    }


    private fun saveAccount() {
        val name = binding.editTextAccountName.text.toString()
        val type = binding.autoCompleteTypeAccount.selectedItem?.toString() ?: ""
        val colorHex = selectedColor?.let {
            String.format("#%06X", 0xFFFFFF and it)
        } ?: currentAccount?.color
        val resNameValue = selectedIcon.takeIf { it.isNotBlank() }
            ?: allIcons.firstOrNull()?.key.orEmpty()

        // Validate account name
        if (name.isBlank()) {
            showValidationWarning(getString(R.string.validation_account_name_empty))
            return
        }

        if (resNameValue.isBlank()) {
            showValidationWarning(getString(R.string.validation_select_icon))
            return
        }

        if (colorHex.isNullOrBlank()) {
            showValidationWarning(getString(R.string.validation_select_color))
            return
        }

        Utils.showLoading(binding.progressBar)
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)

            val now = Date()

            val account = currentAccount?.copy(
                accountName = name,
                accountType = type,
                color = colorHex,
                iconResName = resNameValue
            ) ?: Account(
                accountName = name,
                accountType = type,
                balance = 0.0,
                color = colorHex,
                iconResName  = resNameValue
            )

            if (currentAccount == null) {
                account.createdAt = now
                accountViewModel.insert(account)
                Toast.makeText(requireContext(), getString(R.string.toast_account_added), Toast.LENGTH_SHORT).show()
            } else {
                account.updatedAt = now
                accountViewModel.update(account)
                Toast.makeText(requireContext(), getString(R.string.toast_account_updated), Toast.LENGTH_SHORT).show()
            }

            Utils.hideLoading(binding.progressBar)
            findNavController().navigateUp()
        }

    }

    private fun setupColorAdapter() {
        val colors = listOf(
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt(),
            0xFF4A4A4A.toInt(),
            0xFF8D7B6A.toInt(),
            0xFFB09EFF.toInt(),
            0xFF5D81F7.toInt(),
            0xFF227AF0.toInt(),
            0xFF0094FF.toInt(),
            0xFF5DD0FF.toInt(),
            0xFF4CD4B0.toInt(),
            0xFF8EE700.toInt(),
            0xFFFFD600.toInt(),
            0xFFFF9A00.toInt(),
            0xFFFF6D6D.toInt(),
            0xFFFF4E4E.toInt()
        )

        colorAdapter = ColorAdapter(colors) { selected ->
            selectedColor = selected
        }

        binding.rvColors.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvColors.adapter = colorAdapter
    }

    private fun showValidationWarning(message: String) {
        val snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT)
        snackbar.view.setBackgroundColor(Color.TRANSPARENT)
        val layout = snackbar.view as Snackbar.SnackbarLayout
        layout.setPadding(24, 0, 24, 28)

        val customView = layoutInflater.inflate(R.layout.view_warning_snackbar, null)
        customView.findViewById<TextView>(R.id.tvWarningMessage).text = message

        layout.removeAllViews()
        layout.addView(customView)
        snackbar.show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
        _binding = null
    }
}
