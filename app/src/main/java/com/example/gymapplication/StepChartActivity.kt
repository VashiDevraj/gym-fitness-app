package com.example.gymapplication

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StepChartActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var caloriesChart: LineChart
    private lateinit var tvTotalSteps: TextView
    private lateinit var tvAvgSteps: TextView
    private lateinit var tvGoalDays: TextView
    private lateinit var tvTotalCalories: TextView
    private lateinit var btnWeek: TextView
    private lateinit var btnMonth: TextView
    private lateinit var tabSteps: LinearLayout
    private lateinit var tabCalories: LinearLayout
    private lateinit var cardSteps: CardView
    private lateinit var cardCalories: CardView

    private val db   = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var stepGoal = 8000
    private var showingWeek = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_chart)

        supportActionBar?.title = "Activity Stats"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        barChart       = findViewById(R.id.barChart)
        caloriesChart  = findViewById(R.id.caloriesChart)
        tvTotalSteps   = findViewById(R.id.tvTotalSteps)
        tvAvgSteps     = findViewById(R.id.tvAvgSteps)
        tvGoalDays     = findViewById(R.id.tvGoalDays)
        tvTotalCalories = findViewById(R.id.tvTotalCalories)
        btnWeek        = findViewById(R.id.btnWeek)
        btnMonth       = findViewById(R.id.btnMonth)
        tabSteps       = findViewById(R.id.tabSteps)
        tabCalories    = findViewById(R.id.tabCalories)
        cardSteps      = findViewById(R.id.cardStepsChart)
        cardCalories   = findViewById(R.id.cardCaloriesChart)

        loadGoal()
        setupTabs()
        loadChartData(7)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun setupTabs() {
        btnWeek.setOnClickListener {
            showingWeek = true
            btnWeek.setBackgroundResource(R.drawable.tab_selected_bg)
            btnWeek.setTextColor(Color.WHITE)
            btnMonth.setBackgroundColor(Color.TRANSPARENT)
            btnMonth.setTextColor(Color.parseColor("#888888"))
            loadChartData(7)
        }
        btnMonth.setOnClickListener {
            showingWeek = false
            btnMonth.setBackgroundResource(R.drawable.tab_selected_bg)
            btnMonth.setTextColor(Color.WHITE)
            btnWeek.setBackgroundColor(Color.TRANSPARENT)
            btnWeek.setTextColor(Color.parseColor("#888888"))
            loadChartData(30)
        }

        tabSteps.setOnClickListener {
            cardSteps.visibility    = View.VISIBLE
            cardCalories.visibility = View.GONE
            tabSteps.setBackgroundColor(Color.parseColor("#EEF2FF"))
            tabCalories.setBackgroundColor(Color.TRANSPARENT)
        }
        tabCalories.setOnClickListener {
            cardSteps.visibility    = View.GONE
            cardCalories.visibility = View.VISIBLE
            tabCalories.setBackgroundColor(Color.parseColor("#FFF8E1"))
            tabSteps.setBackgroundColor(Color.TRANSPARENT)
        }

        // Default state
        btnWeek.setBackgroundResource(R.drawable.tab_selected_bg)
        btnWeek.setTextColor(Color.WHITE)
        tabSteps.setBackgroundColor(Color.parseColor("#EEF2FF"))
        cardSteps.visibility    = View.VISIBLE
        cardCalories.visibility = View.GONE
    }

    private fun loadGoal() {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("users/$uid/stepGoal").get().addOnSuccessListener { snap ->
            stepGoal = try {
                snap.getValue(Int::class.java) ?: 8000
            } catch (_: Exception) { 8000 }
        }
    }

    private fun loadChartData(days: Int) {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("users/$uid/steps").get().addOnSuccessListener { snapshot ->
            val sdf   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val labSdf = SimpleDateFormat(if (days <= 7) "EEE" else "dd/MM", Locale.getDefault())
            val cal   = Calendar.getInstance()

            val stepEntries    = ArrayList<BarEntry>()
            val calorieEntries = ArrayList<Entry>()
            val labels         = ArrayList<String>()
            val colors         = ArrayList<Int>()

            var totalSteps    = 0L
            var goalDays      = 0
            var totalCalories = 0L
            var validDays     = 0

            for (i in days - 1 downTo 0) {
                val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dateKey = sdf.format(dayCal.time)
                val label   = labSdf.format(dayCal.time)
                val node    = snapshot.child(dateKey)

                val steps = safeInt(node, "count")
                val cals  = safeInt(node, "calories").takeIf { it > 0 }
                    ?: (steps * 0.04).toInt()

                val idx = (days - 1 - i).toFloat()
                stepEntries.add(BarEntry(idx, steps.toFloat()))
                calorieEntries.add(Entry(idx, cals.toFloat()))
                labels.add(label)

                val goalMet = steps >= stepGoal
                colors.add(if (goalMet) Color.parseColor("#4CAF50") else Color.parseColor("#B0BEC5"))
                if (goalMet) goalDays++
                totalSteps    += steps
                totalCalories += cals
                if (steps > 0) validDays++
            }

            val avgSteps = if (validDays > 0) totalSteps / validDays else 0

            tvTotalSteps.text    = "%,d".format(totalSteps)
            tvAvgSteps.text      = "%,d / day".format(avgSteps)
            tvGoalDays.text      = "$goalDays / $days days"
            tvTotalCalories.text = "%,d kcal".format(totalCalories)

            setupBarChart(stepEntries, labels, colors)
            setupCaloriesChart(calorieEntries, labels)
        }
    }

    private fun safeInt(snap: com.google.firebase.database.DataSnapshot, key: String): Int {
        val child = snap.child(key)
        return try { child.getValue(Int::class.java) ?: 0 }
        catch (_: Exception) {
            try { (child.getValue(Long::class.java) ?: 0L).toInt() }
            catch (_: Exception) { child.getValue(String::class.java)?.toIntOrNull() ?: 0 }
        }
    }

    private fun setupBarChart(
        entries: List<BarEntry>,
        labels: List<String>,
        colors: List<Int>
    ) {
        val dataSet = BarDataSet(entries, "Steps").apply {
            this.colors = colors
            valueTextSize  = 9f
            valueTextColor = Color.parseColor("#555555")
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    if (value > 0) "%,d".format(value.toInt()) else ""
            }
        }

        barChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled      = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            setPinchZoom(false)
            axisRight.isEnabled = false

            axisLeft.apply {
                textColor    = Color.parseColor("#555555")
                textSize     = 11f
                gridColor    = Color.parseColor("#F0F0F0")
                axisLineColor = Color.parseColor("#E0E0E0")
                setDrawAxisLine(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(v: Float) =
                        if (v >= 1000) "${(v/1000).toInt()}k" else v.toInt().toString()
                }
                // Draw goal line
                addLimitLine(
                    com.github.mikephil.charting.components.LimitLine(
                        stepGoal.toFloat(), "Goal"
                    ).apply {
                        lineColor    = Color.parseColor("#FF5722")
                        lineWidth    = 1.5f
                        enableDashedLine(10f, 5f, 0f)
                        textColor    = Color.parseColor("#FF5722")
                        textSize     = 10f
                    }
                )
            }

            xAxis.apply {
                valueFormatter  = IndexAxisValueFormatter(labels)
                position        = XAxis.XAxisPosition.BOTTOM
                granularity     = 1f
                textColor       = Color.parseColor("#555555")
                textSize        = 10f
                gridColor       = Color.TRANSPARENT
                axisLineColor   = Color.parseColor("#E0E0E0")
                setDrawGridLines(false)
            }

            animateY(600)
            invalidate()
        }
    }

    private fun setupCaloriesChart(entries: List<Entry>, labels: List<String>) {
        val dataSet = LineDataSet(entries, "Calories burned").apply {
            color             = Color.parseColor("#FF9800")
            setCircleColor(Color.parseColor("#FF9800"))
            circleRadius      = 4f
            lineWidth         = 2.5f
            setDrawFilled(true)
            fillColor         = Color.parseColor("#FFE0B2")
            fillAlpha         = 120
            valueTextSize     = 9f
            valueTextColor    = Color.parseColor("#555555")
            mode              = LineDataSet.Mode.CUBIC_BEZIER
            valueFormatter    = object : ValueFormatter() {
                override fun getFormattedValue(v: Float) =
                    if (v > 0) v.toInt().toString() else ""
            }
        }

        caloriesChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled      = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            axisRight.isEnabled = false

            axisLeft.apply {
                textColor     = Color.parseColor("#555555")
                textSize      = 11f
                gridColor     = Color.parseColor("#F0F0F0")
                axisLineColor = Color.parseColor("#E0E0E0")
            }

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position       = XAxis.XAxisPosition.BOTTOM
                granularity    = 1f
                textColor      = Color.parseColor("#555555")
                textSize       = 10f
                setDrawGridLines(false)
                axisLineColor  = Color.parseColor("#E0E0E0")
            }

            animateX(600)
            invalidate()
        }
    }
}