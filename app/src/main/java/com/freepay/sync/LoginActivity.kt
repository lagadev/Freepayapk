package com.freepay.sync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var baseUrlInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var errorText: TextView
    private lateinit var loginBtn: Button
    private lateinit var loginProgress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Already logged in? Skip straight to the main screen.
        if (PrefsStore.isConfigured(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)
        baseUrlInput = findViewById(R.id.baseUrlInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        errorText = findViewById(R.id.errorText)
        loginBtn = findViewById(R.id.loginBtn)
        loginProgress = findViewById(R.id.loginProgress)

        loginBtn.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val baseUrl = baseUrlInput.text.toString().trim()
        val apiKey = apiKeyInput.text.toString().trim()

        errorText.visibility = android.view.View.GONE
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            showError("Server URL এবং API Key দুটোই দিতে হবে।")
            return
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            showError("URL অবশ্যই http:// বা https:// দিয়ে শুরু হতে হবে।")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            when (val result = ApiClient.getBrandMe(baseUrl, apiKey)) {
                is ApiResult.Success -> {
                    val brandName = result.json.optString("name", "Brand")
                    val enabled = result.json.optBoolean("enabled", true)
                    if (!enabled) {
                        setLoading(false)
                        showError("এই Brand বর্তমানে ডিজেবল করা আছে। Dashboard থেকে চেক করুন।")
                        return@launch
                    }
                    PrefsStore.saveConfig(this@LoginActivity, baseUrl, apiKey, brandName)
                    setLoading(false)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                is ApiResult.Failure -> {
                    setLoading(false)
                    showError(result.message)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        loginBtn.isEnabled = !loading
        loginProgress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = android.view.View.VISIBLE
    }
}
