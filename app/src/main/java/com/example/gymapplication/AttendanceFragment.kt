package com.example.gymapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AttendanceFragment : Fragment() {

    private lateinit var containerMembers: LinearLayout
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvTotalMembers: TextView
    private lateinit var tvCheckedInToday: TextView
    private lateinit var tvMonthHeader: TextView

    private val db = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_attendance_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerMembers  = view.findViewById(R.id.containerMembers)
        progressLoading   = view.findViewById(R.id.progressLoading)
        tvTotalMembers    = view.findViewById(R.id.tvTotalMembers)
        tvCheckedInToday  = view.findViewById(R.id.tvCheckedInToday)
        tvMonthHeader     = view.findViewById(R.id.tvMonthHeader)

        tvMonthHeader.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

        loadAllUsersAttendance()
    }

    private fun loadAllUsersAttendance() {
        progressLoading.visibility = View.VISIBLE

        val today       = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val cal         = Calendar.getInstance()
        val dayOfMonth  = cal.get(Calendar.DAY_OF_MONTH)

        db.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                progressLoading.visibility = View.GONE
                containerMembers.removeAllViews()

                var totalMembers   = 0
                var checkedInToday = 0

                // Collect all regular users
                val members = mutableListOf<Triple<String, String, DataSnapshot>>()
                snapshot.children.forEach { userSnap ->
                    val accountType = userSnap.child("accountType")
                        .getValue(String::class.java) ?: "user"
                    if (accountType.equals("user", ignoreCase = true)) {
                        val username = userSnap.child("username")
                            .getValue(String::class.java) ?: "Unknown"
                        members.add(Triple(userSnap.key ?: "", username, userSnap))
                        totalMembers++
                        if (userSnap.child("checkins").child(today).exists()) {
                            checkedInToday++
                        }
                    }
                }

                tvTotalMembers.text   = totalMembers.toString()
                tvCheckedInToday.text = checkedInToday.toString()

                // Sort by name
                members.sortBy { it.second.lowercase() }

                members.forEach { (_, username, userSnap) ->
                    val checkinsSnap = userSnap.child("checkins")
                    val allDates     = checkinsSnap.children.mapNotNull { it.key }
                    val monthDates   = allDates.filter { it.startsWith(monthPrefix) }
                    val totalAll     = allDates.size
                    val monthCount   = monthDates.size
                    val pct          = if (dayOfMonth > 0) (monthCount * 100) / dayOfMonth else 0
                    val todayChecked = checkinsSnap.child(today).exists()

                    val lastDate = allDates.sorted().lastOrNull()
                    val lastFormatted = if (lastDate != null) {
                        try {
                            val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastDate)!!
                            SimpleDateFormat("dd MMM", Locale.getDefault()).format(d)
                        } catch (_: Exception) { lastDate }
                    } else "Never"

                    addMemberCard(username, monthCount, pct, totalAll, todayChecked, lastFormatted, dayOfMonth)
                }

                if (totalMembers == 0) {
                    val empty = TextView(requireContext()).apply {
                        text      = "No members registered yet"
                        textSize  = 14f
                        gravity   = android.view.Gravity.CENTER
                        setTextColor(Color.parseColor("#AAAAAA"))
                        setPadding(32, 64, 32, 64)
                    }
                    containerMembers.addView(empty)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) progressLoading.visibility = View.GONE
            }
        })
    }

    private fun addMemberCard(
        name: String,
        monthVisits: Int,
        pct: Int,
        totalVisits: Int,
        todayChecked: Boolean,
        lastVisit: String,
        dayOfMonth: Int
    ) {
        val ctx = context ?: return
        val density = resources.displayMetrics.density

        val card = CardView(ctx).apply {
            radius      = 16 * density
            cardElevation = 2 * density
            setCardBackgroundColor(Color.WHITE)
            val margin = (8 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, margin) }
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Row 1: Avatar + Name + Today badge
        val row1 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * density).toInt() }
        }

        // Avatar circle with initials
        val avatar = TextView(ctx).apply {
            val initial = if (name.isNotEmpty()) name[0].uppercase() else "?"
            text      = initial
            textSize  = 16f
            gravity   = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.circle_blue)
            val sz = (40 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                marginEnd = (12 * density).toInt()
            }
        }

        val nameCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvName = TextView(ctx).apply {
            text      = name
            textSize  = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A2E"))
        }

        val tvSub = TextView(ctx).apply {
            text     = "$totalVisits total visits · Last: $lastVisit"
            textSize = 11f
            setTextColor(Color.parseColor("#888888"))
        }

        nameCol.addView(tvName)
        nameCol.addView(tvSub)

        // Today badge
        val todayBadge = TextView(ctx).apply {
            text      = if (todayChecked) "✓ Today" else "Absent"
            textSize  = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val hPad = (10 * density).toInt()
            val vPad = (4  * density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            if (todayChecked) {
                setBackgroundColor(Color.parseColor("#E8F5E9"))
                setTextColor(Color.parseColor("#2E7D32"))
            } else {
                setBackgroundColor(Color.parseColor("#FFF3E0"))
                setTextColor(Color.parseColor("#E65100"))
            }
        }

        row1.addView(avatar)
        row1.addView(nameCol)
        row1.addView(todayBadge)

        // Row 2: This month label + count + pct
        val row2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (6 * density).toInt() }
        }

        val tvMonthLabel = TextView(ctx).apply {
            text      = "This month"
            textSize  = 12f
            setTextColor(Color.parseColor("#888888"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvCount = TextView(ctx).apply {
            text      = "$monthVisits / $dayOfMonth days"
            textSize  = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A2E"))
        }

        val tvPct = TextView(ctx).apply {
            text      = "$pct%"
            textSize  = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(when {
                pct >= 80 -> Color.parseColor("#2E7D32")
                pct >= 50 -> Color.parseColor("#F57F17")
                else      -> Color.parseColor("#C62828")
            })
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = (12 * density).toInt() }
        }

        row2.addView(tvMonthLabel)
        row2.addView(tvCount)
        row2.addView(tvPct)

        // Progress bar
        val progressBar = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
            max      = 100
            progress = pct
            progressTintList = android.content.res.ColorStateList.valueOf(
                when {
                    pct >= 80 -> Color.parseColor("#43A047")
                    pct >= 50 -> Color.parseColor("#FFA000")
                    else      -> Color.parseColor("#E53935")
                }
            )
            progressBackgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (8 * density).toInt()
            )
        }

        inner.addView(row1)
        inner.addView(row2)
        inner.addView(progressBar)
        card.addView(inner)
        containerMembers.addView(card)
    }
}