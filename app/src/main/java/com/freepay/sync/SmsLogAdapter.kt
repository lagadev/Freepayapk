package com.freepay.sync

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class SmsLogAdapter(private var items: List<SmsLogEntry>) :
    RecyclerView.Adapter<SmsLogAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)

    fun submitList(newItems: List<SmsLogEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val methodBadge: TextView = view.findViewById(R.id.methodBadge)
        val trxIdText: TextView = view.findViewById(R.id.trxIdText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val amountText: TextView = view.findViewById(R.id.amountText)
        val statusBadge: TextView = view.findViewById(R.id.statusBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sms_log, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        val methodColor = when (item.method) {
            "bkash" -> Color.parseColor("#E2136E")
            "nagad" -> Color.parseColor("#F6871F")
            "upay" -> Color.parseColor("#7A2E8C")
            "rocket" -> Color.parseColor("#8C3494")
            "cellfin" -> Color.parseColor("#00A651")
            else -> Color.parseColor("#6A5CF5")
        }
        holder.methodBadge.text = item.method.take(2).uppercase()
        holder.methodBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(methodColor)

        holder.trxIdText.text = item.trxId
        holder.dateText.text = dateFormat.format(java.util.Date(item.receivedAt))
        holder.amountText.text = "৳${"%,.2f".format(item.amount)}"

        val (label, bgColor, textColor) = when (item.status) {
            SyncStatus.PENDING -> Triple("PENDING", ContextCompat.getColor(ctx, R.color.pending_bg), ContextCompat.getColor(ctx, R.color.pending_text))
            SyncStatus.SYNCED -> Triple("SYNCED", ContextCompat.getColor(ctx, R.color.good_bg), ContextCompat.getColor(ctx, R.color.good))
            SyncStatus.MATCHED -> Triple("MATCHED", ContextCompat.getColor(ctx, R.color.good_bg), ContextCompat.getColor(ctx, R.color.good))
            SyncStatus.FAILED -> Triple("FAILED", ContextCompat.getColor(ctx, R.color.bad_bg), ContextCompat.getColor(ctx, R.color.bad))
        }
        holder.statusBadge.text = label
        holder.statusBadge.setTextColor(textColor)
        holder.statusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(bgColor)
    }
}
