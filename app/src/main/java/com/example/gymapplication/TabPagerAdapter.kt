package com.example.gymapplication

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.gymapplication.AdminLoginFragment
import com.example.gymapplication.UserLoginFragment

class TabPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) UserLoginFragment() else AdminLoginFragment()
    }
}