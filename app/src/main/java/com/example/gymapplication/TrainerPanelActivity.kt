package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TrainerPanelActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trainer_panel)

        try {
            auth     = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance()

            val viewPager = findViewById<ViewPager2>(R.id.viewPager)
            val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
            val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

            viewPager.adapter = TrainerViewPagerAdapter(this)
            viewPager.offscreenPageLimit = 4

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Members"
                    1 -> "Attendance"
                    2 -> "Workouts"
                    3 -> "Equipment"
                    4 -> "Diet Plans"
                    else -> "Tab $position"
                }
            }.attach()

            btnLogout.setOnClickListener {
                auth.signOut()
                startActivity(Intent(this, MainActivity2::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(sys.left, sys.top, sys.right, sys.bottom)
                insets
            }

        } catch (e: Exception) {
            Log.e("TrainerPanel", "Init error", e)
            Toast.makeText(this, "Initialization failed. Restart the app.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private inner class TrainerViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount() = 5
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> MembersFragment()
            1 -> AttendanceFragment()
            2 -> AdminWorkoutFragment()
            3 -> AdminEquipmentFragment()
            4 -> DietPlansFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}