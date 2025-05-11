package com.hamid.budgetbuddy

import com.github.mikephil.charting.formatter.ValueFormatter

class IncomeExpenseFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return when (value.toInt()) {
            0 -> "Income"
            1 -> "Expense"
            else -> ""
        }
    }
}
