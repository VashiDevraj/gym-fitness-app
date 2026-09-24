package com.example.gymapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DietSummaryAdapter(
    private val items: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<DietSummaryAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvAvatar    : TextView = v.findViewById(R.id.tvDietAvatar)
        val tvName      : TextView = v.findViewById(R.id.tvDietMemberName)
        val tvGoal      : TextView = v.findViewById(R.id.tvDietGoalChip)
        val tvCalories  : TextView = v.findViewById(R.id.tvDietCaloriesSummary)
        val tvMacros    : TextView = v.findViewById(R.id.tvDietMacrosSummary)
        val tvStatus    : TextView = v.findViewById(R.id.tvDietPlanStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_summary, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val u = items[pos]
        h.tvAvatar.text = (u.username.firstOrNull() ?: 'U').uppercaseChar().toString()
        h.tvName.text   = u.username

        val goal = u.fitnessGoal
        val (label, bg, tc) = when (goal.lowercase()) {
            "lose" -> Triple("Lose Weight", "#FFE0E0", "#C62828")
            "gain" -> Triple("Gain Muscle", "#E8F5E9", "#2E7D32")
            else   -> Triple("Maintain",    "#EEF2FF", "#3333CC")
        }
        h.tvGoal.text = label
        h.tvGoal.setBackgroundColor(Color.parseColor(bg))
        h.tvGoal.setTextColor(Color.parseColor(tc))

        val plan = u.dietPlan
        if (plan != null && plan.calories > 0) {
            h.tvCalories.text = "${plan.calories} kcal/day"
            h.tvMacros.text   = "P: ${plan.protein}g  C: ${plan.carbs}g  F: ${plan.fats}g"
            h.tvStatus.text   = "Plan assigned"
            h.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            h.tvCalories.text = "No plan assigned"
            h.tvMacros.text   = ""
            h.tvStatus.text   = "Tap to assign"
            h.tvStatus.setTextColor(Color.parseColor("#E53935"))
        }

        h.itemView.setOnClickListener { onClick(u) }
    }
}