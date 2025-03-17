package com.mtheusvianna.taptopixassist.common.util

import android.net.Uri
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.mtheusvianna.domain.entity.Command

@Throws(NullPointerException::class, FormatException::class)
fun Command.UpdateBinary.parsePayloadToNdefMessageAndGetUri(): Uri? {
    val payload = getPayload()
    val message = NdefMessage(payload)
    val record = message.records[0] // no need to check for null or array length >= 1 as per the documentation
    val isTnfWellKnown = record.tnf == NdefRecord.TNF_WELL_KNOWN
    val isRecordTypeUri = record.type.contentEquals(NdefRecord.RTD_URI)
    if (isTnfWellKnown && isRecordTypeUri) {
        val ndefPayload = record.payload
        if (ndefPayload.isEmpty()) return null
        val uriIdentifierCode = ndefPayload[0]
        if (uriIdentifierCode == UriConstants.IdentifierCode.NO_URI_PREFIX) {
            val uri = record.toUri()
            return uri
        }
    }
    return null
}
