package com.mtheusvianna.taptopixassist.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.mtheusvianna.domain.entity.Command
import com.mtheusvianna.domain.entity.StatusWord
import com.mtheusvianna.taptopixassist.common.model.TapToPixAid
import com.mtheusvianna.taptopixassist.common.util.parsePayloadToNdefMessageAndDecodeUri
import com.mtheusvianna.taptopixassist.presentation.R
import com.mtheusvianna.taptopixassist.ui.TapToPixAssistActivity

sealed class TapToPixService : HostApduService() {

    private var vibratorManager: VibratorManager? = null
    private var payload: String? = null

    abstract fun handle(payload: String)

    override fun onCreate() {
        super.onCreate()
        vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    }

    override fun processCommandApdu(p0: ByteArray?, p1: Bundle?): ByteArray? {
        val command = p0?.let { Command.from(it) }
        val statusWord = handle(command)

        Toast.makeText(
            this,
            "${command?.let { it::class.java.simpleName } ?: ""}: ${statusWord::class.java.simpleName}",
            Toast.LENGTH_SHORT
        ).show()

        return statusWord.bytes
    }

    override fun onDeactivated(p0: Int) {
        payload?.let { handle(payload = it) }
    }

    private fun handle(command: Command?): StatusWord {
        return when (command) {
            is Command.Select -> handle(select = command)
            is Command.UpdateBinary -> handle(updateBinary = command)
            is Command.Unknown, null -> StatusWord.UnknownCommand
        }
    }

    private fun handle(select: Command.Select): StatusWord {
        vibrate()
        val isSelectCommandWithGoogleAid = select == Command.Select.buildWith(TapToPixAid.Google(this))
        return if (isSelectCommandWithGoogleAid) StatusWord.Success else StatusWord.UnknownCommand
    }

    private fun handle(updateBinary: Command.UpdateBinary): StatusWord {
        return try {
            payload = updateBinary.parsePayloadToNdefMessageAndDecodeUri()
            StatusWord.Success
        } catch (e: Exception) {
            StatusWord.NoPreciseDiagnosis
        }
    }

    private fun vibrate() {
        vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(500, 255)) // TODO
    }
}

class ForegroundOnlyTapToPixService : TapToPixService() {
    override fun handle(payload: String) {
        val intent = Intent(getString(R.string.action_tap_to_pix_received)).apply {
            putExtra(getString(R.string.extra_tap_to_pix_payload), payload)
        }
        sendBroadcast(intent)
    }
}

class DefaultTapToPixService : TapToPixService() { // TODO
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            getNotificationChannelId(),
            "Tap To Pix",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.enableVibration(true)

        val notificationManager = getNotificationManager()
        notificationManager?.createNotificationChannel(channel)
    }

    override fun handle(payload: String) {

        val notifyIntent = Intent(this, TapToPixAssistActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(getString(R.string.extra_tap_to_pix_payload), payload)
        }

        val notifyPendingIntent = PendingIntent.getActivity(
            this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getNotificationManager()

        val notificationBuilder =
            NotificationCompat.Builder(this, getNotificationChannelId())
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.contactless_on)
                .setContentTitle("Tap To Pix detectado")
                .setContentText("Clique para pagar")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(notifyPendingIntent)

        notificationManager?.notify(getNotificationId(), notificationBuilder.build())
    }

    private fun getNotificationManager(): NotificationManager? =
        getSystemService(NOTIFICATION_SERVICE) as? NotificationManager

    private fun getNotificationChannelId() = getString(R.string.notification_channel_tap_to_pix_assist)

    private fun getNotificationId() = getNotificationChannelId().hashCode()
}
