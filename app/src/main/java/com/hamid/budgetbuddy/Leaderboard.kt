package com.hamid.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Leaderboard : AppCompatActivity() {
    private lateinit var homeBtn: ImageView
    private lateinit var transactionBtn: ImageView
    private lateinit var profileBtn: ImageView
    private lateinit var analysisBtn: ImageView
    private lateinit var cancel: ImageView
    private lateinit var addBtn: ImageView

    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val leaderboardAdapter = LeaderboardAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_leaderboard)

        homeBtn = findViewById(R.id.homeBtn)
        transactionBtn = findViewById(R.id.transactionBtn)
        profileBtn = findViewById(R.id.profileBtn)
        analysisBtn = findViewById(R.id.AnalysisBtn)
        cancel = findViewById(R.id.backArrow)
        addBtn = findViewById(R.id.add)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = leaderboardAdapter

        homeBtn.setOnClickListener { startActivity(Intent(this, Home::class.java)) }
        transactionBtn.setOnClickListener { startActivity(Intent(this, TransactionsList::class.java)) }
        profileBtn.setOnClickListener { startActivity(Intent(this, profile::class.java)) }
        analysisBtn.setOnClickListener { startActivity(Intent(this, Income::class.java)) }
        cancel.setOnClickListener { finish() }
        addBtn.setOnClickListener { startActivity(Intent(this, Income::class.java)) }

        fetchLeaderboard()
    }

    private fun fetchLeaderboard() {
        val usersList = mutableListOf<LeaderboardUser>()
        db.collection("Users").get().addOnSuccessListener { result ->
            for (document in result) {
                val income = document.getDouble("income") ?: 0.0
                val expenditure = document.getDouble("expenditure") ?: 0.0
                val username = document.getString("username") ?: "User"
                usersList.add(LeaderboardUser(username, income, expenditure))
            }
            val sortedUsers = usersList.sortedByDescending { it.percentage }
            leaderboardAdapter.submitList(sortedUsers)
        }
    }
}
