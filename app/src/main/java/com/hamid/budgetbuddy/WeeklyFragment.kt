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
import java.util.*

class WeeklyFragment : Fragment() {

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

            // Get the start of the current week (Sunday)
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.SUNDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)  // Set to Sunday
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val weekEnd = calendar.timeInMillis

            db.collection("Users")
                .document(userId)
                .collection("Transaction")
                .whereGreaterThanOrEqualTo("timestamp", weekStart)
                .whereLessThan("timestamp", weekEnd)
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
