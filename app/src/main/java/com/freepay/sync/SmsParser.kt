package com.freepay.sync

/**
 * Parses "money received" confirmation SMS for all 5 supported wallets.
 *
 * IMPORTANT: exact SMS wording varies by operator/app and changes over time.
 * The regexes below cover common current formats for bKash and Nagad (the
 * two with the most predictable wording); Upay/Rocket/Cellfin patterns are
 * best-effort generic matches and may need tuning once you test with real
 * SMS from those wallets on the target phone.
 */

data class ParsedSms(
    val method: String,
    val trxId: String,
    val amount: Double,
    val senderNumber: String?
)

object SmsParser {

    private val BKASH_RECEIVED = Regex(
        """received\s+Tk\.?\s*([\d,]+\.?\d*)\s*(?:from\s+(\d{11}))?.*?TrxID[:\s]+([A-Z0-9]{6,})""",
        RegexOption.IGNORE_CASE
    )
    private val NAGAD_RECEIVED = Regex(
        """received\s+Tk\.?\s*([\d,]+\.?\d*)\s*(?:from\s+(\d{11}))?.*?(?:TxnID|Ref(?:erence)?\s*No)[:\s]+([A-Z0-9]{6,})""",
        RegexOption.IGNORE_CASE
    )
    // Generic fallback used for Upay / Rocket / Cellfin: looks for an amount
    // and a transaction/reference id anywhere in a "received"-style message.
    private val GENERIC_RECEIVED = Regex(
        """received\s+(?:Tk\.?|BDT)\s*([\d,]+\.?\d*)\s*(?:from\s+(\d{11}))?.*?(?:TrxID|Txn(?:ID)?|Ref(?:erence)?(?:\s*No)?)[:\s]+([A-Z0-9]{6,})""",
        RegexOption.IGNORE_CASE
    )

    private fun detectMethod(sender: String, body: String): String? {
        val l = body.lowercase()
        val s = sender.lowercase()
        return when {
            s.contains("bkash") || l.contains("bkash") -> "bkash"
            s.contains("nagad") || l.contains("nagad") -> "nagad"
            s.contains("upay") || l.contains("upay") -> "upay"
            s.contains("rocket") || l.contains("rocket") -> "rocket"
            s.contains("cellfin") || l.contains("cellfin") -> "cellfin"
            else -> null
        }
    }

    fun parse(senderAddress: String, body: String): ParsedSms? {
        val lower = body.lowercase()
        if (!lower.contains("received")) return null // skip sent/cash-out/other messages

        val method = detectMethod(senderAddress, body) ?: return null

        val regex = when (method) {
            "bkash" -> BKASH_RECEIVED
            "nagad" -> NAGAD_RECEIVED
            else -> GENERIC_RECEIVED
        }

        val match = regex.find(body) ?: GENERIC_RECEIVED.find(body) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val sender = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
        val trx = match.groupValues[3]
        return ParsedSms(method, trx, amount, sender)
    }
}
