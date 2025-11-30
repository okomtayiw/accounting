package com.babsnet.accounting.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.babsnet.accounting.ui.journal.MonthlyFragment
import com.babsnet.accounting.ui.journal.WeeklyFragment
import com.babsnet.accounting.ui.journal.YearlyFragment


class JournalPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WeeklyFragment()
            1 -> MonthlyFragment()
            else -> YearlyFragment()
        }
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun containsItem(itemId: Long): Boolean = itemId < itemCount
}
