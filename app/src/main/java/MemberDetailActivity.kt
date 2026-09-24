package com.example.gymapplication

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase

class MemberDetailActivity : AppCompatActivity() {

    private val db by lazy { FirebaseDatabase.getInstance() }

    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvGoalChip: TextView
    private lateinit var tvGoalSuggestion: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvHeight: TextView
    private lateinit var tvBmi: TextView
    private lateinit var tvAge: TextView
    private lateinit var etCalories: TextInputEditText
    private lateinit var etProtein: TextInputEditText
    private lateinit var etCarbs: TextInputEditText
    private lateinit var etFats: TextInputEditText
    private lateinit var etBreakfast: TextInputEditText
    private lateinit var etLunch: TextInputEditText
    private lateinit var etDinner: TextInputEditText
    private lateinit var etSnacks: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private var memberUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_detail)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        memberUid = intent.getStringExtra("uid") ?: run { finish(); return }

        bindViews()
        loadMemberData()
    }

    private fun bindViews() {
        tvAvatar        = findViewById(R.id.tvDetailAvatar)
        tvName          = findViewById(R.id.tvDetailName)
        tvEmail         = findViewById(R.id.tvDetailEmail)
        tvGoalChip      = findViewById(R.id.tvDetailGoalChip)
        tvGoalSuggestion = findViewById(R.id.tvGoalSuggestion)
        tvWeight        = findViewById(R.id.tvDetailWeight)
        tvHeight        = findViewById(R.id.tvDetailHeight)
        tvBmi           = findViewById(R.id.tvDetailBmi)
        tvAge           = findViewById(R.id.tvDetailAge)
        etCalories      = findViewById(R.id.etCalories)
        etProtein       = findViewById(R.id.etProtein)
        etCarbs         = findViewById(R.id.etCarbs)
        etFats          = findViewById(R.id.etFats)
        etBreakfast     = findViewById(R.id.etBreakfast)
        etLunch         = findViewById(R.id.etLunch)
        etDinner        = findViewById(R.id.etDinner)
        etSnacks        = findViewById(R.id.etSnacks)
        etNotes         = findViewById(R.id.etNotes)
        btnSave         = findViewById(R.id.btnSaveDiet)
    }

    private fun loadMemberData() {
        db.getReference("users/$memberUid").get().addOnSuccessListener { snap ->
            val user = User.fromSnapshot(snap) ?: return@addOnSuccessListener

            val initial = (user.username.firstOrNull() ?: 'U').uppercaseChar().toString()
            tvAvatar.text  = initial
            tvName.text    = user.username
            tvEmail.text   = user.email.ifBlank { "No email" }

            val goal = user.fitnessGoal
            tvGoalChip.text = goal.replaceFirstChar { it.uppercase() }
            styleGoalChip(goal)

            val bi = user.basicInfo
            val weight = bi?.weight ?: 0.0
            val height = bi?.height ?: 0.0
            val age    = bi?.age ?: 0

            tvWeight.text = if (weight > 0) "%.1f kg".format(weight) else "—"
            tvHeight.text = if (height > 0) "%.0f cm".format(height) else "—"
            tvAge.text    = if (age > 0) "$age" else "—"

            val bmi = if (weight > 0 && height > 0) weight / ((height / 100) * (height / 100)) else 0.0
            tvBmi.text = if (bmi > 0) "%.1f".format(bmi) else "—"

            tvGoalSuggestion.text = buildSuggestion(goal, bmi, weight)

            supportActionBar?.title = user.username

            // Pre-fill existing diet plan
            user.dietPlan?.let { plan ->
                etCalories.setText(if (plan.calories > 0) plan.calories.toString() else "")
                etProtein.setText(if (plan.protein > 0) plan.protein.toString() else "")
                etCarbs.setText(if (plan.carbs > 0) plan.carbs.toString() else "")
                etFats.setText(if (plan.fats > 0) plan.fats.toString() else "")
                etBreakfast.setText(plan.breakfast)
                etLunch.setText(plan.lunch)
                etDinner.setText(plan.dinner)
                etSnacks.setText(plan.snacks)
                etNotes.setText(plan.notes)
            }

            btnSave.setOnClickListener { saveDiet(goal) }
        }
    }

    private fun styleGoalChip(goal: String) {
        val (text, bgColor, textColor) = when (goal.lowercase()) {
            "lose"     -> Triple("Lose Weight",   "#FFE0E0", "#C62828")
            "gain"     -> Triple("Gain Muscle",   "#E8F5E9", "#2E7D32")
            else       -> Triple("Maintain",      "#EEF2FF", "#3333CC")
        }
        tvGoalChip.text = text
        tvGoalChip.setBackgroundColor(android.graphics.Color.parseColor(bgColor))
        tvGoalChip.setTextColor(android.graphics.Color.parseColor(textColor))
    }

    private fun buildSuggestion(goal: String, bmi: Double, weight: Double): String {
        val bmiNote = when {
            bmi <= 0   -> ""
            bmi < 18.5 -> "BMI is underweight. "
            bmi < 25   -> "BMI is in a healthy range. "
            bmi < 30   -> "BMI is overweight. "
            else       -> "BMI indicates obesity. "
        }
        return bmiNote + when (goal.lowercase()) {
            "lose" ->
                "Recommended: 300–500 kcal deficit. Focus on high protein (1.6g/kg), moderate carbs, low fats. Suggested range: ${(weight * 25).toInt()}–${(weight * 27).toInt()} kcal/day."
            "gain" ->
                "Recommended: 300–500 kcal surplus. High protein (1.8g/kg), high carbs for energy. Suggested range: ${(weight * 33).toInt()}–${(weight * 36).toInt()} kcal/day."
            else ->
                "Recommended: Maintenance calories. Balanced macros. Suggested range: ${(weight * 28).toInt()}–${(weight * 31).toInt()} kcal/day."
        }
    }

    private fun saveDiet(goal: String) {
        val calories  = etCalories.text.toString().toIntOrNull() ?: 0
        val protein   = etProtein.text.toString().toIntOrNull() ?: 0
        val carbs     = etCarbs.text.toString().toIntOrNull() ?: 0
        val fats      = etFats.text.toString().toIntOrNull() ?: 0
        val breakfast = etBreakfast.text.toString().trim()
        val lunch     = etLunch.text.toString().trim()
        val dinner    = etDinner.text.toString().trim()
        val snacks    = etSnacks.text.toString().trim()
        val notes     = etNotes.text.toString().trim()

        if (calories == 0) {
            Toast.makeText(this, "Please enter calorie target", Toast.LENGTH_SHORT).show()
            return
        }

        val plan = DietPlan(
            calories     = calories,
            protein      = protein,
            carbs        = carbs,
            fats         = fats,
            breakfast    = breakfast,
            lunch        = lunch,
            dinner       = dinner,
            snacks       = snacks,
            notes        = notes,
            assignedGoal = goal,
            lastUpdated  = System.currentTimeMillis()
        )

        btnSave.isEnabled = false
        db.getReference("users/$memberUid/dietPlan").setValue(plan)
            .addOnSuccessListener {
                Toast.makeText(this, "Diet plan saved successfully", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}