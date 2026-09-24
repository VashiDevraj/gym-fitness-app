package com.example.gymapplication.fragments

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.gymapplication.MainActivity2
import com.example.gymapplication.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseDatabase.getInstance().reference

    // Header views
    private lateinit var textAvatar: TextView
    private lateinit var textName: TextView
    private lateinit var textEmail: TextView

    // Stats views
    private lateinit var tvStatWeight: TextView
    private lateinit var tvStatHeight: TextView
    private lateinit var tvStatAge: TextView
    private lateinit var tvStatBMI: TextView
    private lateinit var tvStatStepGoal: TextView

    // Buttons
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnEditUsername: MaterialButton
    private lateinit var btnEditGoal: MaterialButton
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var btnLogout: MaterialButton

    // Live listener
    private var userListener: ValueEventListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Bind views
        textAvatar       = view.findViewById(R.id.textAvatar)
        textName         = view.findViewById(R.id.textName)
        textEmail        = view.findViewById(R.id.textEmail)
        tvStatWeight     = view.findViewById(R.id.tvStatWeight)
        tvStatHeight     = view.findViewById(R.id.tvStatHeight)
        tvStatAge        = view.findViewById(R.id.tvStatAge)
        tvStatBMI        = view.findViewById(R.id.tvStatBMI)
        tvStatStepGoal   = view.findViewById(R.id.tvStatStepGoal)
        btnEditProfile   = view.findViewById(R.id.btnEditProfile)
        btnEditUsername  = view.findViewById(R.id.btnEditUsername)
        btnEditGoal      = view.findViewById(R.id.btnEditGoal)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout        = view.findViewById(R.id.btnLogout)

        btnEditProfile.setOnClickListener    { editHeightWeightAge() }
        btnEditUsername.setOnClickListener   { editUsername() }
        btnEditGoal.setOnClickListener       { editGoal() }
        btnChangePassword.setOnClickListener { changePassword() }
        btnLogout.setOnClickListener         { confirmLogout() }

        loadProfileLive()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val uid = auth.currentUser?.uid ?: return
        userListener?.let { db.child("users").child(uid).removeEventListener(it) }
    }

    // ── Live profile data ──────────────────────────────────
    private fun loadProfileLive() {
        val uid = auth.currentUser?.uid ?: return
        textEmail.text = auth.currentUser?.email ?: ""

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                textName.text    = username
                textAvatar.text  = username.first().uppercase()

                // basicInfo
                val bi = snapshot.child("basicInfo")
                val weight = safeDouble(bi, "weight")
                val height = safeDouble(bi, "height")
                val age    = safeInt(bi, "age")

                tvStatWeight.text = if (weight > 0) "%.1f kg".format(weight) else "—"
                tvStatHeight.text = if (height > 0) "%.0f cm".format(height) else "—"
                tvStatAge.text    = if (age > 0) "$age yrs" else "—"

                // BMI
                if (weight > 0 && height > 0) {
                    val hm = height / 100.0
                    val bmi = weight / (hm * hm)
                    tvStatBMI.text = "%.1f".format(bmi)
                } else {
                    tvStatBMI.text = "—"
                }

                // Step goal
                val goal = try {
                    snapshot.child("stepGoal").getValue(Int::class.java)
                        ?: (snapshot.child("stepGoal").getValue(Long::class.java) ?: 8000L).toInt()
                } catch (_: Exception) { 8000 }
                tvStatStepGoal.text = "$goal steps/day"
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        db.child("users").child(uid).addValueEventListener(userListener!!)
    }

    // ── Edit height / weight / age ─────────────────────────
    private fun editHeightWeightAge() {
        val uid = auth.currentUser?.uid ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etHeight = dialogView.findViewById<EditText>(R.id.etHeight)
        val etWeight = dialogView.findViewById<EditText>(R.id.etWeight)

        // Pre-fill current values
        db.child("users").child(uid).child("basicInfo").get()
            .addOnSuccessListener { snap ->
                if (!isAdded) return@addOnSuccessListener
                val h = safeDouble(snap, "height")
                val w = safeDouble(snap, "weight")
                if (h > 0) etHeight.setText("%.0f".format(h))
                if (w > 0) etWeight.setText("%.1f".format(w))
            }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Height & Weight")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val heightStr = etHeight.text.toString().trim()
                val weightStr = etWeight.text.toString().trim()

                val heightVal = heightStr.toDoubleOrNull()
                val weightVal = weightStr.toDoubleOrNull()

                if (heightVal == null || heightVal <= 0 || weightVal == null || weightVal <= 0) {
                    Toast.makeText(requireContext(), "Please enter valid values", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val hm = heightVal / 100.0
                val bmi = weightVal / (hm * hm)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val basicInfoRef = db.child("users").child(uid).child("basicInfo")
                basicInfoRef.updateChildren(mapOf(
                    "height" to heightVal,
                    "weight" to weightVal,
                    "bmi"    to bmi
                )).addOnSuccessListener {
                    // Also write to weightHistory so the chart updates
                    db.child("users").child(uid).child("weightHistory")
                        .child(today).setValue(weightVal)
                    Toast.makeText(requireContext(), "✅ Profile updated", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Edit username ──────────────────────────────────────
    private fun editUsername() {
        val uid = auth.currentUser?.uid ?: return
        val input = EditText(requireContext()).apply {
            hint      = "New username"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(40, 30, 40, 20)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Change Username")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.child("users").child(uid).child("username").setValue(newName)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "✅ Username updated", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Edit step goal ─────────────────────────────────────
    private fun editGoal() {
        val uid = auth.currentUser?.uid ?: return
        val input = EditText(requireContext()).apply {
            hint      = "Steps per day (e.g. 10000)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(40, 30, 40, 20)
        }

        // Pre-fill current goal
        db.child("users").child(uid).child("stepGoal").get()
            .addOnSuccessListener { snap ->
                if (!isAdded) return@addOnSuccessListener
                val current = try {
                    snap.getValue(Int::class.java)
                        ?: (snap.getValue(Long::class.java) ?: 0L).toInt()
                } catch (_: Exception) { 0 }
                if (current > 0) input.setText(current.toString())
            }

        AlertDialog.Builder(requireContext())
            .setTitle("Change Step Goal")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val goal = input.text.toString().trim().toIntOrNull()
                if (goal == null || goal < 100) {
                    Toast.makeText(requireContext(), "Please enter a valid step goal", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.child("users").child(uid).child("stepGoal").setValue(goal)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "✅ Step goal updated to $goal", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Change password ────────────────────────────────────
    private fun changePassword() {
        val email = auth.currentUser?.email ?: return
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "📧 Password reset email sent to $email", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send email: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Logout with confirmation ───────────────────────────
    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), MainActivity2::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Safe helpers ───────────────────────────────────────
    private fun safeDouble(snap: DataSnapshot, key: String): Double {
        val c = snap.child(key)
        return try { c.getValue(Double::class.java) ?: 0.0 }
        catch (_: Exception) {
            try { (c.getValue(Long::class.java) ?: 0L).toDouble() }
            catch (_: Exception) {
                try { c.getValue(Float::class.java)?.toDouble() ?: 0.0 }
                catch (_: Exception) { c.getValue(String::class.java)?.toDoubleOrNull() ?: 0.0 }
            }
        }
    }

    private fun safeInt(snap: DataSnapshot, key: String): Int {
        val c = snap.child(key)
        return try { c.getValue(Int::class.java) ?: 0 }
        catch (_: Exception) {
            try { (c.getValue(Long::class.java) ?: 0L).toInt() }
            catch (_: Exception) { c.getValue(String::class.java)?.toIntOrNull() ?: 0 }
        }
    }
}