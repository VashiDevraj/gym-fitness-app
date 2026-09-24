package com.example.gymapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class WorkoutDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORKOUT_ID   = "workout_id"
        const val EXTRA_WORKOUT_NAME = "workout_name"
        const val EXTRA_GIF_URL      = "gif_url"
        const val EXTRA_GIF_BASE64   = "gif_base64"
        const val EXTRA_DESCRIPTION  = "description"
        const val EXTRA_SETS         = "sets"
        const val EXTRA_REPS         = "reps"
        const val EXTRA_CALORIES     = "calories"
        const val EXTRA_DIFFICULTY   = "difficulty"
        const val EXTRA_CATEGORY     = "category"
        const val EXTRA_DURATION     = "duration"
        const val EXTRA_MUSCLES      = "muscles"
        const val EXTRA_STEPS        = "steps"
        const val EXTRA_BREATHING    = "breathing"
        const val EXTRA_POINTS       = "points"
        const val EXTRA_EQUIPMENT    = "equipment"
        const val EXTRA_GOAL         = "goal"
        const val EXTRA_EMOJI        = "emoji"
    }

    private var countdownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var timeLeftMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        // ── Views ─────────────────────────────────────────────────────────
        val imgGif           = findViewById<ImageView>(R.id.detailImgGif)
        val tvName           = findViewById<TextView>(R.id.detailTvName)
        val tvDesc           = findViewById<TextView>(R.id.detailTvDesc)
        val tvDifficulty     = findViewById<TextView>(R.id.detailTvDifficulty)
        val tvCategory       = findViewById<TextView>(R.id.detailTvCategory)
        val tvStats          = findViewById<TextView>(R.id.detailTvStats)
        val tvEquipment      = findViewById<TextView>(R.id.detailTvEquipment)
        val containerMuscles = findViewById<LinearLayout>(R.id.detailMuscleChips)
        val containerSteps   = findViewById<LinearLayout>(R.id.detailStepsContainer)
        val tvBreathing      = findViewById<TextView>(R.id.detailTvBreathing)
        val containerPoints  = findViewById<LinearLayout>(R.id.detailPointsContainer)
        val btnBack          = findViewById<ImageView>(R.id.btnDetailBack)
        val btnComplete      = findViewById<MaterialButton>(R.id.btnDetailComplete)
        val tvGoalBadge      = findViewById<TextView>(R.id.detailTvGoal)
        val gifLoadingText   = findViewById<TextView>(R.id.tvGifLoading)
        val tvTimer          = findViewById<TextView>(R.id.tvWorkoutTimer)
        val btnStartTimer    = findViewById<Button>(R.id.btnStartTimer)
        val btnResetTimer    = findViewById<Button>(R.id.btnResetTimer)
        val tvSetProgress    = findViewById<TextView>(R.id.tvSetProgress)
        val btnPrevSet       = findViewById<Button>(R.id.btnPrevSet)
        val btnNextSet       = findViewById<Button>(R.id.btnNextSet)
        val progressSets     = findViewById<ProgressBar>(R.id.progressSets)
        val tvCaloriesBig    = findViewById<TextView>(R.id.tvCaloriesBig)
        val btnShare         = findViewById<Button>(R.id.btnShareWorkout)

        // ── Read intent extras ────────────────────────────────────────────
        val workoutId  = intent.getStringExtra(EXTRA_WORKOUT_ID)   ?: ""
        val name       = intent.getStringExtra(EXTRA_WORKOUT_NAME) ?: ""
        val gifUrl     = intent.getStringExtra(EXTRA_GIF_URL)      ?: ""
        val gifBase64  = intent.getStringExtra(EXTRA_GIF_BASE64)   ?: ""
        val desc       = intent.getStringExtra(EXTRA_DESCRIPTION)  ?: ""
        val sets       = intent.getStringExtra(EXTRA_SETS)         ?: "3"
        val reps       = intent.getStringExtra(EXTRA_REPS)         ?: "12"
        val calories   = intent.getIntExtra(EXTRA_CALORIES, 0)
        val difficulty = intent.getStringExtra(EXTRA_DIFFICULTY)   ?: "Beginner"
        val category   = intent.getStringExtra(EXTRA_CATEGORY)     ?: ""
        val duration   = intent.getIntExtra(EXTRA_DURATION, 0)
        val muscles    = intent.getStringExtra(EXTRA_MUSCLES)      ?: ""
        val steps      = intent.getStringExtra(EXTRA_STEPS)        ?: ""
        val breathing  = intent.getStringExtra(EXTRA_BREATHING)    ?: ""
        val points     = intent.getStringExtra(EXTRA_POINTS)       ?: ""
        val equipment  = intent.getStringExtra(EXTRA_EQUIPMENT)    ?: "None"
        val goal       = intent.getStringExtra(EXTRA_GOAL)         ?: ""
        val emoji      = intent.getStringExtra(EXTRA_EMOJI)        ?: "🏋️"

        // ── Basic text ────────────────────────────────────────────────────
        btnBack.setOnClickListener { finish() }
        tvName.text = name
        tvDesc.text = desc
        tvStats.text = "$sets sets  ·  $reps reps  ·  ${duration}min  ·  🔥 $calories kcal"
        tvEquipment.text = "🏋️  Equipment: $equipment"
        tvCategory.text = category
        tvDifficulty.text = difficulty
        tvDifficulty.setTextColor(Color.parseColor(difficultyColor(difficulty)))
        tvGoalBadge.text = goalLabel(goal)
        tvCaloriesBig.text = "$calories\nkcal"

        // ── GIF/Image loader — supports both URL and base64 ───────────────
        val visualSrc = when {
            gifUrl.isNotBlank() -> gifUrl
            gifBase64.isNotBlank() -> gifBase64
            else -> ""
        }
        loadGif(imgGif, gifLoadingText, visualSrc, emoji)

        // ── Muscles chips ─────────────────────────────────────────────────
        buildMuscleChips(containerMuscles, muscles)

        // ── Steps ─────────────────────────────────────────────────────────
        buildSteps(containerSteps, steps)

        // ── Breathing tip ─────────────────────────────────────────────────
        tvBreathing.text = breathing.ifBlank {
            "Breathe steadily throughout. Exhale during the effort phase, inhale during the recovery phase. Never hold your breath."
        }

        // ── Points to remember ────────────────────────────────────────────
        buildPoints(containerPoints, points)

        // ── Set tracker ───────────────────────────────────────────────────
        val totalSets = sets.toIntOrNull() ?: 3
        var currentSet = 1
        progressSets.max = totalSets
        progressSets.progress = 1

        fun updateSetDisplay() {
            tvSetProgress.text = "Set $currentSet of $totalSets"
            progressSets.progress = currentSet
        }
        updateSetDisplay()

        btnNextSet.setOnClickListener {
            if (currentSet < totalSets) { currentSet++; updateSetDisplay() }
            else Toast.makeText(this, "🎉 All $totalSets sets completed!", Toast.LENGTH_SHORT).show()
        }
        btnPrevSet.setOnClickListener {
            if (currentSet > 1) { currentSet--; updateSetDisplay() }
        }

        // ── Rest timer ────────────────────────────────────────────────────
        val restSeconds = when (difficulty) { "Beginner" -> 60; "Intermediate" -> 90; else -> 120 }
        timeLeftMs = restSeconds * 1000L

        fun updateTimerDisplay(ms: Long) {
            val s = (ms / 1000).toInt()
            tvTimer.text = "%02d:%02d".format(s / 60, s % 60)
            val fraction = ms.toFloat() / (restSeconds * 1000f)
            tvTimer.setTextColor(Color.parseColor(when {
                fraction > 0.5f -> "#43A047"
                fraction > 0.25f -> "#FF9800"
                else -> "#E53935"
            }))
        }
        updateTimerDisplay(timeLeftMs)

        btnStartTimer.setOnClickListener {
            if (isTimerRunning) {
                countdownTimer?.cancel(); isTimerRunning = false
                btnStartTimer.text = "▶ Start Rest Timer"
            } else {
                isTimerRunning = true; btnStartTimer.text = "⏸ Pause"
                countdownTimer = object : CountDownTimer(timeLeftMs, 100) {
                    override fun onTick(ms: Long) { timeLeftMs = ms; updateTimerDisplay(ms) }
                    override fun onFinish() {
                        isTimerRunning = false
                        timeLeftMs = restSeconds * 1000L
                        updateTimerDisplay(timeLeftMs)
                        btnStartTimer.text = "▶ Start Rest Timer"
                        Toast.makeText(this@WorkoutDetailActivity, "✅ Rest done! Start your next set.", Toast.LENGTH_LONG).show()
                    }
                }.start()
            }
        }
        btnResetTimer.setOnClickListener {
            countdownTimer?.cancel(); isTimerRunning = false
            timeLeftMs = restSeconds * 1000L; updateTimerDisplay(timeLeftMs)
            btnStartTimer.text = "▶ Start Rest Timer"
        }

        // ── Share ─────────────────────────────────────────────────────────
        btnShare.setOnClickListener {
            val text = buildString {
                append("💪 I'm working on: $name\n")
                append("🎯 Goal: ${goalLabel(goal)}\n")
                append("📊 $sets sets × $reps reps\n")
                append("🔥 Burns ~$calories kcal\n")
                append("⚡ Difficulty: $difficulty\n")
                if (muscles.isNotBlank()) append("💪 Targets: $muscles\n")
                append("\nTracking my fitness with GymApp! 🏋️")
            }
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
                "Share workout"
            ))
        }

        // ── Complete button ───────────────────────────────────────────────
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseDatabase.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        fun setCompletedUI() {
            btnComplete.text = "✓ Already completed today"
            btnComplete.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#43A047"))
            btnComplete.isEnabled = false
        }

        if (uid != null && workoutId.isNotBlank()) {
            db.getReference("users/$uid/completedWorkouts/$today/$workoutId").get()
                .addOnSuccessListener { snap -> if (snap.exists()) setCompletedUI() }
        }

        btnComplete.setOnClickListener {
            if (uid == null) { Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            showCompletionDialog(uid, db, today, workoutId, name, calories, goal) { setCompletedUI() }
        }
    }

    // ── Completion celebration dialog ─────────────────────────────────────
    private fun showCompletionDialog(uid: String, db: FirebaseDatabase, today: String, workoutId: String, name: String, calories: Int, goal: String, onSaved: () -> Unit) {
        val dv = layoutInflater.inflate(R.layout.dialog_workout_complete, null)
        val tvCelebration = dv.findViewById<TextView>(R.id.tvCompletionMessage)
        val tvBonusTip = dv.findViewById<TextView>(R.id.tvBonusTip)
        val etNotes = dv.findViewById<EditText>(R.id.etWorkoutNotes)
        val ratingBar = dv.findViewById<RatingBar>(R.id.workoutRatingBar)

        tvCelebration.text = "🎉 Great work!\nYou just burned ~$calories kcal!"
        tvBonusTip.text = when (goal) {
            "lose_weight" -> "💡 Tip: Drink water now. Your metabolism stays elevated for 2 hours after cardio!"
            "gain_weight" -> "💡 Tip: Eat a protein-rich meal within 30 minutes for maximum muscle recovery!"
            else -> "💡 Tip: Stretch now while your muscles are warm to improve flexibility!"
        }

        AlertDialog.Builder(this)
            .setTitle("Complete Workout")
            .setView(dv)
            .setPositiveButton("Save ✓") { _, _ ->
                val rating = ratingBar.rating
                val notes = etNotes.text.toString().trim()
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val data = mutableMapOf<String, Any>(
                    "name" to name, "calories" to calories, "time" to time,
                    "rating" to rating, "goal" to goal
                )
                if (notes.isNotBlank()) data["notes"] = notes
                db.getReference("users/$uid/completedWorkouts/$today/$workoutId").setValue(data)
                    .addOnSuccessListener {
                        val statsRef = db.getReference("users/$uid/stats")
                        statsRef.get().addOnSuccessListener { snap ->
                            val prev = snap.child("totalCalories").getValue(Int::class.java) ?: 0
                            val prevW = snap.child("totalWorkouts").getValue(Int::class.java) ?: 0
                            statsRef.updateChildren(mapOf("totalCalories" to prev + calories, "totalWorkouts" to prevW + 1, "lastWorkoutDate" to today))
                        }
                        onSaved()
                        Toast.makeText(this, "🎉 $name logged! +$calories kcal", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── GIF/Image loader — supports base64 and URL ────────────────────────
    private fun loadGif(img: ImageView, loadingTv: TextView, source: String, emoji: String) {
        if (source.isBlank()) {
            loadingTv.text = emoji; loadingTv.textSize = 72f; loadingTv.visibility = View.VISIBLE
            return
        }
        loadingTv.textSize = 14f; loadingTv.text = "Loading..."; loadingTv.visibility = View.VISIBLE
        Glide.with(this)
            .load(source)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .placeholder(android.R.color.darker_gray)
            .error(android.R.color.darker_gray)
            .centerCrop()
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                    loadingTv.text = emoji; loadingTv.textSize = 72f; loadingTv.visibility = View.VISIBLE
                    return false
                }
                override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean): Boolean {
                    loadingTv.visibility = View.GONE; return false
                }
            })
            .into(img)
    }

    // ── Builder helpers ───────────────────────────────────────────────────
    private fun buildMuscleChips(container: LinearLayout, muscles: String) {
        container.removeAllViews()
        if (muscles.isBlank()) return
        for (muscle in muscles.split(",")) {
            if (muscle.isBlank()) continue
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.marginEnd = dp(8); p.bottomMargin = dp(6)
            val tv = TextView(this)
            tv.text = muscle.trim(); tv.textSize = 12f
            tv.setTextColor(Color.parseColor("#1565C0"))
            tv.setPadding(dp(12), dp(6), dp(12), dp(6))
            tv.background = makeRoundedBg("#DDEEFF", 20f)
            tv.layoutParams = p
            container.addView(tv)
        }
    }

    private fun buildSteps(container: LinearLayout, steps: String) {
        container.removeAllViews()
        if (steps.isBlank()) return
        val list = steps.split("|").filter { it.isNotBlank() }
        for (i in list.indices) {
            val rp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            rp.bottomMargin = dp(14)
            val row = LinearLayout(this); row.orientation = LinearLayout.HORIZONTAL; row.layoutParams = rp
            val bp = LinearLayout.LayoutParams(dp(32), dp(32)); bp.marginEnd = dp(14); bp.topMargin = dp(2)
            val badge = TextView(this)
            badge.text = "${i + 1}"; badge.textSize = 13f
            badge.setTypeface(null, Typeface.BOLD)
            badge.setTextColor(Color.WHITE); badge.gravity = Gravity.CENTER
            badge.background = makeCircleBg("#3949AB"); badge.layoutParams = bp
            val tp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val tv = TextView(this)
            tv.text = list[i].trim(); tv.textSize = 14f
            tv.setTextColor(Color.parseColor("#222222")); tv.setLineSpacing(4f, 1f); tv.layoutParams = tp
            row.addView(badge); row.addView(tv); container.addView(row)
        }
    }

    private fun buildPoints(container: LinearLayout, points: String) {
        container.removeAllViews()
        if (points.isBlank()) return
        for (point in points.split("|").filter { it.isNotBlank() }) {
            val rp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            rp.bottomMargin = dp(12)
            val row = LinearLayout(this); row.orientation = LinearLayout.HORIZONTAL; row.gravity = Gravity.TOP; row.layoutParams = rp
            val dp_ = LinearLayout.LayoutParams(dp(8), dp(8)); dp_.marginEnd = dp(12); dp_.topMargin = dp(8)
            val dot = View(this); dot.background = makeCircleBg("#FF9800"); dot.layoutParams = dp_
            val tp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val tv = TextView(this)
            tv.text = point.trim(); tv.textSize = 14f
            tv.setTextColor(Color.parseColor("#222222")); tv.setLineSpacing(4f, 1f); tv.layoutParams = tp
            row.addView(dot); row.addView(tv); container.addView(row)
        }
    }

    private fun difficultyColor(d: String) = when (d) { "Beginner" -> "#43A047"; "Intermediate" -> "#FF9800"; else -> "#E53935" }
    private fun goalLabel(g: String) = when (g) { "lose_weight" -> "🔥 Lose Weight"; "gain_weight" -> "💪 Gain Weight"; "maintain" -> "⚖️ Maintain"; else -> g }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun makeRoundedBg(hex: String, r: Float): GradientDrawable {
        val d = GradientDrawable(); d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = r * resources.displayMetrics.density; d.setColor(Color.parseColor(hex)); return d
    }
    private fun makeCircleBg(hex: String): GradientDrawable {
        val d = GradientDrawable(); d.shape = GradientDrawable.OVAL; d.setColor(Color.parseColor(hex)); return d
    }
    override fun onDestroy() { super.onDestroy(); countdownTimer?.cancel() }
}