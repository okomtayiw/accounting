package com.babsnet.accounting.ui.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.JournalWithDetailsAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.JournalDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.databinding.FragmentJournalBinding
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.viewModel.JournalViewModel


class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalViewModel: JournalViewModel
    private lateinit var journalWithDetailsAdapter: JournalWithDetailsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Setup binding
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Add MenuProvider to handle menu
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.header_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_journal -> {
                        // Navigate to AddJournalFragment
                        val navController = findNavController()
                        navController.navigate(R.id.addEditJournalFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Setup database, DAO, and repository
        val journalDao : JournalDao = AppDatabase.getDatabase(requireContext()).journalDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(requireContext()).ledgerDao()
        val repository = JournalRepository(journalDao, ledgerDao)

        // Setup ViewModel with factory
        val journalViewModelFactory = GenericViewModelFactory(
            JournalViewModel::class.java
        ) { JournalViewModel(repository) }

        journalViewModel = ViewModelProvider(this, journalViewModelFactory)[JournalViewModel::class.java]

        // insertMockData()
        // Setup RecyclerView
        journalWithDetailsAdapter = JournalWithDetailsAdapter(
            onDeleteJournal = { journalDetail ->
                // Handle delete logic
                journalViewModel.delete(journalDetail.journal)
            },
            onEditJournal = { journalDetail ->
                // Navigate to AddEditJournalFragment with the journal ID
                val bundle = Bundle().apply {
                    putInt("journalId", journalDetail.journal.journalId)
                }
                findNavController().navigate(R.id.action_journal_to_add_edit_journal, bundle)
            }
        )
        binding.recyclerViewJournal.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewJournal.adapter = journalWithDetailsAdapter

        // Observe data and submit to adapter
        journalViewModel.allJournalsWithDetails.observe(viewLifecycleOwner) { journalWithDetailsList ->
            // Update RecyclerView adapter here
            journalWithDetailsAdapter.submitList(journalWithDetailsList)
            showEmptyState(journalWithDetailsList.isEmpty())

        }

        binding.fabAddAccountJournal.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.addEditJournalFragment)
        }

        return root
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.tvNoData.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewJournal.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
