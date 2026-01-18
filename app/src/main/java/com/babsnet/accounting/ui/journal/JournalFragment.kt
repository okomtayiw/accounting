package com.babsnet.accounting.ui.journal

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.JournalPagerAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.databinding.FragmentJournalBinding
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.viewModel.JournalViewModel
import com.google.android.material.tabs.TabLayoutMediator

class JournalFragment : Fragment() {

    private lateinit var journalViewModel: JournalViewModel
    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupViewModel()
        _binding = FragmentJournalBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.customToolbar)

        val adapter = JournalPagerAdapter(this)
        binding.viewPagerJournal.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPagerJournal) { tab, position ->
            tab.text = when (position) {
                0 -> "Weekly"
                1 -> "Monthly"
                else -> "Annual"
            }
        }.attach()

        // MENU PROVIDER
        requireActivity().addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.header_menu, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

                searchView.queryHint = "Search journal..."

                searchView.setOnQueryTextListener(object :
                    androidx.appcompat.widget.SearchView.OnQueryTextListener {

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        journalViewModel.setSearch(query.orEmpty())
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        journalViewModel.setSearch(newText.orEmpty())
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.fabAddAccountJournal.setOnClickListener {
            findNavController().navigate(R.id.addEditJournalFragment)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = JournalRepository(db.journalDao(), db.ledgerDao())

        journalViewModel = ViewModelProvider(
            requireActivity(),
            GenericViewModelFactory(JournalViewModel::class.java) { JournalViewModel(repo) }
        )[JournalViewModel::class.java]
    }

}
