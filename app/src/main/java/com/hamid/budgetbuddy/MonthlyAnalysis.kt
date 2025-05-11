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

class MonthlyAnalysis : Fragment() {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val monthsOfYear = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

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
                Log.d("MonthlyAnalysis", "User: $userName ($userId)")

                val startOfSixMonthsAgo = getStartOfMonthsAgo(5) // Start 5 months ago
                db.collection("Users").document(userId)
                    .collection("Transaction")
                    .whereGreaterThanOrEqualTo("timestamp", startOfSixMonthsAgo.time) // timestamp in millis
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val incomeByMonth = FloatArray(6) { 0f }
                        val expenseByMonth = FloatArray(6) { 0f }

                        for (doc in snapshot.documents) {
                            val type = doc.getString("type") ?: continue
                            val amount = doc.getDouble("amount")?.toFloat() ?: continue
                            val timestampMillis = doc.getLong("timestamp") ?: continue
                            val date = Date(timestampMillis)

                            val monthIndex = getMonthIndexFromTimestamp(date)
                            if (monthIndex in 0..5) {
                                if (type.equals("Income", ignoreCase = true)) {
                                    incomeByMonth[monthIndex] += amount
                                    Log.d("MonthlyAnalysis", "$userName - Income: $amount in ${monthsOfYear[monthIndex]}")
                                } else if (type.equals("Expense", ignoreCase = true)) {
                                    expenseByMonth[monthIndex] += amount
                                    Log.d("MonthlyAnalysis", "$userName - Expense: $amount in ${monthsOfYear[monthIndex]}")
                                } else {
                                    Log.d("MonthlyAnalysis", "$userName - Unknown type: $type")
                                }
                            } else {
                                Log.d("MonthlyAnalysis", "$userName - Invalid month index: $monthIndex")
                            }
                        }

                        incomeByMonth.forEachIndexed { i, value ->
                            Log.d("MonthlyAnalysis", "$userName - Total Income in ${monthsOfYear[i]}: $value")
                        }
                        expenseByMonth.forEachIndexed { i, value ->
                            Log.d("MonthlyAnalysis", "$userName - Total Expense in ${monthsOfYear[i]}: $value")
                        }

                        showBarChart(incomeByMonth, expenseByMonth)
                        showPieChart(incomeByMonth.sum(), expenseByMonth.sum())
                    }
                    .addOnFailureListener { e ->
                        Log.e("MonthlyAnalysis", "Error fetching transactions", e)
                    }

            }
            .addOnFailureListener { e ->
                Log.e("MonthlyAnalysis", "Error fetching user data", e)
            }
    }

    private fun getStartOfMonthsAgo(monthsAgo: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsAgo)
        cal.set(Calendar.DAY_OF_MONTH, 1) // Set the date to the 1st of the month
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getMonthIndexFromTimestamp(date: Date): Int {
        val cal = Calendar.getInstance()
        cal.time = date
        val monthOfYear = cal.get(Calendar.MONTH)
        return if (monthOfYear in 0..5) {
            monthOfYear
        } else {
            -1 // Invalid month (this will be ignored)
        }
    }

    private fun showBarChart(income: FloatArray, expenditure: FloatArray) {
        val entries = ArrayList<BarEntry>()
        for (i in 0..5) {
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
                return if (index in 0..5) monthsOfYear[index] else ""
            }
        }

        barChart.description = Description().apply { text = "Analysis for Last 6 Months" }
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
