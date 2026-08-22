package com.freepay.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!PrefsStore.isConfigured(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val receivedAt = System.currentTimeMillis()

        for (msg in messages) {
            val sender = msg.originatingAddress ?: ""
            val body = msg.messageBody ?: continue
            val parsed = SmsParser.parse(sender, body) ?: continue

            val logId = UUID.randomUUID().toString()
            SmsLogStore.add(
                context,
                SmsLogEntry(
                    id = logId,
                    method = parsed.method,
                    trxId = parsed.trxId,
                    amount = parsed.amount,
                    senderNumber = parsed.senderNumber,
                    rawSms = body.take(500),
                    receivedAt = receivedAt,
                    status = SyncStatus.PENDING
                )
            )

            val data = Data.Builder()
                .putString("logId", logId)
                .putString("trxId", parsed.trxId)
                .putDouble("amount", parsed.amount)
                .putString("method", parsed.method)
                .putString("senderNumber", parsed.senderNumber)
                .putString("rawSms", body.take(1000))
                .putLong("receivedAt", receivedAt)
                .build()

            val work = OneTimeWorkRequestBuilder<ForwardWorker>().setInputData(data).build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
