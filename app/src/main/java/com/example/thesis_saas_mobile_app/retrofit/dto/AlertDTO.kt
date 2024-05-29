package com.example.thesis_saas_mobile_app.retrofit.dto

import com.example.thesis_saas_mobile_app.adapters.Alert


data class AlertResponse(
val alerts: List<Alert>
)

data class ClaimAlertRequest(
    val alertId: String,
    val machineId: String,
    val avgTime: Int,
)

data class FinishAlertRequest(
    val alertId: String,
)

data class ClaimAlertResponse(
    val message: String
)