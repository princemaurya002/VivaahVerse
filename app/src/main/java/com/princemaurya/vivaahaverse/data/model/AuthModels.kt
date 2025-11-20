package com.princemaurya.vivaahaverse.data.model

data class AuthUser(
    val id: String,
    val email: String,
    val name: String? = null
)

data class AuthResponse(
    val token: String,
    val user: AuthUser
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class SignupRequest(
    val name: String?,
    val email: String,
    val password: String
)

data class AuthSession(
    val token: String,
    val userId: String,
    val email: String,
    val name: String? = null
)

//
//data class UserProfile(
//    val id: String,
//    val name: String,
//    val email: String
//)
//
//data class AuthResponse(
//    val token: String,
//    val user: UserProfile
//)
//
//data class LoginRequest(
//    val email: String,
//    val password: String
//)
//
//data class SignupRequest(
//    val name: String,
//    val email: String,
//    val password: String
//)
