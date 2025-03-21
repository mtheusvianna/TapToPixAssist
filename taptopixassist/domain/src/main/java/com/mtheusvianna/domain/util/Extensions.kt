package com.mtheusvianna.domain.util

import com.mtheusvianna.domain.entity.StatusWord

fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "String length must be even" } // TODO
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun ByteArray.lastTwoBytesMatchesSuccessStatusWord() =
    size >= 2 && byteArrayOf(
        getOrNull(size - 2) ?: 0x00,
        getOrNull(size - 1) ?: 0x00
    ).contentEquals(StatusWord.Success.bytes)
