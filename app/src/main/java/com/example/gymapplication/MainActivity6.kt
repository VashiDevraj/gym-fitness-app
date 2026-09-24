package com.example.gymapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class MainActivity6 : AppCompatActivity() {

    private lateinit var tvHeight: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvBMI: TextView
    private lateinit var tvBMIStatus: TextView
    private lateinit var etDOB: EditText
    private lateinit var btnSaveAge: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCalculatedAge: TextView

    private val calendar = Calendar.getInstance()
    private var currentHeight = 0.0
    private var currentWeight = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main6)

        tvHeight        = findViewById(R.id.tvHeight)
        tvWeight        = findViewById(R.id.tvWeight)
        tvBMI           = findViewById(R.id.tvBMI)
        tvBMIStatus     = findViewById(R.id.tvBMIStatus)
        etDOB           = findViewById(R.id.etDOB)
        btnSaveAge      = findViewById(R.id.btnSaveAge)
        progressBar     = findViewById(R.id.progressBar)
        tvCalculatedAge = findViewById(R.id.tvCalculatedAge)

        progressBar.visibility = View.GONE

        val auth = FirebaseAuth.getInstance()
        val uid  = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        // Try intent extras first (faster), fall back to Firebase read
        val intentHeight = intent.getDoubleExtra("height", -1.0)
        val intentWeight = intent.getDoubleExtra("weight", -1.0)
        val intentBmi    = intent.getDoubleExtra("bmi", -1.0)

        if (intentHeight > 0 && intentWeight > 0) {
            currentHeight = intentHeight
            currentWeight = intentWeight
            displayStats(intentBmi.takeIf { it > 0 })
        } else {
            // Load from Firebase if extras weren't passed
            userRef.child("basicInfo").get().addOnSuccessListener { snapshot ->
                currentHeight = when (val h = snapshot.child("height").value) {
                    is Double -> h
                    is Long   -> h.toDouble()
                    is String -> h.toDoubleOrNull() ?: 0.0
                    else      -> 0.0
                }
                currentWeight = when (val w = snapshot.child("weight").value) {
                    is Double -> w
                    is Long   -> w.toDouble()
                    is String -> w.toDoubleOrNull() ?: 0.0
                    else      -> 0.0
                }
                displayStats(null)
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load profile data.", Toast.LENGTH_SHORT).show()
            }
        }

        // DOB picker — prevent manual keyboard input
        etDOB.isFocusable = false
        etDOB.setOnClickListener { showDatePicker() }

        btnSaveAge.setOnClickListener {
            handleSave(userRef)
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                etDOB.setText("%02d/%02d/%04d".format(day, month + 1, year))
                val age = calculateAge(year, month, day)
                tvCalculatedAge.text = if (age >= 0) "Age: $age years" else "Invalid date"
            },
            calendar.get(Calendar.YEAR) - 18,   // default to 18 years ago
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun displayStats(passedBmi: Double?) {
        tvHeight.text = if (currentHeight > 0) "%.0f cm".format(currentHeight) else "—"
        tvWeight.text = if (currentWeight > 0) "%.1f kg".format(currentWeight) else "—"

        val bmi = passedBmi ?: if (currentHeight > 0 && currentWeight > 0) {
            val hm = currentHeight / 100.0
            currentWeight / (hm * hm)
        } else null

        if (bmi != null && bmi > 0) {
            tvBMI.text = "%.1f".format(bmi)
            val (label, color) = when {
                bmi < 18.5 -> "Underweight" to Color.parseColor("#FF9800")
                bmi < 25.0 -> "Normal ✓"    to Color.parseColor("#43A047")
                bmi < 30.0 -> "Overweight"  to Color.parseColor("#FB8C00")
                else       -> "Obese"        to Color.parseColor("#E53935")
            }
            tvBMIStatus.text = label
            tvBMIStatus.setTextColor(color)
            tvBMI.setTextColor(color)
        } else {
            tvBMI.text       = "N/A"
            tvBMIStatus.text = "Enter height & weight first"
        }
    }

    private fun handleSave(userRef: com.google.firebase.database.DatabaseReference) {
        val dobString = etDOB.text.toString().trim()
        if (dobString.isEmpty()) {
            Toast.makeText(this, "Please select your date of birth", Toast.LENGTH_SHORT).show()
            return
        }

        val parts = dobString.split("/")
        if (parts.size != 3) {
            Toast.makeText(this, "Invalid date format. Please re-select.", Toast.LENGTH_SHORT).show()
            etDOB.text.clear()
            return
        }

        val day   = parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()?.minus(1)  // Calendar months are 0-based
        val year  = parts[2].toIntOrNull()

        if (day == null || month == null || year == null) {
            Toast.makeText(this, "Invalid date. Please re-select.", Toast.LENGTH_SHORT).show()
            return
        }

        val age = calculateAge(year, month, day)

        if (age < 0) {
            Toast.makeText(this, "Invalid date of birth.", Toast.LENGTH_SHORT).show()
            return
        }

        if (age < 16) {
            Toast.makeText(this, "You must be at least 16 years old to use this app.", Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }, 2000)
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSaveAge.isEnabled   = false

        val bmiValue = if (currentHeight > 0 && currentWeight > 0) {
            val hm = currentHeight / 100.0
            currentWeight / (hm * hm)
        } else 0.0

        val updates = mapOf(
            "dob" to dobString,
            "age" to age,
            "bmi" to bmiValue
        )

        userRef.child("basicInfo").updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile completed ✓", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    progressBar.visibility = View.GONE
                    startActivity(
                        Intent(this, UserDashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }, 800)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                btnSaveAge.isEnabled   = true
                Toast.makeText(this, "Error saving. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val today = Calendar.getInstance()
        val dob   = Calendar.getInstance().apply { set(year, month, day) }
        if (dob.after(today)) return -1     // future date = invalid
        var age   = today.get(Calendar.YEAR) - year
        if (today.get(Calendar.MONTH) < month ||
            (today.get(Calendar.MONTH) == month && today.get(Calendar.DAY_OF_MONTH) < day)) {
            age--
        }
        return age
    }
}