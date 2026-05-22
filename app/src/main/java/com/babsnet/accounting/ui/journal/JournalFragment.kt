package com.babsnet.accounting.ui.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
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

        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        binding.btnMenu.setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        binding.viewPagerJournal.adapter = JournalPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPagerJournal) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.weekly)
                1 -> getString(R.string.monthly)
                else -> getString(R.string.yearly)
            }
        }.attach()

        binding.editSearchJournal.doAfterTextChanged { text ->
            journalViewModel.setSearch(text?.toString().orEmpty())
        }

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
