package com.bhavya.smsrelay

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bhavya.smsrelay.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Tabs
        b.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int) = when (position) {
                0 -> HistoryFragment()
                1 -> FiltersFragment()
                else -> SettingsFragment()
            }
        }
        TabLayoutMediator(b.tabLayout, b.viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> getString(R.string.tab_history)
                1 -> getString(R.string.tab_filters)
                else -> getString(R.string.tab_settings)
            }
        }.attach()

        // Android 13+ notifications permission
        ensurePostNotificationsPermission()

        // Start foreground service for status counter (safe even if access not yet enabled)
        ForwardService.start(this)
    }

    override fun onResume() {
        super.onResume()
        if (!isNotifListenerEnabled()) promptEnableNotifListener()
    }

    // ===== Helpers =====
    private fun ensurePostNotificationsPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(perm), 1001)
            }
        }
    }

    private fun isNotifListenerEnabled(): Boolean {
        val cn = ComponentName(this, SmsNotificationListener::class.java)
        val flat = android.provider.Settings.Secure
            .getString(contentResolver, "enabled_notification_listeners") ?: ""
        return flat.split(":").any { it.contains(cn.flattenToString(), true) }
    }

    private fun promptEnableNotifListener() {
        AlertDialog.Builder(this)
            .setTitle("Enable Notification Access")
            .setMessage("To forward SMS from notifications, please enable notification access for SMS Relay.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
