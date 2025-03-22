package com.mtheusvianna.taptopixassist.ui.dashboard

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mtheusvianna.taptopixassist.presentation.R
import com.mtheusvianna.taptopixassist.presentation.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class SendNdefThroughIsoDepFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel: DashboardViewModel by activityViewModels()

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dashboardViewModel.updateChunkedCheckBoxValueWith(binding.chunkedCheckBox.isChecked)
        binding.chunkedCheckBox.setOnCheckedChangeListener { _, isActive ->
            dashboardViewModel.updateChunkedCheckBoxValueWith(isActive)
        }
        binding.aidTextDashboard.doAfterTextChanged { editable ->
            val text = editable.toString()
            dashboardViewModel.updateAidWith(text)
            binding.aidTextCharCount.text = getString(R.string.edit_text_char_count, text.length, 255) // TODO
        }
        binding.payloadTextDashboard.doAfterTextChanged { editable ->
            val text = editable.toString()
            dashboardViewModel.updatePayloadWith(text)
            binding.payloadTextCharCount.text = getString(R.string.edit_text_char_count, text.length, 1024) // TODO
        }

        lifecycleScope.launch {
            dashboardViewModel.latestStatusText.flowWithLifecycle(lifecycle).collectLatest { value ->
                binding.latestStatusTextDashboard.text = value
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(context, requireActivity()::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(
            requireActivity(),
            pendingIntent,
            arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)),
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
