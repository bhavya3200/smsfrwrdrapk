package com.bhavya.smsrelay

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LogStore {
    private const val PREFS = "logs"
    private const val KEY = "items"
    private const val MAX_ENTRIES = 100

    fun append(ctx: Context, item: LogItem) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        val obj = JSONObject()
            .put("ts", item.timestamp)
            .put("sender", item.sender)
            .put("msg", item.message)
        arr.put(obj)
        // Keep only the last MAX_ENTRIES entries
        val finalArr = if (arr.length() > MAX_ENTRIES) {
            val trimmed = JSONArray()
            for (i in arr.length() - MAX_ENTRIES until arr.length()) {
                trimmed.put(arr.getJSONObject(i))
            }
            trimmed
        } else arr
        prefs.edit().putString(KEY, finalArr.toString()).apply()
    }

    fun readAll(ctx: Context): List<LogItem> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        val list = mutableListOf<LogItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                LogItem(
                    obj.getLong("ts"),
                    obj.getString("sender"),
                    obj.getString("msg")
                )
            )
        }
        return list
    }
}
