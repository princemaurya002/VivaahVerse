package com.princemaurya.vivaahaverse.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princemaurya.vivaahaverse.data.model.Expense
import com.princemaurya.vivaahaverse.data.model.ExpenseRequest
import com.princemaurya.vivaahaverse.data.model.Summary
import com.princemaurya.vivaahaverse.data.remote.AuthTokenProvider
import com.princemaurya.vivaahaverse.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val isLoading: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val summary: Summary? = null,
    val selectedCategory: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ExpenseViewModel(
    private val repository: ExpenseRepository = ExpenseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private var authToken: String? = null

    fun loadExpenses(
        category: String? = _uiState.value.selectedCategory,
        startDate: String? = _uiState.value.startDate,
        endDate: String? = _uiState.value.endDate,
        successMessage: String? = null
    ) {
        if (!ensureAuthenticated()) return
        viewModelScope.launch {
            setLoadingState()
            val result = repository.getExpenses(category, startDate, endDate, includeSummary = true)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        expenses = response.expenses,
                        summary = response.summary,
                        selectedCategory = category,
                        startDate = startDate,
                        endDate = endDate,
                        errorMessage = null,
                        successMessage = successMessage
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message,
                        successMessage = null
                    )
                }
            )
        }
    }

    fun addExpense(amount: Double, description: String, date: String, category: String) {
        if (!ensureAuthenticated()) return
        viewModelScope.launch {
            setLoadingState()
            val request = ExpenseRequest(
                amount = amount,
                description = description,
                date = date,
                category = category
            )
            val result = repository.createExpense(request)
            result.fold(
                onSuccess = {
                    loadExpenses(successMessage = "Expense added")
                },
                onFailure = { e ->
                    handleError(e)
                }
            )
        }
    }

    fun updateExpense(id: String, amount: Double, description: String, date: String, category: String) {
        if (!ensureAuthenticated()) return
        viewModelScope.launch {
            setLoadingState()
            val request = ExpenseRequest(
                amount = amount,
                description = description,
                date = date,
                category = category
            )
            val result = repository.updateExpense(id, request)
            result.fold(
                onSuccess = {
                    loadExpenses(successMessage = "Expense updated")
                },
                onFailure = { e ->
                    handleError(e)
                }
            )
        }
    }

    fun deleteExpense(id: String) {
        if (!ensureAuthenticated()) return
        viewModelScope.launch {
            setLoadingState()
            val result = repository.deleteExpense(id)
            result.fold(
                onSuccess = {
                    loadExpenses(successMessage = "Expense deleted")
                },
                onFailure = { e ->
                    handleError(e)
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun onAuthTokenChanged(token: String?) {
        authToken = token
        AuthTokenProvider.token = token
        if (token == null) {
            _uiState.value = ExpenseUiState()
        } else {
            loadExpenses()
        }
    }

    private fun ensureAuthenticated(): Boolean {
        if (authToken == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Please sign in to continue."
            )
            return false
        }
        return true
    }

    private fun setLoadingState() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
    }

    private fun handleError(e: Throwable) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message, successMessage = null)
    }
}
