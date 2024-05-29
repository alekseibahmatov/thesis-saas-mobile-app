package com.example.thesis_saas_mobile_app.retrofit.api

import com.example.thesis_saas_mobile_app.retrofit.dto.AlertResponse
import com.example.thesis_saas_mobile_app.retrofit.dto.ClaimAlertRequest
import com.example.thesis_saas_mobile_app.retrofit.dto.ClaimAlertResponse
import com.example.thesis_saas_mobile_app.retrofit.dto.FinishAlertRequest
import com.example.thesis_saas_mobile_app.retrofit.dto.LoginRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AlertService {
    @GET("alerts")
    fun getAlerts(): Call<AlertResponse>

    @POST("alerts")
    fun claimAlert(@Body request: ClaimAlertRequest): Call<ClaimAlertResponse>

    @POST("alerts/finish")
    fun finishAlert(@Body request: FinishAlertRequest): Call<ClaimAlertResponse>
}