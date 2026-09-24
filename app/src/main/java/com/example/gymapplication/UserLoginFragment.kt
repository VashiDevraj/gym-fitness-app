package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.*

class UserLoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var tilUserId: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etUserId: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: com.google.android.material.button.MaterialButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView
    private lateinit var progressBar: ProgressBar

    // Flag to prevent auto-login listener from firing during manual login flow
    private var isManualLoginInProgress = false
    // Flag to prevent auto-login from re-triggering after we just signed in
    private var hasNavigated = false

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_login, container, false)

        auth     = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        tilUserId   = view.findViewById(R.id.tilUserId)
        tilEmail    = view.findViewById(R.id.tilEmail)
        tilPassword = view.findViewById(R.id.tilPassword)
        etUserId    = view.findViewById(R.id.etUserId)
        etEmail     = view.findViewById(R.id.etEmail)
        etPassword  = view.findViewById(R.id.etPassword)
        btnLogin    = view.findViewById(R.id.btnLogin)
        tvForgotPassword = view.findViewById(R.id.ForgetPassword)
        tvSignUp    = view.findViewById(R.id.btnSignUp)
        progressBar = view.findViewById(R.id.progressBar)

        // ── Auto-login ONLY if user was already logged in before opening the screen ──
        // We use a one-shot check, NOT addAuthStateListener, to avoid navigation loops
        val existingUser = auth.currentUser
        if (existingUser != null) {
            // Silently route without showing the login UI
            routeLoggedInUser(existingUser.uid)
        }

        btnLogin.setOnClickListener { attemptLogin() }

        tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }

        tvSignUp.setOnClickListener {
            startActivity(
                Intent(requireContext(), MainActivity3::class.java)
                    .putExtra("accountType", "user")
            )
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }

    // ── Route user based on their profile completeness ────────────
    private fun routeLoggedInUser(uid: String) {
        if (!isAdded || hasNavigated) return
        hasNavigated = true

        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                // User node doesn't exist → send to profile setup
                if (!snapshot.exists()) {
                    startActivity(Intent(requireContext(), MainActivity4::class.java))
                    requireActivity().finish()
                    return
                }

                val accountType = snapshot.child("accountType")
                    .getValue(String::class.java) ?: "user"

                // If an admin somehow ended up on the user login tab, boot them out
                if (accountType.equals("admin", ignoreCase = true)) {
                    auth.signOut()
                    hasNavigated = false
                    setLoading(false)
                    Toast.makeText(
                        context,
                        "This is an admin account. Please use the Admin tab.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val basicInfo = snapshot.child("basicInfo")
                val height = basicInfo.safeDouble("height")
                val weight = basicInfo.safeDouble("weight")
                val age    = basicInfo.safeInt("age")

                val dest = when {
                    height <= 0 -> Intent(requireContext(), MainActivity4::class.java)
                    weight <= 0 -> Intent(requireContext(), MainActivity5::class.java)
                    age <= 0    -> Intent(requireContext(), MainActivity6::class.java)
                    else        -> Intent(requireContext(), UserDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                }
                startActivity(dest)
                requireActivity().finish()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                hasNavigated = false
                setLoading(false)
                Toast.makeText(
                    context,
                    "Database error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun attemptLogin() {
        // Clear previous errors
        tilUserId.error   = null
        tilEmail.error    = null
        tilPassword.error = null

        val userId   = etUserId.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // ── Field validation ──────────────────────────────────────
        var hasError = false

        if (userId.isEmpty()) {
            tilUserId.error = "User ID is required"
            hasError = true
        }
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            hasError = true
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            hasError = true
        }

        if (hasError) return

        setLoading(true)
        isManualLoginInProgress = true

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!isAdded) return@addOnCompleteListener

                if (!task.isSuccessful) {
                    isManualLoginInProgress = false
                    setLoading(false)

                    // ── Specific, user-friendly error messages ────────────
                    val errorMsg = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> {
                            // No account with this email exists
                            tilEmail.error = "No account found with this email"
                            "No account found with this email. Please sign up first."
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            // Wrong password OR malformed email
                            val exMsg = task.exception?.message ?: ""
                            when {
                                exMsg.contains("password", ignoreCase = true) -> {
                                    tilPassword.error = "Incorrect password"
                                    "Incorrect password. Please try again."
                                }
                                exMsg.contains("email", ignoreCase = true) -> {
                                    tilEmail.error = "Invalid email format"
                                    "Invalid email address."
                                }
                                else -> {
                                    tilPassword.error = "Invalid credentials"
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
                                exMsg.contains("no user", ignoreCase = true) ||
                                        exMsg.contains("identifier", ignoreCase = true) -> {
                                    tilEmail.error = "No account found with this email"
                                    "No account found. Please sign up first."
                                }
                                else -> "Login failed: ${task.exception?.message}"
                            }
                        }
                    }

                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }

                val uid = auth.currentUser?.uid
                if (uid == null) {
                    isManualLoginInProgress = false
                    setLoading(false)
                    Toast.makeText(context, "Authentication error. Please try again.", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                // ── Verify the entered User ID matches what's stored in DB ──
                database.child(uid).child("username")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!isAdded) return

                            val storedUsername = snapshot.getValue(String::class.java) ?: ""

                            if (storedUsername.isEmpty()) {
                                // Username not set yet — might be a partial registration
                                // Still allow login and send to profile setup
                                isManualLoginInProgress = false
                                routeLoggedInUser(uid)
                                return
                            }

                            if (!storedUsername.equals(userId, ignoreCase = true)) {
                                // User ID does not match this account
                                isManualLoginInProgress = false
                                setLoading(false)
                                auth.signOut()
                                tilUserId.error = "Incorrect User ID for this account"
                                Toast.makeText(
                                    context,
                                    "User ID does not match this account. Please check and try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            }

                            // All checks passed — route to the correct screen
                            isManualLoginInProgress = false
                            routeLoggedInUser(uid)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            if (!isAdded) return
                            isManualLoginInProgress = false
                            setLoading(false)
                            Toast.makeText(context, "DB error: ${error.message}", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                        }
                    })
            }
    }

    private fun showForgotPasswordDialog() {
        val emailInput = etEmail.text.toString().trim()
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reset_password, null)
        dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etResetEmail
        )?.setText(emailInput)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Send Reset Link") { _, _ ->
                val email = dialogView
                    .findViewById<com.google.android.material.textfield.TextInputEditText>(
                        R.id.etResetEmail
                    )?.text.toString().trim()

                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Enter a valid email first", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Reset link sent to $email", Toast.LENGTH_LONG).show()
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

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled     = !loading
    }

    // ── Safe Firebase read helpers ────────────────────────────────
    private fun DataSnapshot.safeDouble(key: String): Double {
        val c = child(key)
        return try { c.getValue(Double::class.java) ?: 0.0 }
        catch (_: Exception) {
            try { (c.getValue(Long::class.java) ?: 0L).toDouble() }
            catch (_: Exception) {
                try { c.getValue(Float::class.java)?.toDouble() ?: 0.0 }
                catch (_: Exception) { c.getValue(String::class.java)?.toDoubleOrNull() ?: 0.0 }
            }
        }
    }

    private fun DataSnapshot.safeInt(key: String): Int {
        val c = child(key)
        return try { c.getValue(Int::class.java) ?: 0 }
        catch (_: Exception) {
            try { (c.getValue(Long::class.java) ?: 0L).toInt() }
            catch (_: Exception) { c.getValue(String::class.java)?.toIntOrNull() ?: 0 }
        }
    }
}