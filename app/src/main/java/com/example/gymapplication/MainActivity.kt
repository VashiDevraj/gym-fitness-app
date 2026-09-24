package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        val logo =findViewById<ImageView>(R.id.logo)
        val ScaleAnimation = ScaleAnimation(
            0.5f, 2.0f,
            0.5f, 2.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f)
            ScaleAnimation.duration =1500
        ScaleAnimation.fillAfter =true
        logo.startAnimation(ScaleAnimation)

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this,MainActivity4::class.java)
                startActivity(intent)
                finish()
            }, 2000)

    auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser != null) {
        // User is already logged in
        // Check if admin or normal user
        val uid = currentUser.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("admins").child(uid)
        dbRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Admin user
                startActivity(Intent(this, TrainerPanelActivity::class.java))
            } else {
                // Normal user
                startActivity(Intent(this, UserDashboardActivity::class.java))
            }
            finish()
        }.addOnFailureListener {
            // Fallback to login if error
            startActivity(Intent(this, MainActivity2::class.java))
            finish()
        }

    } else {
        // No user logged in, go to login screen
        startActivity(Intent(this, MainActivity2::class.java))
        finish()
    }
}
    }

