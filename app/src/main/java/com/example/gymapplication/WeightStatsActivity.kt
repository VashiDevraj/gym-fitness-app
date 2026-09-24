package com.example.gymapplication

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WeightStatsActivity : AppCompatActivity() {

    private lateinit var chart: LineChart
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvMinWeight: TextView
    private lateinit var tvMaxWeight: TextView
    private lateinit var tvChange: TextView
    private lateinit var tvChangeLabel: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var cardSummary: CardView

    // Keep a reference so we can remove it when activity is destroyed
    private var weightListener: ValueEventListener? = null
    private var weightRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_stats)
        supportActionBar?.title = "Weight Progress"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        chart           = findViewById(R.id.weightChart)
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight)
        tvMinWeight     = findViewById(R.id.tvMinWeight)
        tvMaxWeight     = findViewById(R.id.tvMaxWeight)
        tvChange        = findViewById(R.id.tvChange)
        tvChangeLabel   = findViewById(R.id.tvChangeLabel)
        tvEmpty         = findViewById(R.id.tvEmpty)
        cardSummary     = findViewById(R.id.cardSummary)

        listenToWeightGraph()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() {
        super.onDestroy()
        weightListener?.let { weightRef.removeEventListener(it) }
    }

    /**
     * Uses a ValueEventListener (real-time) instead of a one-shot .get() so that
     * any weight update from the Profile screen immediately refreshes the chart.
     *
     * The weight history node can store values in several formats coming from
     * the Profile fragment (String from EditText, Double/Float from earlier saves).
     * We handle all variants safely.
     */
    private fun listenToWeightGraph() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showEmpty(); return
        }

        weightRef = FirebaseDatabase.getInstance().getReference("users/$uid/weightHistory")

        weightListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                renderChart(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                showEmpty()
            }
        }

        weightRef.addValueEventListener(weightListener!!)
    }

    private fun renderChart(snapshot: DataSnapshot) {
        if (!snapshot.exists() || snapshot.childrenCount == 0L) {
            showEmpty(); return
        }

        // Sort keys (dates stored as "yyyy-MM-dd")
        val sortedKeys = snapshot.children.mapNotNull { it.key }.sorted()

        val entries = ArrayList<Entry>()
        val labels  = ArrayList<String>()

        sortedKeys.forEachIndexed { idx, key ->
            // Weight may be stored as Double, Float, Long, or String
            val weight = safeWeight(snapshot.child(key))
            if (weight > 0f) {
                entries.add(Entry(idx.toFloat(), weight))
                val parts = key.split("-")
                // Format as "DD/MM" for readability
                labels.add(if (parts.size == 3) "${parts[2]}/${parts[1]}" else key)
            }
        }

        if (entries.isEmpty()) { showEmpty(); return }

        // ── Summary stats ──────────────────────────────────────
        val weights  = entries.map { it.y }
        val current  = weights.last()
        val min      = weights.min()
        val max      = weights.max()
        val change   = current - weights.first()
        val isLoss   = change <= 0f

        tvCurrentWeight.text = "%.1f kg".format(current)
        tvMinWeight.text     = "%.1f kg".format(min)
        tvMaxWeight.text     = "%.1f kg".format(max)
        tvChange.text        = "${if (isLoss) "" else "+"}%.1f kg".format(change)
        tvChange.setTextColor(
            if (isLoss) Color.parseColor("#43A047") else Color.parseColor("#E53935")
        )
        tvChangeLabel.text = if (isLoss) "Lost ✓" else "Gained"

        // ── Hide empty state, show chart ───────────────────────
        tvEmpty.visibility     = View.GONE
        chart.visibility       = View.VISIBLE
        cardSummary.visibility = View.VISIBLE

        // ── Build dataset ──────────────────────────────────────
        val dataSet = LineDataSet(entries, "Weight (kg)").apply {
            color          = Color.parseColor("#5B8DEF")
            setCircleColor(Color.parseColor("#5B8DEF"))
            circleRadius   = 5f
            lineWidth      = 3f
            setDrawFilled(true)
            fillColor      = Color.parseColor("#BBD1FF")
            fillAlpha      = 80
            mode           = LineDataSet.Mode.CUBIC_BEZIER
            valueTextSize  = 10f
            valueTextColor = Color.parseColor("#444444")
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(v: Float) = "%.1f".format(v)
            }
            highLightColor = Color.parseColor("#FF5722")
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            setBackgroundColor(Color.WHITE)
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled  = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled    = false
            axisRight.isEnabled = false

            axisLeft.apply {
                textColor     = Color.parseColor("#555555")
                textSize      = 12f
                gridColor     = Color.parseColor("#F0F0F0")
                axisLineColor = Color.parseColor("#E0E0E0")
                // Give a little padding above/below min/max so dots aren't clipped
                val padding = (max - min) * 0.1f + 1f
                axisMinimum = min - padding
                axisMaximum = max + padding
            }
            xAxis.apply {
                valueFormatter     = IndexAxisValueFormatter(labels)
                position           = XAxis.XAxisPosition.BOTTOM
                granularity        = 1f
                textColor          = Color.parseColor("#555555")
                textSize           = 10f
                setDrawGridLines(false)
                axisLineColor      = Color.parseColor("#E0E0E0")
                labelRotationAngle = -30f
                // Avoid label crowding — show at most 7 labels
                setLabelCount(minOf(labels.size, 7), false)
            }
            animateX(800)
            invalidate()
        }
    }

    /**
     * Safely extract a positive Float weight from a DataSnapshot node.
     * Handles Double, Float, Long, Int, and String storage formats.
     */
    private fun safeWeight(snap: DataSnapshot): Float {
        return try { snap.getValue(Float::class.java) ?: 0f }
        catch (_: Exception) {
            try { snap.getValue(Double::class.java)?.toFloat() ?: 0f }
            catch (_: Exception) {
                try { (snap.getValue(Long::class.java) ?: 0L).toFloat() }
                catch (_: Exception) {
                    snap.getValue(String::class.java)?.toFloatOrNull() ?: 0f
                }
            }
        }
    }

    private fun showEmpty() {
        chart.visibility       = View.GONE
        cardSummary.visibility = View.GONE
        tvEmpty.visibility     = View.VISIBLE
    }
}