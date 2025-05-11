package com.hamid.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TransactionsList : AppCompatActivity() {

    private lateinit var homeBtn: ImageView
    private lateinit var transactionBtn: ImageView
    private lateinit var profileBtn: ImageView
    private lateinit var analysisBtn : ImageView
    private lateinit var cancel: ImageView
    private lateinit var addBtn: ImageView
    private lateinit var totalBalanceText: TextView
    private lateinit var totalExpenseText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressMessage: TextView
    private lateinit var notificationBtn: ImageView

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private val transactionList = ArrayList<Transaction>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transactions_list)

        homeBtn = findViewById(R.id.homeBtn)
        transactionBtn = findViewById(R.id.transactionBtn)
        profileBtn = findViewById(R.id.profileBtn)
        totalBalanceText = findViewById(R.id.totalBalance)
        totalExpenseText = findViewById(R.id.totalExpense)
        progressBar = findViewById(R.id.progressBar)
        progressMessage = findViewById(R.id.progressMessage)
        analysisBtn = findViewById(R.id.AnalysisBtn)
        cancel = findViewById(R.id.backArrow)
        addBtn = findViewById(R.id.add)
        notificationBtn = findViewById(R.id.notificationIcon)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(transactionList)
        recyclerView.adapter = adapter

        analysisBtn.setOnClickListener{
            startActivity(Intent(this, Analysis::class.java))
        }

        homeBtn.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        transactionBtn.setOnClickListener {

        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, profile::class.java))
        }

        cancel.setOnClickListener {
            finish()
        }

        notificationBtn.setOnClickListener {
            startActivity(Intent(this, Notification::class.java))
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
            db.collection("Users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val income = document.getDouble("income") ?: 0.0
                        val expenditure = document.getDouble("expenditure") ?: 0.0

                        Log.d("Firebase", "Income: $income, Expenditure: $expenditure")

                        val balance = income - expenditure
                        val progress = if (income > 0) (expenditure / income * 100).toInt() else 0

                        totalBalanceText.text = "$${"%.2f".format(balance)}"
                        totalExpenseText.text = "- $${"%.2f".format(expenditure)}"

                        progressBar.progress = progress
                        progressBar.visibility = View.VISIBLE
                        updateProgressMessage(progress)
                        updateProgressColors(progress)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase", "Error getting documents: ", exception)
                }

            db.collection("Users")
                .document(userId)
                .collection("Transaction")
                .get()
                .addOnSuccessListener { documents ->
                    transactionList.clear()
                    for (document in documents) {
                        val transaction = document.toObject(Transaction::class.java)
                        transactionList.add(transaction)
                    }
                    transactionList.reverse()
                    adapter = TransactionAdapter(transactionList)
                    recyclerView.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase", "Error getting transactions: ", exception)
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