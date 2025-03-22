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
            val headerMatchesUpToP1 = defaultHeaderUpToP1Matches(bytes)
            require(headerMatchesUpToP1)
        }

        override val header: ByteArray
            get() = bytes.copyOf(CLA_INS_P1_P2_TOTAL_SIZE)

        val payloadStartIndex: Int?
            get() {
                val firstLcByte = bytes.getOrNull(LC_START_INDEX) ?: return null
                val payloadStartIndex = if (firstLcByte == 0x00.toByte()) 7 else 5
                if (bytes.lastIndex <= payloadStartIndex) return null
                return payloadStartIndex
            }

        val payloadLength: Int
            get() {
                val firstLcByte = bytes.getOrNull(LC_START_INDEX) ?: return 0
                val length = when (firstLcByte) {
                    0x00.toByte() -> {
                        val secondLcByte = (bytes.getOrNull(5)?.toInt() ?: return 0) and 0xFF
                        val thirdLcByte = (bytes.getOrNull(6)?.toInt() ?: return 0) and 0xFF
                        (secondLcByte shl 8) + thirdLcByte
                    }

                    else -> firstLcByte.toInt() and 0xFF
                }
                return length
            }

        fun getPayload(): ByteArray? {
            val startIndex = payloadStartIndex ?: return null
            val payloadLength = payloadLength
            val hasPayload = payloadLength > 0
            val payload = if (hasPayload) {
                ByteArray(payloadLength).also {
                    val endIndex = min(startIndex + payloadLength, bytes.size)
                    // src.length=271 srcPos=5 dst.length=264 dstPos=0 length=266
                    bytes.copyInto(it, 0, startIndex, endIndex)
                }
            } else null
            return payload
        }

        companion object {
            const val LC_START_INDEX = 4

            val defaultHeader = byteArrayOf(0x00.toByte(), 0xD6.toByte(), 0x00.toByte(), 0x00.toByte())
            fun defaultHeaderUpToP1Matches(bytes: ByteArray) =
                bytes.copyOf(CLA_INS_P1_TOTAL_SIZE).contentEquals(defaultHeader.copyOf(CLA_INS_P1_TOTAL_SIZE))

            @Throws(IllegalArgumentException::class)
            fun buildWith(payload: ByteArray, p2: Byte? = null): UpdateBinary {
                val payloadLength = payload.size
                val commandHeader = p2?.let { defaultHeader.copyOf().apply { set(3, p2) } } ?: defaultHeader
                require(payloadLength <= ApduConstants.MAX_PAYLOAD_LENGTH_FOR_3_BYTE_LC)
                val isExtendedSize = payloadLength > ApduConstants.MAX_PAYLOAD_LENGTH_FOR_1_BYTE_LC
                val lcBytes = if (isExtendedSize) {
                    val firstLcByte = 0x00.toByte()
                    val secondLcByte = (payloadLength shr 8).toByte()
                    val thirdLcByte = (payloadLength and 0xFF).toByte()
                    byteArrayOf(firstLcByte, secondLcByte, thirdLcByte)
                } else {
                    byteArrayOf(payload.size.toByte())
                }
                val bytes = commandHeader + lcBytes + payload
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
        const val CLA_INS_P1_TOTAL_SIZE = 3
        const val CLA_INS_P1_P2_TOTAL_SIZE = 4

        fun from(bytes: ByteArray): Command {
            val header = bytes.copyOf(CLA_INS_P1_P2_TOTAL_SIZE)
            val command = when {
                header.contentEquals(Select.header) -> Select(bytes)
                UpdateBinary.defaultHeaderUpToP1Matches(bytes) -> UpdateBinary(bytes)
                else -> Unknown(bytes)
            }
            return command
        }
    }
}
