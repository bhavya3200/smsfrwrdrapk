package com.bhavya.smsrelay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bhavya.smsrelay.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(inflater, container, false)
        val p = requireContext().getSharedPreferences("cfg", 0)

        b.cbViaTelegram.isChecked = p.getBoolean("viaTelegram", false)
        b.etBotToken.setText(p.getString("botToken",""))
        b.etChatId.setText(p.getString("chatId",""))
        b.cbViaWa.isChecked = p.getBoolean("viaWa", false)
        b.etWaPhoneId.setText(p.getString("waPhoneNumberId",""))
        b.etWaToken.setText(p.getString("waToken",""))
        b.etWaTo.setText(p.getString("waTo",""))

        b.btnSaveSettings.setOnClickListener {
            p.edit()
                .putBoolean("viaTelegram", b.cbViaTelegram.isChecked)
                .putString("botToken", b.etBotToken.text.toString().trim())
                .putString("chatId", b.etChatId.text.toString().trim())
                .putBoolean("viaWa", b.cbViaWa.isChecked)
                .putString("waPhoneNumberId", b.etWaPhoneId.text.toString().trim())
                .putString("waToken", b.etWaToken.text.toString().trim())
                .putString("waTo", b.etWaTo.text.toString().trim())
                .apply()
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }
        return b.root
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
