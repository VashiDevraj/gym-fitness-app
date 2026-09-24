package com.example.gymapplication

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class EquipmentFragment : Fragment() {

    private lateinit var rvEquipment: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var spinnerFilter: Spinner
    private lateinit var tvWorkingCount: TextView
    private lateinit var tvRepairCount: TextView
    private lateinit var tvOutCount: TextView

    private val allEquipment = mutableListOf<Equipment>()
    private val filtered = mutableListOf<Equipment>()
    private var searchQuery = ""
    private var filterStatus = "All"

    private val db   = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid  get() = auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_equipment_user, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvEquipment    = view.findViewById(R.id.rvEquipment)
        etSearch       = view.findViewById(R.id.etEquipmentSearch)
        progressBar    = view.findViewById(R.id.progressEquipment)
        tvEmpty        = view.findViewById(R.id.tvEquipmentEmpty)
        spinnerFilter  = view.findViewById(R.id.spinnerStatusFilter)
        tvWorkingCount = view.findViewById(R.id.tvWorkingCount)
        tvRepairCount  = view.findViewById(R.id.tvRepairCount)
        tvOutCount     = view.findViewById(R.id.tvOutCount)

        rvEquipment.layoutManager = LinearLayoutManager(requireContext())

        setupFilter()
        setupSearch()
        loadEquipment()
    }

    private fun setupFilter() {
        val statuses = listOf("All", "Working", "Needs Repair", "Out of Order")
        spinnerFilter.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, statuses).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                filterStatus = statuses[pos]; applyFilters()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { searchQuery = s.toString(); applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun loadEquipment() {
        progressBar.visibility = View.VISIBLE
        db.getReference("equipment")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    allEquipment.clear()
                    snapshot.children.forEach { child ->
                        val eq = child.getValue(Equipment::class.java)
                        if (eq != null) { eq.id = child.key ?: ""; allEquipment.add(eq) }
                    }
                    if (allEquipment.isEmpty()) seedDefaultEquipment()
                    updateStatusCounts()
                    applyFilters()
                }
                override fun onCancelled(error: DatabaseError) {
                    if (isAdded) progressBar.visibility = View.GONE
                }
            })
    }

    private fun updateStatusCounts() {
        tvWorkingCount.text = allEquipment.count { it.status == "Working" }.toString()
        tvRepairCount.text  = allEquipment.count { it.status == "Needs Repair" }.toString()
        tvOutCount.text     = allEquipment.count { it.status == "Out of Order" }.toString()
    }

    private fun applyFilters() {
        filtered.clear()
        filtered.addAll(allEquipment.filter { eq ->
            val matchSearch = searchQuery.isBlank() || eq.name.contains(searchQuery, ignoreCase = true)
            val matchStatus = filterStatus == "All" || eq.status == filterStatus
            matchSearch && matchStatus
        })
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvEquipment.adapter = EquipmentUserAdapter(filtered) { equipment ->
            showReportDialog(equipment)
        }
    }

    private fun showReportDialog(equipment: Equipment) {
        val ctx   = context ?: return
        val view  = LayoutInflater.from(ctx).inflate(R.layout.dialog_report_equipment, null)
        val etIssue  = view.findViewById<EditText>(R.id.etIssueDescription)
        val spinSev  = view.findViewById<Spinner>(R.id.spinnerSeverity)

        spinSev.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item,
            listOf("Low","Medium","High")).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinSev.setSelection(1) // Default Medium

        AlertDialog.Builder(ctx)
            .setTitle("Report Issue: ${equipment.name}")
            .setMessage("Describe the problem so our team can fix it quickly.")
            .setView(view)
            .setPositiveButton("Submit Report") { _, _ ->
                val issue = etIssue.text.toString().trim()
                if (issue.isBlank()) {
                    Toast.makeText(ctx, "Please describe the issue", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                submitReport(equipment, issue, spinSev.selectedItem.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitReport(equipment: Equipment, issue: String, severity: String) {
        val userId = uid ?: return
        val today  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val time   = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // Get username
        db.getReference("users/$userId/username").get().addOnSuccessListener { snap ->
            val username = snap.getValue(String::class.java) ?: "User"
            val reportKey = db.getReference("equipmentReports").push().key ?: return@addOnSuccessListener

            val report = EquipmentReport(
                id            = reportKey,
                equipmentId   = equipment.id,
                equipmentName = equipment.name,
                userId        = userId,
                username      = username,
                issue         = issue,
                severity      = severity,
                date          = today,
                time          = time,
                status        = "Open"
            )

            // Save report
            db.getReference("equipmentReports/$reportKey").setValue(report)

            // Increment report count on equipment
            db.getReference("equipment/${equipment.id}/reportCount")
                .setValue(equipment.reportCount + 1)

            // Auto-update status if High severity
            if (severity == "High") {
                db.getReference("equipment/${equipment.id}/status").setValue("Needs Repair")
            }

            // Save to user's reports
            db.getReference("users/$userId/myReports/$reportKey").setValue(mapOf(
                "equipmentName" to equipment.name,
                "issue" to issue,
                "date" to today,
                "status" to "Open"
            ))

            Toast.makeText(context,
                "✅ Report submitted. Our team will fix it soon!",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun seedDefaultEquipment() {
        val defaults = listOf(
            Equipment("eq1","Treadmill","Cardio running machine","","Cardio","Working","Ground Floor",0,"2026-01-15","2026-01-01"),
            Equipment("eq2","Lat Pulldown Machine","Upper back trainer","","Strength","Working","Ground Floor",0,"2026-01-15","2026-01-01"),
            Equipment("eq3","Leg Press Machine","Leg strength equipment","","Strength","Working","First Floor",0,"2026-02-01","2026-01-01"),
            Equipment("eq4","Stationary Bike","Low impact cardio","","Cardio","Working","Ground Floor",0,"2026-01-15","2026-01-01"),
            Equipment("eq5","Cable Crossover","Cable resistance system","","Strength","Working","First Floor",0,"2026-02-01","2026-01-01"),
            Equipment("eq6","Smith Machine","Guided barbell system","","Strength","Working","First Floor",0,"2026-02-01","2026-01-01"),
            Equipment("eq7","Rowing Machine","Full body cardio","","Cardio","Working","Ground Floor",0,"2026-01-15","2026-01-01"),
            Equipment("eq8","Dumbbell Rack","Free weight storage","","Strength","Working","Both Floors",0,"2026-01-01","2026-01-01"),
            Equipment("eq9","Pull Up Bar","Bodyweight training bar","","Strength","Working","Ground Floor",0,"2026-01-01","2026-01-01"),
            Equipment("eq10","Foam Roller","Recovery and flexibility","","Flexibility","Working","Stretching Area",0,"2026-01-01","2026-01-01")
        )
        defaults.forEach { eq ->
            db.getReference("equipment/${eq.id}").setValue(eq)
        }
        allEquipment.addAll(defaults)
        updateStatusCounts()
        applyFilters()
    }
}

// ── User Equipment Adapter ─────────────────────────────────────────────────────

class EquipmentUserAdapter(
    private val items: List<Equipment>,
    private val onReport: (Equipment) -> Unit
) : RecyclerView.Adapter<EquipmentUserAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView     = v.findViewById(R.id.tvEquipName)
        val tvDesc: TextView     = v.findViewById(R.id.tvEquipDesc)
        val tvLocation: TextView = v.findViewById(R.id.tvEquipLocation)
        val tvStatus: TextView   = v.findViewById(R.id.tvEquipStatus)
        val tvCategory: TextView = v.findViewById(R.id.tvEquipCategory)
        val tvReports: TextView  = v.findViewById(R.id.tvEquipReports)
        val btnReport: TextView  = v.findViewById(R.id.btnReportEquip)
        val statusBar: View      = v.findViewById(R.id.statusIndicatorBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_equipment_user, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val eq = items[position]
        holder.tvName.text     = eq.name
        holder.tvDesc.text     = eq.description
        holder.tvLocation.text = "📍 ${eq.location}"
        holder.tvCategory.text = eq.category
        holder.tvReports.text  = if (eq.reportCount > 0) "⚠️ ${eq.reportCount} report(s)" else ""

        val (statusColor, barColor) = when (eq.status) {
            "Working"      -> Pair("#43A047", "#43A047")
            "Needs Repair" -> Pair("#FF9800", "#FF9800")
            else           -> Pair("#E53935", "#E53935")
        }
        holder.tvStatus.text = eq.status
        holder.tvStatus.setTextColor(Color.parseColor(statusColor))
        holder.statusBar.setBackgroundColor(Color.parseColor(barColor))

        if (eq.status == "Out of Order") {
            holder.btnReport.text = "Out of Order"
            holder.btnReport.setBackgroundColor(Color.parseColor("#FFEBEE"))
            holder.btnReport.setTextColor(Color.parseColor("#C62828"))
            holder.btnReport.isClickable = false
        } else {
            holder.btnReport.text = "Report Issue"
            holder.btnReport.setBackgroundColor(Color.parseColor("#FFF8E1"))
            holder.btnReport.setTextColor(Color.parseColor("#F57F17"))
            holder.btnReport.isClickable = true
            holder.btnReport.setOnClickListener { onReport(eq) }
        }
    }
}