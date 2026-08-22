package com.freepay.sync

import android.content.Context

/** Stores the connected FreePay backend URL + brand API key on-device. */
object PrefsStore {
    private const val PREFS_NAME = "freepay_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_BRAND_NAME = "brand_name"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveConfig(context: Context, baseUrl: String, apiKey: String, brandName: String) {
        prefs(context).edit()
            .putString(KEY_BASE_URL, baseUrl.trimEnd('/'))
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_BRAND_NAME, brandName)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun getBaseUrl(context: Context): String? = prefs(context).getString(KEY_BASE_URL, null)
    fun getApiKey(context: Context): String? = prefs(context).getString(KEY_API_KEY, null)
    fun getBrandName(context: Context): String? = prefs(context).getString(KEY_BRAND_NAME, null)

    fun isConfigured(context: Context): Boolean =
        !getBaseUrl(context).isNullOrBlank() && !getApiKey(context).isNullOrBlank()
}
