package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.*

class AdminLoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var adminsRef: DatabaseReference

    private lateinit var etAdminId: TextInputEditText
    private lateinit var etAdminEmail: TextInputEditText
    private lateinit var etAdminPassword: TextInputEditText
    private lateinit var btnAdminLogin: MaterialButton
    private lateinit var btnSkip: MaterialButton
    private lateinit var tvAdminSignup: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var progressBar: ProgressBar

    // Prevent navigation loop from auto-login check
    private var hasNavigated = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_login, container, false)

        auth      = FirebaseAuth.getInstance()
        adminsRef = FirebaseDatabase.getInstance().getReference("admins")

        etAdminId       = view.findViewById(R.id.adminId)
        etAdminEmail    = view.findViewById(R.id.adminEmail)
        etAdminPassword = view.findViewById(R.id.adminPassword)
        btnAdminLogin   = view.findViewById(R.id.adminLogin)
        btnSkip         = view.findViewById(R.id.skip)
        tvAdminSignup   = view.findViewById(R.id.adminSignup)
        tvForgotPassword = view.findViewById(R.id.adminforgetpassword)
        progressBar     = view.findViewById(R.id.progressBar)

        // ── Auto-login: one-shot check only, no listener loop ──
        val existingUser = auth.currentUser
        if (existingUser != null) {
            adminsRef.child(existingUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!isAdded) return
                        val type = snapshot.child("accountType")
                            .getValue(String::class.java) ?: ""
                        if (type.equals("admin", ignoreCase = true)) {
                            goToTrainerPanel()
                        }
                        // If not admin, stay on login screen — don't auto-route
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        btnAdminLogin.setOnClickListener { attemptAdminLogin() }

        btnSkip.setOnClickListener { goToTrainerPanel() }

        tvAdminSignup.setOnClickListener {
            startActivity(
                Intent(requireContext(), MainActivity3::class.java)
                    .putExtra("accountType", "admin")
            )
        }

        tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }

        return view
    }

    private fun attemptAdminLogin() {
        val adminId  = etAdminId.text.toString().trim()
        val email    = etAdminEmail.text.toString().trim()
        val password = etAdminPassword.text.toString().trim()

        var hasError = false

        if (adminId.isEmpty()) {
            etAdminId.error = "Admin ID is required"
            hasError = true
        } else {
            etAdminId.error = null
        }
        if (email.isEmpty()) {
            etAdminEmail.error = "Email is required"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etAdminEmail.error = "Enter a valid email"
            hasError = true
        } else {
            etAdminEmail.error = null
        }
        if (password.isEmpty()) {
            etAdminPassword.error = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            etAdminPassword.error = "Password must be ≥ 6 characters"
            hasError = true
        } else {
            etAdminPassword.error = null
        }

        if (hasError) return

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!isAdded) return@addOnCompleteListener

                if (!task.isSuccessful) {
                    setLoading(false)

                    val errorMsg = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> {
                            etAdminEmail.error = "No account found with this email"
                            "No admin account found with this email. Please sign up first."
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            val exMsg = task.exception?.message ?: ""
                            when {
                                exMsg.contains("password", ignoreCase = true) -> {
                                    etAdminPassword.error = "Incorrect password"
                                    "Incorrect password. Please try again."
                                }
                                else -> {
                                    etAdminEmail.error = "Invalid email or password"
                                    "Invalid email or password."
                                }
                            }
                        }
                        else -> {
                            val exMsg = task.exception?.message ?: ""
                            when {
                                exMsg.contains("network", ignoreCase = true) ->
                                    "Network error. Please check your internet connection."
                                exMsg.contains("too many requests", ignoreCase = true) ->
                                    "Too many failed attempts. Please try again later."
                                else -> "Login failed: ${task.exception?.message}"
                            }
                        }
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }

                val uid = auth.currentUser?.uid ?: run {
                    setLoading(false)
                    Toast.makeText(context, "Authentication error.", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                // ── Verify in "admins" node ──────────────────────────
                adminsRef.child(uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!isAdded) return

                            if (!snapshot.exists()) {
                                setLoading(false)
                                auth.signOut()
                                Toast.makeText(
                                    context,
                                    "No admin account found. Please sign up first.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            }

                            val savedAdminId = snapshot.child("adminId")
                                .getValue(String::class.java) ?: ""
                            val accountType  = snapshot.child("accountType")
                                .getValue(String::class.java) ?: ""

                            if (!savedAdminId.equals(adminId, ignoreCase = false)) {
                                setLoading(false)
                                auth.signOut()
                                etAdminId.error = "Incorrect Admin ID"
                                Toast.makeText(
                                    context,
                                    "Invalid Admin ID. Please check and try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            }

                            if (!accountType.equals("admin", ignoreCase = true)) {
                                setLoading(false)
                                auth.signOut()
                                Toast.makeText(
                                    context,
                                    "Access denied. This is not an admin account.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            }

                            Toast.makeText(context, "Welcome, Admin!", Toast.LENGTH_SHORT).show()
                            goToTrainerPanel()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            if (!isAdded) return
                            setLoading(false)
                            auth.signOut()
                            Toast.makeText(
                                context,
                                "Database error: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
            }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reset_password, null)
        dialogView.findViewById<TextInputEditText>(R.id.etResetEmail)
            ?.setText(etAdminEmail.text.toString().trim())

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Admin Password")
            .setView(dialogView)
            .setPositiveButton("Send Reset Link") { _, _ ->
                val email = dialogView
                    .findViewById<TextInputEditText>(R.id.etResetEmail)
                    ?.text.toString().trim()
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Enter a valid email first", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Reset link sent!", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        val msg = when {
                            it.message?.contains("no user", ignoreCase = true) == true ->
                                "No account found with this email."
                            else -> "Error: ${it.message}"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToTrainerPanel() {
        if (!isAdded || hasNavigated) return
        hasNavigated = true
        startActivity(
            Intent(requireContext(), TrainerPanelActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        requireActivity().finish()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility  = if (loading) View.VISIBLE else View.GONE
        btnAdminLogin.isEnabled = !loading
    }
}