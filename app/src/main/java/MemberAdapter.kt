package com.example.gymapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MemberAdapter(
    private val items: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<MemberAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvAvatar    : TextView = v.findViewById(R.id.tvAvatar)
        val tvName      : TextView = v.findViewById(R.id.tvMemberName)
        val tvBmi       : TextView = v.findViewById(R.id.tvMemberBmi)
        val tvAge       : TextView = v.findViewById(R.id.tvMemberAge)
        val tvGoalChip  : TextView = v.findViewById(R.id.tvGoalChip)
        val tvDietStatus: TextView = v.findViewById(R.id.tvDietStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val u = items[pos]
        val initial = (u.username.firstOrNull() ?: 'U').uppercaseChar().toString()
        h.tvAvatar.text = initial

        h.tvName.text = u.username

        val bi = u.basicInfo
        val weight = bi?.weight ?: 0.0
        val height = bi?.height ?: 0.0
        val bmi = if (weight > 0 && height > 0)
            weight / ((height / 100.0) * (height / 100.0)) else 0.0

        h.tvBmi.text = if (bmi > 0) "BMI %.1f".format(bmi) else "BMI —"
        h.tvAge.text = if ((bi?.age ?: 0) > 0) "Age ${bi!!.age}" else "Age —"

        val goal = u.fitnessGoal
        val (label, bg, tc) = when (goal.lowercase()) {
            "lose" -> Triple("Lose Weight", "#FFE0E0", "#C62828")
            "gain" -> Triple("Gain Muscle", "#E8F5E9", "#2E7D32")
            else   -> Triple("Maintain",    "#EEF2FF", "#3333CC")
        }
        h.tvGoalChip.text = label
        h.tvGoalChip.setBackgroundColor(Color.parseColor(bg))
        h.tvGoalChip.setTextColor(Color.parseColor(tc))

        h.tvDietStatus.text = if (u.dietPlan != null && u.dietPlan!!.calories > 0)
            "Plan assigned" else "No plan yet"

        h.itemView.setOnClickListener { onClick(u) }
    }
}