package com.example.gymapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class DietPlansFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var progress: View
    private lateinit var etSearch: EditText
    private lateinit var adapter: DietSummaryAdapter

    private val allUsers = mutableListOf<User>()
    private val shown    = mutableListOf<User>()

    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("users") }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_diet_plans, container, false)
        rv       = v.findViewById(R.id.rvDietPlans)
        progress = v.findViewById(R.id.progressDietPlans)
        etSearch = v.findViewById(R.id.etSearchDiet)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = DietSummaryAdapter(shown) { user ->
            startActivity(Intent(requireContext(), MemberDetailActivity::class.java)
                .putExtra("uid", user.uid))
        }
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s.toString())
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        loadData()
        return v
    }

    private fun filter(q: String) {
        shown.clear()
        shown.addAll(if (q.isBlank()) allUsers
        else allUsers.filter { it.username.lowercase().contains(q.trim().lowercase()) })
        adapter.notifyDataSetChanged()
    }

    private fun loadData() {
        progress.visibility = View.VISIBLE
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                snapshot.children.forEach { snap ->
                    val u = User.fromSnapshot(snap)
                    if (u != null && u.accountType.equals("user", ignoreCase = true))
                        allUsers.add(u)
                }
                allUsers.sortBy { it.username.lowercase() }
                filter(etSearch.text.toString())
                progress.visibility = View.GONE
            }
            override fun onCancelled(e: DatabaseError) { progress.visibility = View.GONE }
        })
    }
}