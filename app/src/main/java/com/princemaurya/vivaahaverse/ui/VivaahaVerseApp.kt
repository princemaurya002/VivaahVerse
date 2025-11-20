package com.princemaurya.vivaahaverse.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.princemaurya.vivaahaverse.ui.auth.AuthScreen
import com.princemaurya.vivaahaverse.ui.auth.AuthViewModel
import com.princemaurya.vivaahaverse.ui.expenses.ExpenseScreen
import com.princemaurya.vivaahaverse.ui.expenses.ExpenseViewModel

@Composable
fun VivaahaVerseApp(
    authViewModel: AuthViewModel,
    expenseViewModel: ExpenseViewModel
) {
    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authState.session.token) {
        expenseViewModel.onAuthTokenChanged(authState.session.token)
    }

    if (authState.isAuthenticated) {
        ExpenseScreen(
            viewModel = expenseViewModel,
            userName = authState.session.name ?: authState.session.email,
            onLogout = { authViewModel.logout() }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AuthScreen(
                uiState = authState,
                onNameChange = authViewModel::updateName,
                onEmailChange = authViewModel::updateEmail,
                onPasswordChange = authViewModel::updatePassword,
                onAuthenticate = authViewModel::authenticate,
                onToggleMode = authViewModel::toggleMode
            )
        }
    }
}

