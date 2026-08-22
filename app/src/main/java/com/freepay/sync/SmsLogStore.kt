package com.freepay.sync

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class SyncStatus { PENDING, SYNCED, MATCHED, FAILED }

data class SmsLogEntry(
    val id: String,
    val method: String,
    val trxId: String,
    val amount: Double,
    val senderNumber: String?,
    val rawSms: String,
    val receivedAt: Long,
    var status: SyncStatus
)

/**
 * Keeps the last N parsed SMS on-device (SharedPreferences JSON array) so the
 * app can show "all SMS" it has captured, independent of network state.
 * This is a local activity log, not a source of truth — the server's own
 * sms_transactions table is authoritative.
 */
object SmsLogStore {
    private const val PREFS_NAME = "freepay_sms_log"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 200

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun toJson(e: SmsLogEntry): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("method", e.method)
        put("trxId", e.trxId)
        put("amount", e.amount)
        put("senderNumber", e.senderNumber ?: JSONObject.NULL)
        put("rawSms", e.rawSms)
        put("receivedAt", e.receivedAt)
        put("status", e.status.name)
    }

    private fun fromJson(o: JSONObject): SmsLogEntry = SmsLogEntry(
        id = o.getString("id"),
        method = o.getString("method"),
        trxId = o.getString("trxId"),
        amount = o.getDouble("amount"),
        senderNumber = if (o.isNull("senderNumber")) null else o.getString("senderNumber"),
        rawSms = o.optString("rawSms", ""),
        receivedAt = o.getLong("receivedAt"),
        status = SyncStatus.valueOf(o.optString("status", "PENDING"))
    )

    @Synchronized
    fun getAll(context: Context): List<SmsLogEntry> {
        val raw = prefs(context).getString(KEY_ENTRIES, null) ?: return emptyList()
        val arr = JSONArray(raw)
        val out = ArrayList<SmsLogEntry>()
        for (i in 0 until arr.length()) out.add(fromJson(arr.getJSONObject(i)))
        return out.sortedByDescending { it.receivedAt }
    }

    @Synchronized
    fun add(context: Context, entry: SmsLogEntry) {
        val current = getAll(context).toMutableList()
        current.add(0, entry)
        val trimmed = current.take(MAX_ENTRIES)
        val arr = JSONArray()
        trimmed.forEach { arr.put(toJson(it)) }
        prefs(context).edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    @Synchronized
    fun updateStatus(context: Context, id: String, status: SyncStatus) {
        val current = getAll(context).toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx == -1) return
        current[idx] = current[idx].copy(status = status)
        val arr = JSONArray()
        current.forEach { arr.put(toJson(it)) }
        prefs(context).edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    @Synchronized
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ENTRIES).apply()
    }
}
