//package com.princemaurya.vivaahaverse.data.repository
//
//import com.princemaurya.vivaahaverse.data.model.AuthResponse
//import com.princemaurya.vivaahaverse.data.model.LoginRequest
//import com.princemaurya.vivaahaverse.data.model.SignupRequest
//import com.princemaurya.vivaahaverse.data.remote.ExpenseApiClient
//import com.princemaurya.vivaahaverse.data.remote.AuthApiService
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import retrofit2.Response
//import java.io.IOException
//
//class AuthRepository(
//    private val authApiService: AuthApiService = ExpenseApiClient.authService
//) {
//
//    suspend fun login(request: LoginRequest): Result<AuthResponse> = safeApiCall {
//        authApiService.login(request)
//    }
//
//    suspend fun signup(request: SignupRequest): Result<AuthResponse> = safeApiCall {
//        authApiService.signup(request)
//    }
//
//    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> =
//        withContext(Dispatchers.IO) {
//            try {
//                val response = apiCall()
//                if (response.isSuccessful) {
//                    val body = response.body()
//                    if (body != null) {
//                        Result.success(body)
//                    } else {
//                        Result.failure(Exception("Empty response from server"))
//                    }
//                } else {
//                    val message = when (response.code()) {
//                        400 -> "Please check the entered details and try again."
//                        401 -> "Invalid email or password."
//                        in 500..599 -> "Server error. Please try again later."
//                        else -> "Request failed (code ${response.code()}). Please try again."
//                    }
//                    Result.failure(Exception(message))
//                }
//            } catch (e: IOException) {
//                Result.failure(Exception("Unable to reach server. Check your internet connection."))
//            } catch (e: Exception) {
package com.princemaurya.vivaahaverse.data.repository

import com.princemaurya.vivaahaverse.data.local.AuthPreferences
import com.princemaurya.vivaahaverse.data.model.AuthResponse
import com.princemaurya.vivaahaverse.data.model.AuthSession
import com.princemaurya.vivaahaverse.data.model.LoginRequest
import com.princemaurya.vivaahaverse.data.model.SignupRequest
import com.princemaurya.vivaahaverse.data.remote.AuthApiService
import com.princemaurya.vivaahaverse.data.remote.ExpenseApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class AuthRepository(
    private val authApiService: AuthApiService = ExpenseApiClient.authService,
    private val authPreferences: AuthPreferences
) {

    val authSessionFlow: Flow<AuthSession?> = authPreferences.authSessionFlow

    suspend fun login(request: LoginRequest): Result<AuthResponse> =
        safeApiCall { authApiService.login(request) }

    suspend fun signup(request: SignupRequest): Result<AuthResponse> =
        safeApiCall { authApiService.signup(request) }

    suspend fun persistSession(response: AuthResponse) {
        val session = AuthSession(
            token = response.token,
            userId = response.user.id,
            email = response.user.email,
            name = response.user.name
        )
        authPreferences.saveSession(session)
    }

    suspend fun logout() {
        authPreferences.clearSession()
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Unexpected empty response from server"))
                    }
                } else {
                    val message = when (response.code()) {
                        400 -> "Invalid data. Please check your input."
                        401 -> "Invalid credentials. Please try again."
                        409 -> "Account already exists."
                        in 500..599 -> "Server error. Please try again later."
                        else -> "Request failed (${response.code()}). Please try again."
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

