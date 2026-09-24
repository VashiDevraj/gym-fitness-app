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

class MembersFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var progress: View
    private lateinit var etSearch: EditText
    private lateinit var adapter: MemberAdapter

    private val allUsers  = mutableListOf<User>()
    private val shown     = mutableListOf<User>()

    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("users") }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_members, container, false)
        rv       = v.findViewById(R.id.rvMembers)
        progress = v.findViewById(R.id.progressMembers)
        etSearch = v.findViewById(R.id.etSearchMembers)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = MemberAdapter(shown) { user ->
            startActivity(Intent(requireContext(), MemberDetailActivity::class.java)
                .putExtra("uid", user.uid))
        }
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s.toString())
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        loadMembers()
        return v
    }

    private fun filter(query: String) {
        shown.clear()
        val q = query.trim().lowercase()
        shown.addAll(if (q.isEmpty()) allUsers else allUsers.filter {
            it.username.lowercase().contains(q)
        })
        adapter.notifyDataSetChanged()
    }

    private fun loadMembers() {
        progress.visibility = View.VISIBLE
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                snapshot.children.forEach { snap ->
                    val user = User.fromSnapshot(snap)
                    if (user != null && user.accountType.equals("user", ignoreCase = true))
                        allUsers.add(user)
                }
                allUsers.sortBy { it.username.lowercase() }
                filter(etSearch.text.toString())
                progress.visibility = View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                progress.visibility = View.GONE
            }
        })
    }
}