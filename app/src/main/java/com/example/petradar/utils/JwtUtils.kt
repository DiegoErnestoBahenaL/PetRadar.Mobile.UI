package com.example.petradar.utils

import android.util.Base64
import android.util.Log
import org.json.JSONObject

/**
 * Lightweight utility to decode a JWT token without any external library.
 */
object JwtUtils {

    private const val TAG = "JwtUtils"

    private fun decodePayload(token: String?): JSONObject? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payloadEncoded = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                .let { s ->
                    val padding = (4 - s.length % 4) % 4
                    s + "=".repeat(padding)
                }

            val payloadJson = String(Base64.decode(payloadEncoded, Base64.DEFAULT), Charsets.UTF_8)
            Log.d(TAG, "JWT payload: $payloadJson")
            JSONObject(payloadJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode JWT payload", e)
            null
        }
    }

    /**
     * Extracts the user ID from a JWT token.
     * Logs the full payload so we can diagnose which claim key the server uses.
     */
    fun getUserIdFromToken(token: String?): Long? {
        val json = decodePayload(token) ?: return null
        val claimKeys = listOf(
            "nameid",
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier",
            "sub",
            "userId",
            "id"
        )
        for (key in claimKeys) {
            if (json.has(key)) {
                val value = json.getString(key).toLongOrNull()
                Log.d(TAG, "Found userId in claim '$key': $value")
                return value
            }
        }
        Log.w(TAG, "No userId claim found. Available keys: ${json.keys().asSequence().toList()}")
        return null
    }
}



