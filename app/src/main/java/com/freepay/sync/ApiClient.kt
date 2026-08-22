package com.freepay.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ApiResult {
    data class Success(val json: JSONObject) : ApiResult()
    data class Failure(val message: String) : ApiResult()
}

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** GET {baseUrl}/api/brands/me with Bearer auth — used to validate a key on login. */
    suspend fun getBrandMe(baseUrl: String, apiKey: String): ApiResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/brands/me")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                if (resp.isSuccessful) ApiResult.Success(json)
                else ApiResult.Failure(json.optString("error", "Login failed (${resp.code})"))
            }
        } catch (e: Exception) {
            ApiResult.Failure("নেটওয়ার্ক সমস্যা: ${e.message}")
        }
    }

    /** GET {baseUrl}/api/health — used for the "Server: Online/Offline" indicator. */
    suspend fun ping(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl.trimEnd('/')}/api/health").get().build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
