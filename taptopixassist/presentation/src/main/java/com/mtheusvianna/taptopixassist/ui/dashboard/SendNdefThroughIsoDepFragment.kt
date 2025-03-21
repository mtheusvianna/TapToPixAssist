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
import com.mtheusvianna.domain.util.ApduConstants
import com.mtheusvianna.domain.util.ByteArrayChunkIterator
import com.mtheusvianna.domain.util.isSuccessStatusWord
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
        try {
            val isoDep = IsoDep.get(tag)
            isoDep.connect()

            val selectApdu = Command.Select.buildWith(TapToPixAid.Google(requireActivity())).bytes
            val selectResponse = isoDep.transceive(selectApdu)

            if (selectResponse.isSuccessStatusWord()) {
                dashboardViewModel.text.value?.let {
                    val uri = byteArrayOf(UriConstants.IdentifierCode.NO_URI_PREFIX) + it.toByteArray()
                    val record = NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN,
                        NdefRecord.RTD_URI,
                        null,
                        uri
                    )
                    val ndefMessage = NdefMessage(record)
                    val payload = ndefMessage.toByteArray()
                    process(payload, with = isoDep)
                }
            }

            isoDep.close()
        } catch (e: IOException) {
            e
        }
    }

    private fun process(payload: ByteArray, with: IsoDep) {
        val chunkedIterator = ByteArrayChunkIterator(payload, ApduConstants.MAX_PAYLOAD_SIZE)
        for (chunk in chunkedIterator) {
            val updateBinaryCommand = Command.UpdateBinary.buildWith(chunk).bytes
            val updateResponse = with.transceive(updateBinaryCommand)
            if (updateResponse.isSuccessStatusWord()) {
                continue
            } else {
                Toast.makeText(context, "Update failed", Toast.LENGTH_LONG).show()
                return
            }
        }
        Toast.makeText(context, "Update succeeded", Toast.LENGTH_LONG).show()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable?) { dashboardViewModel.updateTextWith(s.toString()) }
}
