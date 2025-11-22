package com.babsnet.accounting.ui.journal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.JournalWithDetailsAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.JournalDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.databinding.FragmentYearlyBinding
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.JournalViewModel
import java.time.LocalDate


class YearlyFragment : Fragment() {
    private var _binding: FragmentYearlyBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalViewModel: JournalViewModel
    private lateinit var journalWithDetailsAdapter: JournalWithDetailsAdapter

    @SuppressLint("NewApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Setup binding
        _binding = FragmentYearlyBinding.inflate(inflater, container, false)
        val root: View = binding.root

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
                Utils.showDeleteConfirmationDialog(requireContext()){
                    journalViewModel.delete(journalDetail.journal)
                }
                // Handle delete logic
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
//        journalViewModel.allJournalsWithDetails.observe(viewLifecycleOwner) { journalWithDetailsList ->
//            // Update RecyclerView adapter here
//            journalWithDetailsAdapter.submitList(journalWithDetailsList)
//            showEmptyState(journalWithDetailsList.isEmpty())
//
//        }
        val today = LocalDate.now()
        val currentYear = today.year
        val (startOfYearMillis, endOfYearMillis) = DateUtil.getStartAndEndOfYear(currentYear)
        journalViewModel.getJournalsForYear(startOfYearMillis, endOfYearMillis).observe(viewLifecycleOwner) { journalWithDetailsList ->
            // Update RecyclerView adapter di sini
            journalWithDetailsAdapter.submitList(journalWithDetailsList)
            showEmptyState(journalWithDetailsList.isEmpty())
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