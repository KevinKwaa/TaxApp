package com.example.taxapp.receiptcategory

import android.content.Context
import org.json.JSONObject
import java.io.IOException

object ApiKeyHelper {

    /**
     * Load API key from credentials.json in assets
     *
     * Example credentials.json:
     * {
     *   "api_key": "YOUR_GEMINI_API_KEY"
     * }
     */
    fun getGeminiApiKey(context: Context): String {
        try {
            val jsonString = context.assets.open("credentials.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            return jsonObject.optString("api_key", "")
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }
}