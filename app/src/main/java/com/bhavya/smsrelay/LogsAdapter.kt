package com.bhavya.smsrelay

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bhavya.smsrelay.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object LogDiff : DiffUtil.ItemCallback<LogItem>() {
    override fun areItemsTheSame(oldItem: LogItem, newItem: LogItem): Boolean =
        oldItem.timestamp == newItem.timestamp && oldItem.sender == newItem.sender

    override fun areContentsTheSame(oldItem: LogItem, newItem: LogItem): Boolean =
        oldItem == newItem
}

class LogsAdapter : ListAdapter<LogItem, LogsAdapter.VH>(LogDiff) {

    inner class VH(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLogBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.sender.text = item.sender
        holder.binding.message.text = item.message
        holder.binding.time.text = formatTime(item.timestamp)
    }

    private fun formatTime(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
