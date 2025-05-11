package com.hamid.budgetbuddy

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class WeeklyAnalysis : Fragment() {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val weeksOfMonth = arrayOf("Week 1", "Week 2", "Week 3", "Week 4")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_analysis, container, false)
        barChart = view.findViewById(R.id.barChart)
        pieChart = view.findViewById(R.id.pieChart)
        fetchDataAndDisplayChart()
        return view
    }

    private fun fetchDataAndDisplayChart() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("Users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("username") ?: "Unknown"
                Log.d("WeeklyAnalysis", "User: $userName ($userId)")

                val startOfMonth = getStartOfMonth()
                db.collection("Users").document(userId)
                    .collection("Transaction")
                    .whereGreaterThanOrEqualTo("timestamp", startOfMonth.time) // timestamp in millis
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val incomeByWeek = FloatArray(4) { 0f }
                        val expenseByWeek = FloatArray(4) { 0f }

                        for (doc in snapshot.documents) {
                            val type = doc.getString("type") ?: continue
                            val amount = doc.getDouble("amount")?.toFloat() ?: continue
                            val timestampMillis = doc.getLong("timestamp") ?: continue
                            val date = Date(timestampMillis)

                            val weekIndex = getWeekIndexFromTimestamp(date)
                            if (weekIndex in 0..3) {
                                if (type.equals("Income", ignoreCase = true)) {
                                    incomeByWeek[weekIndex] += amount
                                    Log.d("WeeklyAnalysis", "$userName - Income: $amount in ${weeksOfMonth[weekIndex]}")
                                } else if (type.equals("Expense", ignoreCase = true)) {
                                    expenseByWeek[weekIndex] += amount
                                    Log.d("WeeklyAnalysis", "$userName - Expense: $amount in ${weeksOfMonth[weekIndex]}")
                                } else {
                                    Log.d("WeeklyAnalysis", "$userName - Unknown type: $type")
                                }
                            } else {
                                Log.d("WeeklyAnalysis", "$userName - Invalid week index: $weekIndex")
                            }
                        }

                        incomeByWeek.forEachIndexed { i, value ->
                            Log.d("WeeklyAnalysis", "$userName - Total Income in ${weeksOfMonth[i]}: $value")
                        }
                        expenseByWeek.forEachIndexed { i, value ->
                            Log.d("WeeklyAnalysis", "$userName - Total Expense in ${weeksOfMonth[i]}: $value")
                        }

                        showBarChart(incomeByWeek, expenseByWeek)
                        showPieChart(incomeByWeek.sum(), expenseByWeek.sum())
                    }
                    .addOnFailureListener { e ->
                        Log.e("WeeklyAnalysis", "Error fetching transactions", e)
                    }

            }
            .addOnFailureListener { e ->
                Log.e("WeeklyAnalysis", "Error fetching user data", e)
            }
    }

    private fun getStartOfMonth(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1) // Set the date to the 1st of the month
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getWeekIndexFromTimestamp(date: Date): Int {
        val cal = Calendar.getInstance()
        cal.time = date
        val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)
        return when (weekOfMonth) {
            1 -> 0 // Week 1
            2 -> 1 // Week 2
            3 -> 2 // Week 3
            4 -> 3 // Week 4
            else -> -1 // Invalid week (can be handled differently if needed)
        }
    }

    private fun showBarChart(income: FloatArray, expenditure: FloatArray) {
        val entries = ArrayList<BarEntry>()
        for (i in 0..3) {
            entries.add(BarEntry(i.toFloat(), floatArrayOf(income[i], expenditure[i])))
        }

        val dataSet = BarDataSet(entries, "Income vs Expenditure").apply {
            stackLabels = arrayOf("Income", "Expenditure")
            setColors(
                resources.getColor(R.color.green, null),
                resources.getColor(R.color.red, null)
            )
        }

        val data = BarData(dataSet)
        barChart.data = data

        barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in 0..3) weeksOfMonth[index] else ""
            }
        }

        barChart.description = Description().apply { text = "Monthly Analysis (Weeks 1–4)" }
        barChart.setFitBars(true)
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun showPieChart(income: Float, expenditure: Float) {
        val entries = listOf(
            PieEntry(income, "Income"),
            PieEntry(expenditure, "Expenditure")
        )

        val dataSet = PieDataSet(entries, "").apply {
            setColors(
                resources.getColor(R.color.green, null),
                resources.getColor(R.color.red, null)
            )
        }

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description = Description().apply { text = "Total Income vs Expenditure" }
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
