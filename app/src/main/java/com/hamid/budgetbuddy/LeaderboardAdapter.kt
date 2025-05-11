package com.hamid.budgetbuddy

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    private val users = mutableListOf<LeaderboardUser>()

    fun submitList(list: List<LeaderboardUser>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.userName)
        val percentage: TextView = view.findViewById(R.id.percentage)
        val rankNumber: TextView = view.findViewById(R.id.rankNumber)
        val rankBadge: ImageView = view.findViewById(R.id.rankBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.userName.text = user.username
        holder.percentage.text = String.format("%.1f%%", user.percentage)

        holder.rankBadge.visibility = View.GONE
        holder.rankNumber.visibility = View.VISIBLE
        holder.rankNumber.text = (position + 1).toString()

        // Reset styles
        holder.itemView.setBackgroundColor(Color.WHITE)
        holder.userName.setTypeface(null, Typeface.NORMAL)
        holder.percentage.setTypeface(null, Typeface.NORMAL)

        when (position) {
            0 -> {
                holder.rankBadge.setImageResource(R.drawable.ic_gold)
                holder.rankBadge.visibility = View.VISIBLE
                holder.rankNumber.visibility = View.GONE
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.gold_highlight))
                holder.userName.setTypeface(null, Typeface.BOLD)
                holder.percentage.setTypeface(null, Typeface.BOLD)
            }
            1 -> {
                holder.rankBadge.setImageResource(R.drawable.ic_silver)
                holder.rankBadge.visibility = View.VISIBLE
                holder.rankNumber.visibility = View.GONE
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.silver_highlight))
                holder.userName.setTypeface(null, Typeface.BOLD)
                holder.percentage.setTypeface(null, Typeface.BOLD)
            }
            2 -> {
                holder.rankBadge.setImageResource(R.drawable.ic_bronze)
                holder.rankBadge.visibility = View.VISIBLE
                holder.rankNumber.visibility = View.GONE
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.bronze_highlight))
                holder.userName.setTypeface(null, Typeface.BOLD)
                holder.percentage.setTypeface(null, Typeface.BOLD)
            }
        }
    }

}
