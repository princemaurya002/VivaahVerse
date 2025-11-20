package com.princemaurya.vivaahaverse.data.repository

import com.princemaurya.vivaahaverse.data.model.Expense
import com.princemaurya.vivaahaverse.data.model.ExpenseRequest
import com.princemaurya.vivaahaverse.data.model.ExpensesResponse
import com.princemaurya.vivaahaverse.data.remote.ExpenseApiClient
import com.princemaurya.vivaahaverse.data.remote.ExpenseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class ExpenseRepository(
    private val apiService: ExpenseApiService = ExpenseApiClient.expenseService
) {

    suspend fun getExpenses(
        category: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        includeSummary: Boolean = true
    ): Result<ExpensesResponse> = safeApiCall(
        apiCall = {
            apiService.getExpenses(
                category = category,
                startDate = startDate,
                endDate = endDate,
                includeSummary = includeSummary
            )
        }
    )

    suspend fun createExpense(request: ExpenseRequest): Result<Expense> = safeApiCall(
        apiCall = { apiService.createExpense(request = request) }
    )

    suspend fun updateExpense(id: String, request: ExpenseRequest): Result<Expense> = safeApiCall(
        apiCall = { apiService.updateExpense(id = id, request = request) }
    )

    suspend fun deleteExpense(id: String): Result<Unit> = safeApiCall(
        apiCall = { apiService.deleteExpense(id = id) },
        onEmptyBody = { Unit }
    )

    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>,
        onEmptyBody: (() -> T)? = null
    ): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else if (onEmptyBody != null) {
                        Result.success(onEmptyBody())
                    } else if (response.code() == 204) {
                        Result.failure(Exception("Operation completed but no content was returned."))
                    } else {
                        Result.failure(Exception("Unexpected empty response from server"))
                    }
                } else {
                    val message = when (response.code()) {
                        400 -> "Invalid data. Please check the entered values and try again."
                        401 -> "You are not authorized to perform this action."
                        404 -> "Requested item was not found. It may have been deleted."
                        in 500..599 -> "Server error. Please try again in a few moments."
                        else -> "Request failed (code ${response.code()}). Please try again."
                    }
                    Result.failure(Exception(message))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Unable to reach server. Check your internet connection and try again."))
            } catch (e: Exception) {
                Result.failure(Exception("Unexpected error: ${e.message ?: "Something went wrong"}"))
            }
        }
}
