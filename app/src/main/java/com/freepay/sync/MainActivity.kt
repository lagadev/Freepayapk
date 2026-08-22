package com.freepay.sync

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: SmsLogAdapter
    private lateinit var statusText: TextView
    private lateinit var statusDot: View
    private lateinit var emptyText: TextView
    private lateinit var grantPermissionBtn: android.widget.Button

    private val requestSmsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        grantPermissionBtn.visibility = if (granted) View.GONE else View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PrefsStore.isConfigured(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        findViewById<TextView>(R.id.brandNameText).text =
            "Connected as: ${PrefsStore.getBrandName(this) ?: "Brand"}"

        findViewById<MaterialCardView>(R.id.telegramBanner).setOnClickListener {
            openTelegram()
        }

        statusText = findViewById(R.id.statusText)
        statusDot = findViewById(R.id.statusDot)
        emptyText = findViewById(R.id.emptyText)
        grantPermissionBtn = findViewById(R.id.grantPermissionBtn)

        grantPermissionBtn.setOnClickListener {
            requestSmsPermission.launch(Manifest.permission.RECEIVE_SMS)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.smsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SmsLogAdapter(emptyList())
        recyclerView.adapter = adapter

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            refreshList()
            checkServerStatus()
            swipeRefresh.isRefreshing = false
        }

        refreshList()
        checkServerStatus()
        updatePermissionState()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        updatePermissionState()
    }

    private fun updatePermissionState() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        grantPermissionBtn.visibility = if (granted) View.GONE else View.VISIBLE
    }

    private fun refreshList() {
        val items = SmsLogStore.getAll(this)
        adapter.submitList(items)
        emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun checkServerStatus() {
        val baseUrl = PrefsStore.getBaseUrl(this) ?: return
        lifecycleScope.launch {
            val online = ApiClient.ping(baseUrl)
            statusText.text = if (online) "Server: Online ✓" else "Server: Offline — নেটওয়ার্ক চেক করুন"
            statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this@MainActivity, if (online) R.color.good else R.color.bad)
            )
        }
    }

    private fun openTelegram() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=devugly")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/devugly")))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshList()
                checkServerStatus()
                true
            }
            R.id.action_logout -> {
                PrefsStore.clear(this)
                SmsLogStore.clear(this)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
