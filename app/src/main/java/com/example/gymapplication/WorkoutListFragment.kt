package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class WorkoutListFragment : Fragment(R.layout.fragment_workout_list) {

    companion object {
        fun newInstance(category: String): WorkoutListFragment {
            val f = WorkoutListFragment()
            val args = Bundle()
            args.putString("category", category)
            f.arguments = args
            return f
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val category = arguments?.getString("category") ?: return
        view.findViewById<TextView>(R.id.textWorkoutCategory).text = category

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerWorkoutList)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        val results = mutableListOf<WorkoutItem>()
        val goals   = listOf("lose_weight", "maintain", "gain_weight")
        var loaded  = 0

        for (goal in goals) {
            FirebaseDatabase.getInstance().getReference("workouts/$goal")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            val w = child.getValue(WorkoutItem::class.java) ?: continue
                            w.id = child.key ?: ""
                            if (w.category.equals(category, ignoreCase = true)) {
                                results.add(w)
                            }
                        }
                        loaded++
                        if (loaded == goals.size && isAdded) {
                            recycler.adapter = WorkoutUserAdapter(
                                items       = results,
                                completed   = emptySet(),
                                onComplete  = {},
                                onCardClick = { workout -> openDetail(workout) }
                            )
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        loaded++
                    }
                })
        }
    }

    private fun openDetail(workout: WorkoutItem) {
        val intent = Intent(requireContext(), WorkoutDetailActivity::class.java)
        intent.putExtra(WorkoutDetailActivity.EXTRA_WORKOUT_ID,   workout.id)
        intent.putExtra(WorkoutDetailActivity.EXTRA_WORKOUT_NAME, workout.name)
        intent.putExtra(WorkoutDetailActivity.EXTRA_GIF_URL,      workout.gifUrl)
        intent.putExtra(WorkoutDetailActivity.EXTRA_DESCRIPTION,  workout.description)
        intent.putExtra(WorkoutDetailActivity.EXTRA_SETS,         workout.sets)
        intent.putExtra(WorkoutDetailActivity.EXTRA_REPS,         workout.reps)
        intent.putExtra(WorkoutDetailActivity.EXTRA_CALORIES,     workout.caloriesBurn)
        intent.putExtra(WorkoutDetailActivity.EXTRA_DIFFICULTY,   workout.difficulty)
        intent.putExtra(WorkoutDetailActivity.EXTRA_CATEGORY,     workout.category)
        intent.putExtra(WorkoutDetailActivity.EXTRA_DURATION,     workout.durationMinutes)
        intent.putExtra(WorkoutDetailActivity.EXTRA_MUSCLES,      workout.musclesTargeted)
        intent.putExtra(WorkoutDetailActivity.EXTRA_STEPS,        workout.steps)
        intent.putExtra(WorkoutDetailActivity.EXTRA_BREATHING,    workout.breathingTip)
        intent.putExtra(WorkoutDetailActivity.EXTRA_POINTS,       workout.pointsToRemember)
        intent.putExtra(WorkoutDetailActivity.EXTRA_EQUIPMENT,    workout.equipment)
        intent.putExtra(WorkoutDetailActivity.EXTRA_GOAL,         workout.goal)
        intent.putExtra(WorkoutDetailActivity.EXTRA_EMOJI,        workout.emoji)
        startActivity(intent)
    }
}