package com.mtheusvianna.taptopixassist.ui.dashboard

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtheusvianna.domain.entity.Aid
import com.mtheusvianna.domain.entity.Command
import com.mtheusvianna.domain.util.ApduConstants
import com.mtheusvianna.domain.util.ByteArrayChunkIterator
import com.mtheusvianna.domain.util.lastTwoBytesMatchesSuccessStatusWord
import com.mtheusvianna.taptopixassist.common.model.TapToPixAid
import com.mtheusvianna.taptopixassist.common.util.UriConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private var defaultAid: Aid? = null
    private var isChunked = false
    private var aidText = ""
    private var payloadText = ""
    private val _latestStatusText = MutableStateFlow<String>("")
    val latestStatusText: StateFlow<String> = _latestStatusText

    private var resetLatestStatusJob: Job? = null

    fun setDefault(aid: Aid) {
        defaultAid = aid
    }

    fun updateAidWith(newText: String) {
        aidText = newText
    }

    fun updatePayloadWith(newText: String) {
        payloadText = newText
    }

    fun updateChunkedCheckBoxValueWith(isActive: Boolean) {
        isChunked = isActive
    }

    fun updateLatestStatusWith(newText: String) {
        resetLatestStatusJob?.cancel()
        _latestStatusText.value = newText
        resetLatestStatusJob = viewModelScope.launch {
            delay(10000)
            _latestStatusText.value = ""
        }
    }

    fun handle(tag: Tag) {
        var isoDep: IsoDep? = null
        runCatching {
            isoDep = IsoDep.get(tag)
            isoDep.run {
                connect()
                val isSelectSuccessful = transceiveSelectCommand()
                if (isSelectSuccessful) transceiveUpdateBinaryCommands()
                close()
            }
        }.onFailure {
            runCatching { isoDep?.close() }
        }
    }

    private fun IsoDep.transceiveSelectCommand(): Boolean {
        val aid = if (aidText.isNotEmpty()) TapToPixAid.Unknown(aidText) else defaultAid
        checkNotNull(aid)
        val selectApdu = Command.Select.buildWith(aid).bytes
        val selectResponse = transceive(selectApdu)
        val wasSuccessful = selectResponse.lastTwoBytesMatchesSuccessStatusWord()
        updateLatestStatusWith(if (wasSuccessful) "select succeeded" else "select failed")
        return wasSuccessful
    }

    private fun IsoDep.transceiveUpdateBinaryCommands(): Boolean {
        val uri = byteArrayOf(UriConstants.IdentifierCode.NO_URI_PREFIX) + payloadText.toByteArray()
        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, null, uri)
        val ndefMessage = NdefMessage(record)
        val payload = ndefMessage.toByteArray()
        val wasSuccessful = if (isChunked) processChunked(payload, with = this) else processSingle(payload, with = this)
        val newLatestStatusPrefix = "${if (isChunked) "chunked " else ""}update binary"
        val newLatestStatus = if (wasSuccessful) "$newLatestStatusPrefix succeeded" else "$newLatestStatusPrefix failed"
        updateLatestStatusWith(newLatestStatus)
        return wasSuccessful
    }

    private fun processChunked(payload: ByteArray, with: IsoDep): Boolean {
        val chunkedIterator = ByteArrayChunkIterator(payload, ApduConstants.MAX_PAYLOAD_LENGTH_FOR_1_BYTE_LC)
        var chunkedBytes = 0
        for (chunk in chunkedIterator) {
            val isResponseSuccess = processSingle(chunk, with, andP2 = chunkedBytes.toByte())
            chunkedBytes += chunk.size
            if (!isResponseSuccess) return false
        }
        return true
    }

    private fun processSingle(payload: ByteArray, with: IsoDep, andP2: Byte? = null): Boolean {
        val updateBinaryCommand = Command.UpdateBinary.buildWith(payload, andP2).bytes
        val updateResponse = with.transceive(updateBinaryCommand)
        val isResponseSuccess = updateResponse.lastTwoBytesMatchesSuccessStatusWord()
        return isResponseSuccess
    }
}