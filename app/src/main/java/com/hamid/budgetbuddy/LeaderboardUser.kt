package com.hamid.budgetbuddy

data class LeaderboardUser(
    val username: String,
    val income: Double,
    val expenditure: Double
) {
    val percentage: Double
        get() = if (expenditure == 0.0) 100.0 else ((income-expenditure) / income) * 100
}

