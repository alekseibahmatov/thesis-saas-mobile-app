package com.example.thesis_saas_mobile_app.utils.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.thesis_saas_mobile_app.utils.BTManager

class BTManagerFactory(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BTManager::class.java)) {
            return BTManager(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}