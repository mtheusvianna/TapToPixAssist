package com.mtheusvianna.domain.util

fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "String length must be even" } // TODO
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
