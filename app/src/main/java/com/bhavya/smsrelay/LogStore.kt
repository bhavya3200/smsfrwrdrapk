package com.bhavya.smsrelay

import android.content.Context

/** Simple in-memory log storage used by HistoryFragment and SmsNotificationListener. */
data class LogEntry(
    val timestamp: Long,
    val sender: String,
    val message: String,
    val status: String
)

object LogStore {
    private val entries = mutableListOf<LogEntry>()

    /** Append a new log entry. Currently stored only in memory. */
    fun append(context: Context, entry: LogEntry) {
        entries.add(0, entry)
    }

    /** Return all stored log entries mapped to [LogItem] for UI consumption. */
    fun readAll(context: Context): List<LogItem> =
        entries.map { LogItem(it.timestamp, it.sender, it.message) }
}
