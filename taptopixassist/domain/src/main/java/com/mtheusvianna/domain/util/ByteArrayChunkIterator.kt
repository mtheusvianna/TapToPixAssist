package com.mtheusvianna.domain.util

class ByteArrayChunkIterator(
    private val data: ByteArray,
    private val chunkSize: Int
) : Iterator<ByteArray> {
    private var currentIndex = 0

    override fun hasNext(): Boolean = currentIndex < data.size

    override fun next(): ByteArray {
        if (!hasNext()) throw NoSuchElementException()
        val endIndex = minOf(currentIndex + chunkSize, data.size)
        val chunk = data.copyOfRange(currentIndex, endIndex)
        currentIndex = endIndex
        return chunk
    }
}
