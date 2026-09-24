package com.example.gymapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AdminWorkoutActivity : AppCompatActivity() {

    // ── Views ─────────────────────────────────────────────────────────────
    private lateinit var rvWorkouts: RecyclerView
    private lateinit var btnAdd: com.google.android.material.button.MaterialButton
    private lateinit var progress: ProgressBar
    private lateinit var tvWorkoutCount: TextView
    private lateinit var spinnerFilterGoal: Spinner
    private lateinit var spinnerFilterCat: Spinner
    private lateinit var tvEmptyState: TextView

    // ── Firebase ──────────────────────────────────────────────────────────
    private val db = FirebaseDatabase.getInstance()

    // ── Data ──────────────────────────────────────────────────────────────
    private val allWorkouts = mutableListOf<WorkoutItem>()
    private val filteredWorkouts = mutableListOf<WorkoutItem>()
    private var filterGoal = "All"
    private var filterCat = "All"

    // ── Gallery / dialog state ────────────────────────────────────────────
    private val PICK_IMAGE_REQUEST = 101
    private var pendingImageView: ImageView? = null
    private var pendingBase64Callback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_workout)

        supportActionBar?.title = "💪 Manage Workouts"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvWorkouts = findViewById(R.id.rvAdminWorkouts)
        btnAdd = findViewById(R.id.btnAddWorkout)
        progress = findViewById(R.id.progressAdminWorkout)
        tvWorkoutCount = findViewById(R.id.tvAdminWorkoutCount)
        spinnerFilterGoal = findViewById(R.id.spinnerAdminFilterGoal)
        spinnerFilterCat = findViewById(R.id.spinnerAdminFilterCat)
        tvEmptyState = findViewById(R.id.tvAdminEmptyState)

        rvWorkouts.layoutManager = LinearLayoutManager(this)

        setupFilterSpinners()
        loadWorkouts()

        btnAdd.setOnClickListener { showWorkoutDialog(null) }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    // ── Gallery result ────────────────────────────────────────────────────
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                val base64 = convertImageToBase64(uri)
                pendingImageView?.let { iv ->
                    iv.visibility = View.VISIBLE
                    Glide.with(this).load(uri).centerCrop().into(iv)
                }
                pendingBase64Callback?.invoke(base64)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertImageToBase64(uri: Uri): String {
        val inputStream: InputStream = contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image")
        val options = BitmapFactory.Options().apply {
            inSampleSize = 2  // Downsample to reduce size
        }
        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            ?: throw Exception("Cannot decode image")
        inputStream.close()

        // Compress to JPEG at 60% quality — keeps size manageable for Realtime DB
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        val bytes = baos.toByteArray()

        if (bytes.size > 4 * 1024 * 1024) {
            throw Exception("Image too large (max ~3MB). Please choose a smaller image.")
        }

        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // ── Filter spinners ───────────────────────────────────────────────────
    private fun setupFilterSpinners() {
        val goals = listOf("All", "lose_weight", "gain_weight", "maintain")
        val cats = listOf("All", "Cardio", "Chest", "Back", "Legs", "Abs", "Arms", "Shoulders")
        spinnerSetup(spinnerFilterGoal, goals)
        spinnerSetup(spinnerFilterCat, cats)

        spinnerFilterGoal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                filterGoal = goals[pos]; applyFilters()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerFilterCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                filterCat = cats[pos]; applyFilters()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ── Load all workouts from Firebase ───────────────────────────────────
    private fun loadWorkouts() {
        progress.visibility = View.VISIBLE
        allWorkouts.clear()
        val goals = listOf("lose_weight", "gain_weight", "maintain")
        var loaded = 0
        for (goal in goals) {
            db.getReference("workouts/$goal")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        for (child in snap.children) {
                            val w = child.getValue(WorkoutItem::class.java) ?: continue
                            w.id = child.key ?: ""
                            allWorkouts.add(w)
                        }
                        loaded++
                        if (loaded == goals.size) {
                            progress.visibility = View.GONE
                            applyFilters()
                        }
                    }
                    override fun onCancelled(e: DatabaseError) {
                        loaded++
                        if (loaded == goals.size) progress.visibility = View.GONE
                    }
                })
        }
    }

    private fun applyFilters() {
        filteredWorkouts.clear()
        for (w in allWorkouts) {
            val goalOk = filterGoal == "All" || w.goal == filterGoal
            val catOk = filterCat == "All" || w.category.equals(filterCat, ignoreCase = true)
            if (goalOk && catOk) filteredWorkouts.add(w)
        }
        tvWorkoutCount.text = "${filteredWorkouts.size} workouts"
        tvEmptyState.visibility = if (filteredWorkouts.isEmpty()) View.VISIBLE else View.GONE

        rvWorkouts.adapter = AdminWorkoutListAdapter(
            items = filteredWorkouts,
            onEdit = { showWorkoutDialog(it) },
            onDelete = { confirmDelete(it) },
            onManageMedia = { showMediaDialog(it) }
        )
    }

    // ── Add / Edit workout dialog ─────────────────────────────────────────
    private fun showWorkoutDialog(existing: WorkoutItem?) {
        val dv = LayoutInflater.from(this).inflate(R.layout.dialog_add_workout_admin, null)

        val etName = dv.findViewById<EditText>(R.id.etAdminWorkoutName)
        val etDesc = dv.findViewById<EditText>(R.id.etAdminWorkoutDesc)
        val etSets = dv.findViewById<EditText>(R.id.etAdminWorkoutSets)
        val etReps = dv.findViewById<EditText>(R.id.etAdminWorkoutReps)
        val etCals = dv.findViewById<EditText>(R.id.etAdminWorkoutCals)
        val etDuration = dv.findViewById<EditText>(R.id.etAdminWorkoutDuration)
        val etMuscles = dv.findViewById<EditText>(R.id.etAdminMuscles)
        val etSteps = dv.findViewById<EditText>(R.id.etAdminSteps)
        val etBreathing = dv.findViewById<EditText>(R.id.etAdminBreathing)
        val etPoints = dv.findViewById<EditText>(R.id.etAdminPoints)
        val etEquipment = dv.findViewById<EditText>(R.id.etAdminEquipment)
        val etGifUrl = dv.findViewById<EditText>(R.id.etAdminGifUrl)
        val etEmoji = dv.findViewById<EditText>(R.id.etAdminEmoji)
        val imgPreview = dv.findViewById<ImageView>(R.id.imgGifPreview)
        val btnPreviewUrl = dv.findViewById<Button>(R.id.btnPreviewGif)
        val btnPickGallery = dv.findViewById<Button>(R.id.btnPickFromGallery)
        val tvMediaStatus = dv.findViewById<TextView>(R.id.tvMediaStatus)
        val spGoal = dv.findViewById<Spinner>(R.id.spAdminGoal)
        val spCategory = dv.findViewById<Spinner>(R.id.spAdminCategory)
        val spDifficulty = dv.findViewById<Spinner>(R.id.spAdminDifficulty)

        spinnerSetup(spGoal, listOf("lose_weight", "gain_weight", "maintain"))
        spinnerSetup(spCategory, listOf("Cardio", "Chest", "Back", "Legs", "Abs", "Arms", "Shoulders"))
        spinnerSetup(spDifficulty, listOf("Beginner", "Intermediate", "Advanced"))

        // Holds base64 if user picks from gallery within this dialog
        var localBase64 = ""

        // Pre-fill for edit
        existing?.let { w ->
            etName.setText(w.name)
            etDesc.setText(w.description)
            etSets.setText(w.sets)
            etReps.setText(w.reps)
            etCals.setText(w.caloriesBurn.toString())
            etDuration.setText(w.durationMinutes.toString())
            etMuscles.setText(w.musclesTargeted)
            etSteps.setText(w.steps)
            etBreathing.setText(w.breathingTip)
            etPoints.setText(w.pointsToRemember)
            etEquipment.setText(w.equipment)
            etGifUrl.setText(w.gifUrl)
            etEmoji.setText(w.emoji)
            localBase64 = w.gifBase64
            spinnerSelect(spGoal, w.goal)
            spinnerSelect(spCategory, w.category)
            spinnerSelect(spDifficulty, w.difficulty)

            when {
                w.gifUrl.isNotBlank() -> {
                    imgPreview.visibility = View.VISIBLE
                    tvMediaStatus.text = "✅ URL image set"
                    tvMediaStatus.setTextColor(Color.parseColor("#43A047"))
                    Glide.with(this).load(w.gifUrl).diskCacheStrategy(DiskCacheStrategy.DATA).centerCrop().into(imgPreview)
                }
                w.gifBase64.isNotBlank() -> {
                    imgPreview.visibility = View.VISIBLE
                    tvMediaStatus.text = "✅ Gallery image set"
                    tvMediaStatus.setTextColor(Color.parseColor("#43A047"))
                    Glide.with(this).load(w.gifBase64).centerCrop().into(imgPreview)
                }
                else -> {
                    tvMediaStatus.text = "⚠️ No image — add one below"
                    tvMediaStatus.setTextColor(Color.parseColor("#FF9800"))
                }
            }
        }

        // Preview URL button
        btnPreviewUrl.setOnClickListener {
            val url = etGifUrl.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(this, "Enter a GIF/image URL first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            imgPreview.visibility = View.VISIBLE
            localBase64 = "" // URL takes priority, clear base64
            tvMediaStatus.text = "Loading URL preview..."
            tvMediaStatus.setTextColor(Color.parseColor("#FF9800"))
            Glide.with(this).load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.holo_red_light)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                        tvMediaStatus.text = "❌ URL failed to load"
                        tvMediaStatus.setTextColor(Color.parseColor("#E53935"))
                        return false
                    }
                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean): Boolean {
                        tvMediaStatus.text = "✅ URL image loaded"
                        tvMediaStatus.setTextColor(Color.parseColor("#43A047"))
                        return false
                    }
                })
                .into(imgPreview)
        }

        // Pick from gallery button
        btnPickGallery.setOnClickListener {
            pendingImageView = imgPreview
            pendingBase64Callback = { base64 ->
                localBase64 = base64
                etGifUrl.setText("") // Clear URL when gallery image is chosen
                tvMediaStatus.text = "✅ Gallery image selected"
                tvMediaStatus.setTextColor(Color.parseColor("#43A047"))
            }
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "➕ Add Workout" else "✏️ Edit Workout")
            .setView(dv)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, "Exercise name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val goalVal = spGoal.selectedItem.toString()
                val id = existing?.id
                    ?: (db.getReference("workouts/$goalVal").push().key ?: return@setPositiveButton)

                val gifUrl = etGifUrl.text.toString().trim()
                val emoji = etEmoji.text.toString().trim().ifBlank { "🏋️" }

                val item = WorkoutItem(
                    id = id,
                    name = name,
                    description = etDesc.text.toString().trim(),
                    gifUrl = gifUrl,
                    gifBase64 = localBase64,
                    sets = etSets.text.toString().ifBlank { "3" },
                    reps = etReps.text.toString().ifBlank { "12" },
                    caloriesBurn = etCals.text.toString().toIntOrNull() ?: 0,
                    goal = goalVal,
                    category = spCategory.selectedItem.toString(),
                    durationMinutes = etDuration.text.toString().toIntOrNull() ?: 0,
                    difficulty = spDifficulty.selectedItem.toString(),
                    musclesTargeted = etMuscles.text.toString().trim(),
                    steps = etSteps.text.toString().trim(),
                    breathingTip = etBreathing.text.toString().trim(),
                    pointsToRemember = etPoints.text.toString().trim(),
                    equipment = etEquipment.text.toString().ifBlank { "None" },
                    emoji = emoji,
                    isActive = true,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                writeToDb(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Media management dialog for existing workout ──────────────────────
    private fun showMediaDialog(workout: WorkoutItem) {
        val dv = LayoutInflater.from(this).inflate(R.layout.dialog_gif_url, null)
        val etUrl = dv.findViewById<EditText>(R.id.etGifUrlInput)
        val imgPrev = dv.findViewById<ImageView>(R.id.imgGifPreviewDialog)
        val btnPrev = dv.findViewById<Button>(R.id.btnPreviewGifDialog)
        val btnGallery = dv.findViewById<Button>(R.id.btnPickGalleryDialog)
        val tvSample = dv.findViewById<TextView>(R.id.tvGifSampleUrls)
        val tvStatus = dv.findViewById<TextView>(R.id.tvMediaStatusDialog)

        var localBase64 = workout.gifBase64
        etUrl.setText(workout.gifUrl)

        tvSample.text = "💡 Free GIF sources:\n• giphy.com → Share → Copy GIF Link\n• tenor.com → Share → Direct Link\n• imgur.com → any direct .gif or .jpg link\n\nOR pick any image/GIF from your gallery below ↓"

        // Show current image
        when {
            workout.gifUrl.isNotBlank() -> {
                imgPrev.visibility = View.VISIBLE
                tvStatus.text = "✅ Currently showing URL image"
                tvStatus.setTextColor(Color.parseColor("#43A047"))
                Glide.with(this).load(workout.gifUrl).diskCacheStrategy(DiskCacheStrategy.DATA).centerCrop().into(imgPrev)
            }
            workout.gifBase64.isNotBlank() -> {
                imgPrev.visibility = View.VISIBLE
                tvStatus.text = "✅ Currently showing gallery image"
                tvStatus.setTextColor(Color.parseColor("#43A047"))
                Glide.with(this).load(workout.gifBase64).centerCrop().into(imgPrev)
            }
            else -> {
                tvStatus.text = "⚠️ No image set"
                tvStatus.setTextColor(Color.parseColor("#FF9800"))
            }
        }

        btnPrev.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isBlank()) { Toast.makeText(this, "Enter a URL first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            imgPrev.visibility = View.VISIBLE
            localBase64 = ""
            Glide.with(this).load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.holo_red_light)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                        tvStatus.text = "❌ URL failed to load"
                        tvStatus.setTextColor(Color.parseColor("#E53935"))
                        return false
                    }
                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean): Boolean {
                        tvStatus.text = "✅ URL loaded — tap Save"
                        tvStatus.setTextColor(Color.parseColor("#43A047"))
                        return false
                    }
                })
                .into(imgPrev)
        }

        btnGallery.setOnClickListener {
            pendingImageView = imgPrev
            pendingBase64Callback = { base64 ->
                localBase64 = base64
                etUrl.setText("")
                tvStatus.text = "✅ Gallery image selected — tap Save"
                tvStatus.setTextColor(Color.parseColor("#43A047"))
            }
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        AlertDialog.Builder(this)
            .setTitle("🖼️ Manage Image — ${workout.name}")
            .setView(dv)
            .setPositiveButton("Save") { _, _ ->
                val url = etUrl.text.toString().trim()
                workout.gifUrl = url
                workout.gifBase64 = localBase64

                val updates = mapOf<String, Any>(
                    "workouts/${workout.goal}/${workout.id}/gifUrl" to url,
                    "workouts/${workout.goal}/${workout.id}/gifBase64" to localBase64
                )
                progress.visibility = View.VISIBLE
                db.reference.updateChildren(updates)
                    .addOnSuccessListener {
                        progress.visibility = View.GONE
                        Toast.makeText(this, "✅ Image saved!", Toast.LENGTH_SHORT).show()
                        val idx = allWorkouts.indexOfFirst { it.id == workout.id }
                        if (idx >= 0) allWorkouts[idx] = workout
                        applyFilters()
                    }
                    .addOnFailureListener {
                        progress.visibility = View.GONE
                        Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNeutralButton("Remove Image") { _, _ ->
                workout.gifUrl = ""
                workout.gifBase64 = ""
                val updates = mapOf<String, Any>(
                    "workouts/${workout.goal}/${workout.id}/gifUrl" to "",
                    "workouts/${workout.goal}/${workout.id}/gifBase64" to ""
                )
                db.reference.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show()
                    val idx = allWorkouts.indexOfFirst { it.id == workout.id }
                    if (idx >= 0) allWorkouts[idx] = workout
                    applyFilters()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Write to Firebase DB ──────────────────────────────────────────────
    private fun writeToDb(item: WorkoutItem) {
        progress.visibility = View.VISIBLE
        db.getReference("workouts/${item.goal}/${item.id}").setValue(item)
            .addOnSuccessListener {
                progress.visibility = View.GONE
                Toast.makeText(this, "✅ ${item.name} saved!", Toast.LENGTH_SHORT).show()
                allWorkouts.clear()
                loadWorkouts()
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                Toast.makeText(this, "Save failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ── Delete ────────────────────────────────────────────────────────────
    private fun confirmDelete(item: WorkoutItem) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Delete ${item.name}?")
            .setMessage("This permanently removes the workout for all users.")
            .setPositiveButton("Delete") { _, _ ->
                progress.visibility = View.VISIBLE
                db.getReference("workouts/${item.goal}/${item.id}").removeValue()
                    .addOnSuccessListener {
                        progress.visibility = View.GONE
                        Toast.makeText(this, "${item.name} deleted", Toast.LENGTH_SHORT).show()
                        allWorkouts.removeAll { it.id == item.id }
                        applyFilters()
                    }
                    .addOnFailureListener {
                        progress.visibility = View.GONE
                        Toast.makeText(this, "Delete failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Spinner helpers ───────────────────────────────────────────────────
    private fun spinnerSetup(s: Spinner, items: List<String>) {
        val a = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        s.adapter = a
    }

    private fun spinnerSelect(s: Spinner, value: String) {
        val a = s.adapter as? ArrayAdapter<*> ?: return
        for (i in 0 until a.count) {
            if (a.getItem(i).toString() == value) { s.setSelection(i); break }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Admin RecyclerView Adapter
// ═══════════════════════════════════════════════════════════════════════════
class AdminWorkoutListAdapter(
    private val items: List<WorkoutItem>,
    private val onEdit: (WorkoutItem) -> Unit,
    private val onDelete: (WorkoutItem) -> Unit,
    private val onManageMedia: (WorkoutItem) -> Unit
) : RecyclerView.Adapter<AdminWorkoutListAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvAdminWorkoutName)
        val tvGoal: TextView = v.findViewById(R.id.tvAdminWorkoutGoal)
        val tvCat: TextView = v.findViewById(R.id.tvAdminWorkoutCat)
        val tvDiff: TextView = v.findViewById(R.id.tvAdminWorkoutDiff)
        val tvCals: TextView = v.findViewById(R.id.tvAdminWorkoutCals)
        val tvDuration: TextView = v.findViewById(R.id.tvAdminWorkoutDuration)
        val tvMuscles: TextView = v.findViewById(R.id.tvAdminWorkoutMuscles)
        val tvGifStatus: TextView = v.findViewById(R.id.tvAdminGifStatus)
        val imgGif: ImageView = v.findViewById(R.id.imgAdminWorkoutGif)
        val tvEmoji: TextView = v.findViewById(R.id.tvAdminWorkoutEmoji)
        val btnEdit: Button = v.findViewById(R.id.btnAdminEdit)
        val btnDelete: Button = v.findViewById(R.id.btnAdminDelete)
        val btnSetMedia: Button = v.findViewById(R.id.btnAdminSetGif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_workout, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val w = items[pos]
        h.tvName.text = w.name
        h.tvGoal.text = w.goal.replace("_", " ").replaceFirstChar { it.uppercase() }
        h.tvCat.text = "📁 ${w.category}"
        h.tvDiff.text = w.difficulty
        h.tvCals.text = "🔥 ${w.caloriesBurn} kcal"
        h.tvDuration.text = "⏱ ${w.durationMinutes} min"
        h.tvMuscles.text = if (w.musclesTargeted.isNotBlank())
            "💪 ${w.musclesTargeted.split(",").take(3).joinToString(", ")}" else ""

        h.tvDiff.setTextColor(Color.parseColor(when (w.difficulty) {
            "Beginner" -> "#43A047"
            "Intermediate" -> "#FF9800"
            else -> "#E53935"
        }))

        // Show image preview
        val visualSrc = w.getVisualSource()
        if (visualSrc.isNotBlank()) {
            h.tvEmoji.visibility = View.GONE
            h.imgGif.visibility = View.VISIBLE
            h.tvGifStatus.text = if (w.gifBase64.isNotBlank()) "✅ Gallery image" else "✅ URL image"
            h.tvGifStatus.setTextColor(Color.parseColor("#43A047"))
            Glide.with(h.imgGif.context)
                .load(visualSrc)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(h.imgGif)
        } else {
            h.imgGif.setImageDrawable(null)
            h.imgGif.visibility = View.GONE
            h.tvEmoji.text = w.emoji
            h.tvEmoji.visibility = View.VISIBLE
            h.tvGifStatus.text = "⚠️ No image — tap Image"
            h.tvGifStatus.setTextColor(Color.parseColor("#FF9800"))
        }

        h.btnEdit.setOnClickListener { onEdit(w) }
        h.btnDelete.setOnClickListener { onDelete(w) }
        h.btnSetMedia.setOnClickListener { onManageMedia(w) }
    }
}