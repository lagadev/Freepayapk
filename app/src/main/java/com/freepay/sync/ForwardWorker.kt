package com.freepay.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Uploads one parsed SMS to POST {baseUrl}/api/sms/ingest using the
 * connected brand's API key, and reflects the outcome back into
 * SmsLogStore so the app's list screen shows live status.
 */
class ForwardWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val logId = inputData.getString("logId")
        val baseUrl = PrefsStore.getBaseUrl(applicationContext) ?: return fail(logId)
        val apiKey = PrefsStore.getApiKey(applicationContext) ?: return fail(logId)

        val trxId = inputData.getString("trxId") ?: return fail(logId)
        val amount = inputData.getDouble("amount", -1.0)
        val method = inputData.getString("method") ?: "bkash"
        val senderNumber = inputData.getString("senderNumber")
        val rawSms = inputData.getString("rawSms")
        val receivedAt = inputData.getLong("receivedAt", System.currentTimeMillis())
        if (amount <= 0) return fail(logId)

        val payload = JSONObject().apply {
            put("trxId", trxId)
            put("amount", amount)
            put("method", method)
            if (senderNumber != null) put("senderNumber", senderNumber)
            if (rawSms != null) put("rawSms", rawSms)
            put("receivedAt", receivedAt)
        }

        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/api/sms/ingest")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                when {
                    resp.isSuccessful -> {
                        val respBody = resp.body?.string() ?: "{}"
                        val matched = JSONObject(respBody).optBoolean("matched", false)
                        if (logId != null) {
                            SmsLogStore.updateStatus(
                                applicationContext, logId,
                                if (matched) SyncStatus.MATCHED else SyncStatus.SYNCED
                            )
                        }
                        Result.success()
                    }
                    resp.code in 500..599 -> Result.retry()
                    else -> fail(logId)
                }
            }
        } catch (e: Exception) {
            Result.retry() // network hiccup — WorkManager backs off and retries
        }
    }

    private fun fail(logId: String?): Result {
        if (logId != null) SmsLogStore.updateStatus(applicationContext, logId, SyncStatus.FAILED)
        return Result.failure()
    }
}
