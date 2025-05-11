package com.hamid.budgetbuddy

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter1(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DailyAnalysis()
            1 -> WeeklyAnalysis()
            2 -> MonthlyAnalysis()
            else -> DailyAnalysis()
        }
    }
}
