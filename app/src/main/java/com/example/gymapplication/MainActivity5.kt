package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity5 : AppCompatActivity() {

    private lateinit var weightPickerInt: NumberPicker
    private lateinit var weightPickerDecimal: NumberPicker
    private lateinit var saveButton: Button
    private lateinit var tvCurrentWeight: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var height: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main5)

        supportActionBar?.title = "Update Weight"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        weightPickerInt = findViewById(R.id.weightPickerInt)
        weightPickerDecimal = findViewById(R.id.weightPickerDecimal)
        saveButton = findViewById(R.id.btn_weight)
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        weightPickerInt.minValue = 30
        weightPickerInt.maxValue = 200
        weightPickerInt.value = 70

        weightPickerDecimal.minValue = 0
        weightPickerDecimal.maxValue = 9
        weightPickerDecimal.value = 0

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        fetchHeightAndCurrentWeight(uid)

        saveButton.setOnClickListener {
            saveWeightAndBMI(uid)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun fetchHeightAndCurrentWeight(userId: String) {
        database.child("users").child(userId).child("basicInfo")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Read height — handle Double, Long, or String from Firebase
                    height = when (val h = snapshot.child("height").value) {
                        is Double -> h
                        is Long   -> h.toDouble()
                        is String -> h.toDoubleOrNull() ?: 0.0
                        else      -> 0.0
                    }

                    if (height <= 0) {
                        Toast.makeText(
                            this@MainActivity5,
                            "Please enter your height first",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return
                    }

                    // Pre-fill with current weight if it exists
                    val currentWeight = when (val w = snapshot.child("weight").value) {
                        is Double -> w
                        is Long   -> w.toDouble()
                        is String -> w.toDoubleOrNull() ?: 70.0
                        else      -> 70.0
                    }

                    weightPickerInt.value = currentWeight.toInt().coerceIn(30, 200)
                    weightPickerDecimal.value = ((currentWeight % 1) * 10).toInt().coerceIn(0, 9)
                    tvCurrentWeight.text = "Current: ${"%.1f".format(currentWeight)} kg"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity5", "Failed to fetch data: ${error.message}")
                    Toast.makeText(
                        this@MainActivity5,
                        "Failed to load data. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun saveWeightAndBMI(userId: String) {
        val weight = weightPickerInt.value + weightPickerDecimal.value / 10.0

        if (height <= 0) {
            Toast.makeText(this, "Height not available. Please go back and enter height.", Toast.LENGTH_LONG).show()
            return
        }

        val bmi = calculateBMI(weight, height)
        saveButton.isEnabled = false

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Save weight and BMI as numbers, not strings
        val basicUpdates = mapOf(
            "weight" to weight,
            "bmi"    to bmi
        )

        database.child("users").child(userId).child("basicInfo")
            .updateChildren(basicUpdates)
            .addOnSuccessListener {
                // Also save to weightHistory for chart
                database.child("users").child(userId)
                    .child("weightHistory").child(today)
                    .setValue(weight.toFloat())

                Toast.makeText(this, "Weight & BMI saved ✓", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this, MainActivity6::class.java)
                        .putExtra("weight", weight)
                        .putExtra("height", height)
                        .putExtra("bmi", bmi)
                )
                finish()
            }
            .addOnFailureListener { e ->
                saveButton.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity5", "Failed to save weight", e)
            }
    }

    private fun calculateBMI(weight: Double, heightCm: Double): Double {
        val hm = heightCm / 100.0
        return weight / (hm * hm)
    }
}