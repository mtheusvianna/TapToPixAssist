package com.mtheusvianna.taptopixassist.common.util

import android.net.Uri
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord

@Throws(NullPointerException::class, FormatException::class)
fun ByteArray.parseToNdefMessageAndGetUri(): Uri? {
    val message = NdefMessage(this)
    val record = message.records[0] // no need to check for null or array length >= 1 as per the documentation
    val isTnfWellKnown = record.tnf == NdefRecord.TNF_WELL_KNOWN
    val isRecordTypeUri = record.type.contentEquals(NdefRecord.RTD_URI)
    if (isTnfWellKnown && isRecordTypeUri) {
        val uri = record.toUri() // also handles the uri identifier code
        return uri
    }
    return null
}
