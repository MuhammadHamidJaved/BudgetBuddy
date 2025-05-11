package com.hamid.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Notification : AppCompatActivity() {

    private lateinit var homeBtn: ImageView
    private lateinit var transactionBtn: ImageView
    private lateinit var profileBtn: ImageView
    private lateinit var analysisBtn: ImageView
    private lateinit var cancel: ImageView
    private lateinit var addBtn: ImageView

    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        homeBtn = findViewById(R.id.homeBtn)
        transactionBtn = findViewById(R.id.transactionBtn)
        profileBtn = findViewById(R.id.profileBtn)
        analysisBtn = findViewById(R.id.AnalysisBtn)
        cancel = findViewById(R.id.backArrow)
        addBtn = findViewById(R.id.add)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        homeBtn.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        transactionBtn.setOnClickListener {
            startActivity(Intent(this, TransactionsList::class.java))
        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, profile::class.java))
        }

        analysisBtn.setOnClickListener {
            startActivity(Intent(this, Analysis::class.java))
        }

        cancel.setOnClickListener {
            finish()
        }

        addBtn.setOnClickListener {
            startActivity(Intent(this, Income::class.java))
        }

        fetchDataFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        fetchDataFromFirebase()
    }

    private fun fetchDataFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("Users").document(userId)
                .collection("Notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val notifications = mutableListOf<NotificationData>()
                    for (document in documents) {
                        val notification = document.toObject(NotificationData::class.java)
                        notifications.add(notification)
                    }
                    recyclerView.adapter = NotificationAdapter(notifications)
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationActivity", "Error loading notifications", e)
                }
        }
    }
}
