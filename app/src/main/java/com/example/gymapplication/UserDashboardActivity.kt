package com.example.gymapplication

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class UserDashboardActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var imgProfile: ImageView

    val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedSet ->
        Log.d("HealthConnectDebug", "Permission result — granted: $grantedSet")
        if (grantedSet.containsAll(HealthConnectManager.PERMISSIONS)) {
            Toast.makeText(this, "✓ Step tracking enabled!", Toast.LENGTH_SHORT).show()
            // After permissions granted, refresh home — it will re-run initUserStepData
            // which re-records the baseline now that HC is accessible.
            refreshHomeFragment()
        } else {
            Log.w("HealthConnectDebug", "Missing: ${HealthConnectManager.PERMISSIONS - grantedSet}")
            Toast.makeText(
                this,
                "Open Health Connect → App permissions → allow Steps",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        viewPager  = findViewById(R.id.viewPager)
        bottomNav  = findViewById(R.id.bottomNavigation)
        imgProfile = findViewById(R.id.imgProfile)

        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.offscreenPageLimit = 5

        setupNav()
        checkHealthConnect()
    }

    private fun setupNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home      -> viewPager.currentItem = 0
                R.id.nav_checkin   -> viewPager.currentItem = 1
                R.id.nav_workout   -> viewPager.currentItem = 2
                R.id.nav_equipment -> viewPager.currentItem = 3
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> bottomNav.selectedItemId = R.id.nav_home
                    1 -> bottomNav.selectedItemId = R.id.nav_checkin
                    2 -> bottomNav.selectedItemId = R.id.nav_workout
                    3 -> bottomNav.selectedItemId = R.id.nav_equipment
                    // position 4 = Profile tab, no bottom nav item
                }
            }
        })

        imgProfile.setOnClickListener {
            viewPager.currentItem = 4
        }
    }

    private fun checkHealthConnect() {
        val status = HealthConnectManager.getSdkStatus(this)
        when (status) {
            HealthConnectClient.SDK_AVAILABLE -> {
                lifecycleScope.launch {
                    if (!HealthConnectManager.hasAllPermissions(this@UserDashboardActivity)) {
                        kotlinx.coroutines.delay(400)
                        healthPermissionLauncher.launch(HealthConnectManager.PERMISSIONS)
                    } else {
                        // Permissions already granted — HomeFragment.initUserStepData()
                        // handles baseline + data load on its own via onViewCreated.
                        // No need to call refreshHomeFragment() here; it would be a
                        // duplicate call that races with the fragment's own init.
                    }
                }
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Toast.makeText(this, "Please update Health Connect", Toast.LENGTH_LONG).show()
                HealthConnectManager.openPlayStore(this)
            }
            else -> {
                // Health Connect not installed — steps will show 0 (correct for new user)
                Log.d("HealthConnectDebug", "Health Connect not available — steps will be 0")
            }
        }
    }

    fun requestPermissionsFromFragment() {
        Log.d("HealthConnectDebug", "requestPermissionsFromFragment called")
        healthPermissionLauncher.launch(HealthConnectManager.PERMISSIONS)
    }

    fun goHome() {
        viewPager.currentItem = 0
    }

    fun refreshHomeFragment() {
        val frag = supportFragmentManager.findFragmentByTag("f0")
        Log.d("HealthConnectDebug", "refreshHomeFragment — found: ${frag != null}")
        (frag as? HomeFragment)?.loadFitData()
    }
}