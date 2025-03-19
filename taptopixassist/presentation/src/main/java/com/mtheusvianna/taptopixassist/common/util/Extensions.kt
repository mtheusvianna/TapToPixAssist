package com.mtheusvianna.taptopixassist.common.util

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.mtheusvianna.domain.entity.Command
import java.net.URLDecoder

@Throws(NullPointerException::class, FormatException::class)
fun Command.UpdateBinary.parsePayloadToNdefMessageAndDecodeUri(): String? {
    val payload = getPayload()
    val message = NdefMessage(payload)
    val record = message.records[0] // no need to check for null or array length >= 1 as per the documentation
    val isTnfWellKnown = record.tnf == NdefRecord.TNF_WELL_KNOWN
    val isRecordTypeUri = record.type.contentEquals(NdefRecord.RTD_URI)
    if (isTnfWellKnown && isRecordTypeUri) {
        val uri = record.toUri() // also handles the uri identifier code
        return URLDecoder.decode(uri.toString(), Charsets.UTF_8)
    }
    return null
}
