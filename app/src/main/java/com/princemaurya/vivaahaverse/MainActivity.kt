package com.princemaurya.vivaahaverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.princemaurya.vivaahaverse.ui.auth.AuthScreen
import com.princemaurya.vivaahaverse.ui.auth.AuthViewModel
import com.princemaurya.vivaahaverse.ui.expenses.ExpenseScreen
import com.princemaurya.vivaahaverse.ui.expenses.ExpenseViewModel
import com.princemaurya.vivaahaverse.ui.theme.VivaahaVerseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VivaahaVerseTheme {
                VivaahaVerseApp()
            }
        }
    }
}

@Composable
private fun VivaahaVerseApp() {
    val authViewModel: AuthViewModel = viewModel()
    val expenseViewModel: ExpenseViewModel = viewModel()

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