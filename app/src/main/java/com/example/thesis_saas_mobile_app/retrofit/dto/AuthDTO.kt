package com.example.thesis_saas_mobile_app.retrofit.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String
)