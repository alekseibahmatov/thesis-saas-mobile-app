package com.example.thesis_saas_mobile_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.example.thesis_saas_mobile_app.databinding.LoginLayoutBinding
import com.example.thesis_saas_mobile_app.retrofit.api.AuthService
import com.example.thesis_saas_mobile_app.retrofit.dto.LoginRequest
import com.example.thesis_saas_mobile_app.retrofit.dto.LoginResponse
import com.example.thesis_saas_mobile_app.retrofit.getRetrofitClient
import com.example.thesis_saas_mobile_app.utils.SharedPreferencesManager
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.Charset
import java.util.Date

class LoginActivity : ComponentActivity() {
    private lateinit var binding: LoginLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for a valid token
        val token = SharedPreferencesManager.getToken(this)
        if (token != null && !isTokenExpired(token)) {
            // Token is valid, navigate to MainActivity
            navigateToMainActivity()
        } else {
            // Token is not valid, setup login button listener
            binding.loginBTN.setOnClickListener {
                login()
            }
        }
    }

    private fun login() {
        val serverIP = binding.serverIpET.text.toString()
        val email = binding.loginEmailET.text.toString()
        val password = binding.loginPasswordET.text.toString()

        if (serverIP == "" || email == "" || password == "") {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_LONG).show()
            return
        }

        val loginRequest = LoginRequest(email, password)
        val authService = getRetrofitClient(this, serverIP).create(AuthService::class.java)
        val call = authService.signIn(loginRequest)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val token = response.body()?.access_token
                    token?.let {
                        SharedPreferencesManager.saveToken(this@LoginActivity, it)
                        SharedPreferencesManager.saveServerIp(this@LoginActivity, serverIP)
                        navigateToMainActivity()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("LoginActivity", "$t")
                Toast.makeText(this@LoginActivity, "An error occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        finish()
    }

    private fun isTokenValid(token: String): Boolean {
        val jwt: DecodedJWT = JWT.decode(token)
        val expirationTime: Date? = jwt.expiresAt
        return expirationTime == null || Date().after(expirationTime)
    }

    fun decodeJWT(token: String): JSONObject {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWT token")
        }

        val payload = parts[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
        val decodedString = String(decodedBytes, Charset.forName("UTF-8"))
        return JSONObject(decodedString)
    }

    fun isTokenExpired(token: String): Boolean {
        val payload = decodeJWT(token)
        val exp = payload.optLong("exp", -1)
        if (exp == -1L) {
            return true
        }

        val expirationTime = Date(exp * 1000)
        return Date().after(expirationTime)
    }
}

