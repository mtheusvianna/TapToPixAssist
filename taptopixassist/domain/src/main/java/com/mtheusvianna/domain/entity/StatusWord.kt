package com.mtheusvianna.domain.entity

sealed class StatusWord : Apdu {
    object Success : StatusWord() {
        override val bytes: ByteArray
            get() = byteArrayOf(0x90.toByte(), 0x00.toByte())
    }

    object NoPreciseDiagnosis : StatusWord() {
        override val bytes: ByteArray
            get() = byteArrayOf(0x6F.toByte(), 0x00.toByte())
    }

    object UnknownCommand : StatusWord() {
        override val bytes: ByteArray
            get() = byteArrayOf(0x00.toByte(), 0x00.toByte())
    }
}
