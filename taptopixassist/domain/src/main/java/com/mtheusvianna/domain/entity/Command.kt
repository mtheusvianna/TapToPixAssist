package com.mtheusvianna.domain.entity

import com.mtheusvianna.domain.util.ApduConstants
import kotlin.math.min

sealed class Command(override val bytes: ByteArray) : Apdu {
    init {
        val containsAtLeastClaInsP1AndP2Bytes = bytes.size >= CLA_INS_P1_P2_TOTAL_SIZE
        require(containsAtLeastClaInsP1AndP2Bytes)
    }

    abstract val header: ByteArray

    class Select internal constructor(bytes: ByteArray) : Command(bytes) {
        init {
            val headerMatches = bytes.copyOf(CLA_INS_P1_P2_TOTAL_SIZE).contentEquals(Select.header)
            require(headerMatches)
        }

        override val header: ByteArray
            get() = Select.header

        companion object {
            val header = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte())

            fun buildWith(aid: Aid): Select {
                val bytes = aid.bytes
                val length = bytes.size.toByte()
                return Select(header + byteArrayOf(length) + bytes)
            }
        }
    }

    class UpdateBinary internal constructor(bytes: ByteArray) : Command(bytes) {
        init {
            val headerMatches = bytes.copyOf(CLA_INS_P1_P2_TOTAL_SIZE).contentEquals(UpdateBinary.header)
            require(headerMatches)
        }

        override val header: ByteArray
            get() = UpdateBinary.header

        fun hasPayload(): Boolean {
            val hasPayload = bytes.size >= PAYLOAD_START_INDEX + 1
            return hasPayload
        }

        fun getPayloadLength(): Int {
            val length = if (hasPayload()) bytes[LC_BYTE_INDEX].toInt() and 0xFF else 0
            return length
        }

        fun getPayload(): ByteArray? {
            val payload = if (hasPayload()) {
                val length = getPayloadLength()
                ByteArray(length).also {
                    val endIndex = min(PAYLOAD_START_INDEX + length, bytes.size)
                    bytes.copyInto(it, 0, PAYLOAD_START_INDEX, endIndex)
                }
            } else null
            return payload
        }

        companion object {
            const val PAYLOAD_START_INDEX = 5
            const val LC_BYTE_INDEX = 4

            val header = byteArrayOf(0x00.toByte(), 0xD6.toByte(), 0x00.toByte(), 0x00.toByte())

            fun buildWith(payload: ByteArray): UpdateBinary {
                val maximumPayloadSize = ApduConstants.MAX_PAYLOAD_SIZE
                val length = payload.size
                require(length <= maximumPayloadSize)
                val bytes = header + payload.size.toByte() + payload
                return UpdateBinary(bytes)
            }
        }
    }

    class Unknown(bytes: ByteArray) : Command(bytes) {
        override val header: ByteArray
            get() = bytes.copyOf(4)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Command

        if (!bytes.contentEquals(other.bytes)) return false
        if (!header.contentEquals(other.header)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + header.contentHashCode()
        return result
    }

    companion object {
        const val CLA_INS_P1_P2_TOTAL_SIZE = 4

        fun from(bytes: ByteArray): Command {
            val header = bytes.copyOf(CLA_INS_P1_P2_TOTAL_SIZE)
            val command = when {
                header.contentEquals(Select.header) -> Select(bytes)
                header.contentEquals(UpdateBinary.header) -> UpdateBinary(bytes)
                else -> Unknown(bytes)
            }
            return command
        }
    }
}
