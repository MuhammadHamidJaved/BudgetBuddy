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

class DailyAnalysis : Fragment() {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val daysOfWeek = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

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
                Log.d("DailyAnalysis", "User: $userName ($userId)")

                val startOfWeek = getStartOfWeek()
                db.collection("Users").document(userId)
                    .collection("Transaction")
                    .whereGreaterThanOrEqualTo("timestamp", startOfWeek.time) // timestamp in millis
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val incomeByDay = FloatArray(7) { 0f }
                        val expenseByDay = FloatArray(7) { 0f }

                        for (doc in snapshot.documents) {
                            val type = doc.getString("type") ?: continue
                            val amount = doc.getDouble("amount")?.toFloat() ?: continue
                            val timestampMillis = doc.getLong("timestamp") ?: continue
                            val date = Date(timestampMillis)

                            val dayIndex = getDayIndexFromTimestamp(date)
                            if (dayIndex in 0..6) {
                                if (type.equals("Income", ignoreCase = true)) {
                                    incomeByDay[dayIndex] += amount
                                    Log.d("DailyAnalysis", "$userName - Income: $amount on ${daysOfWeek[dayIndex]}")
                                } else if (type.equals("Expense", ignoreCase = true)) {
                                    expenseByDay[dayIndex] += amount
                                    Log.d("DailyAnalysis", "$userName - Expense: $amount on ${daysOfWeek[dayIndex]}")
                                } else {
                                    Log.d("DailyAnalysis", "$userName - Unknown type: $type")
                                }
                            } else {
                                Log.d("DailyAnalysis", "$userName - Invalid day index: $dayIndex")
                            }
                        }

                        incomeByDay.forEachIndexed { i, value ->
                            Log.d("DailyAnalysis", "$userName - Total Income on ${daysOfWeek[i]}: $value")
                        }
                        expenseByDay.forEachIndexed { i, value ->
                            Log.d("DailyAnalysis", "$userName - Total Expense on ${daysOfWeek[i]}: $value")
                        }

                        showBarChart(incomeByDay, expenseByDay)
                        showPieChart(incomeByDay.sum(), expenseByDay.sum())
                    }
                    .addOnFailureListener { e ->
                        Log.e("DailyAnalysis", "Error fetching transactions", e)
                    }

            }
            .addOnFailureListener { e ->
                Log.e("DailyAnalysis", "Error fetching user data", e)
            }
    }

    private fun getStartOfWeek(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getDayIndexFromTimestamp(date: Date): Int {
        val cal = Calendar.getInstance()
        cal.time = date
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> -1
        }
    }

    private fun showBarChart(income: FloatArray, expenditure: FloatArray) {
        val entries = ArrayList<BarEntry>()
        for (i in 0..6) {
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
                return if (index in 0..6) daysOfWeek[index] else ""
            }
        }

        barChart.description = Description().apply { text = "Weekly Analysis (Mon–Sun)" }
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
