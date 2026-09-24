package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment

/**
 * AdminWorkoutFragment — now simply a launcher for AdminWorkoutActivity.
 * All workout management logic lives in AdminWorkoutActivity to avoid
 * the AdminWorkoutAdapter redeclaration conflict.
 */
class AdminWorkoutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val btn = Button(requireContext()).apply {
            text = "Manage Workouts"
            setOnClickListener {
                startActivity(Intent(requireContext(), AdminWorkoutActivity::class.java))
            }
        }
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(btn)
        }
        return layout
    }
}