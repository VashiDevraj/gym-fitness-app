package com.example.gymapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class WorkoutListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_list)

        val category = intent.getStringExtra("category") ?: return

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, WorkoutListFragment.newInstance(category))
            .commit()

    }
}