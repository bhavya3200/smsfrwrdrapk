package com.bhavya.smsrelay


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.bhavya.smsrelay.databinding.FragmentFiltersBinding
import java.time.ZoneId
import java.util.TimeZone

class FiltersFragment : Fragment() {
    private var _b: FragmentFiltersBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentFiltersBinding.inflate(inflater, container, false)

        val p = requireContext().getSharedPreferences("cfg", 0)
        b.cbEnabled.isChecked = p.getBoolean("enabled", false)
        b.cbSkipOtp.isChecked = p.getBoolean("skipOtp", true)
        val defaultTz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ZoneId.systemDefault().id
        } else {
            TimeZone.getDefault().id
        }
        b.etTimezone.setText(p.getString("timezone", defaultTz))
        b.etWhitelist.setText(p.getString("whitelistSenders", ""))
        b.etBlacklist.setText(p.getString("blacklistSenders", ""))
        b.etOnlyKeywords.setText(p.getString("onlyKeywords", ""))
        b.etNeverKeywords.setText(p.getString("neverKeywords", ""))

        b.btnGrantNotifAccess.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            Toast.makeText(requireContext(),"Enable notification access for SMS Relay", Toast.LENGTH_LONG).show()
        }

        b.btnSave.setOnClickListener {
            p.edit {
                putBoolean("enabled", b.cbEnabled.isChecked)
                putBoolean("skipOtp", b.cbSkipOtp.isChecked)
                putString("timezone", b.etTimezone.text.toString().trim())
                putString("whitelistSenders", b.etWhitelist.text.toString())
                putString("blacklistSenders", b.etBlacklist.text.toString())
                putString("onlyKeywords", b.etOnlyKeywords.text.toString())
                putString("neverKeywords", b.etNeverKeywords.text.toString())
            }
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }
        return b.root
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

