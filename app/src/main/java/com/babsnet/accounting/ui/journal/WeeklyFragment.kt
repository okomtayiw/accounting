package com.babsnet.accounting.ui.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.JournalWithDetailsAdapter
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.databinding.FragmentWeeklyBinding
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.GenericViewModelFactory
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.JournalViewModel

class WeeklyFragment : Fragment() {

    private var _binding: FragmentWeeklyBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalViewModel: JournalViewModel

    /** Adapter dibuat tetap (tidak recreate saat fragment re-attach) */
    private val adapter by lazy {
        JournalWithDetailsAdapter(
            onDeleteJournal = { item ->
                Utils.showDeleteConfirmationDialog(requireContext()) {
                    journalViewModel.delete(item.journal)
                }
            },
            onEditJournal = { item ->
                findNavController().navigate(
                    R.id.action_journal_to_add_edit_journal,
                    Bundle().apply { putInt("journalId", item.journal.journalId) }
                )
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeeklyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecycler()
        observeData()
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = JournalRepository(db.journalDao(), db.ledgerDao())

        journalViewModel = ViewModelProvider(
            this,
            GenericViewModelFactory(JournalViewModel::class.java) { JournalViewModel(repo) }
        )[JournalViewModel::class.java]
    }

    private fun setupRecycler() {
        binding.recyclerViewJournal.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WeeklyFragment.adapter
            itemAnimator?.apply {
                addDuration = 180
                changeDuration = 180
                moveDuration = 180
            }
        }
    }

    private fun observeData() {
        val (start, end) = DateUtil.getStartAndEndOfWeek()

        journalViewModel.getJournalsForWeek(start, end).observe(viewLifecycleOwner) {
            adapter.submitList(it)
            showEmptyState(it.isEmpty())
        }
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
