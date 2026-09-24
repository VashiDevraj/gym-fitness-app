package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class MainActivity3 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        auth     = Firebase.auth
        database = FirebaseDatabase.getInstance()

        val etUsername  = findViewById<EditText>(R.id.etUsername)
        val etEmail     = findViewById<EditText>(R.id.etEmail)
        val etPassword  = findViewById<EditText>(R.id.etPassword)
        val btnSignUp   = findViewById<Button>(R.id.btnSignUp)
        val tvLogin     = findViewById<TextView>(R.id.tvLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val accountType = intent.getStringExtra("accountType")?.lowercase() ?: run {
            Toast.makeText(this, "Error: Account type missing!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        btnSignUp.setOnClickListener {
            val userId = etUsername.text.toString().trim()
            val mail   = etEmail.text.toString().trim()
            val pass   = etPassword.text.toString().trim()

            // ── Field validation ──────────────────────────────────
            var hasError = false

            if (userId.isEmpty()) {
                etUsername.error = "Please enter a username"
                etUsername.requestFocus()
                hasError = true
            } else if (userId.length < 3) {
                etUsername.error = "Username must be at least 3 characters"
                etUsername.requestFocus()
                hasError = true
            } else {
                etUsername.error = null
            }

            if (mail.isEmpty()) {
                etEmail.error = "Email is required"
                if (!hasError) etEmail.requestFocus()
                hasError = true
            } else if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                etEmail.error = "Enter a valid email address"
                if (!hasError) etEmail.requestFocus()
                hasError = true
            } else {
                etEmail.error = null
            }

            if (pass.isEmpty()) {
                etPassword.error = "Password is required"
                if (!hasError) etPassword.requestFocus()
                hasError = true
            } else if (pass.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                if (!hasError) etPassword.requestFocus()
                hasError = true
            } else {
                etPassword.error = null
            }

            if (hasError) return@setOnClickListener

            btnSignUp.isEnabled    = false
            progressBar.visibility = View.VISIBLE

            auth.createUserWithEmailAndPassword(mail, pass)
                .addOnCompleteListener(this) { authTask ->
                    if (!authTask.isSuccessful) {
                        progressBar.visibility = View.GONE
                        btnSignUp.isEnabled    = true

                        val errorMsg = when (authTask.exception) {
                            is FirebaseAuthUserCollisionException -> {
                                etEmail.error = "An account already exists with this email"
                                "An account already exists with this email. Please log in instead."
                            }
                            is FirebaseAuthWeakPasswordException -> {
                                etPassword.error = "Password is too weak"
                                "Password is too weak. Please use at least 6 characters."
                            }
                            else -> {
                                val msg = authTask.exception?.message ?: ""
                                when {
                                    msg.contains("network", ignoreCase = true) ->
                                        "Network error. Please check your internet connection."
                                    else -> "Registration failed: ${authTask.exception?.message}"
                                }
                            }
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    val uid = auth.currentUser?.uid ?: run {
                        progressBar.visibility = View.GONE
                        btnSignUp.isEnabled    = true
                        Toast.makeText(this, "Auth error. Try again.", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    if (accountType == "admin") {
                        // ── Admin: save to "admins" node ──────────────────────
                        val adminData = mapOf(
                            "uid"         to uid,
                            "adminId"     to userId,
                            "username"    to userId,
                            "email"       to mail,
                            "accountType" to "admin"
                        )
                        database.getReference("admins").child(uid)
                            .setValue(adminData)
                            .addOnSuccessListener {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Admin account created!", Toast.LENGTH_SHORT).show()
                                startActivity(
                                    Intent(this, TrainerPanelActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                )
                                finish()
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                btnSignUp.isEnabled    = true
                                Toast.makeText(this, "DB error: ${e.message}", Toast.LENGTH_SHORT).show()
                                auth.currentUser?.delete()
                            }
                    } else {
                        // ── Regular user: save to "users" node ────────────────
                        // IMPORTANT: Initialize stepGoal to 8000 and empty steps map
                        // so new users always start at 0 steps, not inheriting data
                        val userObj = User(
                            uid         = uid,
                            username    = userId,
                            email       = mail,
                            accountType = "user",
                            basicInfo   = BasicInfo(),
                            dietPlan    = DietPlan(),
                            checkins    = emptyList()
                        )
                        val userRef = database.getReference("users").child(uid)

                        userRef.setValue(userObj)
                            .addOnSuccessListener {
                                // Initialize stepGoal to default 8000 for new user
                                // Steps node is empty by default — Health Connect will populate it
                                userRef.child("stepGoal").setValue(8000)
                                    .addOnCompleteListener {
                                        progressBar.visibility = View.GONE
                                        Toast.makeText(
                                            this,
                                            "Account created! Let's set up your profile.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startActivity(Intent(this, MainActivity4::class.java))
                                        finish()
                                    }
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                btnSignUp.isEnabled    = true
                                Toast.makeText(this, "DB error: ${e.message}", Toast.LENGTH_SHORT).show()
                                auth.currentUser?.delete()
                            }
                    }
                }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
            finish()
        }
    }
}