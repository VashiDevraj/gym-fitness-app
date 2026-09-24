package com.example.gymapplication

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AdminEquipmentFragment : Fragment() {

    private lateinit var tabEquipment: TextView
    private lateinit var tabReports: TextView
    private lateinit var rvContent: RecyclerView
    private lateinit var btnAddEquipment: com.google.android.material.button.MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvOpenReports: TextView

    private val equipment = mutableListOf<Equipment>()
    private val reports   = mutableListOf<EquipmentReport>()
    private val db = FirebaseDatabase.getInstance()
    private var showingReports = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_admin_equipment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabEquipment  = view.findViewById(R.id.tabAdminEquipment)
        tabReports    = view.findViewById(R.id.tabAdminReports)
        rvContent     = view.findViewById(R.id.rvAdminEquipContent)
        btnAddEquipment = view.findViewById(R.id.btnAddEquipment)
        progressBar   = view.findViewById(R.id.progressAdminEquip)
        tvOpenReports = view.findViewById(R.id.tvOpenReports)

        rvContent.layoutManager = LinearLayoutManager(requireContext())

        selectEquipTab()
        loadEquipment()
        loadReports()

        tabEquipment.setOnClickListener {
            showingReports = false
            selectEquipTab()
            buildEquipmentList()
        }
        tabReports.setOnClickListener {
            showingReports = true
            selectReportsTab()
            buildReportsList()
        }
        btnAddEquipment.setOnClickListener { showAddEditEquipmentDialog(null) }
    }

    private fun selectEquipTab() {
        tabEquipment.setBackgroundResource(R.drawable.tab_selected_bg)
        tabEquipment.setTextColor(Color.WHITE)
        tabReports.setBackgroundColor(Color.TRANSPARENT)
        tabReports.setTextColor(Color.parseColor("#888888"))
        btnAddEquipment.visibility = View.VISIBLE
    }

    private fun selectReportsTab() {
        tabReports.setBackgroundResource(R.drawable.tab_selected_bg)
        tabReports.setTextColor(Color.WHITE)
        tabEquipment.setBackgroundColor(Color.TRANSPARENT)
        tabEquipment.setTextColor(Color.parseColor("#888888"))
        btnAddEquipment.visibility = View.GONE
    }

    private fun loadEquipment() {
        db.getReference("equipment").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                equipment.clear()
                snapshot.children.forEach { child ->
                    val eq = child.getValue(Equipment::class.java)
                    if (eq != null) { eq.id = child.key ?: ""; equipment.add(eq) }
                }
                if (!showingReports) buildEquipmentList()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadReports() {
        db.getReference("equipmentReports").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                reports.clear()
                snapshot.children.forEach { child ->
                    val r = child.getValue(EquipmentReport::class.java)
                    if (r != null) { r.id = child.key ?: ""; reports.add(r) }
                }
                reports.sortWith(
                    compareByDescending<EquipmentReport> { it.severity == "High" }
                        .thenByDescending { it.date + it.time }
                )
                val openCount = reports.count { it.status == "Open" }
                tvOpenReports.text = if (openCount > 0) "$openCount open" else ""
                if (showingReports) buildReportsList()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun buildEquipmentList() {
        rvContent.adapter = AdminEquipmentListAdapter(equipment,
            onEdit   = { showAddEditEquipmentDialog(it) },
            onStatus = { eq, status -> updateEquipmentStatus(eq, status) },
            onDelete = { confirmDeleteEquipment(it) }
        )
    }

    private fun buildReportsList() {
        rvContent.adapter = AdminReportAdapter(reports,
            onResolve = { resolveReport(it) },
            onInProgress = { markInProgress(it) }
        )
    }

    private fun showAddEditEquipmentDialog(existing: Equipment?) {
        val ctx = context ?: return
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_equipment, null)

        val etName      = view.findViewById<EditText>(R.id.etEquipName)
        val etDesc      = view.findViewById<EditText>(R.id.etEquipDesc)
        val etLocation  = view.findViewById<EditText>(R.id.etEquipLocation)
        val spinCat     = view.findViewById<Spinner>(R.id.spinnerEquipCategory)
        val spinStatus  = view.findViewById<Spinner>(R.id.spinnerEquipStatus)
        val etMaintDate = view.findViewById<EditText>(R.id.etLastMaintenance)

        val cats     = listOf("Cardio","Strength","Flexibility","Free Weights","Machines")
        val statuses = listOf("Working","Needs Repair","Out of Order")

        spinCat.adapter    = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, cats)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinStatus.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, statuses)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        existing?.let {
            etName.setText(it.name)
            etDesc.setText(it.description)
            etLocation.setText(it.location)
            etMaintDate.setText(it.lastMaintenanceDate)
            spinCat.setSelection(cats.indexOf(it.category).coerceAtLeast(0))
            spinStatus.setSelection(statuses.indexOf(it.status).coerceAtLeast(0))
        }

        AlertDialog.Builder(ctx)
            .setTitle(if (existing == null) "Add Equipment" else "Edit Equipment")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isBlank()) { Toast.makeText(ctx, "Name required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val eq = Equipment(
                    id                  = existing?.id ?: (db.getReference("equipment").push().key ?: return@setPositiveButton),
                    name                = name,
                    description         = etDesc.text.toString().trim(),
                    location            = etLocation.text.toString().trim(),
                    category            = spinCat.selectedItem.toString(),
                    status              = spinStatus.selectedItem.toString(),
                    lastMaintenanceDate = etMaintDate.text.toString().ifBlank { today },
                    addedDate           = existing?.addedDate ?: today,
                    reportCount         = existing?.reportCount ?: 0
                )
                db.getReference("equipment/${eq.id}").setValue(eq)
                    .addOnSuccessListener { Toast.makeText(ctx, "Saved ✓", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateEquipmentStatus(eq: Equipment, status: String) {
        db.getReference("equipment/${eq.id}/status").setValue(status)
            .addOnSuccessListener {
                Toast.makeText(context, "Status updated: $status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDeleteEquipment(eq: Equipment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Equipment")
            .setMessage("Delete \"${eq.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                db.getReference("equipment/${eq.id}").removeValue()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun resolveReport(report: EquipmentReport) {
        db.getReference("equipmentReports/${report.id}/status").setValue("Resolved")
        db.getReference("equipment/${report.equipmentId}/status").setValue("Working")
        db.getReference("equipment/${report.equipmentId}/lastMaintenanceDate")
            .setValue(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        Toast.makeText(context, "Marked as resolved ✓", Toast.LENGTH_SHORT).show()
    }

    private fun markInProgress(report: EquipmentReport) {
        db.getReference("equipmentReports/${report.id}/status").setValue("In Progress")
        db.getReference("equipment/${report.equipmentId}/status").setValue("Needs Repair")
        Toast.makeText(context, "Marked as in progress", Toast.LENGTH_SHORT).show()
    }
}

// ── Admin Equipment List Adapter ───────────────────────────────────────────────

class AdminEquipmentListAdapter(
    private val items: List<Equipment>,
    private val onEdit: (Equipment) -> Unit,
    private val onStatus: (Equipment, String) -> Unit,
    private val onDelete: (Equipment) -> Unit
) : RecyclerView.Adapter<AdminEquipmentListAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView     = v.findViewById(R.id.tvAdminEquipName)
        val tvMeta: TextView     = v.findViewById(R.id.tvAdminEquipMeta)
        val tvStatus: TextView   = v.findViewById(R.id.tvAdminEquipStatus)
        val tvReports: TextView  = v.findViewById(R.id.tvAdminEquipReports)
        val btnWorking: TextView = v.findViewById(R.id.btnSetWorking)
        val btnRepair: TextView  = v.findViewById(R.id.btnSetRepair)
        val btnOut: TextView     = v.findViewById(R.id.btnSetOut)
        val btnEdit: TextView    = v.findViewById(R.id.btnAdminEditEquip)
        val btnDel: TextView     = v.findViewById(R.id.btnAdminDeleteEquip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_equipment, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val eq = items[position]
        holder.tvName.text    = eq.name
        holder.tvMeta.text    = "${eq.category} · ${eq.location} · Last maint: ${eq.lastMaintenanceDate.ifBlank { "—" }}"
        holder.tvReports.text = if (eq.reportCount > 0) "⚠️ ${eq.reportCount} reports" else "No reports"

        val color = when (eq.status) {
            "Working"      -> "#43A047"
            "Needs Repair" -> "#FF9800"
            else           -> "#E53935"
        }
        holder.tvStatus.text = eq.status
        holder.tvStatus.setTextColor(Color.parseColor(color))

        holder.btnWorking.setOnClickListener { onStatus(eq, "Working") }
        holder.btnRepair.setOnClickListener  { onStatus(eq, "Needs Repair") }
        holder.btnOut.setOnClickListener     { onStatus(eq, "Out of Order") }
        holder.btnEdit.setOnClickListener    { onEdit(eq) }
        holder.btnDel.setOnClickListener     { onDelete(eq) }
    }
}

// ── Admin Report Adapter ───────────────────────────────────────────────────────

class AdminReportAdapter(
    private val items: List<EquipmentReport>,
    private val onResolve: (EquipmentReport) -> Unit,
    private val onInProgress: (EquipmentReport) -> Unit
) : RecyclerView.Adapter<AdminReportAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvEquip: TextView    = v.findViewById(R.id.tvReportEquipName)
        val tvUser: TextView     = v.findViewById(R.id.tvReportUser)
        val tvIssue: TextView    = v.findViewById(R.id.tvReportIssue)
        val tvDate: TextView     = v.findViewById(R.id.tvReportDate)
        val tvSeverity: TextView = v.findViewById(R.id.tvReportSeverity)
        val tvStatus: TextView   = v.findViewById(R.id.tvReportStatus)
        val btnProgress: TextView = v.findViewById(R.id.btnInProgress)
        val btnResolve: TextView = v.findViewById(R.id.btnResolveReport)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_report, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.tvEquip.text    = r.equipmentName
        holder.tvUser.text     = "By: ${r.username}"
        holder.tvIssue.text    = r.issue
        holder.tvDate.text     = "${r.date} at ${r.time}"

        val sevColor = when (r.severity) {
            "High"   -> "#E53935"
            "Medium" -> "#FF9800"
            else     -> "#43A047"
        }
        holder.tvSeverity.text = r.severity
        holder.tvSeverity.setTextColor(Color.parseColor(sevColor))

        val statusColor = when (r.status) {
            "Resolved"    -> "#43A047"
            "In Progress" -> "#1565C0"
            else          -> "#E53935"
        }
        holder.tvStatus.text = r.status
        holder.tvStatus.setTextColor(Color.parseColor(statusColor))

        val resolved = r.status == "Resolved"
        holder.btnProgress.isEnabled = !resolved
        holder.btnResolve.isEnabled  = !resolved
        holder.btnProgress.alpha = if (resolved) 0.5f else 1f
        holder.btnResolve.alpha  = if (resolved) 0.5f else 1f

        holder.btnProgress.setOnClickListener { if (!resolved) onInProgress(r) }
        holder.btnResolve.setOnClickListener  { if (!resolved) onResolve(r) }
    }
}