package com.hamid.budgetbuddy

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private val transactionList: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amount: TextView = itemView.findViewById(R.id.amount)
        val category: TextView = itemView.findViewById(R.id.category)
        val date: TextView = itemView.findViewById(R.id.date)
        val type: TextView = itemView.findViewById(R.id.type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]

        holder.amount.text = "$${transaction.amount}"
        holder.category.text = transaction.category

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.date.text = sdf.format(Date(transaction.timestamp))

        holder.type.text = transaction.type

        if (transaction.type.equals("Income", ignoreCase = true)) {
            holder.type.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Green
        } else {
            holder.type.setTextColor(android.graphics.Color.parseColor("#F44336")) // Red
        }
    }

    override fun getItemCount(): Int {
        return transactionList.size
    }
}
