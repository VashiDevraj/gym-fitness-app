package com.example.gymapplication

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class WorkoutFragment : Fragment() {

    private lateinit var tabLose: TextView
    private lateinit var tabMaintain: TextView
    private lateinit var tabGain: TextView
    private lateinit var rvWorkouts: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTodayCount: TextView
    private lateinit var tvTodayCalories: TextView
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvWeeklyTotal: TextView
    private lateinit var tvStreakDays: TextView
    private lateinit var tvSearchHint: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView

    private var currentGoal = "lose_weight"
    private var currentCategory = "All"
    private var searchQuery = ""
    private val allWorkouts = mutableListOf<WorkoutItem>()
    private val filteredWorkouts = mutableListOf<WorkoutItem>()
    private val completedToday = mutableSetOf<String>()

    private val db = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_workout_user, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLose = view.findViewById(R.id.tabLoseWeight)
        tabMaintain = view.findViewById(R.id.tabMaintain)
        tabGain = view.findViewById(R.id.tabGainWeight)
        rvWorkouts = view.findViewById(R.id.rvWorkouts)
        tvEmpty = view.findViewById(R.id.tvWorkoutEmpty)
        progressBar = view.findViewById(R.id.progressWorkout)
        tvTodayCount = view.findViewById(R.id.tvTodayCount)
        tvTodayCalories = view.findViewById(R.id.tvTodayCalories)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        tvWeeklyTotal = view.findViewById(R.id.tvWeeklyTotal)
        tvStreakDays = view.findViewById(R.id.tvStreakDays)
        etSearch = view.findViewById(R.id.etWorkoutSearch)
        btnClearSearch = view.findViewById(R.id.btnClearSearch)

        rvWorkouts.layoutManager = LinearLayoutManager(requireContext())
        rvWorkouts.setHasFixedSize(false)

        setupSearch()
        setupCategorySpinner()
        loadUserGoalAndSetup()
        loadCompletedToday()
        loadWeeklyStats()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            searchQuery = ""
            btnClearSearch.visibility = View.GONE
        }
    }

    private fun loadUserGoalAndSetup() {
        val userId = uid
        if (userId != null) {
            db.getReference("users/$userId/basicInfo/fitnessGoal")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        currentGoal = snap.getValue(String::class.java) ?: "lose_weight"
                        setupGoalTabs()
                        loadWorkouts()
                    }
                    override fun onCancelled(e: DatabaseError) { setupGoalTabs(); loadWorkouts() }
                })
        } else {
            setupGoalTabs()
            loadWorkouts()
        }
    }

    private fun setupCategorySpinner() {
        val cats = listOf("All", "Cardio", "Chest", "Back", "Legs", "Abs", "Arms", "Shoulders")
        val a = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cats)
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = a
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentCategory = cats[pos]; applyFilters()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupGoalTabs() {
        when (currentGoal) {
            "lose_weight" -> selectTab(tabLose)
            "gain_weight" -> selectTab(tabGain)
            else -> selectTab(tabMaintain)
        }
        tabLose.setOnClickListener { currentGoal = "lose_weight"; selectTab(tabLose); loadWorkouts() }
        tabMaintain.setOnClickListener { currentGoal = "maintain"; selectTab(tabMaintain); loadWorkouts() }
        tabGain.setOnClickListener { currentGoal = "gain_weight"; selectTab(tabGain); loadWorkouts() }
    }

    private fun selectTab(selected: TextView) {
        listOf(tabLose, tabMaintain, tabGain).forEach { tab ->
            tab.setBackgroundColor(Color.TRANSPARENT)
            tab.setTextColor(Color.parseColor("#888888"))
        }
        selected.setBackgroundResource(R.drawable.tab_selected_bg)
        selected.setTextColor(Color.WHITE)
    }

    private fun loadCompletedToday() {
        val userId = uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.getReference("users/$userId/completedWorkouts/$today")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (!isAdded) return
                    completedToday.clear()
                    var cals = 0
                    for (child in snap.children) {
                        child.key?.let { completedToday.add(it) }
                        cals += child.child("calories").getValue(Int::class.java) ?: 0
                    }
                    tvTodayCount.text = completedToday.size.toString()
                    tvTodayCalories.text = "$cals kcal"
                    buildAdapter()
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun loadWeeklyStats() {
        val userId = uid ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        var weeklyCalories = 0
        var activeDays = 0
        var checked = 0
        for (i in 0..6) {
            cal.time = Date(); cal.add(Calendar.DAY_OF_YEAR, -i)
            val day = sdf.format(cal.time)
            db.getReference("users/$userId/completedWorkouts/$day")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        if (!isAdded) return
                        if (snap.hasChildren()) {
                            activeDays++
                            for (child in snap.children)
                                weeklyCalories += child.child("calories").getValue(Int::class.java) ?: 0
                        }
                        if (++checked == 7) {
                            tvWeeklyTotal.text = "$weeklyCalories kcal\nthis week"
                            tvStreakDays.text = "$activeDays days\nactive"
                        }
                    }
                    override fun onCancelled(e: DatabaseError) { checked++ }
                })
        }
    }

    private fun loadWorkouts() {
        if (!isAdded) return
        progressBar.visibility = View.VISIBLE
        allWorkouts.clear()
        db.getReference("workouts/$currentGoal")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    allWorkouts.clear()
                    for (child in snap.children) {
                        val w = child.getValue(WorkoutItem::class.java) ?: continue
                        w.id = child.key ?: ""
                        allWorkouts.add(w)
                    }
                    if (allWorkouts.isEmpty()) seedDefaultWorkouts() else applyFilters()
                }
                override fun onCancelled(e: DatabaseError) {
                    if (isAdded) progressBar.visibility = View.GONE
                }
            })
    }

    private fun applyFilters() {
        filteredWorkouts.clear()
        for (w in allWorkouts) {
            val catOk = currentCategory == "All" || w.category.equals(currentCategory, ignoreCase = true)
            val searchOk = searchQuery.isEmpty() ||
                    w.name.contains(searchQuery, ignoreCase = true) ||
                    w.category.contains(searchQuery, ignoreCase = true) ||
                    w.musclesTargeted.contains(searchQuery, ignoreCase = true) ||
                    w.difficulty.contains(searchQuery, ignoreCase = true)
            if (catOk && searchOk) filteredWorkouts.add(w)
        }
        // Sort: not completed first, then by category
        filteredWorkouts.sortWith(compareBy({ completedToday.contains(it.id) }, { it.category }))

        tvEmpty.visibility = if (filteredWorkouts.isEmpty()) View.VISIBLE else View.GONE
        if (filteredWorkouts.isEmpty()) {
            tvEmpty.text = if (searchQuery.isNotEmpty()) "No workouts match \"$searchQuery\""
            else "No workouts found.\nAdmin hasn't added any yet."
        }
        buildAdapter()
    }

    private fun buildAdapter() {
        if (!isAdded) return
        rvWorkouts.adapter = WorkoutUserAdapter(
            items = filteredWorkouts,
            completed = completedToday,
            onComplete = { markComplete(it) },
            onCardClick = { openDetail(it) }
        )
    }

    private fun openDetail(w: WorkoutItem) {
        val i = Intent(requireContext(), WorkoutDetailActivity::class.java).apply {
            putExtra(WorkoutDetailActivity.EXTRA_WORKOUT_ID, w.id)
            putExtra(WorkoutDetailActivity.EXTRA_WORKOUT_NAME, w.name)
            putExtra(WorkoutDetailActivity.EXTRA_GIF_URL, w.gifUrl)
            putExtra(WorkoutDetailActivity.EXTRA_GIF_BASE64, w.gifBase64)
            putExtra(WorkoutDetailActivity.EXTRA_DESCRIPTION, w.description)
            putExtra(WorkoutDetailActivity.EXTRA_SETS, w.sets)
            putExtra(WorkoutDetailActivity.EXTRA_REPS, w.reps)
            putExtra(WorkoutDetailActivity.EXTRA_CALORIES, w.caloriesBurn)
            putExtra(WorkoutDetailActivity.EXTRA_DIFFICULTY, w.difficulty)
            putExtra(WorkoutDetailActivity.EXTRA_CATEGORY, w.category)
            putExtra(WorkoutDetailActivity.EXTRA_DURATION, w.durationMinutes)
            putExtra(WorkoutDetailActivity.EXTRA_MUSCLES, w.musclesTargeted)
            putExtra(WorkoutDetailActivity.EXTRA_STEPS, w.steps)
            putExtra(WorkoutDetailActivity.EXTRA_BREATHING, w.breathingTip)
            putExtra(WorkoutDetailActivity.EXTRA_POINTS, w.pointsToRemember)
            putExtra(WorkoutDetailActivity.EXTRA_EQUIPMENT, w.equipment)
            putExtra(WorkoutDetailActivity.EXTRA_GOAL, w.goal)
            putExtra(WorkoutDetailActivity.EXTRA_EMOJI, w.emoji)
        }
        startActivity(i)
    }

    private fun markComplete(w: WorkoutItem) {
        val userId = uid ?: return
        if (completedToday.contains(w.id)) {
            Toast.makeText(context, "✓ Already completed today!", Toast.LENGTH_SHORT).show()
            return
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val data = mapOf("name" to w.name, "calories" to w.caloriesBurn, "time" to time, "goal" to w.goal)
        db.getReference("users/$userId/completedWorkouts/$today/${w.id}").setValue(data)
            .addOnSuccessListener {
                val statsRef = db.getReference("users/$userId/stats")
                statsRef.get().addOnSuccessListener { snap ->
                    val prev = snap.child("totalCalories").getValue(Int::class.java) ?: 0
                    val prevW = snap.child("totalWorkouts").getValue(Int::class.java) ?: 0
                    statsRef.updateChildren(mapOf(
                        "totalCalories" to prev + w.caloriesBurn,
                        "totalWorkouts" to prevW + 1,
                        "lastWorkoutDate" to today
                    ))
                }
                Toast.makeText(context, "✅ ${w.name} done! +${w.caloriesBurn} kcal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun seedDefaultWorkouts() {
        for ((goal, list) in WorkoutItem.DEFAULT_WORKOUTS) {
            for (w in list) db.getReference("workouts/$goal/${w.id}").setValue(w)
        }
        allWorkouts.addAll(WorkoutItem.DEFAULT_WORKOUTS[currentGoal] ?: emptyList())
        applyFilters()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// User-facing workout list adapter
// ═══════════════════════════════════════════════════════════════════════════
class WorkoutUserAdapter(
    private val items: List<WorkoutItem>,
    private val completed: Set<String>,
    private val onComplete: (WorkoutItem) -> Unit,
    private val onCardClick: (WorkoutItem) -> Unit
) : RecyclerView.Adapter<WorkoutUserAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgGif: ImageView = v.findViewById(R.id.imgWorkoutGif)
        val tvEmoji: TextView = v.findViewById(R.id.tvGifLoadingCard)
        val tvName: TextView = v.findViewById(R.id.tvWorkoutName)
        val tvDesc: TextView = v.findViewById(R.id.tvWorkoutDesc)
        val tvSets: TextView = v.findViewById(R.id.tvWorkoutSets)
        val tvCals: TextView = v.findViewById(R.id.tvWorkoutCals)
        val tvDiff: TextView = v.findViewById(R.id.tvDifficulty)
        val tvCat: TextView = v.findViewById(R.id.tvWorkoutCategory)
        val btnDone: TextView = v.findViewById(R.id.btnMarkDone)
        val card: CardView = v.findViewById(R.id.cardWorkout)
        val tvTapHint: TextView = v.findViewById(R.id.tvTapForDetails)
        val tvDuration: TextView = v.findViewById(R.id.tvWorkoutDuration)
        val tvMuscles: TextView = v.findViewById(R.id.tvWorkoutMuscles)
        val tvEquipment: TextView = v.findViewById(R.id.tvWorkoutEquipment)
        val completedOverlay: View = v.findViewById(R.id.completedOverlay)
        val tvCompletedBadge: TextView = v.findViewById(R.id.tvCompletedBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_workout_user, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val w = items[pos]
        val done = completed.contains(w.id)

        h.tvName.text = w.name
        h.tvDesc.text = w.description
        h.tvSets.text = "${w.sets} sets  ×  ${w.reps}"
        h.tvCals.text = "🔥 ${w.caloriesBurn} kcal"
        h.tvDiff.text = w.difficulty
        h.tvCat.text = w.category
        h.tvDuration.text = "⏱ ${w.durationMinutes} min"
        h.tvMuscles.text = if (w.musclesTargeted.isNotBlank())
            "💪 ${w.musclesTargeted.split(",").take(2).joinToString(", ")}" else ""
        h.tvEquipment.text = if (w.equipment.isNotBlank() && w.equipment != "None") "🏋️ ${w.equipment}" else ""
        h.tvEquipment.visibility = if (h.tvEquipment.text.isNotEmpty()) View.VISIBLE else View.GONE

        h.tvDiff.setTextColor(Color.parseColor(when (w.difficulty) {
            "Beginner" -> "#43A047"
            "Intermediate" -> "#FF9800"
            else -> "#E53935"
        }))

        // Load image - supports both URL and base64
        val visualSrc = w.getVisualSource()
        if (visualSrc.isNotBlank()) {
            h.tvEmoji.visibility = View.GONE
            h.imgGif.visibility = View.VISIBLE
            Glide.with(h.imgGif.context)
                .load(visualSrc)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .placeholder(android.R.color.darker_gray)
                .centerCrop()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                        h.tvEmoji.text = w.emoji
                        h.tvEmoji.visibility = View.VISIBLE
                        h.imgGif.visibility = View.GONE
                        return false
                    }
                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean): Boolean {
                        h.tvEmoji.visibility = View.GONE
                        return false
                    }
                })
                .into(h.imgGif)
        } else {
            h.imgGif.setImageDrawable(null)
            h.imgGif.visibility = View.VISIBLE
            h.tvEmoji.text = w.emoji
            h.tvEmoji.visibility = View.VISIBLE
        }

        h.tvTapHint.visibility = View.VISIBLE

        // Completed state
        if (done) {
            h.completedOverlay.visibility = View.VISIBLE
            h.tvCompletedBadge.visibility = View.VISIBLE
            h.btnDone.text = "✓ Done today"
            h.btnDone.setBackgroundColor(Color.parseColor("#E8F5E9"))
            h.btnDone.setTextColor(Color.parseColor("#2E7D32"))
            h.card.setCardBackgroundColor(Color.parseColor("#F5FFF5"))
            h.card.alpha = 0.88f
        } else {
            h.completedOverlay.visibility = View.GONE
            h.tvCompletedBadge.visibility = View.GONE
            h.btnDone.text = "Mark Complete"
            h.btnDone.setBackgroundColor(Color.parseColor("#EEF2FF"))
            h.btnDone.setTextColor(Color.parseColor("#3949AB"))
            h.card.setCardBackgroundColor(Color.WHITE)
            h.card.alpha = 1f
        }

        h.btnDone.setOnClickListener { if (!done) onComplete(w) }
        h.card.setOnClickListener { onCardClick(w) }
    }
}