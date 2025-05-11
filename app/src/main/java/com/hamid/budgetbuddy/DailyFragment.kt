package com.hamid.budgetbuddy

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DailyFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private val transactionList = ArrayList<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.recycler_fragment, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(transactionList)
        recyclerView.adapter = adapter

        fetchTransactions()

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchTransactions()
    }

    private fun fetchTransactions() {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            val now = java.util.Calendar.getInstance()
            now.set(java.util.Calendar.HOUR_OF_DAY, 0)
            now.set(java.util.Calendar.MINUTE, 0)
            now.set(java.util.Calendar.SECOND, 0)
            now.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = now.timeInMillis

            now.add(java.util.Calendar.DAY_OF_MONTH, 1)
            val tomorrowStart = now.timeInMillis

            db.collection("Users")
                .document(userId)
                .collection("Transaction")
                .whereGreaterThanOrEqualTo("timestamp", todayStart)
                .whereLessThan("timestamp", tomorrowStart)
                .get()
                .addOnSuccessListener { documents ->
                    transactionList.clear()
                    for (document in documents) {
                        val transaction = document.toObject(Transaction::class.java)
                        transactionList.add(transaction)
                    }
                    transactionList.reverse()
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    // handle error
                }
        }
    }

}
