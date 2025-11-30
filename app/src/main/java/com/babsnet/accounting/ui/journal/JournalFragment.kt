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
import androidx.navigation.fragment.findNavController


import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.JournalPagerAdapter
import com.babsnet.accounting.databinding.FragmentJournalBinding
import com.google.android.material.tabs.TabLayoutMediator

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val adapter = JournalPagerAdapter(this)
        binding.viewPagerJournal.adapter = adapter


        TabLayoutMediator(binding.tabLayout, binding.viewPagerJournal) { tab, position ->
            tab.text = when (position) {
                0 -> "Weekly"
                1 -> "Monthly"
                else -> "Annual"
            }
        }.attach()

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.header_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_journal -> {
                        // Navigasi ke AddJournalFragment
                        val navController = findNavController()
                        navController.navigate(R.id.addEditJournalFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.fabAddAccountJournal.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.addEditJournalFragment)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
