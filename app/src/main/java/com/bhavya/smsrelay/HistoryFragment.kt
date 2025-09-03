package com.bhavya.smsrelay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhavya.smsrelay.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    // Initialize when view is created
    private lateinit var adapter: LogsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LogsAdapter()
        binding.recyclerView.adapter = adapter

        val logs: List<LogItem> = loadLogsFromDbOrPrefs() // your source
        adapter.submitList(logs)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this screen
        val items = LogStore.readAll(requireContext())
        adapter.submitList(items) // or adapter.submit(items) if that's your API
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
