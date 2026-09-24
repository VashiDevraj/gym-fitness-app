package com.example.gymapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    // ── Views ─────────────────────────────────────────────
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvStreak: TextView
    private lateinit var stepsCard: StepsCardView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvActiveMinutes: TextView
    private lateinit var tvBMI: TextView
    private lateinit var tvBMIStatus: TextView
    private lateinit var tvWater: TextView
    private lateinit var tvWaterGoal: TextView
    private lateinit var waterProgress: ProgressBar
    private lateinit var btnAddWater: MaterialButton
    private lateinit var tvDietCalories: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView
    private lateinit var tvBreakfast: TextView
    private lateinit var tvLunch: TextView
    private lateinit var tvDinner: TextView
    private lateinit var tvSnacks: TextView
    private lateinit var layoutMeals: android.widget.LinearLayout
    private lateinit var tvNoDietPlan: TextView
    private lateinit var tvDietNotes: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvHeight: TextView
    private lateinit var tvAge: TextView
    private lateinit var btnViewWeightChart: MaterialButton
    private lateinit var textMotivation: TextView
    private lateinit var cardWorkout: androidx.cardview.widget.CardView
    private lateinit var cardCheckin: androidx.cardview.widget.CardView
    private lateinit var cardDiet: androidx.cardview.widget.CardView

    // ── State ─────────────────────────────────────────────
    private var stepGoal = 8000
    private var waterMl = 0
    private val waterGoalMl = 2500
    private var cardOwnerUid: String? = null

    /**
     * PER-USER STEP BASELINE — isolates today's steps per user on a shared device.
     *
     * Health Connect reads the phone's hardware pedometer, counting ALL steps since
     * midnight regardless of which app-user is logged in. There is no way to reset it.
     *
     * ── TODAY fix ────────────────────────────────────────────────────────────────
     * On a user's first login of the day, snapshot the current raw sensor value as
     * their "baseline" and save it in Firebase under:
     *     users/{uid}/stepBaseline/{yyyy-MM-dd}
     * Displayed today steps = rawSensor − baseline  (floored at 0).
     *
     * ── YESTERDAY fix (key change in this version) ───────────────────────────────
     * NEVER read yesterday from the raw HC sensor.
     * The sensor window for yesterday (prev-midnight → today-midnight) includes every
     * step taken on this phone by every user. It has no per-user isolation.
     *
     * Instead: yesterday is read EXCLUSIVELY from this user's own Firebase node
     *     users/{uid}/steps/{yyyy-MM-dd}/count
     * If no node exists (brand-new user, or user not logged in yesterday), we show 0.
     * That is the correct and safe behaviour — no bleed-over from other users.
     *
     * ── Scenario ─────────────────────────────────────────────────────────────────
     * User A logs in at raw=0, walks 2000 steps, logs out  → raw=2000
     * User B logs in → baseline=2000; B walks 500 → raw=2500 → B sees 500 ✓
     *   B's yesterday = Firebase[B/steps/yesterday] = 0 (new user) ✓
     * User A logs back in → baseline reloaded from Firebase = 0
     *   A sees raw−0 = 2000 ✓; A's yesterday = Firebase[A/steps/yesterday] ✓
     */
    private var todayStepBaseline = 0
    private var baselineLoaded    = false

    // ── Live listeners ────────────────────────────────────
    private var dietListener: ValueEventListener? = null
    private var basicInfoListener: ValueEventListener? = null

    private val db   by lazy { FirebaseDatabase.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val uid  get() = auth.currentUser?.uid

    // ── Safe Firebase helpers ─────────────────────────────
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

    private fun safeIntNode(snap: DataSnapshot, key: String): Int {
        val c = snap.child(key)
        return try { c.getValue(Int::class.java) ?: 0 }
        catch (_: Exception) {
            try { (c.getValue(Long::class.java) ?: 0L).toInt() }
            catch (_: Exception) { c.getValue(String::class.java)?.toIntOrNull() ?: 0 }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupDate()
        setupMotivation()
        setGreeting()

        forceResetCardForCurrentUser()
        initUserStepData()

        loadUserDataLive()
        loadDietPlan()
        loadWaterIntake()
        loadStreak()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        val currentUid = uid
        if (currentUid != null && currentUid != cardOwnerUid) {
            // Different user switched in — full reset
            baselineLoaded    = false
            todayStepBaseline = 0
            forceResetCardForCurrentUser()
            initUserStepData()
            stepGoal = 8000
        } else {
            // Same user resuming — refresh live data only, baseline already set
            loadGoalThenFitData()
            loadWaterIntake()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val userId = uid ?: return
        dietListener?.let      { db.getReference("users/$userId/dietPlan").removeEventListener(it) }
        basicInfoListener?.let { db.getReference("users/$userId/basicInfo").removeEventListener(it) }
    }

    // ── Hard-zero card for current user ───────────────────
    private fun forceResetCardForCurrentUser() {
        cardOwnerUid = uid
        stepsCard.todayData = StepsCardView.DayData(
            steps = 0, goal = stepGoal, label = "Today",
            calories = 0, activeMinutes = 0
        )
        stepsCard.yesterdayData = StepsCardView.DayData(
            steps = 0, goal = stepGoal, label = "Yesterday",
            calories = 0, activeMinutes = 0
        )
        if (isAdded) {
            tvLastUpdated.text   = "⏳ Loading your steps..."
            tvCalories.text      = "0"
            tvActiveMinutes.text = "0"
        }
    }

    /**
     * Full init for a user:
     * 1. Load their saved baseline from Firebase (or record a new one from sensor)
     * 2. Immediately show their own cached Firebase steps (yesterday = 0 for new user)
     * 3. Kick off a fresh HC read for today only
     */
    private fun initUserStepData() {
        val userId      = uid ?: return
        val capturedUid = userId
        val today       = dateStr(0)

        db.getReference("users/$userId/stepBaseline/$today").get()
            .addOnSuccessListener { snap ->
                if (!isAdded) return@addOnSuccessListener
                if (uid != capturedUid || cardOwnerUid != capturedUid) return@addOnSuccessListener

                if (snap.exists()) {
                    todayStepBaseline = try {
                        snap.getValue(Int::class.java)
                            ?: (snap.getValue(Long::class.java) ?: 0L).toInt()
                    } catch (_: Exception) { 0 }
                    baselineLoaded = true
                    Log.d("HomeFragment", "Loaded baseline for $capturedUid: $todayStepBaseline")
                    loadUserSavedStepsFromFirebase(capturedUid)
                    loadGoalThenFitData()
                } else {
                    // First login today for this user — snapshot the sensor right now
                    recordBaselineForUser(capturedUid)
                }
            }
            .addOnFailureListener {
                baselineLoaded = true
                loadUserSavedStepsFromFirebase(capturedUid)
                loadGoalThenFitData()
            }
    }

    /**
     * Reads the raw sensor NOW and saves it as this user's today-baseline.
     * Steps registered before this moment (by other users or before login) are excluded.
     */
    private fun recordBaselineForUser(capturedUid: String) {
        val ctx = context ?: run {
            baselineLoaded = true
            loadGoalThenFitData()
            return
        }

        if (!HealthConnectManager.isAvailable(ctx)) {
            todayStepBaseline = 0
            baselineLoaded    = true
            db.getReference("users/$capturedUid/stepBaseline/${dateStr(0)}").setValue(0)
            loadUserSavedStepsFromFirebase(capturedUid)
            loadGoalThenFitData()
            return
        }

        lifecycleScope.launch {
            try {
                val rawSteps = if (HealthConnectManager.hasAllPermissions(ctx)) {
                    HealthConnectManager.getRawTodaySteps(ctx).first
                } else { 0 }

                if (uid != capturedUid || cardOwnerUid != capturedUid) return@launch

                todayStepBaseline = rawSteps
                baselineLoaded    = true
                Log.d("HomeFragment", "Recorded new baseline for $capturedUid: $rawSteps")

                db.getReference("users/$capturedUid/stepBaseline/${dateStr(0)}")
                    .setValue(rawSteps)

                loadUserSavedStepsFromFirebase(capturedUid)
                loadGoalThenFitData()

            } catch (e: Exception) {
                Log.e("HomeFragment", "recordBaselineForUser error: ${e.message}")
                todayStepBaseline = 0
                baselineLoaded    = true
                loadUserSavedStepsFromFirebase(capturedUid)
                loadGoalThenFitData()
            }
        }
    }

    /**
     * Shows this user's own Firebase-stored steps immediately (before HC finishes).
     *
     * Yesterday comes 100% from Firebase — never from the HC sensor.
     * New user with no Firebase history → yesterday = 0. Correct by design.
     */
    private fun loadUserSavedStepsFromFirebase(capturedUid: String) {
        val today = dateStr(0)
        val yest  = dateStr(-1)

        db.getReference("users/$capturedUid/steps").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                if (uid != capturedUid || cardOwnerUid != capturedUid) return@addOnSuccessListener

                val todaySteps = safeIntNode(snapshot.child(today), "count")
                val todayMins  = safeIntNode(snapshot.child(today), "activeMinutes")
                val todayCals  = safeIntNode(snapshot.child(today), "calories")

                // ONLY use this user's own Firebase data for yesterday — never the raw sensor
                val yesterdaySteps = safeIntNode(snapshot.child(yest), "count")
                val yesterdayMins  = safeIntNode(snapshot.child(yest), "activeMinutes")
                val yesterdayCals  = safeIntNode(snapshot.child(yest), "calories")

                stepsCard.todayData = StepsCardView.DayData(
                    steps = todaySteps, goal = stepGoal, label = "Today",
                    calories = todayCals, activeMinutes = todayMins
                )
                stepsCard.yesterdayData = StepsCardView.DayData(
                    steps = yesterdaySteps,   // 0 for brand-new user ✓
                    goal = stepGoal, label = "Yesterday",
                    calories = yesterdayCals, activeMinutes = yesterdayMins
                )

                if (todaySteps > 0) {
                    tvCalories.text      = "$todayCals"
                    tvActiveMinutes.text = "$todayMins"
                    if (isAdded) {
                        tvLastUpdated.text = "📱 Cached — tap to refresh"
                        tvLastUpdated.setOnClickListener { loadFitData() }
                    }
                }
            }
    }

    private fun bindViews(v: View) {
        tvGreeting         = v.findViewById(R.id.tvGreeting)
        tvDate             = v.findViewById(R.id.tvDate)
        tvStreak           = v.findViewById(R.id.tvStreak)
        stepsCard          = v.findViewById(R.id.stepsCard)
        tvLastUpdated      = v.findViewById(R.id.tvLastUpdated)
        tvCalories         = v.findViewById(R.id.tvCalories)
        tvActiveMinutes    = v.findViewById(R.id.tvActiveMinutes)
        tvBMI              = v.findViewById(R.id.tvBMI)
        tvBMIStatus        = v.findViewById(R.id.tvBMIStatus)
        tvWater            = v.findViewById(R.id.tvWater)
        tvWaterGoal        = v.findViewById(R.id.tvWaterGoal)
        waterProgress      = v.findViewById(R.id.waterProgress)
        btnAddWater        = v.findViewById(R.id.btnAddWater)
        tvDietCalories     = v.findViewById(R.id.tvDietCalories)
        tvProtein          = v.findViewById(R.id.tvProtein)
        tvCarbs            = v.findViewById(R.id.tvCarbs)
        tvFats             = v.findViewById(R.id.tvFats)
        tvBreakfast        = v.findViewById(R.id.tvBreakfast)
        tvLunch            = v.findViewById(R.id.tvLunch)
        tvDinner           = v.findViewById(R.id.tvDinner)
        tvSnacks           = v.findViewById(R.id.tvSnacks)
        layoutMeals        = v.findViewById(R.id.layoutMeals)
        tvNoDietPlan       = v.findViewById(R.id.tvNoDietPlan)
        tvDietNotes        = v.findViewById(R.id.tvDietNotes)
        tvWeight           = v.findViewById(R.id.tvWeight)
        tvHeight           = v.findViewById(R.id.tvHeight)
        tvAge              = v.findViewById(R.id.tvAge)
        btnViewWeightChart = v.findViewById(R.id.btnViewWeightChart)
        textMotivation     = v.findViewById(R.id.textMotivation)
        cardWorkout        = v.findViewById(R.id.cardWorkout)
        cardCheckin        = v.findViewById(R.id.cardCheckin)
        cardDiet           = v.findViewById(R.id.cardDiet)
    }

    private fun setupDate() {
        tvDate.text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
    }

    private fun setupMotivation() {
        val quotes = listOf(
            "\"The only bad workout is the one that didn't happen.\"",
            "\"Push yourself because no one else is going to do it for you.\"",
            "\"Your body can stand almost anything. It's your mind you have to convince.\"",
            "\"Success starts with self-discipline.\"",
            "\"Every step forward is a step away from where you used to be.\"",
            "\"Small daily improvements are the key to staggering long-term results.\"",
            "\"Believe in yourself and all that you are.\"",
            "\"Don't limit your challenges — challenge your limits.\""
        )
        textMotivation.text =
            quotes[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % quotes.size]
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val base = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else      -> "Good evening"
        }
        val userId = uid ?: run { tvGreeting.text = "$base 👋"; return }
        db.getReference("users/$userId/username").get()
            .addOnSuccessListener { snap ->
                if (!isAdded) return@addOnSuccessListener
                tvGreeting.text =
                    "$base, ${snap.getValue(String::class.java) ?: "there"} 👋"
            }
            .addOnFailureListener { if (isAdded) tvGreeting.text = "$base 👋" }
    }

    // ── Step goal → then Health Connect ───────────────────
    private fun loadGoalThenFitData() {
        val userId      = uid ?: run { loadFitData(); return }
        val capturedUid = userId

        db.getReference("users/$userId/stepGoal").get()
            .addOnSuccessListener { snap ->
                if (!isAdded) return@addOnSuccessListener
                if (uid != capturedUid) return@addOnSuccessListener

                stepGoal = try {
                    snap.getValue(Int::class.java)
                        ?: (snap.getValue(Long::class.java) ?: 8000L).toInt()
                } catch (_: Exception) {
                    snap.getValue(String::class.java)?.toIntOrNull() ?: 8000
                }

                if (!snap.exists()) {
                    db.getReference("users/$userId/stepGoal").setValue(8000)
                    stepGoal = 8000
                }

                stepsCard.todayData     = stepsCard.todayData.copy(goal = stepGoal)
                stepsCard.yesterdayData = stepsCard.yesterdayData.copy(goal = stepGoal)

                loadFitData()
                loadStreak()
            }
            .addOnFailureListener { if (isAdded) loadFitData() }
    }

    // ── Health Connect sync ───────────────────────────────
    /**
     * Reads TODAY's steps from HC sensor, applies the per-user baseline.
     *
     * Yesterday is NOT read from HC here. It is loaded from Firebase only
     * (inside the addOnSuccessListener below), so a new user always sees 0
     * and an existing user always sees their own personal data.
     */
    fun loadFitData() {
        val ctx         = context ?: return
        val userId      = uid ?: return
        val capturedUid = userId

        if (cardOwnerUid != capturedUid) forceResetCardForCurrentUser()

        if (!HealthConnectManager.isAvailable(ctx)) {
            if (isAdded && cardOwnerUid == capturedUid) {
                stepsCard.todayData = StepsCardView.DayData(
                    steps = 0, goal = stepGoal, calories = 0, activeMinutes = 0
                )
                tvLastUpdated.text = "📲 Install Health Connect to track steps"
            }
            return
        }

        if (isAdded) tvLastUpdated.text = "⏳ Syncing steps..."

        lifecycleScope.launch {
            try {
                val hasPerms = HealthConnectManager.hasAllPermissions(ctx)
                if (!hasPerms) {
                    if (!isAdded) return@launch
                    if (uid != capturedUid || cardOwnerUid != capturedUid) return@launch
                    tvLastUpdated.text = "🔒 Tap to grant step permission"
                    tvLastUpdated.setOnClickListener {
                        (activity as? UserDashboardActivity)?.requestPermissionsFromFragment()
                    }
                    return@launch
                }

                // Read raw sensor for TODAY only — yesterday never comes from HC
                val (rawTodaySteps, _) = HealthConnectManager.getRawTodaySteps(ctx)

                if (!isAdded) return@launch
                if (uid != capturedUid) {
                    Log.w("HomeFragment", "User changed mid-HC-load; discarding for $capturedUid")
                    return@launch
                }
                if (cardOwnerUid != capturedUid) {
                    Log.w("HomeFragment", "Card owner mismatch; discarding HC result")
                    return@launch
                }

                // Subtract this user's login-time baseline → user-specific step count
                val adjustedTodaySteps = (rawTodaySteps - todayStepBaseline).coerceAtLeast(0)
                val adjustedTodayMins  = adjustedTodaySteps / 100
                val todayCals          = (adjustedTodaySteps * 0.04).toInt()

                Log.d("HomeFragment",
                    "uid=$capturedUid raw=$rawTodaySteps baseline=$todayStepBaseline adjusted=$adjustedTodaySteps")

                // ── Yesterday: read from THIS user's Firebase only ────────────
                // New user → node doesn't exist → safeIntNode returns 0 ✓
                // Existing user → sees their own saved data ✓
                // Other users' sensor data never reaches here ✓
                val yestKey = dateStr(-1)
                db.getReference("users/$capturedUid/steps/$yestKey").get()
                    .addOnSuccessListener { yestSnap ->
                        if (!isAdded) return@addOnSuccessListener
                        if (uid != capturedUid || cardOwnerUid != capturedUid) return@addOnSuccessListener

                        val yesterdaySteps = safeIntNode(yestSnap, "count")
                        val yesterdayMins  = safeIntNode(yestSnap, "activeMinutes")
                        val yesterdayCals  = safeIntNode(yestSnap, "calories")

                        stepsCard.todayData = StepsCardView.DayData(
                            steps = adjustedTodaySteps, goal = stepGoal, label = "Today",
                            calories = todayCals, activeMinutes = adjustedTodayMins
                        )
                        stepsCard.yesterdayData = StepsCardView.DayData(
                            steps = yesterdaySteps,   // 0 for new user ✓
                            goal = stepGoal, label = "Yesterday",
                            calories = yesterdayCals, activeMinutes = yesterdayMins
                        )

                        tvCalories.text      = "$todayCals"
                        tvActiveMinutes.text = "$adjustedTodayMins"

                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        tvLastUpdated.text = "🔄 Updated: ${sdf.format(Date())} — tap to refresh"
                        tvLastUpdated.setOnClickListener { loadFitData() }
                    }

                // Save today's adjusted steps under this user's own Firebase path
                val todayKey = dateStr(0)
                db.getReference("users/$capturedUid/steps").apply {
                    child(todayKey).child("count").setValue(adjustedTodaySteps)
                    child(todayKey).child("activeMinutes").setValue(adjustedTodayMins)
                    child(todayKey).child("calories").setValue(todayCals)
                }

                loadStreak()

            } catch (e: Exception) {
                Log.e("HomeFragment", "loadFitData error: ${e.message}", e)
                if (isAdded && uid == capturedUid) {
                    tvLastUpdated.text = "⚠️ Error loading steps — tap to retry"
                    tvLastUpdated.setOnClickListener { loadFitData() }
                }
            }
        }
        stepsCard.refreshToday()
    }

    // ── User body stats (LIVE) ────────────────────────────
    private fun loadUserDataLive() {
        val userId = uid ?: return
        val ref    = db.getReference("users/$userId/basicInfo")

        basicInfoListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (!isAdded) return
                val weight = snap.safeDouble("weight")
                val height = snap.safeDouble("height")
                val age    = snap.safeInt("age")
                val bmi    = if (weight > 0 && height > 0) {
                    val hm = height / 100.0; weight / (hm * hm)
                } else snap.safeDouble("bmi")

                tvWeight.text = if (weight > 0) "%.1f kg".format(weight) else "— kg"
                tvHeight.text = if (height > 0) "%.0f cm".format(height) else "— cm"
                tvAge.text    = if (age > 0) "$age yr" else "— yr"

                if (bmi > 0) {
                    tvBMI.text = "%.1f".format(bmi)
                    val (label, color) = when {
                        bmi < 18.5 -> "Underweight" to Color.parseColor("#FF9800")
                        bmi < 25.0 -> "Normal ✓"    to Color.parseColor("#43A047")
                        bmi < 30.0 -> "Overweight"  to Color.parseColor("#E53935")
                        else       -> "Obese"        to Color.parseColor("#B71C1C")
                    }
                    tvBMIStatus.text = label
                    tvBMI.setTextColor(color)
                    tvBMIStatus.setTextColor(color)
                } else {
                    tvBMI.text = "—"; tvBMIStatus.text = "BMI"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(basicInfoListener!!)
    }

    // ── Diet (live) ───────────────────────────────────────
    private fun loadDietPlan() {
        val userId = uid ?: return
        val ref    = db.getReference("users/$userId/dietPlan")

        dietListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                val plan = if (snapshot.exists()) DietPlan.fromAny(snapshot.value) else null

                if (plan == null || plan.calories == 0) {
                    tvDietCalories.text     = "—"
                    tvProtein.text          = "— g"
                    tvCarbs.text            = "— g"
                    tvFats.text             = "— g"
                    layoutMeals.visibility  = View.GONE
                    tvNoDietPlan.visibility = View.VISIBLE
                    tvDietNotes.visibility  = View.GONE
                    return
                }

                tvNoDietPlan.visibility = View.GONE
                tvDietCalories.text     = "${plan.calories} kcal/day"
                tvProtein.text          = "${plan.protein} g"
                tvCarbs.text            = "${plan.carbs} g"
                tvFats.text             = "${plan.fats} g"

                val hasMeals = plan.breakfast.isNotBlank() || plan.lunch.isNotBlank()
                        || plan.dinner.isNotBlank() || plan.snacks.isNotBlank()
                if (hasMeals) {
                    layoutMeals.visibility = View.VISIBLE
                    tvBreakfast.text = plan.breakfast.ifBlank { "—" }
                    tvLunch.text     = plan.lunch.ifBlank { "—" }
                    tvDinner.text    = plan.dinner.ifBlank { "—" }
                    tvSnacks.text    = plan.snacks.ifBlank { "—" }
                } else {
                    layoutMeals.visibility = View.GONE
                }

                if (plan.notes.isNotBlank()) {
                    tvDietNotes.text       = plan.notes
                    tvDietNotes.visibility = View.VISIBLE
                } else {
                    tvDietNotes.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(dietListener!!)
    }

    // ── Water ─────────────────────────────────────────────
    private fun loadWaterIntake() {
        val userId = uid ?: return
        db.getReference("users/$userId/water/${dateStr(0)}").get()
            .addOnSuccessListener { snap ->
                if (!isAdded) return@addOnSuccessListener
                waterMl = try {
                    snap.getValue(Int::class.java)
                        ?: (snap.getValue(Long::class.java) ?: 0L).toInt()
                } catch (_: Exception) { 0 }
                updateWaterUI()
            }
    }

    private fun addWater() {
        waterMl += 250
        updateWaterUI()
        db.getReference("users/${uid ?: return}/water/${dateStr(0)}").setValue(waterMl)
        if (waterMl >= waterGoalMl)
            Toast.makeText(context, "🎉 Daily water goal reached!", Toast.LENGTH_SHORT).show()
    }

    private fun updateWaterUI() {
        tvWater.text     = "$waterMl / $waterGoalMl ml"
        tvWaterGoal.text = "Goal: $waterGoalMl ml"
        waterProgress.progress =
            ((waterMl.toFloat() / waterGoalMl) * 100).toInt().coerceIn(0, 100)
    }

    // ── Streak ────────────────────────────────────────────
    private fun loadStreak() {
        val userId      = uid ?: return
        val capturedUid = userId

        db.getReference("users/$userId/stepGoal").get()
            .addOnSuccessListener { goalSnap ->
                if (!isAdded) return@addOnSuccessListener
                if (uid != capturedUid) return@addOnSuccessListener

                val currentGoal = try {
                    goalSnap.getValue(Int::class.java)
                        ?: (goalSnap.getValue(Long::class.java) ?: 8000L).toInt()
                } catch (_: Exception) {
                    goalSnap.getValue(String::class.java)?.toIntOrNull() ?: stepGoal
                }
                stepGoal = currentGoal

                db.getReference("users/$userId/steps").get()
                    .addOnSuccessListener { snapshot ->
                        if (!isAdded) return@addOnSuccessListener
                        if (uid != capturedUid) return@addOnSuccessListener

                        if (!snapshot.exists()) {
                            tvStreak.text = "0 day streak"; return@addOnSuccessListener
                        }

                        var streak = 0
                        val cal    = Calendar.getInstance()
                        val sdf    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        for (i in 0..60) {
                            val key   = sdf.format(cal.time)
                            val steps = readStepCount(snapshot, key)
                            when {
                                i == 0 && steps < currentGoal ->
                                    cal.add(Calendar.DAY_OF_YEAR, -1)
                                steps >= currentGoal -> {
                                    streak++; cal.add(Calendar.DAY_OF_YEAR, -1)
                                }
                                else -> break
                            }
                        }
                        tvStreak.text = "$streak day streak"
                    }
            }
            .addOnFailureListener { computeStreakWithGoal(stepGoal) }
    }

    private fun computeStreakWithGoal(goal: Int) {
        val userId      = uid ?: return
        val capturedUid = userId
        db.getReference("users/$userId/steps").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                if (uid != capturedUid) return@addOnSuccessListener
                if (!snapshot.exists()) {
                    tvStreak.text = "0 day streak"; return@addOnSuccessListener
                }
                var streak = 0
                val cal    = Calendar.getInstance()
                val sdf    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                for (i in 0..60) {
                    val key   = sdf.format(cal.time)
                    val steps = readStepCount(snapshot, key)
                    when {
                        i == 0 && steps < goal -> cal.add(Calendar.DAY_OF_YEAR, -1)
                        steps >= goal          -> { streak++; cal.add(Calendar.DAY_OF_YEAR, -1) }
                        else                   -> break
                    }
                }
                tvStreak.text = "$streak day streak"
            }
    }

    private fun readStepCount(snapshot: DataSnapshot, key: String): Int {
        return try {
            snapshot.child(key).child("count").getValue(Int::class.java)
                ?: (snapshot.child(key).child("count").getValue(Long::class.java) ?: 0L).toInt()
        } catch (_: Exception) { 0 }
    }

    // ── Listeners ─────────────────────────────────────────
    private fun setupListeners() {
        btnAddWater.setOnClickListener { addWater() }
        btnViewWeightChart.setOnClickListener {
            startActivity(Intent(requireContext(), WeightStatsActivity::class.java))
        }
        stepsCard.onGoalTap = {
            startActivity(Intent(requireContext(), StepChartActivity::class.java))
        }
        cardWorkout.setOnClickListener {
            (activity as? UserDashboardActivity)?.viewPager?.currentItem = 2
        }
        cardCheckin.setOnClickListener {
            (activity as? UserDashboardActivity)?.viewPager?.currentItem = 1
        }
        cardDiet.setOnClickListener {
            Toast.makeText(context, "Diet plan shown above ☝️", Toast.LENGTH_SHORT).show()
        }
        tvLastUpdated.setOnClickListener { loadFitData() }
    }

    // ── Helpers ───────────────────────────────────────────
    private fun dateStr(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
}