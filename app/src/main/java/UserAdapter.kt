package com.example.gymapplication

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class UserAdapter(
    private val context: Context,
    private val userList: List<User>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvAge: TextView = itemView.findViewById(R.id.tvAge)
        val tvBMI: TextView = itemView.findViewById(R.id.tvBMI)
        val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        val tvProtein: TextView = itemView.findViewById(R.id.tvProtein)
        val tvCarbs: TextView = itemView.findViewById(R.id.tvCarbs)
        val tvFats: TextView = itemView.findViewById(R.id.tvFats)
        val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
        val btnAssignDiet: Button = itemView.findViewById(R.id.btnAssignDiet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_diet_plan, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.tvUsername.text = user.username ?: "Unknown"

        val basic = user.basicInfo
        holder.tvAge.text = "Age: ${basic?.age ?: "N/A"}"

        val heightCm = basic?.height ?: 0.0
        val weightKg = basic?.weight ?: 0.0
        val bmiText = if (heightCm > 0 && weightKg > 0) {
            val bmi = weightKg / ((heightCm / 100) * (heightCm / 100))
            "BMI: %.2f".format(bmi)
        } else "BMI: N/A"
        holder.tvBMI.text = bmiText

        val diet = user.dietPlan
        holder.tvCalories.text = "Calories: ${diet?.calories ?: 0} kcal"
        holder.tvProtein.text = "Protein: ${diet?.protein ?: 0} g"
        holder.tvCarbs.text = "Carbs: ${diet?.carbs ?: 0} g"
        holder.tvFats.text = "Fats: ${diet?.fats ?: 0} g"
        holder.tvNotes.text = diet?.notes ?: ""

        holder.btnAssignDiet.setOnClickListener {
            showDietDialog(user)
        }
    }

    private fun showDietDialog(user: User) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_diet_plan, null)

        val etCalories = dialogView.findViewById<EditText>(R.id.etCalories)
        val etProtein = dialogView.findViewById<EditText>(R.id.etProtein)
        val etCarbs = dialogView.findViewById<EditText>(R.id.etCarbs)
        val etFats = dialogView.findViewById<EditText>(R.id.etFats)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        user.dietPlan?.let {
            etCalories.setText(it.calories.toString())
            etProtein.setText(it.protein.toString())
            etCarbs.setText(it.carbs.toString())
            etFats.setText(it.fats.toString())
            etNotes.setText(it.notes)
        }

        AlertDialog.Builder(context)
            .setTitle("Diet Plan for ${user.username}")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val plan = DietPlan(
                    calories = etCalories.text.toString().toIntOrNull() ?: 0,
                    protein = etProtein.text.toString().toIntOrNull() ?: 0,
                    carbs = etCarbs.text.toString().toIntOrNull() ?: 0,
                    fats = etFats.text.toString().toIntOrNull() ?: 0,
                    notes = etNotes.text.toString()
                )

                val uid = user.uid ?: return@setPositiveButton

                dbRef.child(uid).child("dietPlan").setValue(plan)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Diet Plan Saved", Toast.LENGTH_SHORT).show()
                        notifyDataSetChanged()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error saving plan", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}