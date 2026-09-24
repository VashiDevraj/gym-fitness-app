package com.example.gymapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gymapplication.databinding.ActivityMain2Binding
import com.example.gymapplication.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

private lateinit var binding: ActivityMain2Binding

class MainActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMain2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = TabPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "User" else "Admin"
        }.attach()

        binding.viewPager.currentItem = 0 // User tab default
    }
}