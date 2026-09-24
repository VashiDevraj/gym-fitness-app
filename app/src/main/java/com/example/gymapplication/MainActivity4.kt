package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gymapplication.databinding.ActivityMain4Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity4 : AppCompatActivity() {

    private lateinit var binding: ActivityMain4Binding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        auth     = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // FIXED: Use addAuthStateListener instead of checking currentUser once.
        // Firebase restores auth state asynchronously — checking immediately can
        // return null even when the user IS logged in.
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // Only redirect if we are NOT finishing already
                if (!isFinishing) {
                    Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(this, MainActivity2::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
            } else {
                // User confirmed — wire up the button
                binding.submitButton.setOnClickListener {
                    saveHeight(user.uid)
                }
            }
        }
    }

    private fun saveHeight(userId: String) {
        val heightText = binding.heightInput.text?.toString()?.trim() ?: ""

        if (heightText.isEmpty()) {
            binding.heightInput.error = "Please enter your height"
            binding.heightInput.requestFocus()
            return
        }

        val height = heightText.toDoubleOrNull()
        if (height == null || height <= 0) {
            binding.heightInput.error = "Please enter a valid height"
            binding.heightInput.requestFocus()
            return
        }

        val heightInCm = if (binding.InRadio.isChecked) height * 2.54 else height

        if (heightInCm < 50 || heightInCm > 300) {
            binding.heightInput.error = "Please enter a realistic height (50–300 cm)"
            binding.heightInput.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.submitButton.isEnabled = false

        database.child("users").child(userId).child("basicInfo")
            .updateChildren(mapOf("height" to heightInCm))
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Height saved ✓", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity5::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.submitButton.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}