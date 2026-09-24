package com.example.gymapplication

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CheckInFragment : Fragment() {

    private lateinit var btnCheckIn: MaterialButton
    private lateinit var tvCheckinStatus: TextView
    private lateinit var tvTotalCheckins: TextView
    private lateinit var tvThisMonth: TextView
    private lateinit var tvLastCheckin: TextView
    private lateinit var tvAttendancePct: TextView
    private lateinit var tvCurrentMonth: TextView
    private lateinit var calendarGrid: GridLayout
    private lateinit var tvStreakCount: TextView

    private val db   = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid  get() = auth.currentUser?.uid

    private val checkedInDates = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_checkin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCheckIn      = view.findViewById(R.id.btnCheckIn)
        tvCheckinStatus = view.findViewById(R.id.tvCheckinStatus)
        tvTotalCheckins = view.findViewById(R.id.tvTotalCheckins)
        tvThisMonth     = view.findViewById(R.id.tvThisMonth)
        tvLastCheckin   = view.findViewById(R.id.tvLastCheckin)
        tvAttendancePct = view.findViewById(R.id.tvAttendancePct)
        tvCurrentMonth  = view.findViewById(R.id.tvCurrentMonth)
        calendarGrid    = view.findViewById(R.id.calendarGrid)
        tvStreakCount   = view.findViewById(R.id.tvStreakCount)

        tvCurrentMonth.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

        loadCheckins()
        btnCheckIn.setOnClickListener { performCheckIn() }
    }

    private fun todayKey() =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun nowTime() =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    private fun performCheckIn() {
        val userId = uid ?: return
        val today  = todayKey()
        if (checkedInDates.contains(today)) {
            Toast.makeText(context, "Already checked in today ✓", Toast.LENGTH_SHORT).show()
            return
        }
        // Date IS the key — enforces one check-in per day at the DB level
        val data = mapOf("time" to nowTime(), "timestamp" to ServerValue.TIMESTAMP)
        db.getReference("users/$userId/checkins/$today").setValue(data)
            .addOnSuccessListener {
                Toast.makeText(context, "✅ Checked in at ${nowTime()}!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Check-in failed. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCheckins() {
        val userId = uid ?: return
        db.getReference("users/$userId/checkins")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return
                    checkedInDates.clear()
                    snapshot.children.forEach { it.key?.let { k -> checkedInDates.add(k) } }
                    updateUI()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateUI() {
        val today       = todayKey()
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthDates  = checkedInDates.filter { it.startsWith(monthPrefix) }
        val cal         = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayOfMonth  = cal.get(Calendar.DAY_OF_MONTH)
        val pct         = if (dayOfMonth > 0) (monthDates.size * 100) / dayOfMonth else 0

        tvTotalCheckins.text = checkedInDates.size.toString()
        tvThisMonth.text     = monthDates.size.toString()
        tvAttendancePct.text = "$pct%"
        tvStreakCount.text   = calculateStreak().toString()

        val lastDate = checkedInDates.sorted().lastOrNull()
        tvLastCheckin.text = if (lastDate != null) {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastDate)!!
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(d)
            } catch (_: Exception) { lastDate }
        } else "—"

        val checkedInToday = checkedInDates.contains(today)
        if (checkedInToday) {
            btnCheckIn.text = "✓ Checked in today"
            btnCheckIn.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#43A047"))
            tvCheckinStatus.text = "Great job! See you tomorrow 💪"
            tvCheckinStatus.setTextColor(Color.parseColor("#43A047"))
        } else {
            btnCheckIn.text = "Check In Now"
            btnCheckIn.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#3949AB"))
            tvCheckinStatus.text = "You haven't checked in today yet"
            tvCheckinStatus.setTextColor(Color.parseColor("#888888"))
        }

        buildCalendarDots(daysInMonth, dayOfMonth, monthDates, monthPrefix)
    }

    private fun buildCalendarDots(
        daysInMonth: Int, todayNum: Int,
        checkedDays: List<String>, monthPrefix: String
    ) {
        calendarGrid.removeAllViews()
        calendarGrid.columnCount = 7

        val density = resources.displayMetrics.density
        val size = (34 * density).toInt()

        val checkedNums = checkedDays.mapNotNull { date ->
            try { date.removePrefix("$monthPrefix-").toInt() } catch (_: Exception) { null }
        }.toSet()

        for (day in 1..daysInMonth) {
            val cell = TextView(requireContext()).apply {
                text     = day.toString()
                textSize = 11f
                gravity  = Gravity.CENTER
                width    = size
                height   = size
                layoutParams = GridLayout.LayoutParams().apply { setMargins(2, 2, 2, 2) }

                when {
                    day == todayNum && checkedNums.contains(day) -> {
                        setBackgroundResource(R.drawable.circle_green)
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                    }
                    day == todayNum -> {
                        setBackgroundResource(R.drawable.circle_blue_outline)
                        setTextColor(Color.parseColor("#3949AB"))
                        setTypeface(null, Typeface.BOLD)
                    }
                    checkedNums.contains(day) -> {
                        setBackgroundResource(R.drawable.circle_green_light)
                        setTextColor(Color.parseColor("#2E7D32"))
                    }
                    day < todayNum -> setTextColor(Color.parseColor("#DDDDDD"))
                    else           -> setTextColor(Color.parseColor("#BBBBBB"))
                }
            }
            calendarGrid.addView(cell)
        }
    }

    private fun calculateStreak(): Int {
        var streak = 0
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        if (!checkedInDates.contains(sdf.format(cal.time)))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        for (i in 0..365) {
            if (checkedInDates.contains(sdf.format(cal.time))) {
                streak++; cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return streak
    }
}