package com.mtheusvianna.taptopixassist.service

import android.content.Intent
import android.net.Uri
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.mtheusvianna.domain.entity.Command
import com.mtheusvianna.domain.entity.StatusWord
import com.mtheusvianna.taptopixassist.common.model.TapToPixAid
import com.mtheusvianna.taptopixassist.common.util.parsePayloadToNdefMessageAndGetUri
import com.mtheusvianna.taptopixassist.presentation.R

class TapToPixService : HostApduService() {

    private var uri: Uri? = null

    override fun processCommandApdu(p0: ByteArray?, p1: Bundle?): ByteArray? {
        val command = p0?.let { Command.from(it) }
        val statusWord = handle(command)
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
            uri = updateBinary.parsePayloadToNdefMessageAndGetUri()
            StatusWord.Success
        } catch (e: Exception) {
            StatusWord.NoPreciseDiagnosis
        }
    }

    private fun broadcastUriIfReceived() {
        uri?.let {
            val intent = Intent(getString(R.string.action_tap_to_pix_received)).apply {
                putExtra(getString(R.string.extra_tap_to_pix_uri), uri.toString())
            }
            sendBroadcast(intent)
        }
    }
}
