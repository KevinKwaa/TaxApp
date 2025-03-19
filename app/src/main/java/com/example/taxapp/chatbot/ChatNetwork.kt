package com.example.taxapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

object NetworkUtil {
    // Modify NetworkUtil to log more details
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: run {
            Log.d("NetworkUtil", "No active network")
            return false
        }
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: run {
            Log.d("NetworkUtil", "No network capabilities")
            return false
        }

        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                Log.d("NetworkUtil", "WiFi connected")
                true
            }
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                Log.d("NetworkUtil", "Cellular connected")
                true
            }
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                Log.d("NetworkUtil", "Ethernet connected")
                true
            }
            else -> {
                Log.d("NetworkUtil", "No valid transport")
                false
            }
        }
    }
}