package com.hamid.budgetbuddy

data class Transaction(
    val amount: Double = 0.0,
    val category: String = "",
    val note: String = "",
    val timestamp: Long = 0,
    val type: String = ""
)
