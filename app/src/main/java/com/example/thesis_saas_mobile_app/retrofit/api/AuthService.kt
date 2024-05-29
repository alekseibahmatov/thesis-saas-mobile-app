package com.example.thesis_saas_mobile_app.retrofit.api

import com.example.thesis_saas_mobile_app.retrofit.dto.LoginRequest
import com.example.thesis_saas_mobile_app.retrofit.dto.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/signIn")
    fun signIn(@Body request: LoginRequest): Call<LoginResponse>
}