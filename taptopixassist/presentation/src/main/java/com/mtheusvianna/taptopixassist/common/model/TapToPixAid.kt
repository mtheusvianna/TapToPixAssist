package com.mtheusvianna.taptopixassist.common.model

import android.content.Context
import com.mtheusvianna.domain.entity.Aid
import com.mtheusvianna.domain.util.decodeHex
import com.mtheusvianna.taptopixassist.presentation.R

sealed class TapToPixAid : Aid() {

    class Google(context: Context) : TapToPixAid() {
        override val hex: String = context.getString(R.string.tap_and_pix_google_aid)
        override val bytes: ByteArray = hex.decodeHex()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TapToPixAid

        return hex == other.hex
    }

    override fun hashCode(): Int {
        return hex.hashCode()
    }
}
