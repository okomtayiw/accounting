package com.babsnet.accounting.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.babsnet.accounting.ui.journal.MonthlyFragment
import com.babsnet.accounting.ui.journal.WeeklyFragment
import com.babsnet.accounting.ui.journal.YearlyFragment

class JournalPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WeeklyFragment()
            1 -> MonthlyFragment()
            2 -> YearlyFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}