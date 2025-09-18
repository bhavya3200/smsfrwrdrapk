package com.bhavya.smsrelay

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class SmsNotificationListener : NotificationListenerService() {

    private val client = OkHttpClient()

    private val smsPackages = setOf(
        "com.google.android.apps.messaging",
        "com.android.messaging",
        "com.samsung.android.messaging",
        "com.miui.mms",
        "com.coloros.mms",
        "com.huawei.messenger"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        if (!smsPackages.contains(sbn.packageName)) return

        val extras = sbn.notification.extras
        val title = (extras.getCharSequence("android.title") ?: "").toString()
        val bodyText = listOf("android.bigText", "android.text")
            .mapNotNull { key -> extras.getCharSequence(key)?.toString() }
            .firstOrNull { it.isNotBlank() } ?: ""

        if (title.isBlank() || bodyText.isBlank()) return

        // Filters
        val whitelist = parseList(prefs.getString("whitelistSenders", ""))
        val blacklist = parseList(prefs.getString("blacklistSenders", ""))
        val onlyIfKw  = parseList(prefs.getString("onlyKeywords", ""))
        val neverKw   = parseList(prefs.getString("neverKeywords", ""))
        val skipOtp = prefs.getBoolean("skipOtp", true)

        if (matchesSender(title, blacklist)) return
        if (whitelist.isNotEmpty() && !matchesSender(title, whitelist)) return
        if (matchesKeyword(bodyText, neverKw)) return
        if (onlyIfKw.isNotEmpty() && !matchesKeyword(bodyText, onlyIfKw)) return
        if (skipOtp && looksLikeOtp(bodyText)) return

        val zoneId = runCatching {
            ZoneId.of(prefs.getString("timezone", ZoneId.systemDefault().id)!!)
        }.getOrElse { ZoneId.systemDefault() }
        val ts = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(zoneId).format(Instant.now())

        val text = "[FWD @ $ts] From: $title\n$bodyText"

        val viaTelegram = prefs.getBoolean("viaTelegram", false)
        val viaWa = prefs.getBoolean("viaWa", false)

        if (viaTelegram) {
            val token = prefs.getString("botToken","")!!.trim()
            val chatId = prefs.getString("chatId","")!!.trim()
            if (token.isNotEmpty() && chatId.isNotEmpty()) {
                sendToTelegram(token, chatId, text) { success ->
                    if (success) {
                        bumpCounter(this)
                    } else {
                        Log.e("SmsRelay", "Failed to forward via Telegram")
                    }
                }
            }
        }
        if (viaWa) {
            val phoneId = prefs.getString("waPhoneNumberId","")!!.trim()
            val waToken = prefs.getString("waToken","")!!.trim()
            val waTo = prefs.getString("waTo","")!!.trim()
            if (phoneId.isNotEmpty() && waToken.isNotEmpty() && waTo.isNotEmpty()) {
                sendToWhatsApp(phoneId, waToken, waTo, text) { success ->
                    if (success) {
                        bumpCounter(this)
                    } else {
                        Log.e("SmsRelay", "Failed to forward via WhatsApp")
                    }
                }
            }
        }

        LogStore.append(this, LogEntry(System.currentTimeMillis(), title, bodyText, "forwarded"))
    }

    private fun parseList(raw: String?): List<String> =
        raw?.split('\n', ',', ';')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    private fun normalizeNumber(s: String): String {
        val digits = s.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }
    private fun matchesSender(senderRaw: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) return false
        val sender = senderRaw.trim()
        val senderDigits = normalizeNumber(sender)
        val up = sender.uppercase()
        for (p in patterns) {
            val isRegex = p.startsWith("re:", true)
            val pat = if (isRegex) p.substring(3) else p
            if (isRegex) {
                runCatching { if (Regex(pat, RegexOption.IGNORE_CASE).containsMatchIn(sender)) return true }.getOrNull()
            } else {
                val pDigits = normalizeNumber(p)
                if (pDigits.isNotEmpty() && senderDigits.isNotEmpty()) {
                    if (senderDigits == pDigits || senderDigits.endsWith(pDigits)) return true
                }
                if (up.contains(p.uppercase())) return true
            }
        }
        return false
    }
    private fun matchesKeyword(textRaw: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) return false
        val up = textRaw.uppercase()
        for (p in patterns) {
            val isRegex = p.startsWith("re:", true)
            val pat = if (isRegex) p.substring(3) else p
            if (isRegex) {
                runCatching { if (Regex(pat, RegexOption.IGNORE_CASE).containsMatchIn(textRaw)) return true }.getOrNull()
            } else if (up.contains(p.uppercase())) return true
        }
        return false
    }
    private fun looksLikeOtp(s: String): Boolean {
        val up = s.uppercase()
        if (!up.contains("OTP") && !up.contains("ONE TIME") && !up.contains("VERIFY")) return false
        return Regex("""\b\d{4,8}\b""").find(s) != null
    }

    private fun sendToTelegram(token: String, chatId: String, text: String, onResult: (Boolean) -> Unit) {
        val url = "https://api.telegram.org/bot$token/sendMessage"
        val form = FormBody.Builder().add("chat_id", chatId).add("text", text).build()
        val request = Request.Builder().url(url).post(form).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                onResult(response.isSuccessful)
            }
        })
    }

    private fun sendToWhatsApp(phoneNumberId: String, token: String, to: String, text: String, onResult: (Boolean) -> Unit) {
        val url = "https://graph.facebook.com/v20.0/$phoneNumberId/messages"
        val payload = JSONObject()
            .put("messaging_product", "whatsapp")
            .put("to", to)
            .put("type", "text")
            .put("text", JSONObject().put("body", text))
            .toString()
        val body = payload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                onResult(response.isSuccessful)
            }
        })
    }
}
