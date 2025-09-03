package com.bhavya.smsrelay

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

class ForwardService : Service() {
    companion object {
        private const val CH_ID = "sms_forwarder_service"
        private const val NOTIF_ID = 1001

        fun start(ctx: Context) {
            val i = Intent(ctx, ForwardService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun updateCounter(ctx: Context, count: Int) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                val ch = NotificationChannel(CH_ID, "SMS Relay Service", NotificationManager.IMPORTANCE_LOW)
                nm.createNotificationChannel(ch)
            }
            val pi = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notif = NotificationCompat.Builder(ctx, CH_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("SMS Relay running")
                .setContentText("$count messages forwarded")
                .setOngoing(true)
                .setContentIntent(pi)
                .build()
            nm.notify(NOTIF_ID, notif)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val sent = getSharedPreferences("cfg", 0).getInt("sentCount", 0)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CH_ID, "SMS Relay Service", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        val initial = NotificationCompat.Builder(this, CH_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SMS Relay running")
            .setContentText("$sent messages forwarded")
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ))
            .build()
        if (Build.VERSION.SDK_INT >= 26) startForeground(NOTIF_ID, initial)
        updateCounter(this, sent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?) = null
}

fun bumpCounter(context: Context) {
    val p = context.getSharedPreferences("cfg", 0)
    val n = p.getInt("sentCount", 0) + 1
    p.edit { putInt("sentCount", n) }
    ForwardService.updateCounter(context, n)
}
