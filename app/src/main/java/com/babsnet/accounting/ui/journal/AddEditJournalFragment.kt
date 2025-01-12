package com.babsnet.accounting.ui.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.entity.Journal
import com.babsnet.accounting.databinding.FragmentAddEditJournalBinding
import com.babsnet.accounting.repository.JournalRepository
import com.babsnet.accounting.utils.DateUtil
import com.babsnet.accounting.viewModel.JournalViewModel
import com.babsnet.accounting.utils.GenericViewModelFactory
import kotlinx.coroutines.DelicateCoroutinesApi

class AddEditJournalFragment : Fragment() {

    private var _binding: FragmentAddEditJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalViewModel: JournalViewModel
    private var journalId: Int? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditJournalBinding.inflate(inflater, container, false)
        return binding.root
    }


    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val application = requireActivity().application
        val journalDao = AppDatabase.getDatabase(application).journalDao()
        val repository = JournalRepository(journalDao)
        val journalViewModelFactory = GenericViewModelFactory(
            JournalViewModel::class.java
        ) { JournalViewModel(repository) }

        journalViewModel = ViewModelProvider(this, journalViewModelFactory)[JournalViewModel::class.java]
        journalId = arguments?.getInt("journalId", -1)?.takeIf { it != -1 }
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // If editing, populate the fields with the journal data
        if (journalId != null) {
            journalViewModel.getJournalById(journalId!!).observe(viewLifecycleOwner) { journal ->
                journal?.let {
                    binding.inputDescription.setText(it.description)
                    binding.inputTotalDebit.setText("0")
                    binding.inputDate.setText(it.date?.let { it1 -> DateUtil.dateToString(it1) })
                }
            }
        }


        // Handle save button click
        binding.btnSave.setOnClickListener {
            val description = binding.inputDescription.text.toString()
            val total = binding.inputTotalDebit.text.toString()
            val date = binding.inputDate.text.toString()

            if (description.isEmpty() || total.isEmpty()  || date.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            } else {
               val journal = Journal(
                    date = DateUtil.stringToDate(date),
                    description = description,
                    createdAt = java.util.Date()
                )
                if(journalId == null) {
                    journalViewModel.insert(journal)
                } else {
                    journal.journalId = journalId!!.toInt()
                    journalViewModel.update(journal)
                }

                // Add logic to save journal (e.g., ViewModel or Repository)
                Toast.makeText(requireContext(), "Journal added successfully", Toast.LENGTH_SHORT).show()

                // Go back to previous fragment
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        binding.inputDate.setOnClickListener {
            DateUtil.showDatePicker(requireActivity(), binding.inputDate)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().supportFragmentManager.popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}