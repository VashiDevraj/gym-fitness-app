package com.example.gymapplication

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.gymapplication.fragments.ProfileFragment

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    // Tab order:
    // 0 = Home        (bottom nav: nav_home)
    // 1 = Check In    (bottom nav: nav_checkin)
    // 2 = Workout     (bottom nav: nav_workout)
    // 3 = Equipment   (bottom nav: nav_equipment)
    // 4 = Profile     (profile icon in toolbar — no bottom nav item)

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> HomeFragment()
        1 -> CheckInFragment()
        2 -> WorkoutFragment()
        3 -> EquipmentFragment()
        4 -> ProfileFragment()
        else -> HomeFragment()
    }
}