package com.mtheusvianna.taptopixassist.service

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.widget.Toast
import com.mtheusvianna.domain.entity.Command
import com.mtheusvianna.domain.entity.StatusWord
import com.mtheusvianna.taptopixassist.common.model.TapToPixAid
import com.mtheusvianna.taptopixassist.common.util.parsePayloadToNdefMessageAndGetDecodedUriAsString
import com.mtheusvianna.taptopixassist.presentation.R

class TapToPixService : HostApduService() {

    private var decodedUriAsString: String? = null

    override fun processCommandApdu(p0: ByteArray?, p1: Bundle?): ByteArray? {
        val command = p0?.let { Command.from(it) }
        val statusWord = handle(command)

        Toast.makeText(
            this,
            "${command?.let {it::class.java.simpleName} ?: ""}: ${statusWord::class.java.simpleName}",
            Toast.LENGTH_SHORT
        ).show()

        return statusWord.bytes
    }

    override fun onDeactivated(p0: Int) {
        broadcastUriIfReceived()
    }

    private fun handle(command: Command?): StatusWord {
        return when (command) {
            is Command.Select -> handle(select = command)
            is Command.UpdateBinary -> handle(updateBinary = command)
            is Command.Unknown, null -> StatusWord.UnknownCommand
        }
    }

    private fun handle(select: Command.Select): StatusWord {
        val isSelectCommandWithGoogleAid = select == Command.Select.buildWith(TapToPixAid.Google(this))
        return if (isSelectCommandWithGoogleAid) StatusWord.Success else StatusWord.UnknownCommand
    }

    private fun handle(updateBinary: Command.UpdateBinary): StatusWord {
        return try {
            decodedUriAsString = updateBinary.parsePayloadToNdefMessageAndGetDecodedUriAsString()
            StatusWord.Success
        } catch (e: Exception) {
            StatusWord.NoPreciseDiagnosis
        }
    }

    private fun broadcastUriIfReceived() {
        decodedUriAsString?.let {
            val intent = Intent(getString(R.string.action_tap_to_pix_received)).apply {
                putExtra(getString(R.string.extra_tap_to_pix_uri), decodedUriAsString)
            }
            sendBroadcast(intent)
        }
    }
}
