package com.babsnet.accounting.ui.journal

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.JournalWithDetailsAdapter
import com.babsnet.accounting.databinding.FragmentYearlyBinding
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.utils.Utils
import com.babsnet.accounting.viewModel.JournalViewModel
import java.time.LocalDate

class YearlyFragment : Fragment() {

    private var _binding: FragmentYearlyBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalViewModel: JournalViewModel

    /** Adapter dibuat Lazy agar tidak recreate saat tab di-swipe */
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
        _binding = FragmentYearlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecycler()
        observeData()
    }

    private fun setupViewModel() {
        journalViewModel = ViewModelProvider(requireActivity())[JournalViewModel::class.java]
    }

    private fun setupRecycler() {
        binding.recyclerViewJournal.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@YearlyFragment.adapter
            itemAnimator?.apply {
                addDuration = 180
                changeDuration = 180
                moveDuration = 180
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeData() {
        val currentYear = LocalDate.now().year
        val (start, end) = DateUtil.getStartAndEndOfYear(currentYear)

        journalViewModel.searchByRange(start, end).observe(viewLifecycleOwner) {
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
