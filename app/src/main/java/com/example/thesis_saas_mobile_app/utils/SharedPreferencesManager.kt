package com.example.thesis_saas_mobile_app.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesManager {
    private const val PREFS_NAME = "my_prefs"
    private const val TOKEN_KEY = "token_key"
    private const val SERVER_IP_KEY = "server_ip_key"

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(TOKEN_KEY, token)
        editor.apply()
    }

    fun removeToken(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(TOKEN_KEY)
        editor.apply()
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)
    }

    fun saveServerIp(context: Context, serverIp: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(SERVER_IP_KEY, serverIp)
        editor.apply()
    }

    fun getServerIp(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SERVER_IP_KEY, null)
    }
}
