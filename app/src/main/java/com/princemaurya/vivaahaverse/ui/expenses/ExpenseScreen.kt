package com.princemaurya.vivaahaverse.ui.expenses

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.princemaurya.vivaahaverse.data.model.Expense
import com.princemaurya.vivaahaverse.data.model.Summary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    userName: String?,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance() }

    var showDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expensePendingDelete by remember { mutableStateOf<Expense?>(null) }

    var categoryFilter by remember { mutableStateOf("") }
    var startDateFilter by remember { mutableStateOf("") }
    var endDateFilter by remember { mutableStateOf("") }
    var startDateError by remember { mutableStateOf<String?>(null) }
    var endDateError by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    val hasActiveFilters = categoryFilter.isNotBlank() || startDateFilter.isNotBlank() || endDateFilter.isNotBlank()

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        var consumed = false
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            consumed = true
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            consumed = true
        }
        if (consumed) {
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Expenses")
                        userName?.let {
                            Text(
                                text = "Hi, $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log out"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingExpense = null
                    showDialog = true
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add expense"
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }

            uiState.summary?.let { summary ->
                DashboardSection(summary = summary) { amount ->
                    currencyFormatter.format(amount)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "All expenses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasActiveFilters) {
                                Text(
                                    text = "Filters applied",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasActiveFilters) {
                                TextButton(
                                    onClick = {
                                        categoryFilter = ""
                                        startDateFilter = ""
                                        endDateFilter = ""
                                        startDateError = null
                                        endDateError = null
                                        showFilters = false
                                        viewModel.loadExpenses(category = null, startDate = null, endDate = null)
                                    }
                                ) {
                                    Text(text = "Reset filter")
                                }
                            }

                            TextButton(onClick = { showFilters = !showFilters }) {
                                Text(text = if (showFilters) "Hide filters" else "Apply filter")
                            }
                        }
                    }

                    if (showFilters) {
                        Spacer(modifier = Modifier.height(4.dp))

                        FilterSection(
                            category = categoryFilter,
                            startDate = startDateFilter,
                            endDate = endDateFilter,
                            startDateError = startDateError,
                            endDateError = endDateError,
                            onCategoryChange = { categoryFilter = it },
                            onStartDateChange = {
                                startDateFilter = it
                                startDateError = null
                            },
                            onEndDateChange = {
                                endDateFilter = it
                                endDateError = null
                            },
                            onApplyFilters = {
                                val (startError, endError) = validateFilterDates(startDateFilter, endDateFilter)
                                startDateError = startError
                                endDateError = endError
                                if (startError == null && endError == null) {
                                    val category = categoryFilter.ifBlank { null }
                                    val start = startDateFilter.ifBlank { null }
                                    val end = endDateFilter.ifBlank { null }
                                    viewModel.loadExpenses(category = category, startDate = start, endDate = end)
                                    showFilters = false
                                }
                            },
                            onClearFilters = {
                                categoryFilter = ""
                                startDateFilter = ""
                                endDateFilter = ""
                                startDateError = null
                                endDateError = null
                                viewModel.loadExpenses(category = null, startDate = null, endDate = null)
                                showFilters = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ExpenseList(
                        expenses = uiState.expenses,
                        formatCurrency = { amount -> currencyFormatter.format(amount) },
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth(),
                        onEdit = { expense ->
                            editingExpense = expense
                            showDialog = true
                        },
                        onDelete = { expense ->
                            expensePendingDelete = expense
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AddEditExpenseDialog(
            expense = editingExpense,
            onDismiss = { showDialog = false },
            onSave = { amount, description, date, category ->
                if (editingExpense == null) {
                    viewModel.addExpense(amount, description, date, category)
                } else {
                    val id = editingExpense?.id
                    if (id != null) {
                        viewModel.updateExpense(id, amount, description, date, category)
                    }
                }
                showDialog = false
            }
        )
    }

    expensePendingDelete?.let { expense ->
        ConfirmDeleteDialog(
            description = expense.description,
            onDismiss = { expensePendingDelete = null },
            onConfirm = {
                expense.id?.let { viewModel.deleteExpense(it) }
                expensePendingDelete = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    category: String,
    startDate: String,
    endDate: String,
    startDateError: String?,
    endDateError: String?,
    onCategoryChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit
) {
    Column {
        val categories = listOf("All", "Food", "Travel", "Shopping", "Bills", "Other")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { option ->
                val isSelected = if (option == "All") category.isBlank() else category == option
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (option == "All") {
                            onCategoryChange("")
                        } else {
                            onCategoryChange(option)
                        }
                    },
                    label = { Text(option) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = startDate,
            onValueChange = onStartDateChange,
            label = { Text("Start date (YYYY-MM-DD)") },
            isError = startDateError != null,
            supportingText = {
                startDateError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = endDate,
            onValueChange = onEndDateChange,
            label = { Text("End date (YYYY-MM-DD)") },
            isError = endDateError != null,
            supportingText = {
                endDateError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onClearFilters) {
                Text(text = "Reset")
            }
            Button(onClick = onApplyFilters) {
                Text(text = "Apply")
            }
        }
    }
}

@Composable
private fun DashboardSection(
    summary: Summary,
    formatCurrency: (Double) -> String
) {
    val perCategory = summary.perCategory.orEmpty()
    val total = summary.total ?: perCategory.values.sum()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Total spent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Spending by category",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (perCategory.isEmpty()) {
                Text(text = "Start adding expenses to see category breakdown.")
            } else {
                perCategory.forEach { (category, amount) ->
                    CategorySpendRow(
                        label = category,
                        amount = amount,
                        total = total,
                        formatCurrency = formatCurrency
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CategorySpendRow(
    label: String,
    amount: Double,
    total: Double,
    formatCurrency: (Double) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = formatCurrency(amount), fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        val progress = if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ExpenseList(
    expenses: List<Expense>,
    formatCurrency: (Double) -> String,
    modifier: Modifier = Modifier,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    if (expenses.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No expenses yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Tap the + button to add your first expense.")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(
                items = expenses,
                key = { expense -> expense.id ?: expense.hashCode() }
            ) { expense ->
                ExpenseItem(
                    expense = expense,
                    formatCurrency = formatCurrency,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun ExpenseItem(
    expense: Expense,
    formatCurrency: (Double) -> String,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit(expense) }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = expense.date,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SuggestionChip(
                onClick = {},
                label = { Text(expense.category) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onEdit(expense) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit expense"
                    )
                }
                IconButton(onClick = { onDelete(expense) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete expense"
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditExpenseDialog(
    expense: Expense?,
    onDismiss: () -> Unit,
    onSave: (amount: Double, description: String, date: String, category: String) -> Unit
) {
    var amountText by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(expense?.description ?: "") }
    var date by remember { mutableStateOf(expense?.date ?: "") }
    var category by remember { mutableStateOf(expense?.category ?: "") }

    var amountError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance() }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                date = dateFormatter.format(calendar.time)
                dateError = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun validate(): Boolean {
        var isValid = true

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            amountError = "Enter a valid positive amount"
            isValid = false
        } else {
            amountError = null
        }

        if (description.isBlank()) {
            descriptionError = "Description is required"
            isValid = false
        } else {
            descriptionError = null
        }

        if (date.isBlank()) {
            dateError = "Date is required (YYYY-MM-DD)"
            isValid = false
        } else if (!isValidDateInput(date)) {
            dateError = "Enter a valid date (YYYY-MM-DD)"
            isValid = false
        } else {
            dateError = null
        }

        if (category.isBlank()) {
            categoryError = "Category is required"
            isValid = false
        } else {
            categoryError = null
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                if (validate()) {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    onSave(amount, description, date, category)
                }
            }) {
                Text(text = if (expense == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = if (expense == null) "Add Expense" else "Edit Expense")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    isError = amountError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        amountError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    isError = descriptionError != null,
                    supportingText = {
                        descriptionError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        dateError = null
                    },
                    label = { Text("Date (YYYY-MM-DD)") },
                    isError = dateError != null,
                    supportingText = {
                        dateError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick date"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val allCategories = listOf("Food", "Travel", "Shopping", "Bills", "Other")
                var expanded by remember { mutableStateOf(false) }
                val filteredCategories = allCategories.filter {
                    category.isBlank() || it.contains(category, ignoreCase = true)
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {
                            category = it
                            categoryError = null
                            expanded = true
                        },
                        label = { Text("Category") },
                        isError = categoryError != null,
                        supportingText = {
                            categoryError?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select category"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = expanded && filteredCategories.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredCategories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryError = null
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    description: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete expense?") },
        text = { Text(text = "Are you sure you want to delete \"$description\"?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

private fun validateFilterDates(start: String, end: String): Pair<String?, String?> {
    var startError: String? = null
    var endError: String? = null

    if (start.isNotBlank() && !isValidDateInput(start)) {
        startError = "Invalid date format"
    }
    if (end.isNotBlank() && !isValidDateInput(end)) {
        endError = "Invalid date format"
    }

    if (startError == null && endError == null && start.isNotBlank() && end.isNotBlank()) {
        val startDate = parseDate(start)
        val endDate = parseDate(end)
        if (startDate != null && endDate != null && startDate.after(endDate)) {
            endError = "End date must be on or after start date"
        }
    }

    return startError to endError
}

private fun isValidDateInput(value: String): Boolean {
    return parseDate(value) != null
}

private fun parseDate(value: String): Date? {
    return runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        format.isLenient = false
        format.parse(value)
    }.getOrNull()
}
