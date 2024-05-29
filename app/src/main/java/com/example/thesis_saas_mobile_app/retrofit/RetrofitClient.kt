package com.example.thesis_saas_mobile_app.retrofit

import android.content.Context
import android.content.Intent
import com.example.thesis_saas_mobile_app.LoginActivity
import com.example.thesis_saas_mobile_app.utils.SharedPreferencesManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = SharedPreferencesManager.getToken(context)

        val requestBuilder = originalRequest.newBuilder()

        token?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        if (response.code == 401) { // HTTP 401 Unauthorized
            // Redirect to login activity
            SharedPreferencesManager.removeToken(context)
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }

        return response
    }
}


fun getRetrofitClient(context: Context): Retrofit {
    val serverIP = SharedPreferencesManager.getServerIp(context)


    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(context))
        .build()

    return Retrofit.Builder()
        .baseUrl("http://$serverIP:3030/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

fun getRetrofitClient(context: Context, baseURL: String): Retrofit {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(context))
        .build()

    return Retrofit.Builder()
        .baseUrl("http://$baseURL:3030")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

private fun baseUrlPreProcess(baseURL: String): String {
    if (!baseURL.contains("http://") || !baseURL.contains("https://")) {
        return if (baseURL.contains(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,5}")))
            "http://%s".format(baseURL)
        else "https://%s".format(baseURL)
    }
    return baseURL
}