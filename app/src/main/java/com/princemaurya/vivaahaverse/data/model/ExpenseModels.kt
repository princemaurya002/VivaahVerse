package com.princemaurya.vivaahaverse.data.model

import com.google.gson.annotations.SerializedName

data class Expense(
    @SerializedName("_id") val id: String? = null,
    val userId: String,
    val amount: Double,
    val description: String,
    val date: String,
    val category: String
)

data class Summary(
    val perCategory: Map<String, Double>?,
    val total: Double?
)

data class ExpensesResponse(
    val expenses: List<Expense>,
    val summary: Summary? = null
)

data class ExpenseRequest(
    val amount: Double,
    val description: String,
    val date: String,
    val category: String
)
