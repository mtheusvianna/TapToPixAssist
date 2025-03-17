package com.mtheusvianna.taptopixassist.ui.dashboard

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mtheusvianna.domain.entity.Command
import com.mtheusvianna.taptopixassist.common.model.TapToPixAid
import com.mtheusvianna.taptopixassist.common.util.UriConstants
import com.mtheusvianna.taptopixassist.presentation.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

internal class SendNdefThroughIsoDepFragment : Fragment(), TextWatcher {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel: DashboardViewModel by activityViewModels()

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectTagDiscoveredEvent()
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
        binding.textDashboard.addTextChangedListener(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    }

    override fun onResume() {
        super.onResume()
        pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, requireActivity()::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
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

    private fun collectTagDiscoveredEvent() {
        lifecycleScope.launch {
            dashboardViewModel.tagDiscoveredEvent.flowWithLifecycle(lifecycle).collectLatest { tag -> handle(tag) }
        }
    }

    fun handle(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        try {
            isoDep.connect()

            val selectApdu = Command.Select.buildWith(TapToPixAid.Google(requireActivity())).bytes
            val selectResponse = isoDep.transceive(selectApdu)

            fun ByteArray.isSuccess() =
                size >= 2 && selectResponse[size - 2] == 0x90.toByte() && selectResponse[size - 1] == 0x00.toByte()

            if (selectResponse.isSuccess()) {
                dashboardViewModel.text.value?.let {
                    val uri = byteArrayOf(UriConstants.IdentifierCode.NO_URI_PREFIX) + it.toByteArray()
                    val record = NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN,
                        NdefRecord.RTD_URI,
                        ByteArray(0),
                        uri
                    )
                    val ndefMessage = NdefMessage(record)
                    val updateBinaryCommand = Command.UpdateBinary.buildWith(ndefMessage.toByteArray()).bytes
                    val updateResponse = isoDep.transceive(updateBinaryCommand)
                    if (updateResponse.isSuccess()) {
                        Toast.makeText(context, "Update succeeded", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Update failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: IOException) {
            e
        } finally {
            isoDep.close()
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable?) {
        dashboardViewModel.updateTextWith(s.toString())
    }
}