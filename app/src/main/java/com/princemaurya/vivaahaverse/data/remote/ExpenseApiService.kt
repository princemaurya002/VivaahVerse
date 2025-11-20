package com.princemaurya.vivaahaverse.data.remote

import com.princemaurya.vivaahaverse.data.model.Expense
import com.princemaurya.vivaahaverse.data.model.ExpenseRequest
import com.princemaurya.vivaahaverse.data.model.ExpensesResponse
import com.princemaurya.vivaahaverse.data.model.AuthResponse
import com.princemaurya.vivaahaverse.data.model.LoginRequest
import com.princemaurya.vivaahaverse.data.model.SignupRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ExpenseApiService {

    @GET("expenses")
    suspend fun getExpenses(
        @Query("category") category: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("includeSummary") includeSummary: Boolean = true
    ): Response<ExpensesResponse>

    @POST("expenses")
    suspend fun createExpense(
        @Body request: ExpenseRequest
    ): Response<Expense>

    @PUT("expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: String,
        @Body request: ExpenseRequest
    ): Response<Expense>

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(
        @Path("id") id: String
    ): Response<Unit>
}

interface AuthApiService {
    @POST("auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>
}

object ExpenseApiClient {

    // 10.0.2.2 is the host loopback address for Android emulators
    private const val BASE_URL = "http://10.0.2.2:3000/"

    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        AuthTokenProvider.token?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val expenseService: ExpenseApiService by lazy { retrofit.create(ExpenseApiService::class.java) }
    val authService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
}
