package com.hamid.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class Home : AppCompatActivity() {

    private lateinit var homeBtn: ImageView
    private lateinit var addBtn: ImageView
    private lateinit var transactionBtn: ImageView
    private lateinit var profileBtn: ImageView
    private lateinit var analysisBtn : ImageView
    private lateinit var totalBalanceText: TextView
    private lateinit var totalExpenseText: TextView
    private lateinit var userNameText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressMessage: TextView
    private lateinit var notificationBtn: ImageView
    private lateinit var leaderBtn: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        homeBtn = findViewById(R.id.homeBtn)
        transactionBtn = findViewById(R.id.transactionBtn)
        profileBtn = findViewById(R.id.profileBtn)
        analysisBtn = findViewById(R.id.AnalysisBtn)
        addBtn = findViewById(R.id.add)
        userNameText = findViewById(R.id.goodMorningText)
        totalBalanceText = findViewById(R.id.totalBalance)
        totalExpenseText = findViewById(R.id.totalExpense)
        progressBar = findViewById(R.id.progressBar)
        progressMessage = findViewById(R.id.progressMessage)
        notificationBtn = findViewById(R.id.notificationIcon)
        leaderBtn = findViewById(R.id.leaderboardIcon)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Daily"
                1 -> tab.text = "Weekly"
                2 -> tab.text = "Monthly"
            }
        }.attach()

        analysisBtn.setOnClickListener{
            startActivity(Intent(this, Analysis::class.java))
        }

        homeBtn.setOnClickListener {

        }

        transactionBtn.setOnClickListener {
            startActivity(Intent(this, TransactionsList::class.java))
        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, profile::class.java))
        }

        addBtn.setOnClickListener {
            startActivity(Intent(this, Income::class.java))
        }

        notificationBtn.setOnClickListener {
            startActivity(Intent(this, Notification::class.java))
        }

        leaderBtn.setOnClickListener {
            startActivity(Intent(this, Leaderboard::class.java))
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
            db.collection("Users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val income = document.getDouble("income") ?: 0.0
                        val expenditure = document.getDouble("expenditure") ?: 0.0
                        val userName = document.getString("username") ?: ""

                        Log.d("Firebase", "Income: $income, Expenditure: $expenditure")

                        val balance = income - expenditure
                        val progress = if (income > 0) (expenditure / income * 100).toInt() else 0

                        totalBalanceText.text = "$${"%.2f".format(balance)}"
                        totalExpenseText.text = "- $${"%.2f".format(expenditure)}"
                        userNameText.text = userName

                        progressBar.progress = progress
                        progressBar.visibility = View.VISIBLE
                        updateProgressMessage(progress)
                        updateProgressColors(progress)

                        if (progress > 70) {
                            val notification = hashMapOf(
                                "title" to "Overspending Alert",
                                "message" to "You have spent more than 70% of your planned budget. Be cautious!",
                                "timestamp" to System.currentTimeMillis(),
                                "type" to "alert"
                            )

                            val userDocRef = db.collection("Users").document(userId)
                            userDocRef.collection("Notifications")
                                .add(notification)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Overspending notification added")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error adding notification: ${e.message}")
                                }
                        }
                        else if (progress in 31..70) {
                            val notification = hashMapOf(
                                "title" to "Spending Alert",
                                "message" to "You are spending moderately. Keep an eye on your expenses.",
                                "timestamp" to System.currentTimeMillis(),
                                "type" to "alert"
                            )

                            val userDocRef = db.collection("Users").document(userId)
                            userDocRef.collection("Notifications")
                                .add(notification)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Moderate spending notification added")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error adding notification: ${e.message}")
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase", "Error getting documents: ", exception)
                }
        }
    }


    private fun updateProgressMessage(progress: Int) {
        when {
            progress <= 30 -> {
                progressMessage.text = "30% of your expenses. Looks good."
            }
            progress in 31..70 -> {
                progressMessage.text = "Be careful, you're spending more than planned!"
            }
            else -> {
                progressMessage.text = "Warning! You're overspending."
            }
        }
    }

    private fun updateProgressColors(progress: Int) {
        when {
            progress <= 30 -> {
                progressBar.progressTintList = getColorStateList(R.color.green) // Adjust colors for safe spending
                progressMessage.setTextColor(getColor(R.color.green))
            }
            progress in 31..70 -> {
                progressBar.progressTintList = getColorStateList(R.color.orange) // Adjust colors for moderate spending
                progressMessage.setTextColor(getColor(R.color.orange))
            }
            else -> {
                progressBar.progressTintList = getColorStateList(R.color.red) // Adjust colors for overspending
                progressMessage.setTextColor(getColor(R.color.red))
            }
        }
    }
}
